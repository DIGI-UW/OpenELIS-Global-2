package org.openelisglobal.module.api;

import java.util.Map;
import java.util.Optional;

/**
 * Typed access to a module's configuration. Values come from the application's
 * property sources under {@code modules.<id>.<key>} (classpath defaults,
 * {@code /run/secrets}, the mounted properties directory and the environment);
 * the manifest's {@code configuration.properties} supplies types and defaults.
 */
public interface ModuleConfiguration {

    /**
     * The raw value of a key, the manifest default when the property is unset,
     * empty when neither exists.
     */
    Optional<String> get(String key);

    /**
     * The value of a key, or the fallback when the property is unset and the
     * manifest has no default.
     */
    String getString(String key, String fallback);

    Optional<Integer> getInteger(String key);

    Optional<Boolean> getBoolean(String key);

    /**
     * The value of a required key.
     *
     * @throws ModuleValidationException when the key is unset and has no default
     */
    String require(String key);

    /**
     * The value of a required integer key within a range.
     *
     * @throws ModuleValidationException when the key is unset, not an integer or
     *                                   outside the range
     */
    int requireInteger(String key, int min, int max);

    /**
     * Every key the manifest declares with its effective value (unset keys without
     * a default are absent).
     */
    Map<String, String> asMap();
}
