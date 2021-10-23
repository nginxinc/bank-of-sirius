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

import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import sirius.samples.bankofsirius.ledger.Transaction;
import sirius.samples.bankofsirius.security.AuthenticationException;
import sirius.samples.bankofsirius.security.Authenticator;

import java.util.Deque;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class TransactionHistoryControllerTest {

    private TransactionHistoryController transactionHistoryController;
    @Mock
    private Authenticator authenticator;
    @Mock
    private LoadingCache<String, Deque<Transaction>> cache;
    @Mock
    private CacheStats stats;
    @Mock
    private Tracer tracer;
    @Mock
    Span.Builder spanBuilder;
    @Mock
    private Deque<Transaction> transactions;

    private static final MeterRegistry METER_REGISTRY = null;
    private static final String JWT_ACCOUNT_KEY = "acct";
    private static final String AUTHED_ACCOUNT_NUM = "1234567890";
    private static final String NON_AUTHED_ACCOUNT_NUM = "9876543210";
    private static final String BEARER_TOKEN = "Bearer abc";
    private static final String TOKEN = "abc";
    private static final Integer EXTRA_LATENCY_MILLIS = 0;

    @BeforeEach
    void setUp() {
        initMocks(this);

        when(cache.stats()).thenReturn(stats);
        transactionHistoryController = new TransactionHistoryController(authenticator, METER_REGISTRY, cache, tracer, EXTRA_LATENCY_MILLIS);
        doThrow(AuthenticationException.class).when(authenticator)
                .verify(anyString(), anyString());
        doNothing().when(authenticator).verify(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);
        when(tracer.spanBuilder()).thenReturn(spanBuilder);
        when(spanBuilder.name(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(mock(Span.class));
    }

    @Test
    @DisplayName("Given the user is authenticated for the account, return HTTP Status 200")
    void getTransactionsSucceedsWhenAccountMatchesAuthenticatedUser() throws Exception {
        // Given
        when(cache.get(AUTHED_ACCOUNT_NUM)).thenReturn(transactions);

        // When
        final ResponseEntity<?> actualResult = transactionHistoryController
            .getTransactions(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.OK, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the user is authenticated but cannot access the account, return 401")
    void getTransactionsFailsWhenAccountDoesNotMatchAuthenticatedUser() {
        // Given

        // When
        final ResponseEntity<?> actualResult = transactionHistoryController.getTransactions(BEARER_TOKEN, NON_AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the user is not authenticated, return 401")
    void getTransactionsFailsWhenUserNotAuthenticated() {
        // Given
        final String badBearerToken = "Bearer foobar";

        // When
        final ResponseEntity<?> actualResult = transactionHistoryController.getTransactions(badBearerToken, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the cache throws an error for an authenticated user, return 500")
    void getTransactionsFailsWhenCacheThrowsError() throws Exception {
        // Given
        when(cache.get(AUTHED_ACCOUNT_NUM)).thenThrow(ExecutionException.class);

        // When
        final ResponseEntity<?> actualResult = transactionHistoryController
            .getTransactions(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, actualResult.getStatusCode());
    }
}
