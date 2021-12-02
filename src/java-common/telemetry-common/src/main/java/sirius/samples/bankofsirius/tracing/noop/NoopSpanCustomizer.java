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
