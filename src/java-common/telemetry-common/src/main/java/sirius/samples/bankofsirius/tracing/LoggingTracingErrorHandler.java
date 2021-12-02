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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.ErrorHandler;

public class LoggingTracingErrorHandler implements ErrorHandler, Thread.UncaughtExceptionHandler {
    protected final Logger logger = LoggerFactory.getLogger(LoggingTracingErrorHandler.class);
    protected final Tracer tracer;

    public LoggingTracingErrorHandler(final Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void handleError(final Throwable throwable) {
        uncaughtException(null, throwable);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        Span span = tracer.currentSpan();
        if (span == null) {
            span = tracer.spanBuilder().name("uncaught_exception").start();
        }

        logger.error("Unexpected error occurred", throwable);
        final Thread invokingThread = thread == null
                ? Thread.currentThread() : thread;

        span.tag("thread", invokingThread.getName());
        span.error(throwable);
        span.end();
    }
}
