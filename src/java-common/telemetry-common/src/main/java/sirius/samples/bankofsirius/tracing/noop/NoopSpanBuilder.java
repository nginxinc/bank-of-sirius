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

public class NoopSpanBuilder implements Span.Builder {
    public static final NoopSpanBuilder INSTANCE = new NoopSpanBuilder();

    @Override
    public Span.Builder setParent(final TraceContext context) {
        return INSTANCE;
    }

    @Override
    public Span.Builder setNoParent() {
        return INSTANCE;
    }

    @Override
    public Span.Builder name(final String name) {
        return INSTANCE;
    }

    @Override
    public Span.Builder event(final String value) {
        return INSTANCE;
    }

    @Override
    public Span.Builder tag(final String key, final String value) {
        return INSTANCE;
    }

    @Override
    public Span.Builder error(final Throwable throwable) {
        return INSTANCE;
    }

    @Override
    public Span.Builder kind(final Span.Kind spanKind) {
        return INSTANCE;
    }

    @Override
    public Span.Builder remoteServiceName(final String remoteServiceName) {
        return INSTANCE;
    }

    @Override
    public Span start() {
        return NoopSpan.INSTANCE;
    }

    @Override
    public Span.Builder remoteIpAndPort(final String ip, final int port) {
        return INSTANCE;
    }
}
