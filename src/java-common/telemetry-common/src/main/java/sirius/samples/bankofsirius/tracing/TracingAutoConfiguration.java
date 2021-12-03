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

@Configuration
@ConditionalOnProperty(value = "spring.sleuth.enabled", havingValue = "true")
@ConditionalOnClass(name = {"org.springframework.cloud.sleuth.Tracer"})
public class TracingAutoConfiguration {
    {
        String tracingEnabled = System.getenv().getOrDefault("ENABLE_TRACING", "true");

        if ("true".equalsIgnoreCase(tracingEnabled)) {
            String otelEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
            if (otelEndpoint == null) {
                final String msg = "OTEL_EXPORTER_OTLP_ENDPOINT environment variable "
                        + "must be set if ENABLE_TRACING=true";
                throw new IllegalArgumentException(msg);
            }
        }
    }

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
}
