package org.openelisglobal.module.runtime.spring;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.module.api.ModuleConfiguration;
import org.openelisglobal.module.api.ModuleValidationException;
import org.openelisglobal.module.api.manifest.ConfigurationSection;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.runtime.ModuleSystemProperties;

/**
 * {@code modules.<id>.<key>} properties with the manifest's types and defaults.
 * Values are read live from the environment, so a property set in the mounted
 * properties directory or the environment wins over the manifest default, which
 * is the core's own precedence for configuration.
 */
public final class ModuleConfigurationImpl implements ModuleConfiguration {

    private final String moduleId;
    private final ModuleSystemProperties properties;
    private final Map<String, ConfigurationSection.PropertyDeclaration> declared = new LinkedHashMap<>();

    public ModuleConfigurationImpl(ModuleManifest manifest, ModuleSystemProperties properties) {
        this.moduleId = manifest.getId();
        this.properties = properties;
        if (manifest.getConfiguration() != null) {
            for (ConfigurationSection.PropertyDeclaration property : manifest.getConfiguration().getProperties()) {
                declared.put(property.getKey(), property);
            }
        }
    }

    @Override
    public Optional<String> get(String key) {
        Optional<String> value = properties.moduleProperty(moduleId, key);
        if (value.isPresent()) {
            return value;
        }
        ConfigurationSection.PropertyDeclaration declaration = declared.get(key);
        return declaration == null ? Optional.empty() : Optional.ofNullable(declaration.getDefaultValue());
    }

    @Override
    public String getString(String key, String fallback) {
        return get(key).orElse(fallback);
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.get().trim()));
        } catch (NumberFormatException e) {
            throw new ModuleValidationException(
                    "modules." + moduleId + "." + key + " must be an integer, not '" + value.get() + "'");
        }
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return get(key).map(value -> {
            String trimmed = value.trim().toLowerCase();
            if (trimmed.equals("true") || trimmed.equals("false")) {
                return Boolean.valueOf(trimmed);
            }
            throw new ModuleValidationException(
                    "modules." + moduleId + "." + key + " must be true or false, not '" + value + "'");
        });
    }

    @Override
    public String require(String key) {
        return get(key).orElseThrow(() -> new ModuleValidationException(
                "modules." + moduleId + "." + key + " is required and has no value or default"));
    }

    @Override
    public int requireInteger(String key, int min, int max) {
        int value = getInteger(key).orElseThrow(() -> new ModuleValidationException(
                "modules." + moduleId + "." + key + " is required and has no value or default"));
        if (value < min || value > max) {
            throw new ModuleValidationException(
                    "modules." + moduleId + "." + key + " must be between " + min + " and " + max + ", not " + value);
        }
        return value;
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : declared.keySet()) {
            get(key).ifPresent(value -> values.put(key, value));
        }
        return values;
    }

    /**
     * The declared properties marked {@code exposeToFrontend}, with their effective
     * values.
     */
    public Map<String, String> exposedToFrontend() {
        Map<String, String> values = new LinkedHashMap<>();
        for (ConfigurationSection.PropertyDeclaration property : declared.values()) {
            if (property.isExposeToFrontend()) {
                get(property.getKey()).ifPresent(value -> values.put(property.getKey(), value));
            }
        }
        return values;
    }
}
