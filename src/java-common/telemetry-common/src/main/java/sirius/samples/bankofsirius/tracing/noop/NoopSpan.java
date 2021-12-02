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

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;

public class NoopSpan implements Span, Tracer.SpanInScope {
    public static final NoopSpan INSTANCE = new NoopSpan();

    @Override
    public boolean isNoop() {
        return true;
    }

    @Override
    public TraceContext context() {
        return NoopTraceContext.INSTANCE;
    }

    @Override
    public Span start() {
        return INSTANCE;
    }

    @Override
    public Span name(final String name) {
        return INSTANCE;
    }

    @Override
    public Span event(final String value) {
        return INSTANCE;
    }

    @Override
    public Span tag(final String key, final String value) {
        return INSTANCE;
    }

    @Override
    public Span error(final Throwable throwable) {
        return INSTANCE;
    }

    @Override
    public void end() {
    }

    @Override
    public void abandon() {
    }

    @Override
    public Span remoteServiceName(final String remoteServiceName) {
        return INSTANCE;
    }

    @Override
    public Span remoteIpAndPort(final String ip, final int port) {
        return INSTANCE;
    }

    @Override
    public void close() {
    }
}
