package sirius.samples.bankofsirius.ledgerwriter.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.StringJoiner;

@Configuration
@ConfigurationProperties("http.client.pooling")
public class PoolingProperties {
    /**
     * Properties related to the number of connections available within a pool.
     */
    private ConnectionsProperties connections;
    /**
     * When true a client HTTP connection pool is enabled.
     */
    private boolean enabled = false;
    /**
     * The idle time of connections to be closed by the idle connection monitor.
     */
    private int idleConnectionWaitTime;
    /**
     * Time interval between idle connection checks by the idle connection monitor.
     */
    private int idleConnectionCheckInterval;

    @Autowired
    public PoolingProperties(final ConnectionsProperties connections) {
        this.connections = connections;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getIdleConnectionWaitTime() {
        return idleConnectionWaitTime;
    }

    public void setIdleConnectionWaitTime(final int idleConnectionWaitTime) {
        this.idleConnectionWaitTime = idleConnectionWaitTime;
    }

    public int getIdleConnectionCheckInterval() {
        return idleConnectionCheckInterval;
    }

    public void setIdleConnectionCheckInterval(final int idleConnectionCheckInterval) {
        this.idleConnectionCheckInterval = idleConnectionCheckInterval;
    }

    public ConnectionsProperties getConnections() {
        return connections;
    }

    public void setConnections(final ConnectionsProperties connections) {
        this.connections = connections;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PoolingProperties.class.getSimpleName() + "[", "]")
                .add("connections=" + connections)
                .add("enabled=" + enabled)
                .add("idleConnectionWaitTime=" + idleConnectionWaitTime)
                .add("idleConnectionCheckInterval=" + idleConnectionCheckInterval)
                .toString();
    }
}
