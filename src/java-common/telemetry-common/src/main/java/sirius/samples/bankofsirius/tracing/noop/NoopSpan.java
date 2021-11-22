package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

public class NoopSpan implements Span, Tracer.SpanInScope {
    public static final NoopSpan INSTANCE = new NoopSpan();

    @Override
    public boolean isNoop() {
        return true;
    }

    @Override
    public TraceContext context() {
        return NoopTraceContext.INSTANCE;
    }

    @Override
    public Span start() {
        return INSTANCE;
    }

    @Override
    public Span name(final String name) {
        return INSTANCE;
    }

    @Override
    public Span event(final String value) {
        return INSTANCE;
    }

    @Override
    public Span tag(final String key, final String value) {
        return INSTANCE;
    }

    @Override
    public Span error(final Throwable throwable) {
        return INSTANCE;
    }

    @Override
    public void end() {
    }

    @Override
    public void abandon() {
    }

    @Override
    public Span remoteServiceName(final String remoteServiceName) {
        return INSTANCE;
    }

    @Override
    public Span remoteIpAndPort(final String ip, final int port) {
        return INSTANCE;
    }

    @Override
    public void close() {
    }
}
