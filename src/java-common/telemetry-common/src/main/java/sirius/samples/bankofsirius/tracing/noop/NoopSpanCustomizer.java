package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.SpanCustomizer;

public class NoopSpanCustomizer implements SpanCustomizer {
    public static final NoopSpanCustomizer INSTANCE = new NoopSpanCustomizer();

    @Override
    public SpanCustomizer name(final String name) {
        return INSTANCE;
    }

    @Override
    public SpanCustomizer tag(final String key, final String value) {
        return INSTANCE;
    }

    @Override
    public SpanCustomizer event(final String value) {
        return INSTANCE;
    }
}
