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
