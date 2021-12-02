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
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.ScopedSpan;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanCustomizer;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

import java.util.Collections;
import java.util.Map;

public class NoopTracer implements Tracer {
    public static final NoopTracer INSTANCE = new NoopTracer();

    @Override
    public Span nextSpan() {
        return NoopSpan.INSTANCE;
    }

    @Override
    public Span nextSpan(final Span parent) {
        return NoopSpan.INSTANCE;
    }

    @Override
    public SpanInScope withSpan(final Span span) {
        return NoopSpan.INSTANCE;
    }

    @Override
    public ScopedSpan startScopedSpan(final String name) {
        return NoopScopedSpan.INSTANCE;
    }

    @Override
    public Span.Builder spanBuilder() {
        return NoopSpanBuilder.INSTANCE;
    }

    @Override
    public TraceContext.Builder traceContextBuilder() {
        return NoopTraceContextBuilder.INSTANCE;
    }

    @Override
    public SpanCustomizer currentSpanCustomizer() {
        return NoopSpanCustomizer.INSTANCE;
    }

    @Override
    public Span currentSpan() {
        return NoopSpan.INSTANCE;
    }

    @Override
    public Map<String, String> getAllBaggage() {
        return Collections.emptyMap();
    }

    @Override
    public BaggageInScope getBaggage(final String name) {
        return NoopBaggageInScope.INSTANCE;
    }

    @Override
    public BaggageInScope getBaggage(final TraceContext traceContext, final String name) {
        return NoopBaggageInScope.INSTANCE;
    }

    @Override
    public BaggageInScope createBaggage(final String name) {
        return NoopBaggageInScope.INSTANCE;
    }

    @Override
    public BaggageInScope createBaggage(final String name, final String value) {
        return NoopBaggageInScope.INSTANCE;
    }

    @Override
    public CurrentTraceContext currentTraceContext() {
        return NoopCurrentTraceContext.INSTANCE;
    }
}
