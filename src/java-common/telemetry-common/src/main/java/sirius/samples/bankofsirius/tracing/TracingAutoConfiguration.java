package sirius.samples.bankofsirius.tracing;

import io.opentelemetry.sdk.extension.resources.HostResource;
import io.opentelemetry.sdk.extension.resources.OsResource;
import io.opentelemetry.sdk.extension.resources.ProcessResource;
import io.opentelemetry.sdk.extension.resources.ProcessRuntimeResource;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true,
    havingValue = "true")
@ConditionalOnClass(name = {"org.springframework.cloud.sleuth.Tracer"})
public class TracingAutoConfiguration {
    @Bean
    @ConditionalOnClass(name = {"org.hibernate.resource.jdbc.spi.StatementInspector"})
    @ConditionalOnProperty(
            value = "spring.sleuth.hibernate.inject-tracing-ids-in-queries.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public HibernatePropertiesCustomizer newHibernateTracer(final Tracer tracer) {
        return hibernateProperties -> {
            final String key = "hibernate.session_factory.statement_inspector";
            final TracingStatementInspector tracingStatementInspector =
                    new TracingStatementInspector(tracer);
            hibernateProperties.put(key, tracingStatementInspector);
            final Logger logger = LoggerFactory.getLogger(TracingAutoConfiguration.class);
            logger.info("Hibernate query trace comment injection enabled");
        };
    }

    @Bean
    @ConditionalOnClass(name = {
            "org.springframework.web.servlet.HandlerInterceptor",
            "org.springframework.web.method.HandlerMethod",
            "javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletResponse"
    })
    @ConditionalOnProperty(
            value = "spring.sleuth.web.trace.naming.mvc-method-names-as-span-names.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public WebMvcConfigurer newRenamingMvcTracer(
            final Tracer tracer,
            @Value("${spring.sleuth.web.trace.naming.convert-to-underscores.enabled}")
            final boolean convertToUnderscores) {
        final TraceNamingHandlerInterceptor interceptor = new TraceNamingHandlerInterceptor(
                tracer, convertToUnderscores);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
                final Logger logger = LoggerFactory.getLogger(TracingAutoConfiguration.class);
                logger.info("Spring MVC trace method renaming enabled [underscores={}]",
                        convertToUnderscores);
            }
        };
    }

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
