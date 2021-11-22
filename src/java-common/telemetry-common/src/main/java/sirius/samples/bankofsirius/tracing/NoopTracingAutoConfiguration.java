package sirius.samples.bankofsirius.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sirius.samples.bankofsirius.tracing.noop.NoopTracer;

@Configuration
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true, havingValue = "false")
@ConditionalOnClass(name = {"org.springframework.cloud.sleuth.Tracer"})
public class NoopTracingAutoConfiguration {
    @Bean
    public Tracer noopTracer() {
        return NoopTracer.INSTANCE;
    }
}
