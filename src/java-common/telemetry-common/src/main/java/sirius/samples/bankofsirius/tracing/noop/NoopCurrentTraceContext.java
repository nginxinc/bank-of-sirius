/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return () -> {
        };
    }

    @Override
    public Scope maybeScope(final TraceContext context) {
        return () -> {
        };
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
