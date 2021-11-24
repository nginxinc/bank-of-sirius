package sirius.samples.bankofsirius.endpoints;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

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
        final ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.builder();
        final Resource resource = tracingResourceSupplier.get();
        final Attributes attributes = resource.getAttributes();
        attributes.forEach((k, o) -> mapBuilder.put(k.getKey(), o));

        builder.withDetail("trace.attributes", mapBuilder.build());
    }
}
