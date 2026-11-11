package org.openelisglobal.module.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;

/**
 * The module system's own settings, read from the application's property
 * sources (classpath defaults, the mounted properties directory,
 * {@code /run/secrets}, the environment). Everything has a default, so an
 * installation without modules needs none of them.
 *
 * <table>
 * <tr>
 * <th>Property</th>
 * <th>Default</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>{@code modules.directory}</td>
 * <td>{@code /var/lib/openelis-global/modules}</td>
 * <td>Where module JARs are read from</td>
 * </tr>
 * <tr>
 * <td>{@code modules.cacheDirectory}</td>
 * <td>{@code /var/lib/openelis-global/modules-cache}</td>
 * <td>Where bundled libraries are extracted</td>
 * </tr>
 * <tr>
 * <td>{@code modules.dataDirectory}</td>
 * <td>{@code /var/lib/openelis-global/modules-data}</td>
 * <td>Per-module data directories</td>
 * </tr>
 * <tr>
 * <td>{@code modules.disabled}</td>
 * <td>empty</td>
 * <td>Comma-separated ids to skip</td>
 * </tr>
 * <tr>
 * <td>{@code modules.<id>.enabled}</td>
 * <td>{@code true}</td>
 * <td>Per-module switch</td>
 * </tr>
 * <tr>
 * <td>{@code modules.<id>.allowDowngrade}</td>
 * <td>{@code false}</td>
 * <td>Accept a JAR older than the recorded version</td>
 * </tr>
 * <tr>
 * <td>{@code modules.startTimeoutSeconds}</td>
 * <td>{@code 60}</td>
 * <td>Limit for a module's {@code onStart}</td>
 * </tr>
 * <tr>
 * <td>{@code modules.lock}</td>
 * <td>{@code <modules.directory>/modules.lock}</td>
 * <td>Digest allowlist; strict mode when the file exists</td>
 * </tr>
 * <tr>
 * <td>{@code spi.<name>.provider}</td>
 * <td>unset</td>
 * <td>The active provider of a single-implementation extension point</td>
 * </tr>
 * </table>
 */
public final class ModuleSystemProperties {

    public static final String DIRECTORY = "modules.directory";
    public static final String CACHE_DIRECTORY = "modules.cacheDirectory";
    public static final String DATA_DIRECTORY = "modules.dataDirectory";
    public static final String DISABLED = "modules.disabled";
    public static final String START_TIMEOUT_SECONDS = "modules.startTimeoutSeconds";
    public static final String LOCK_FILE = "modules.lock";

    public static final String DEFAULT_DIRECTORY = "/var/lib/openelis-global/modules";
    public static final String DEFAULT_CACHE_DIRECTORY = "/var/lib/openelis-global/modules-cache";
    public static final String DEFAULT_DATA_DIRECTORY = "/var/lib/openelis-global/modules-data";
    public static final int DEFAULT_START_TIMEOUT_SECONDS = 60;

    private final Environment environment;

    public ModuleSystemProperties(Environment environment) {
        this.environment = environment;
    }

    public Path directory() {
        return Paths.get(environment.getProperty(DIRECTORY, DEFAULT_DIRECTORY));
    }

    public Path cacheDirectory() {
        return Paths.get(environment.getProperty(CACHE_DIRECTORY, DEFAULT_CACHE_DIRECTORY));
    }

    public Path dataDirectory() {
        return Paths.get(environment.getProperty(DATA_DIRECTORY, DEFAULT_DATA_DIRECTORY));
    }

    /** The digest allowlist; strict mode is on when the file exists. */
    public Path lockFile() {
        String configured = environment.getProperty(LOCK_FILE);
        return configured == null || configured.isBlank() ? directory().resolve("modules.lock") : Paths.get(configured);
    }

    public Set<String> disabledIds() {
        String value = environment.getProperty(DISABLED, "");
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isEnabled(String moduleId) {
        if (disabledIds().contains(moduleId)) {
            return false;
        }
        return environment.getProperty("modules." + moduleId + ".enabled", Boolean.class, Boolean.TRUE);
    }

    public boolean allowDowngrade(String moduleId) {
        return environment.getProperty("modules." + moduleId + ".allowDowngrade", Boolean.class, Boolean.FALSE);
    }

    public int startTimeoutSeconds() {
        return environment.getProperty(START_TIMEOUT_SECONDS, Integer.class, DEFAULT_START_TIMEOUT_SECONDS);
    }

    /**
     * The module id configured as the active provider of a single-implementation
     * extension point.
     */
    public Optional<String> selectedProvider(String extensionPointName) {
        String value = environment.getProperty("spi." + extensionPointName + ".provider");
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    /** The raw value of a {@code modules.<id>.<key>} property. */
    public Optional<String> moduleProperty(String moduleId, String key) {
        String value = environment.getProperty("modules." + moduleId + "." + key);
        return value == null ? Optional.empty() : Optional.of(value);
    }
}
