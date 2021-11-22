package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.TraceContext;

public class NoopTraceContext implements TraceContext {
    public static final NoopTraceContext INSTANCE = new NoopTraceContext();

    @Override
    public String traceId() {
        return null;
    }

    @Override
    public String parentId() {
        return null;
    }

    @Override
    public String spanId() {
        return null;
    }

    @Override
    public Boolean sampled() {
        return null;
    }
}
