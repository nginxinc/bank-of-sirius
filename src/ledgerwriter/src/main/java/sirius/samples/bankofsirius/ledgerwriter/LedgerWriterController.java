/*
 * Copyright 2020, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sirius.samples.bankofsirius.ledgerwriter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestOperations;
import sirius.samples.bankofsirius.ledger.Transaction;
import sirius.samples.bankofsirius.ledger.TransactionRepository;
import sirius.samples.bankofsirius.ledger.TransactionValidationException;
import sirius.samples.bankofsirius.ledger.TransactionValidator;
import sirius.samples.bankofsirius.security.AuthenticationException;
import sirius.samples.bankofsirius.security.Authenticator;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static sirius.samples.bankofsirius.ledger.ExceptionMessages.EXCEPTION_MESSAGE_INSUFFICIENT_BALANCE;

@RestController
public final class LedgerWriterController {

    private static final Logger LOGGER =
        LogManager.getLogger(LedgerWriterController.class);

    public static final String READINESS_CODE = "ok";
    public static final String UNAUTHORIZED_CODE = "not authorized";

    private final TransactionRepository transactionRepository;
    private final TransactionValidator transactionValidator;
    private final Authenticator authenticator;
    private final String localRoutingNum;
    private final String balancesApiUri;
    private final Cache<String, Long> cache;
    private final RestOperations restOperations;
    private final Tracer tracer;

    /**
    * Constructor.
    *
    * Initializes JWT verifier.
    */
    @Autowired
    @SuppressWarnings("checkstyle:ParameterNumber")
    public LedgerWriterController(
            final Authenticator authenticator,
            final MeterRegistry meterRegistry,
            final TransactionRepository transactionRepository,
            final TransactionValidator transactionValidator,
            @Value("${LOCAL_ROUTING_NUM}") final String localRoutingNum,
            @Value("http://${BALANCES_API_ADDR}/balances") final String balancesApiUri,
            final RestOperations restOperations,
            final Tracer tracer) {

        this.authenticator = authenticator;
        this.transactionRepository = transactionRepository;
        this.transactionValidator = transactionValidator;
        this.localRoutingNum = localRoutingNum;
        this.balancesApiUri = balancesApiUri;
        this.restOperations = restOperations;
        this.tracer = tracer;

        // Initialize cache to ignore duplicate transactions
        this.cache = CacheBuilder.newBuilder()
                            .recordStats()
                            .expireAfterWrite(1, TimeUnit.HOURS)
                            .build();

        if (meterRegistry != null) {
            GuavaCacheMetrics.monitor(meterRegistry, this.cache, "Guava");
        }
    }

    /**
     * Submit a new transaction to the ledger.
     *
     * @param authorization  HTTP request 'Authorization' header
     * @param transaction  transaction to submit
     *
     * @return  HTTP Status 200 if transaction was successfully submitted
     */
    @PostMapping(value = "/transactions", consumes = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> addTransaction(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Transaction transaction) {
        if (authorization == null || authorization.isEmpty()) {
            return new ResponseEntity<>("HTTP request 'Authorization' header is null",
                    HttpStatus.BAD_REQUEST);
        }
        final String accountId = transaction.getFromAccountNum();

        try {
            authenticator.verify(authorization, accountId);
        } catch (AuthenticationException e) {
            LOGGER.error("Authentication failed: {}", e.getMessage());
            return new ResponseEntity<>("not authorized",
                    HttpStatus.UNAUTHORIZED);
        }


        final Span span = tracer.currentSpan();
        Objects.requireNonNull(span);
        span.tag("transaction.id", Long.toString(transaction.getTransactionId()));

        // Check against cache for duplicate transactions
        final Span cacheSpan = tracer.spanBuilder().name("cache_lookup").start();
        try {
            if (this.cache.asMap().containsKey(transaction.getRequestUuid())) {
                LOGGER.error("Duplicate transaction add attempted [requestUuid={}]",
                        transaction.getRequestUuid());
                cacheSpan.tag("error", "true");
                cacheSpan.tag("request.uuid", transaction.getRequestUuid());
                cacheSpan.tag("transaction", transaction.toString());
                cacheSpan.event("Duplicate transaction add attempted");
                return new ResponseEntity<>("unable to add duplicate transaction",
                        HttpStatus.BAD_REQUEST);
            }
        } finally {
            cacheSpan.end();
        }

        // Validate transaction
        try {
            transactionValidator.validateTransaction(localRoutingNum, accountId, transaction);
        } catch (NullPointerException e) {
            String msg = String.format("Invalid transaction properties [transaction=%s]",
                    transaction);
            LOGGER.error(msg);
            return new ResponseEntity<>("invalid transaction", HttpStatus.BAD_REQUEST);
        } catch (TransactionValidationException e) {
            String msg = String.format("Transaction failed validation check [transaction=%s]",
                    transaction);
            LOGGER.error(msg);
            return new ResponseEntity<>(e.getHttpErrorMessage(), HttpStatus.BAD_REQUEST);
        }

        // Ensure sender balance can cover transaction.
        try {
            if (localRoutingNum.equals(transaction.getFromRoutingNum())) {
                int balance = getAvailableBalance(authorization, transaction.getFromAccountNum());
                if (balance < transaction.getAmount()) {
                    String msg = String.format("Transaction submission failed: Insufficient balance [balance=%d]",
                            balance);
                    LOGGER.error(msg);
                    span.tag("error", "true");
                    span.tag("transaction", transaction.toString());
                    span.tag("account.balance", Integer.toString(balance));
                    span.tag("account.id", transaction.getFromAccountNum());
                    span.event(msg);
                    return new ResponseEntity<>(EXCEPTION_MESSAGE_INSUFFICIENT_BALANCE,
                            HttpStatus.BAD_REQUEST);
                }
            }
        } catch (ReadAvailableBalanceException e) {
            final ResponseEntity<String> response;
            if (e.isCauseFromRemoteAccessFailure()) {
                response = new ResponseEntity<>("unable to read available balance",
                        HttpStatus.INTERNAL_SERVER_ERROR);
                LOGGER.error("Failed to retrieve account balance", e);
            } else {
                response = new ResponseEntity<>("remote resource unavailable",
                        HttpStatus.SERVICE_UNAVAILABLE);
                LOGGER.error(e.getMessage());
            }

            span.error(e);
            return response;
        }

        // Add to ledger
        try {
            transactionRepository.save(transaction);
            this.cache.put(transaction.getRequestUuid(),
                    transaction.getTransactionId());
            LOGGER.info("Submitted transaction successfully");
        } catch (RuntimeException e) {
            String msg = String.format("Unable to persist transaction [transaction=%s]",
                    transaction);

            if (e instanceof ReadAvailableBalanceException) {
                LOGGER.error(msg);
            } else {
                LOGGER.error(msg, e);
            }

            return new ResponseEntity<>("unable to persist transaction",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // If we made it this far, there have been no problems, so we let the
        // user know it was created successfully.
        return new ResponseEntity<>(READINESS_CODE, HttpStatus.CREATED);
    }

    /**
     * Retrieve the balance for the transaction's sender.
     *
     * @param token  the token used to authenticate request
     * @param fromAcct  sender account number
     *
     * @return available balance of the sender account
     *
     * @throws HttpServerErrorException  if balance service returns 500
     */
    protected int getAvailableBalance(String token, String fromAcct)
            throws ReadAvailableBalanceException {
        LOGGER.debug("Retrieving balance for transaction sender");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String uri = balancesApiUri + "/" + fromAcct;

        try {
            final ResponseEntity<Integer> response = restOperations.exchange(
                    uri, HttpMethod.GET, entity, Integer.class);
            final Integer senderBalance = response.getBody();
            if (senderBalance == null) {
                final String msg = String.format("Null response for getBalance from remote api [uri=%s]", uri);
                throw new ReadAvailableBalanceException(msg);
            }

            return senderBalance;
        } catch (IllegalArgumentException e) {
            final String msg = String.format("Invalid URI for getBalance from remote api [uri=%s]", uri);
            throw new ReadAvailableBalanceException(msg, e);
        } catch (RuntimeException e) {
            final String msg = String.format("Unable to read balance from remote api [uri=%s]", uri);
            throw new ReadAvailableBalanceException(msg, e);
        }
    }
}
