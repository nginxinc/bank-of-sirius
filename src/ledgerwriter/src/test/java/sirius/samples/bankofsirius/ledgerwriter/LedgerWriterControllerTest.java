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

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mock;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;
import sirius.samples.bankofsirius.ledger.Transaction;
import sirius.samples.bankofsirius.ledger.TransactionRepository;
import sirius.samples.bankofsirius.ledger.TransactionValidationException;
import sirius.samples.bankofsirius.ledger.TransactionValidator;
import sirius.samples.bankofsirius.security.AuthenticationException;
import sirius.samples.bankofsirius.security.Authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static sirius.samples.bankofsirius.ledger.ExceptionMessages.EXCEPTION_MESSAGE_WHEN_AUTHORIZATION_HEADER_NULL;

class LedgerWriterControllerTest {

    private LedgerWriterController ledgerWriterController;

    @Mock
    private TransactionValidator transactionValidator;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private Authenticator authenticator;
    @Mock
    private RestOperations restOperations;
    @Mock
    private Transaction transaction;
    @Mock
    private Tracer tracer;
    @Mock
    private Span span;
    @Mock
    Span.Builder spanBuilder;

    private static final MeterRegistry METER_REGISTRY = null;
    private static final String LOCAL_ROUTING_NUM = "123456789";
    private static final String NON_LOCAL_ROUTING_NUM = "987654321";
    private static final String BALANCES_API_ADDR = "http://balancereader:8080";
    private static final String AUTHED_ACCOUNT_NUM = "1234567890";
    private static final String BEARER_TOKEN = "Bearer abc";
    private static final String TOKEN = "abc";
    private static final String EXCEPTION_MESSAGE = "Invalid variable";
    private static final int SENDER_BALANCE = 40;
    private static final int LARGER_THAN_SENDER_BALANCE = 1000;
    private static final int SMALLER_THAN_SENDER_BALANCE = 10;

    @BeforeEach
    void setUp() {
        initMocks(this);

        ledgerWriterController = new LedgerWriterController(authenticator,
                METER_REGISTRY, transactionRepository, transactionValidator,
                LOCAL_ROUTING_NUM, BALANCES_API_ADDR, restOperations,
                tracer);
        doThrow(AuthenticationException.class).when(authenticator)
                .verify(nullable(String.class), anyString());
        doNothing().when(authenticator).verify(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);
        when(tracer.currentSpan()).thenReturn(span);
        when(tracer.spanBuilder()).thenReturn(spanBuilder);
        when(spanBuilder.name(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(span);
    }

    @Test
    @DisplayName("Given the transaction is external, return HTTP Status 201")
    void addTransactionSuccessWhenDiffThanLocalRoutingNum(TestInfo testInfo) {
        // Given
        when(transaction.getFromRoutingNum()).thenReturn(NON_LOCAL_ROUTING_NUM);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());

        // When
        final ResponseEntity<?> actualResult =
                ledgerWriterController.addTransaction(BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals(LedgerWriterController.READINESS_CODE,
                actualResult.getBody());
        assertEquals(HttpStatus.CREATED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the transaction is internal and the transaction amount == sender balance, " +
            "return HTTP Status 201")
    void addTransactionSuccessWhenAmountEqualToBalance(TestInfo testInfo) {
        // Given
        LedgerWriterController spyLedgerWriterController =
                spy(ledgerWriterController);
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getAmount()).thenReturn(SENDER_BALANCE);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doReturn(SENDER_BALANCE).when(
                spyLedgerWriterController).getAvailableBalance(
                TOKEN, AUTHED_ACCOUNT_NUM);

        // When
        final ResponseEntity<?> actualResult =
                spyLedgerWriterController.addTransaction(
                        BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals(LedgerWriterController.READINESS_CODE,
                actualResult.getBody());
        assertEquals(HttpStatus.CREATED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the transaction is internal and the transaction amount < sender balance, " +
            "return HTTP Status 201")
    void addTransactionSuccessWhenAmountSmallerThanBalance(TestInfo testInfo) {
        // Given
        LedgerWriterController spyLedgerWriterController =
                spy(ledgerWriterController);
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getAmount()).thenReturn(SMALLER_THAN_SENDER_BALANCE);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doReturn(SENDER_BALANCE).when(
                spyLedgerWriterController).getAvailableBalance(
                TOKEN, AUTHED_ACCOUNT_NUM);

        // When
        final ResponseEntity<?> actualResult =
                spyLedgerWriterController.addTransaction(
                        BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals(LedgerWriterController.READINESS_CODE,
                actualResult.getBody());
        assertEquals(HttpStatus.CREATED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the transaction is internal and the transaction amount > sender balance, " +
            "return HTTP Status 400")
    void addTransactionFailWhenWhenAmountLargerThanBalance(TestInfo testInfo) {
        // Given
        LedgerWriterController spyLedgerWriterController =
                spy(ledgerWriterController);
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getFromAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getAmount()).thenReturn(LARGER_THAN_SENDER_BALANCE);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doReturn(new ResponseEntity<>(SENDER_BALANCE, HttpStatus.OK))
                .when(restOperations).exchange(anyString(), eq(HttpMethod.GET),
                        any(HttpEntity.class), eq(Integer.class));

        // When
        final ResponseEntity<?> actualResult =
                spyLedgerWriterController.addTransaction(
                        BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals("insufficient balance",
                actualResult.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given JWT verifier cannot verify the given bearer token, " +
            "return HTTP Status 401")
    void addTransactionWhenJWTVerificationExceptionThrown() {
        // Given
        final String badBearerToken = "Bearer foobar";

        // When
        when(transaction.getFromAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        final ResponseEntity<?> actualResult = ledgerWriterController
                .addTransaction(badBearerToken, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals(LedgerWriterController.UNAUTHORIZED_CODE,
                actualResult.getBody());
        assertEquals(HttpStatus.UNAUTHORIZED, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given exception thrown on validation, return HTTP Status 400")
    void addTransactionWhenIllegalArgumentExceptionThrown() throws TransactionValidationException {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        doThrow(new TransactionValidationException("Unit test controlled failure", EXCEPTION_MESSAGE)).
                when(transactionValidator).validateTransaction(
                        LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);

        // When
        final ResponseEntity<?> actualResult = ledgerWriterController
                .addTransaction(BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals(EXCEPTION_MESSAGE,
                actualResult.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given HTTP request 'Authorization' header is null, " +
            "return HTTP Status 400")
    void addTransactionWhenBearerTokenNull() {
        // When
        final ResponseEntity<?> actualResult =
                ledgerWriterController.addTransaction(
                        null, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals(EXCEPTION_MESSAGE_WHEN_AUTHORIZATION_HEADER_NULL,
                actualResult.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the transaction is internal, check available balance and the balance " +
            "reader throws an error, return HTTP Status 500")
    void addTransactionWhenResourceAccessExceptionThrown(TestInfo testInfo) {
        // Given
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getFromAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doThrow(new ResourceAccessException(EXCEPTION_MESSAGE))
                .when(restOperations).exchange(anyString(), eq(HttpMethod.GET),
                        any(HttpEntity.class), eq(Integer.class));

        // When
        final ResponseEntity<?> actualResult = ledgerWriterController
                .addTransaction(BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals("remote resource unavailable", actualResult.getBody());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the transaction is external and the transaction cannot be saved to the " +
            "transaction repository, return HTTP Status 500")
    void addTransactionWhenCannotCreateTransactionExceptionExceptionThrown(TestInfo testInfo) {
        // Given
        when(transaction.getFromRoutingNum()).thenReturn(NON_LOCAL_ROUTING_NUM);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doThrow(new CannotCreateTransactionException(EXCEPTION_MESSAGE)).when(
                transactionRepository).save(transaction);

        // When
        final ResponseEntity<?> actualResult =
                ledgerWriterController.addTransaction(
                        TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals("unable to persist transaction",
                actualResult.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                actualResult.getStatusCode());
    }

    @Test
    @DisplayName("Given the transaction is internal, check available balance and the balance " +
            "service returns 500, return HTTP Status 500")
    void addTransactionWhenHttpServerErrorExceptionThrown(TestInfo testInfo) {
        // Given
        LedgerWriterController spyLedgerWriterController =
                spy(ledgerWriterController);
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getFromAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doThrow(new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR)).when(
                        spyLedgerWriterController).getAvailableBalance(
                TOKEN, AUTHED_ACCOUNT_NUM);

        // When
        final ResponseEntity<?> actualResult =
                spyLedgerWriterController.addTransaction(
                        BEARER_TOKEN, transaction);

        // Then
        assertNotNull(actualResult);
        assertEquals("unable to read available balance", actualResult.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                actualResult.getStatusCode());
    }

    @Test
    @DisplayName("When duplicate UUID transactions are sent, " +
            "second one is rejected with HTTP status 400")
    void addTransactionWhenDuplicateUuidExceptionThrown(TestInfo testInfo) {
        // Given
        LedgerWriterController spyLedgerWriterController =
                spy(ledgerWriterController);
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getAmount()).thenReturn(SMALLER_THAN_SENDER_BALANCE);
        when(transaction.getRequestUuid()).thenReturn(testInfo.getDisplayName());
        doReturn(SENDER_BALANCE).when(
                spyLedgerWriterController).getAvailableBalance(
                TOKEN, AUTHED_ACCOUNT_NUM);

        // When
        final ResponseEntity<?> originalResult =
                spyLedgerWriterController.addTransaction(
                        BEARER_TOKEN, transaction);
        final ResponseEntity<?> duplicateResult =
                spyLedgerWriterController.addTransaction(
                        BEARER_TOKEN, transaction);

        // Then
        assertNotNull(originalResult);
        assertEquals(LedgerWriterController.READINESS_CODE,
                originalResult.getBody());
        assertEquals(HttpStatus.CREATED, originalResult.getStatusCode());

        assertNotNull(duplicateResult);
        assertEquals(
                "unable to add duplicate transaction",
                duplicateResult.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, duplicateResult.getStatusCode());
    }
}
