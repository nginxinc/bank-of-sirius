/*
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
package sirius.samples.bankofsirius.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(value = "jwt.account.authentication.enabled",
    matchIfMissing = true, havingValue = "true")
public class JWTAuthenticator implements Authenticator {
    private static final String ACCOUNT_ID_JWT_CLAIM_NAME = "acct";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JWTAuthenticator.class);

    private final Tracer tracer;
    private final JWTVerifier verifier;

    @Autowired
    public JWTAuthenticator(final Tracer tracer, final JWTVerifier verifier) {
        this.tracer = tracer;
        this.verifier = verifier;
    }

    @Override
    public String verify(final String authorization, final String... accountIds)
            throws AuthenticationException {
        final Span span = tracer.spanBuilder().name("jwt_verify").start();

        try {
            if (authorization == null) {
                throw new NullPointerException("JWT authorization header is null");
            }
            if (authorization.isEmpty()) {
                throw new IllegalArgumentException("JWT authorization header is empty");
            }

            final String bearerToken = extractBearerToken(authorization);
            final Claim claim = extractClaimFromBearerToken(bearerToken, ACCOUNT_ID_JWT_CLAIM_NAME);
            final String expectedAccountId = claim.asString();
            if (expectedAccountId.isEmpty()) {
                throw new AuthenticationException("Account id in JWT bearer token is empty");
            }

            final boolean accountIdMatched = verifyTokens(expectedAccountId, accountIds);

            if (accountIdMatched) {
                span.tag("account.id", expectedAccountId);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Authenticated request [accountId={}]", expectedAccountId);
                }
            } else {
                String msg = String.format(
                        "No account id matches JWT token contexts [accountIdInJwtToken=%s,expectedIds=%s]",
                        expectedAccountId,
                        String.join(", ", accountIds));
                throw new AuthenticationException(msg);
            }

            return expectedAccountId;
        } finally {
            span.end();
        }
    }

    String extractBearerToken(final String authorization) {
        Objects.requireNonNull(authorization, "authorization must not be null");

        final String prefix = "Bearer ";

        final int pos = authorization.indexOf(prefix);
        if (pos < 0) {
            String msg = String.format("JWT authorization header is in an invalid format: %s", authorization);
            throw new IllegalArgumentException(msg);
        }

        return authorization.substring(pos + prefix.length());
    }

    Claim extractClaimFromBearerToken(final String bearerToken, final String claimName) {
        final DecodedJWT jwt;
        try {
            jwt = verifier.verify(bearerToken);
        } catch (JWTVerificationException e) {
            throw new AuthenticationException("Unable to decode or verify JWT bearer token", e);
        }

        final Claim claim = jwt.getClaim(claimName);
        if (claim.isNull()) {
            throw new AuthenticationException("Unable to extract account id claim from JWT bearer token");
        }

        return claim;
    }

    boolean verifyTokens(final String expectedAccountId, final String... accountIds) {
        boolean accountIdFound = false;
        for (String accountId : accountIds) {
            if (expectedAccountId.equals(accountId)) {
                accountIdFound = true;
                break;
            }
        }

        return accountIdFound;
    }
}
