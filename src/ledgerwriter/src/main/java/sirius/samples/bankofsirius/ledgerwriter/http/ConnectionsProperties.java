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
package sirius.samples.bankofsirius.ledgerwriter.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.StringJoiner;

@Configuration
@ConfigurationProperties("http.client.pooling.connections")
public class ConnectionsProperties {
    public static final int DEFAULT_MAX_PER_ROUTE = 30;
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 30;

    /**
     * Maximum amount of connections for each http route in pool
     */
    private int maxPerRoute = DEFAULT_MAX_PER_ROUTE;
    /**
     * Total amount of connections across all HTTP routes.
     */
    private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;

    public int getMaxPerRoute() {
        return maxPerRoute;
    }

    public void setMaxPerRoute(final int maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(final int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConnectionsProperties.class.getSimpleName() + "[", "]")
                .add("maxPerRoute=" + maxPerRoute)
                .add("maxTotalConnections=" + maxTotalConnections)
                .toString();
    }
}
