package sirius.samples.bankofsirius.ledgerwriter.http;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class IdleConnectionMonitor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleConnectionMonitor.class);
    private final PoolingHttpClientConnectionManager pool;
    private final Duration idleTimeout;
    private final Tracer tracer;

    public IdleConnectionMonitor(final PoolingHttpClientConnectionManager pool,
                                 final Duration idleTimeout,
                                 final Tracer tracer) {
        Objects.requireNonNull(pool);
        Objects.requireNonNull(idleTimeout);
        Objects.requireNonNull(tracer);

        this.pool = pool;
        this.idleTimeout = idleTimeout;
        this.tracer = tracer;
    }

    @Override
    public void run() {
        final Span span = tracer.spanBuilder()
                .name("close_idle_connections")
                .tag("thread", Thread.currentThread().getName()).start();
        tagPoolStats(span, "before");

        try {
            pool.closeExpiredConnections();
            pool.closeIdleConnections(idleTimeout.toMillis(), TimeUnit.MILLISECONDS);
            LOGGER.info("Idle connection monitor: Closed expired and idle connections");
            tagPoolStats(span, "after");
        } finally {
            span.end();
        }
    }

    private void tagPoolStats(final Span span, final String suffix) {
        final PoolStats poolStats = pool.getTotalStats();

        final String available = Integer.toString(poolStats.getAvailable());
        span.tag(String.format("available_%s", suffix), available);
        final String leased = Integer.toString(poolStats.getLeased());
        span.tag(String.format("leased_%s", suffix), leased);
        final String max = Integer.toString(poolStats.getMax());
        span.tag(String.format("max_%s", suffix), max);
        final String pending = Integer.toString(poolStats.getPending());
        span.tag(String.format("pending_%s", suffix), pending);
    }
}
