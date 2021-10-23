package sirius.samples.bankofsirius.tracing;

import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TracingAttributesTest {
    public static final String APP_NAME = "unit-test";

    private static TracingAttributes instance(final Properties properties) {
        return new TracingAttributes(APP_NAME, properties, Collections.emptyMap(),
                path -> stringToInputStream(""));
    }

    private static TracingAttributes instance(final Map<String, String> environment) {
        final Properties properties = new Properties();
        return new TracingAttributes(APP_NAME, properties, environment,
                path -> stringToInputStream(""));
    }

    private static TracingAttributes instance(final String fileContentsToReturn) {
        final Properties properties = new Properties();
        return new TracingAttributes(APP_NAME, properties, Collections.emptyMap(),
                path -> stringToInputStream(fileContentsToReturn));
    }

    private static InputStream stringToInputStream(final String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readServiceVersionFromEnv() {
        final String version = "1.2.3";
        final Map<String, String> env = new HashMap<>();
        env.put("VERSION", version);
        final TracingAttributes attributes = instance(env);
        final String actual = attributes.get(ResourceAttributes.SERVICE_VERSION);
        assertEquals(version, actual);
    }

    @Test
    public void readServiceVersionFromSystemProperty() {
        final String version = "1.2.9";
        final Properties properties = new Properties();
        properties.put("service.version", version);
        final TracingAttributes attributes = instance(properties);
        final String actual = attributes.get(ResourceAttributes.SERVICE_VERSION);
        assertEquals(version, actual);
    }

    @Test
    public void readServiceInstanceIdK8s() {
        final String expected = new UUID(0L, 0L).toString();
        final TracingAttributes attributes = instance(expected);
        final String actual = attributes.get(ResourceAttributes.SERVICE_INSTANCE_ID);
        assertEquals(expected, actual);
    }

    @Test
    void readContainerIdNone() {
        final String rawCpuSetInfo = "/";
        final TracingAttributes attributes = instance(rawCpuSetInfo);
        final String actual = attributes.get(ResourceAttributes.CONTAINER_ID);
        assertNull(actual);
    }

    @Test
    void readContainerDocker() {
        final String rawCpuSetInfo = "/docker/1bfde5a828d33da2aeb5aab0d340f3a032b46bc1d0ca5765c502828b6f148c91";
        final String expected = "1bfde5a828d33da2aeb5aab0d340f3a032b46bc1d0ca5765c502828b6f148c91";
        final TracingAttributes attributes = instance(rawCpuSetInfo);
        final String actual = attributes.get(ResourceAttributes.CONTAINER_ID);
        assertEquals(expected, actual);
    }

    @Test
    void readContainerIdK8s() {
        final String rawCpuSetInfo = "/kubepods/besteffort/pod72832d24-7655-487c-8b85-3f01844639a9/5046b447f1dacb1849cff896e47e3d9b1aa5bcfd513a98e382eae3343e6ab5c2";
        final String expected = "5046b447f1dacb1849cff896e47e3d9b1aa5bcfd513a98e382eae3343e6ab5c2";
        final TracingAttributes attributes = instance(rawCpuSetInfo);
        final String actual = attributes.get(ResourceAttributes.CONTAINER_ID);
        assertEquals(expected, actual);
    }

    @Test
    void readMachineId() {
        final String machineId = "9bcc0df29af9454298607489a54040e2";
        final TracingAttributes attributes = instance(machineId);
        final String actual = attributes.get(TracingAttributes.MACHINE_ID);
        assertEquals(machineId, actual);
    }

    @Test
    void readPodNameValid() {
        final String hostnameThatIsPodName = "fooservice-b55497fc6-x9f56";
        final Map<String, String> env = new HashMap<>();
        env.put("HOSTNAME", hostnameThatIsPodName);
        final TracingAttributes attributes = instance(env);
        final String actual = attributes.get(ResourceAttributes.K8S_POD_NAME);
        assertEquals(hostnameThatIsPodName, actual);
    }

    @Test
    void readPodNameInvalid() {
        final String hostnameThatIsNotPodName = "generic-hostname";
        final Map<String, String> env = new HashMap<>();
        env.put("HOSTNAME", hostnameThatIsNotPodName);
        final TracingAttributes attributes = instance(env);
        final String actual = attributes.get(ResourceAttributes.K8S_POD_NAME);
        assertNull(actual);
    }

    @Test
    void readContainerNameValid() {
        final String hostnameThatIsPodName = "fooservice-b55497fc6-x9f56";
        final Map<String, String> env = new HashMap<>();
        env.put("HOSTNAME", hostnameThatIsPodName);
        final TracingAttributes attributes = instance(env);
        final String actual = attributes.get(ResourceAttributes.K8S_CONTAINER_NAME);
        final String expected = "fooservice";
        assertEquals(expected, actual);
    }

    @Test
    void readContainerNameInvalid() {
        final String hostnameThatIsNotPodName = "generic-hostname";
        final Map<String, String> env = new HashMap<>();
        env.put("HOSTNAME", hostnameThatIsNotPodName);
        final TracingAttributes attributes = instance(env);
        final String actual = attributes.get(ResourceAttributes.K8S_CONTAINER_NAME);
        assertNull(actual);
    }
}
