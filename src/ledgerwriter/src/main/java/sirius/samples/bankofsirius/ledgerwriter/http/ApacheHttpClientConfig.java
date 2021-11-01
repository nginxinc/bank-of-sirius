package sirius.samples.bankofsirius.ledgerwriter.http;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.httpcomponents.MicrometerHttpRequestExecutor;
import io.micrometer.core.instrument.binder.httpcomponents.PoolingHttpClientConnectionManagerMetricsBinder;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultBackoffStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpRequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

import java.time.Duration;

@Configuration
@EnableScheduling
public class ApacheHttpClientConfig {
    @Bean
    @ConditionalOnProperty(value = "http.client.pooling.enabled", havingValue = "true")
    public HttpClientConnectionManager poolingConnectionManager(
            final MeterRegistry meterRegistry,
            final ConnectionsProperties connectionConfig,
            @Value("${ENABLE_METRICS}") final boolean enableMetrics) {
        final PoolingHttpClientConnectionManager poolingConnectionManager =
                new PoolingHttpClientConnectionManager();
        // total amount of connections across all HTTP routes
        final int maxTotalConns = connectionConfig.getMaxTotalConnections();
        poolingConnectionManager.setMaxTotal(maxTotalConns);
        // maximum amount of connections for each http route in pool
        final int maxPerRoute = connectionConfig.getMaxPerRoute();
        poolingConnectionManager.setDefaultMaxPerRoute(maxPerRoute);

        if (enableMetrics) {
            PoolingHttpClientConnectionManagerMetricsBinder metricsBinder =
                    new PoolingHttpClientConnectionManagerMetricsBinder(
                            poolingConnectionManager, "restTemplate");

            metricsBinder.bindTo(meterRegistry);
        }

        return poolingConnectionManager;
    }

    @Bean
    @ConditionalOnProperty(value = "http.client.pooling.enabled",
            matchIfMissing = true, havingValue = "false")
    public HttpClientConnectionManager basicConnectionManager() {
        return new BasicHttpClientConnectionManager();
    }

    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy(
            final HttpClientProperties clientProperties) {
        final ConnectionKeepAliveStrategy defaultConnectionKeepAliveStrategy =
                new DefaultConnectionKeepAliveStrategy();

        return (httpResponse, httpContext) -> {
            final long keepAliveTime = defaultConnectionKeepAliveStrategy
                    .getKeepAliveDuration(httpResponse, httpContext);

            if (keepAliveTime < 0) {
                return clientProperties.getDefaultKeepAliveTime();
            } else {
                return keepAliveTime;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(value = "http.client.pooling.enabled", havingValue = "true")
    public TaskScheduler taskScheduler(final ErrorHandler errorHandler,
                                       final HttpClientConnectionManager manager,
                                       final PoolingProperties poolingProperties,
                                       final Tracer tracer,
                                       @Value("${http.client.pooling.idle-connect-check-interval-ms:20000}")
                                           final Integer idleEvictionCheckms) {
        if (manager instanceof PoolingHttpClientConnectionManager) {
            final int checkInterval = poolingProperties.getIdleConnectionCheckInterval();
            final PoolingHttpClientConnectionManager pool =
                    (PoolingHttpClientConnectionManager) manager;
            final Duration idleConnectionWaitTime = Duration.ofMillis(checkInterval);
            final IdleConnectionMonitor idleConnectionMonitor =
                    new IdleConnectionMonitor(pool, idleConnectionWaitTime, tracer);
            final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setThreadNamePrefix("http-client-idle-monitor-");
            scheduler.setPoolSize(1);
            scheduler.setErrorHandler(errorHandler);
            scheduler.initialize();

            scheduler.scheduleWithFixedDelay(idleConnectionMonitor, idleEvictionCheckms);

            return scheduler;
        }

        return null;
    }

    @Bean
    public HttpRequestExecutor requestExecutor(final MeterRegistry meterRegistry,
                                               @Value("${ENABLE_METRICS}") final boolean enableMetrics) {
        if (enableMetrics) {
            return MicrometerHttpRequestExecutor.builder(meterRegistry).build();
        } else {
            return new HttpRequestExecutor();
        }
    }

    @Bean
    public CloseableHttpClient httpClient(final HttpClientProperties clientProperties,
                                          final HttpClientConnectionManager connectionManager,
                                          final ConnectionKeepAliveStrategy connectionKeepAliveStrategy) {
        final TimeoutsProperties timeoutsProperties = clientProperties.getTimeouts();
        final int requestTimeout = timeoutsProperties.getRequestTimeout();
        final int socketTimeout = timeoutsProperties.getSocketTimeout();
        final int connectionTimeout = timeoutsProperties.getConnectionTimeout();
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(requestTimeout)
                .setSocketTimeout(socketTimeout)
                .build();

        final DnsResolver dnsResolver = new ShufflingDnsResolver();

        final CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .setDnsResolver(dnsResolver)
                .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
                .setKeepAliveStrategy(connectionKeepAliveStrategy)
                .setConnectionBackoffStrategy(new DefaultBackoffStrategy())
                .build();

        final Logger logger = LoggerFactory.getLogger(getClass());
        logger.info("Instantiated new Apache HTTP Client {}", clientProperties);

        return client;
    }
}
