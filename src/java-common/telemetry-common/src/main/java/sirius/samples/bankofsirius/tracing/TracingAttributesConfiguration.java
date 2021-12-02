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
