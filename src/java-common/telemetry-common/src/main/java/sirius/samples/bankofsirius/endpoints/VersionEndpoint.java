package sirius.samples.bankofsirius.endpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import sirius.samples.bankofsirius.tracing.TracingAttributes;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_VERSION;

@Component
@Endpoint(id = "version")
public class VersionEndpoint {
    private final ResponseEntity<String> response;

    @Autowired
    public VersionEndpoint(final TracingAttributes tracingAttributes) {
        final String serviceVersion = tracingAttributes.get(SERVICE_VERSION);
        final String version;
        if (serviceVersion == null || serviceVersion.isEmpty()) {
            version = "unknown";
        } else {
            version = serviceVersion;
        }

        response = new ResponseEntity<>(version, headers(), HttpStatus.OK);
    }

    @ReadOperation
    public ResponseEntity<String> version() {
        return this.response;
    }

    private static MultiValueMap<String, String> headers() {
        final MultiValueMap<String, String> headers =
                new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        return headers;
    }
}
