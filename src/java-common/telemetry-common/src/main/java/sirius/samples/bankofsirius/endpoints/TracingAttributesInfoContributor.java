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
package sirius.samples.bankofsirius.endpoints;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

@Component
public class TracingAttributesInfoContributor implements InfoContributor {
    private final Supplier<Resource> tracingResourceSupplier;

    public TracingAttributesInfoContributor(@Qualifier("applicationDetails")
                                            final Supplier<Resource> tracingResourceSupplier) {
        this.tracingResourceSupplier = tracingResourceSupplier;
    }

    @Override
    public void contribute(final Info.Builder builder) {
        final Resource resource = tracingResourceSupplier.get();
        final Attributes attributes = resource.getAttributes();
        final Map<String, Object> attrMap = new TreeMap<>();
        attributes.forEach((k, o) -> attrMap.put(k.getKey(), o));

        builder.withDetail("trace.attributes", attrMap);
    }
}
