package sirius.samples.bankofsirius.tracing.noop;

import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.TraceContext;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class NoopCurrentTraceContext implements CurrentTraceContext {
    public static final NoopCurrentTraceContext INSTANCE = new NoopCurrentTraceContext();

    @Override
    public TraceContext context() {
        return NoopTraceContext.INSTANCE;
    }

    @Override
    public Scope newScope(final TraceContext context) {
        return () -> {};
    }

    @Override
    public Scope maybeScope(final TraceContext context) {
        return () -> {};
    }

    @Override
    public <C> Callable<C> wrap(final Callable<C> task) {
        return task;
    }

    @Override
    public Runnable wrap(final Runnable task) {
        return task;
    }

    @Override
    public Executor wrap(final Executor delegate) {
        return delegate;
    }

    @Override
    public ExecutorService wrap(final ExecutorService delegate) {
        return delegate;
    }
}
