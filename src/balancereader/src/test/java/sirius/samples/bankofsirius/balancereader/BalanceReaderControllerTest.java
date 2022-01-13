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

import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import sirius.samples.bankofsirius.security.AuthenticationException;
import sirius.samples.bankofsirius.security.Authenticator;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class BalanceReaderControllerTest {
    private BalanceReaderController balanceReaderController;
    @Mock
    private LoadingCache<String, Long> cache;
    @Mock
    private CacheStats stats;
    @Mock
    private Tracer tracer;

    private AutoCloseable mocks;

    private Authenticator authenticator = (authorization, accountIds) -> {
        if (authorization.equals(BEARER_TOKEN)) {
            if (Arrays.asList(accountIds).contains(AUTHED_ACCOUNT_NUM)) {
                return "";
            }
        }
        throw new AuthenticationException("Unit test authentication failure");
    };

    private static final MeterRegistry METER_REGISTRY = null;
    private static final long BALANCE = 100L;
    private static final String AUTHED_ACCOUNT_NUM = "1234567890";
    private static final String NON_AUTHED_ACCOUNT_NUM = "9876543210";
    private static final String BEARER_TOKEN = "Bearer abc";

    @BeforeEach
    void setUp() {
        this.mocks = openMocks(this);

        when(cache.stats()).thenReturn(stats);
        balanceReaderController = new BalanceReaderController(authenticator,
                METER_REGISTRY, cache, tracer);
        when(tracer.currentSpan()).thenReturn(mock(Span.class));
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (this.mocks != null) {
            this.mocks.close();
        }
    }

    @Test
    @DisplayName("Given the user is authenticated for the account, return HTTP Status 200")
    void getBalanceSucceedsWhenAccountMatchesAuthenticatedUser() throws Exception {
        // Given
        when(cache.get(AUTHED_ACCOUNT_NUM)).thenReturn(BALANCE);

        // When
        final ResponseEntity<?> actualResult = balanceReaderController.getBalance(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.OK, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the user is authenticated for the account, return correct balance.")
    void getBalanceIsCorrectWhenAccountMatchesAuthenticatedUser() throws Exception {
        // Given
        when(cache.get(AUTHED_ACCOUNT_NUM)).thenReturn(BALANCE);

        // When
        final ResponseEntity<?> actualResult = balanceReaderController.getBalance(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(BALANCE, actualResult.getBody());
    }
    @Test
    @DisplayName("Given the user is authenticated but cannot access the account, return 401")
    void getBalanceFailsWhenAccountDoesNotMatchAuthenticatedUser() {
        // Given

        // When
        final ResponseEntity<?> actualResult = balanceReaderController.getBalance(BEARER_TOKEN, NON_AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the user is not authenticated, return 401")
    void getBalanceFailsWhenUserNotAuthenticated() {
        // Given
        final String badBearerToken = "Bearer foobar";

        // When
        final ResponseEntity<?> actualResult = balanceReaderController.getBalance(badBearerToken, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.UNAUTHORIZED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the cache throws an error for an authenticated user, return 500")
    void getBalanceFailsWhenCacheThrowsError() throws Exception {
        // Given
        when(cache.get(AUTHED_ACCOUNT_NUM)).thenThrow(ExecutionException.class);

        // When
        final ResponseEntity<?> actualResult = balanceReaderController.getBalance(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);

        // Then
        assertNotNull(actualResult);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, actualResult.getStatusCode());
    }
}
