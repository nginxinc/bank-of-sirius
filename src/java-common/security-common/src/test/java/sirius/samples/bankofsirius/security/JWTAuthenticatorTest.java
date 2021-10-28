package sirius.samples.bankofsirius.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class JWTAuthenticatorTest {
    private static final String JWT_ACCOUNT_KEY = "acct";
    private static final String AUTHED_ACCOUNT_NUM = "1234567890";
    private static final String NON_AUTHED_ACCOUNT_NUM = "9876543210";
    private static final String BEARER_TOKEN = "Bearer abc";
    private static final String TOKEN = "abc";

    @Mock
    private JWTVerifier verifier;
    @Mock
    private DecodedJWT jwt;
    @Mock
    private Claim claim;
    @Mock
    private Tracer tracer;
    @Mock
    private Span.Builder spanBuilder;

    private JWTAuthenticator instance;

    @BeforeEach
    void setUp() {
        initMocks(this);

        when(jwt.getClaim(JWT_ACCOUNT_KEY)).thenReturn(claim);
        when(tracer.spanBuilder()).thenReturn(spanBuilder);
        when(spanBuilder.name(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(mock(Span.class));
        this.instance = new JWTAuthenticator(tracer, verifier);
    }

    /*
            when(verifier.verify(TOKEN)).thenReturn(jwt);
        when(jwt.getClaim(JWT_ACCOUNT_KEY)).thenReturn(claim);
        when(claim.asString()).thenReturn(AUTHED_ACCOUNT_NUM);
     */

    @Test
    @DisplayName("Given the authorization is valid, no exceptions are thrown")
    void validAuthorization() {
        // Given
        when(verifier.verify(TOKEN)).thenReturn(jwt);
        when(jwt.getClaim(JWT_ACCOUNT_KEY)).thenReturn(claim);
        when(claim.asString()).thenReturn(AUTHED_ACCOUNT_NUM);

        // Then - no exceptions
        instance.verify(BEARER_TOKEN, AUTHED_ACCOUNT_NUM);
    }

    @Test
    @DisplayName("Given the authorization is invalid, an exception is thrown")
    void invalidAuthorizationBadAccountNumber() {
        // Given
        when(verifier.verify(TOKEN)).thenReturn(jwt);
        when(jwt.getClaim(JWT_ACCOUNT_KEY)).thenReturn(claim);
        when(claim.asString()).thenReturn(AUTHED_ACCOUNT_NUM);

        // Then
        assertThrows(AuthenticationException.class, () ->
            instance.verify(BEARER_TOKEN, NON_AUTHED_ACCOUNT_NUM));
    }

    @Test
    @DisplayName("Given the authorization is invalid, an exception is thrown")
    void invalidAuthorizationBadToken() {
        // Given
        when(verifier.verify(TOKEN)).thenReturn(jwt);
        when(jwt.getClaim(JWT_ACCOUNT_KEY)).thenReturn(claim);
        when(claim.asString()).thenReturn(NON_AUTHED_ACCOUNT_NUM);

        // Then
        assertThrows(AuthenticationException.class, () ->
                instance.verify(BEARER_TOKEN, AUTHED_ACCOUNT_NUM));
    }

    @Test
    @DisplayName("Given the bearer token is null, return empty string")
    void extractBearerTokenNullToken() {
        assertThrows(NullPointerException.class, () ->
                instance.extractBearerToken(null));
    }

    @Test
    @DisplayName("Given the bearer token doesn't start with [Bearer ], return empty string")
    void extractBearerTokenIncorrectPrefixToken() {
        assertThrows(IllegalArgumentException.class, () ->
                instance.extractBearerToken(""));
    }

    @Test
    @DisplayName("Given the bearer token starts with [Bearer ], return string after")
    void extractBearerTokenWithoutValueToken() {
        String actual = instance.extractBearerToken("Bearer ");
        assertEquals("", actual);
    }

    @Test
    @DisplayName("Given the bearer token starts with [Bearer ], return string after")
    void extractBearerTokenValidToken() {
        String expected = "ThisValue";
        String actual = instance.extractBearerToken("Bearer ThisValue");
        assertEquals(expected, actual);
    }
}
