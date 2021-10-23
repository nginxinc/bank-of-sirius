package sirius.samples.bankofsirius.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "jwt.account.authentication.enabled",
        havingValue = "false")
public class NoopAuthenticator implements Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoopAuthenticator.class);

    @Override
    public void verify(final String authorization, final String accountId) throws AuthenticationException {
        LOGGER.warn("Skipping authentication because JWT is disabled. [accountID={}]",
                accountId);
    }
}
