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
    public void verify(final String authorization, final String accountId) {
        final Span span = tracer.spanBuilder().name("jwt_verify").start();

        try {
            if (authorization == null) {
                throw new NullPointerException("JWT authorization header is null");
            }
            if (authorization.isEmpty()) {
                throw new IllegalArgumentException("JWT authorization header is empty");
            }
            if (accountId == null) {
                throw new NullPointerException("account id is null");
            }
            if (accountId.isEmpty()) {
                throw new IllegalArgumentException("account id is blank");
            }

            final String bearerToken = extractBearerToken(authorization);
            verifyToken(bearerToken, accountId);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Authenticated request [accountId={}]", accountId);
            }
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

    void verifyToken(final String bearerToken,
                     final String accountId) throws AuthenticationException {
        final Span span = tracer.spanBuilder().name("jwt_token_verify").start();

        try {
            final DecodedJWT jwt = verifier.verify(bearerToken);
            final Claim decodedAccountId = jwt.getClaim("acct");

            if (decodedAccountId.isNull()) {
                throw new AuthenticationException("Unable to decode account id from bearer token");
            }

            // Check that the authenticated user can access this account.
            if (!accountId.equals(decodedAccountId.asString())) {
                String msg = "Requested account id does not match bearer token";
                throw new AuthenticationException(msg);
            }
        } catch (JWTVerificationException e) {
            String msg = "Unable to verify JWT token";
            throw new AuthenticationException(msg, e);
        } catch (RuntimeException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
