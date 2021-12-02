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
package sirius.samples.bankofsirius.tracing;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraceNamingHandlerInterceptor implements HandlerInterceptor {
    private static final Pattern CAMEL_TO_UNDERSCORE = Pattern.compile("(?<=[a-z])[A-Z]");

    private final boolean convertToUnderscores;
    private Tracer tracer;

    public TraceNamingHandlerInterceptor(
            final Tracer tracer, final boolean convertToUnderscores) {
        this.tracer = tracer;
        this.convertToUnderscores = convertToUnderscores;
    }

    private String extractSpanName(final Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }

        final Method handlerMethod = ((HandlerMethod) handler).getMethod();
        final String handlerMethodName = handlerMethod.getName();
        final String methodName = convertToUnderscores
                ? toUnderscoreDelimitedText(handlerMethodName) : handlerMethodName;

        return methodName;
    }

    private static String toUnderscoreDelimitedText(final String input) {
        final Matcher matcher = CAMEL_TO_UNDERSCORE.matcher(input);
        final StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer,
                    "_" + matcher.group().toLowerCase(Locale.ENGLISH));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {
        final Span span = tracer.currentSpan();
        if (span != null) {
            final String newSpanName = extractSpanName(handler);
            if (newSpanName != null) {
                span.name(newSpanName);
            }
        }

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
