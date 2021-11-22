package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.BaggageInScope;
import org.springframework.cloud.sleuth.TraceContext;

public class NoopBaggageInScope implements BaggageInScope {
    public static final NoopBaggageInScope INSTANCE = new NoopBaggageInScope();

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public String get() {
        return "noop";
    }

    @Override
    public String get(final TraceContext traceContext) {
        return "noop";
    }

    @Override
    public BaggageInScope set(final String value) {
        return INSTANCE;
    }

    @Override
    public BaggageInScope set(final TraceContext traceContext, final String value) {
        return INSTANCE;
    }

    @Override
    public BaggageInScope makeCurrent() {
        return INSTANCE;
    }

    @Override
    public void close() {
    }
}
