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

package sirius.samples.bankofsirius.transactionhistory;

import com.google.common.cache.LoadingCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import sirius.samples.bankofsirius.ledger.Transaction;
import sirius.samples.bankofsirius.security.AuthenticationException;
import sirius.samples.bankofsirius.security.Authenticator;

import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ExecutionException;

/**
 * Controller for the TransactionHistory service.
 *
 * Functions to show the transaction history for each user account.
 */
@RestController
public final class TransactionHistoryController {
    private static final Logger LOGGER =
        LogManager.getLogger(TransactionHistoryController.class);

    private final Integer extraLatencyMillis;

    private final Tracer tracer;
    private final Authenticator authenticator;
    private final LoadingCache<String, Deque<Transaction>> cache;

    /**
     * Constructor.
     *
     * Initializes JWT verifier and a connection to the bank ledger.
     */
    @Autowired
    public TransactionHistoryController(final Authenticator authenticator,
                                        final MeterRegistry meterRegistry,
                                        final LoadingCache<String, Deque<Transaction>> cache,
                                        final Tracer tracer,
                                        @Value("${EXTRA_LATENCY_MILLIS:#{null}}") final Integer extraLatencyMillis) {
        this.authenticator = authenticator;
        this.cache = cache;
        this.extraLatencyMillis = extraLatencyMillis;
        this.tracer = tracer;

        if (meterRegistry != null) {
            GuavaCacheMetrics.monitor(meterRegistry, this.cache, "Guava");
        }

        // Initialize transaction processor.
        LOGGER.debug("Initialized transaction processor");
    }

    /**
     * Return a list of transactions for the specified account.
     *
     * The currently authenticated user must be allowed to access the account.
     * @param authorization  HTTP request 'Authorization' header
     * @param accountId    the account to get transactions for.
     * @return             a list of transactions for this account.
     */
    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<?> getTransactions(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String accountId) {
        if (authorization == null || authorization.isEmpty()) {
            return new ResponseEntity<>("HTTP request 'Authorization' header is null",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            authenticator.verify(authorization, accountId);
        } catch (AuthenticationException e) {
            LOGGER.error("Authentication failed: {}", e.getMessage());
            return new ResponseEntity<>("not authorized",
                    HttpStatus.UNAUTHORIZED);
        }

        LOGGER.debug("Transaction history requested [accountId={}]", accountId);

        final Span span = tracer.spanBuilder().name("cache_lookup").start();
        cache.refresh(accountId);

        try {
            // Load from cache
            Deque<Transaction> historyList = cache.get(accountId);

            if (extraLatencyMillis != null && extraLatencyMillis > 0) {
                // Set artificial extra latency.
                LOGGER.debug("Setting artificial latency [extraLatencyMillis={}]",
                        extraLatencyMillis);
                try {
                    Thread.sleep(extraLatencyMillis);
                } catch (InterruptedException e) {
                    // Fake latency interrupted. Continue.
                }
            }

            return new ResponseEntity<Collection<Transaction>>(
                    historyList, HttpStatus.OK);
        } catch (ExecutionException | RuntimeException e) {
            final ResponseEntity<String> response;

            if (e.getCause() instanceof DataAccessResourceFailureException) {
                response = new ResponseEntity<>("unable to load data into cache",
                        HttpStatus.SERVICE_UNAVAILABLE);
            } else {
                response = new ResponseEntity<>("cache error",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String msg = String.format("Unable to read transactions from cache [accountId=%s]",
                    accountId);
            span.error(e);
            LOGGER.error(msg, e);
            return response;
        } finally {
            span.end();
        }
    }
}
