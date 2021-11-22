package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.ScopedSpan;
import org.springframework.cloud.sleuth.TraceContext;

public class NoopScopedSpan implements ScopedSpan {
    public static final NoopScopedSpan INSTANCE = new NoopScopedSpan();

    @Override
    public boolean isNoop() {
        return true;
    }

    @Override
    public TraceContext context() {
        return NoopTraceContext.INSTANCE;
    }

    @Override
    public ScopedSpan name(final String name) {
        return INSTANCE;
    }

    @Override
    public ScopedSpan tag(final String key, final String value) {
        return INSTANCE;
    }

    @Override
    public ScopedSpan event(final String value) {
        return INSTANCE;
    }

    @Override
    public ScopedSpan error(final Throwable throwable) {
        return INSTANCE;
    }

    @Override
    public void end() {
    }
}
