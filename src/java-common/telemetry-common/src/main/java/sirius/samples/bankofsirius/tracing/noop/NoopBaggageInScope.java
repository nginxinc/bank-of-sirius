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
