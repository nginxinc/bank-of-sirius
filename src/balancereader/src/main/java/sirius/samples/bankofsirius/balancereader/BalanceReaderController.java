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

package sirius.samples.bankofsirius.balancereader;

import com.google.common.cache.LoadingCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import sirius.samples.bankofsirius.security.AuthenticationException;
import sirius.samples.bankofsirius.security.Authenticator;

import java.util.concurrent.ExecutionException;

/**
 * REST service to retrieve the current balance for the authenticated user.
 */
@RestController
public final class BalanceReaderController {
    private static final Logger LOGGER =
        LogManager.getLogger(BalanceReaderController.class);

    private final Tracer tracer;
    private final Authenticator authenticator;
    private final LoadingCache<String, Long> cache;

    /**
     * Constructor.
     *
     * Initializes JWT verifier and a connection to the bank ledger.
     */
    @Autowired
    public BalanceReaderController(final Authenticator authenticator,
                                   final MeterRegistry meterRegistry,
                                   final LoadingCache<String, Long> cache,
                                   final Tracer tracer) {
        this.authenticator = authenticator;
        // Initialize cache
        this.cache = cache;
        LOGGER.debug("Initialized cache");
        this.tracer = tracer;

        if (meterRegistry != null) {
            GuavaCacheMetrics.monitor(meterRegistry, this.cache, "Guava");
        }
    }

    /**
     * Return the balance for the specified account.
     *
     * The currently authenticated user must be allowed to access the account.
     *
     * @param authorization  HTTP request 'Authorization' header
     * @param accountId    the account to get the balance for
     * @return             the balance of the account
     */
    @GetMapping("/balances/{accountId}")
    public ResponseEntity<?> getBalance(
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

        final Span span = tracer.spanBuilder().name("cache_lookup").start();
        try {
            // Load from cache
            final Long balance = cache.get(accountId);
            return new ResponseEntity<>(balance, HttpStatus.OK);
        } catch (ExecutionException | RuntimeException e) {
            final String msg;
            final ResponseEntity<String> response;
            final Throwable errorToLog;

            if (e.getCause() instanceof DataAccessResourceFailureException) {
                msg = e.getCause().getMessage();
                errorToLog = e;
                response = new ResponseEntity<>("unable to load data into cache", HttpStatus.SERVICE_UNAVAILABLE);
            } else {
                msg = String.format("Unable to read balance from cache [accountId=%s]", accountId);
                response = new ResponseEntity<>("cache error",
                        HttpStatus.INTERNAL_SERVER_ERROR);
                errorToLog = e;
            }

            span.error(errorToLog);
            LOGGER.error(msg, errorToLog);

            return response;
        } finally {
            span.end();
        }
    }
}
