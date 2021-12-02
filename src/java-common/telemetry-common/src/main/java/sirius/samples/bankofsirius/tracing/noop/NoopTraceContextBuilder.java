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
