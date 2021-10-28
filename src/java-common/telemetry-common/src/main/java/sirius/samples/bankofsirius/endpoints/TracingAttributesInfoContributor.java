package sirius.samples.bankofsirius.endpoints;

import com.google.common.collect.ImmutableMap;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import sirius.samples.bankofsirius.tracing.TracingAttributes;

@Component
public class TracingAttributesInfoContributor implements InfoContributor {
    private final TracingAttributes tracingAttributes;

    public TracingAttributesInfoContributor(final TracingAttributes tracingAttributes) {
        this.tracingAttributes = tracingAttributes;
    }

    @Override
    public void contribute(final Info.Builder builder) {
        final ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.builder();
        tracingAttributes.forEach((k, o) -> mapBuilder.put(k.getKey(), o));

        builder.withDetail("trace.attributes", mapBuilder.build());
    }
}
