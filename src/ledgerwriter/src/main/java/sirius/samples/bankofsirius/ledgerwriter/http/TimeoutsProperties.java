package sirius.samples.bankofsirius.ledgerwriter.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.StringJoiner;

@Configuration
@ConfigurationProperties("http.client.timeouts")
public class TimeoutsProperties {
    public static final int DEFAULT_CONNECTION_TIMEOUT = 500;
    public static final int DEFAULT_SOCKET_TIMEOUT = 1_000;
    public static final int DEFAULT_REQUEST_TIMEOUT = 500;

    /**
     * Time to wait for a connection to be established in milliseconds.
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    /**
     * Time to wait for data to become available in milliseconds.
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    /**
     * Time to wait for a connection from the connection pool in milliseconds.
     */
    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TimeoutsProperties.class.getSimpleName() + "[", "]")
                .add("connectionTimeout=" + connectionTimeout)
                .add("socketTimeout=" + socketTimeout)
                .add("requestTimeout=" + requestTimeout)
                .toString();
    }
}
