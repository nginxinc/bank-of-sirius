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
