package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.TraceContext;

public class NoopTraceContextBuilder implements TraceContext.Builder {
    public static final NoopTraceContextBuilder INSTANCE = new NoopTraceContextBuilder();

    @Override
    public TraceContext.Builder traceId(final String traceId) {
        return INSTANCE;
    }

    @Override
    public TraceContext.Builder parentId(final String parentId) {
        return INSTANCE;
    }

    @Override
    public TraceContext.Builder spanId(final String spanId) {
        return INSTANCE;
    }

    @Override
    public TraceContext.Builder sampled(final Boolean sampled) {
        return INSTANCE;
    }

    @Override
    public TraceContext build() {
        return NoopTraceContext.INSTANCE;
    }
}
