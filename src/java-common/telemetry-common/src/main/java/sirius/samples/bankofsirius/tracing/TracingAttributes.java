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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.CONTAINER_ID;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.K8S_CONTAINER_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.K8S_NAMESPACE_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.K8S_POD_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_VERSION;

/**
 * Class that does a best effort to read OpenTelemetry system attributes from
 * the running application and load them into a {@link Attributes} compatible
 * data structure.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md">OpenTelemetry Semantic Conventions</a>
 */
public class TracingAttributes implements Attributes {
    public static final AttributeKey<String> MACHINE_ID = AttributeKey.stringKey("machine.id");
    public static final String MACHINE_ID_FILE_PATH = "/etc/machine-id";
    public static final String PROC_1_CPUSET_FILE_PATH = "/proc/1/cpuset";

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final Pattern POD_NAME_PATTERN = Pattern.compile("(\\w+)-([a-z0-9]{9})-([a-z0-9]{5})");

    private final Attributes inner;
    private final String applicationName;
    private final FileSystem fileSystem;

    public TracingAttributes(final String applicationName) {
        this(applicationName, System.getProperties(), System.getenv(), FileSystems.getDefault());
    }

    public TracingAttributes(final String applicationName,
                             final Properties properties,
                             final Map<String, String> environment,
                             final FileSystem fileSystem) {
        this.applicationName = applicationName;
        this.fileSystem = fileSystem;
        this.inner = buildAttributes(properties, environment);
    }

    protected Attributes buildAttributes(final Properties properties,
                                         final Map<String, String> environment) {
        final AttributesBuilder builder = Attributes.builder();
        addToBuilder(builder, SERVICE_VERSION, serviceVersion(properties, environment));
        addToBuilder(builder, SERVICE_INSTANCE_ID, serviceInstanceId());
        addToBuilder(builder, MACHINE_ID, machineId());
        addToBuilder(builder, CONTAINER_ID, containerId());
        addToBuilder(builder, K8S_POD_NAME, podName(environment));
        addToBuilder(builder, K8S_CONTAINER_NAME, containerName(environment));
        addToBuilder(builder, K8S_NAMESPACE_NAME, namespaceName(environment));
        return builder.build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected <T> void addToBuilder(final AttributesBuilder builder,
                                    final AttributeKey<T> key,
                                    final Optional<T> value) {
        try {
            value.ifPresent(v -> builder.put(key, v));
        } catch (RuntimeException e) {
            Logger logger = LoggerFactory.getLogger(TracingAttributes.class);
            String msg = String.format("Unable to add attribute [key=%s]",
                    key.getKey());
            logger.error(msg, e);
        }
    }

    protected Optional<String> serviceVersion(final Properties properties,
                                              final Map<String, String> environment) {
        // Try to get a version from the VERSION environment variable
        final String envVersion = environment.get("VERSION");
        if (envVersion != null && !envVersion.isEmpty()) {
            return Optional.of(envVersion);
        }
        // Try to get a version from the service.version system property
        final String syspropVersion = properties.getProperty("service.version");
        if (syspropVersion != null && !syspropVersion.isEmpty()) {
            return Optional.of(syspropVersion);
        }

        return Optional.empty();
    }

    protected Optional<String> serviceInstanceId() {
        final Path path = getServiceIdFilePath(fileSystem, applicationName);

        try {
            return readFirstLineFromFile(path);
        } catch (FileNotFoundException | NoSuchFileException e) {
            final Optional<String> serviceInstanceId =
                    createNewServiceIdFile(path);

            Logger logger = LoggerFactory.getLogger(TracingAttributes.class);
            logger.debug("No service instance id file found; created new file [path={}",
                    path);

            return serviceInstanceId;
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(TracingAttributes.class);
            String msg = String.format("Unable to read service instance id [path=%s]",
                    path);
            logger.debug(msg, e);
        }

        return Optional.empty();
    }

    protected Optional<String> createNewServiceIdFile(final Path serviceInstanceIdFilePath) {
        try (OutputStream fileOut = writeDataToPath(serviceInstanceIdFilePath);
             Writer writer = new OutputStreamWriter(fileOut, StandardCharsets.UTF_8.name())) {
            final String serviceInstanceId = UUID.randomUUID().toString().replaceAll("-", "");
            writer.append(serviceInstanceId).append("\n");
            return Optional.of(serviceInstanceId);
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(TracingAttributes.class);
            logger.debug("Unable to write service instance id", e);
        }

        return Optional.empty();
    }

    protected Optional<String> machineId() {
        try {
            return readFirstLineFromFile(MACHINE_ID_FILE_PATH);
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(TracingAttributes.class);
            logger.debug("Unable to read machine id", e);
        }

        return Optional.empty();
    }

    protected Optional<String> podName(final Map<String, String> environment) {
        final String hostnameEnv = environment.get("HOSTNAME");
        if (hostnameEnv != null && !hostnameEnv.isEmpty()
                && POD_NAME_PATTERN.matcher(hostnameEnv).matches()) {
            return Optional.of(hostnameEnv);
        }

        return Optional.empty();
    }

    protected Optional<String> containerId() {
        try {
            return readFirstLineFromFile(PROC_1_CPUSET_FILE_PATH).flatMap(s -> {
                final String containerId = parseContainerIdFromCpuSet(s);
                if (containerId.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(containerId);
            });
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(TracingAttributes.class);
            logger.debug("Unable to read container id", e);
        }

        return Optional.empty();
    }

    protected static String parseContainerIdFromCpuSet(final String cpuSetData) {
        if (cpuSetData == null || cpuSetData.isEmpty()) {
            return "";
        }

        final String delimiter = "/";
        final StringTokenizer tokenizer = new StringTokenizer(cpuSetData, delimiter);
        String nextToken = "";
        while (tokenizer.hasMoreTokens()) {
            nextToken = tokenizer.nextToken();
        }

        return nextToken;
    }

    protected Optional<String> containerName(final Map<String, String> environment) {
        final Optional<String> maybePodName = podName(environment);
        return maybePodName.flatMap(podName -> {
            final Matcher matcher = POD_NAME_PATTERN.matcher(podName);
            if (!matcher.matches()) {
                return Optional.empty();
            }
            final String containerName = matcher.group(1);
            if (!containerName.isEmpty()) {
                return Optional.of(containerName);
            }

            return Optional.empty();
        });
    }

    protected Optional<String> namespaceName(final Map<String, String> environment) {
        final String namespaceNameEnv = environment.get("NAMESPACE");
        if (namespaceNameEnv != null && !namespaceNameEnv.isEmpty()) {
            return Optional.of(namespaceNameEnv);
        }

        return Optional.empty();
    }

    protected Optional<String> readFirstLineFromFile(final String filePath) throws IOException {
        final Path path = fileSystem.getPath(filePath);
        return readFirstLineFromFile(path);
    }

    protected Optional<String> readFirstLineFromFile(final Path path) throws IOException {
        if (path == null) {
            throw new NullPointerException("path must not be null");
        }

        try (InputStream fileStream = readDataFromPath(path);
             Scanner scanner = new Scanner(fileStream, StandardCharsets.UTF_8.name())) {
            if (scanner.hasNextLine()) {
                final String firstLine = scanner.nextLine();
                if (firstLine != null && !firstLine.isEmpty()) {
                    return Optional.of(firstLine);
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        return Optional.empty();
    }

    protected InputStream readDataFromPath(final Path path) throws IOException {
        final FileSystemProvider fileSystemProvider = fileSystem.provider();
        return fileSystemProvider.newInputStream(path);
    }

    protected OutputStream writeDataToPath(final Path path) throws IOException {
        final FileSystemProvider fileSystemProvider = fileSystem.provider();
        return fileSystemProvider.newOutputStream(path);
    }

    @Override
    public <T> T get(final AttributeKey<T> key) {
        return inner.get(key);
    }

    @Override
    public void forEach(final BiConsumer<? super AttributeKey<?>, ? super Object> consumer) {
        inner.forEach(consumer);
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public Map<AttributeKey<?>, Object> asMap() {
        return inner.asMap();
    }

    @Override
    public AttributesBuilder toBuilder() {
        return inner.toBuilder();
    }

    public static Path getServiceIdFilePath(final FileSystem fileSystem,
                                              final String applicationName) {
        String filename = String.format("%s-service-instance-id", applicationName);
        return fileSystem.getPath(TMP_DIR, filename);
    }
}
