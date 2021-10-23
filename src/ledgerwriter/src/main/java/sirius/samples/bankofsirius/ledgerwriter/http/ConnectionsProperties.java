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
