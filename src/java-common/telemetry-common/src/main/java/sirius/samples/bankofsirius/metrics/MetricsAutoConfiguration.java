package sirius.samples.bankofsirius.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.ErrorHandler;
import sirius.samples.bankofsirius.tracing.LoggingTracingErrorHandler;
import sirius.samples.bankofsirius.tracing.TracingAutoConfiguration;

import java.util.Collections;

@Configuration
@AutoConfigureAfter({
        org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration.class,
        TracingAutoConfiguration.class})
public class MetricsAutoConfiguration {
    @Bean
    @ConditionalOnProperty(value = "spring.sleuth.enabled",
            havingValue = "false")
    public ErrorHandler defaultTaskErrorHandler() {
        return TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER;
    }

    @Bean
    @ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true,
        havingValue = "true")
    public ErrorHandler taskErrorHandler(final Tracer tracer) {
        return new LoggingTracingErrorHandler(tracer);
    }

    @Bean
    public SchedulingConfigurer executorServiceMetrics(final MeterRegistry registry,
                                                       final ErrorHandler errorHandler) {
        final String serviceName = "spring-scheduler";
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix(String.format("%s-", serviceName));
        scheduler.setErrorHandler(errorHandler);
        scheduler.initialize();

        final Iterable<Tag> tags = Collections.emptyList();

        final MeterBinder executorServiceMetrics =
                new ExecutorServiceMetrics(scheduler.getScheduledExecutor(), serviceName, tags);
        executorServiceMetrics.bindTo(registry);

        return taskRegistrar -> taskRegistrar.setScheduler(scheduler);
    }
}
