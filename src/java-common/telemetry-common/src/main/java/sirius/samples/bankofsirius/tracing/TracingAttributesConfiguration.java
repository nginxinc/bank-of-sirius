package sirius.samples.bankofsirius.tracing;

import io.opentelemetry.sdk.extension.resources.HostResource;
import io.opentelemetry.sdk.extension.resources.OsResource;
import io.opentelemetry.sdk.extension.resources.ProcessResource;
import io.opentelemetry.sdk.extension.resources.ProcessRuntimeResource;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Supplier;

@Configuration
public class TracingAttributesConfiguration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TracingAttributes tracingAttributes(
            @Value("${spring.application.name}") final String springApplicationName) {
        return new TracingAttributes(springApplicationName);
    }

    @Bean
    public Supplier<Resource> applicationDetails(final TracingAttributes tracingAttributes) {
        final Resource resource = Resource.empty()
                .merge(OsResource.get())
                .merge(ProcessResource.get())
                .merge(ProcessRuntimeResource.get())
                .merge(HostResource.get())
                .merge(Resource.create(tracingAttributes));

        return () -> resource;
    }
}
