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
    public String verify(final String authorization, final String... accountIds) throws AuthenticationException {
        LOGGER.warn("Skipping authentication because JWT is disabled. [accountIds={}]",
                String.join(", ", accountIds));
        return "";
    }
}
