package org.openelisglobal.module.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.openelisglobal.module.api.manifest.BackendSection;
import org.openelisglobal.module.api.manifest.ConfigurationSection;
import org.openelisglobal.module.api.manifest.ModuleManifest;

/**
 * Reads {@code META-INF/openelis-module.json} into the {@link ModuleManifest}
 * model. The two manifest keys that are Java keywords ({@code class},
 * {@code default}) are mapped through Jackson mix-ins so that the API model
 * stays free of Jackson annotations.
 */
public final class ManifestReader {

    /** Where the manifest lives inside a module JAR. */
    public static final String MANIFEST_ENTRY = "META-INF/openelis-module.json";

    private final ObjectMapper mapper;

    public ManifestReader() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addMixIn(BackendSection.ExtensionDeclaration.class, ExtensionDeclarationMixin.class);
        mapper.addMixIn(ConfigurationSection.PropertyDeclaration.class, PropertyDeclarationMixin.class);
    }

    /** Parses the manifest text into the JSON tree the schema validator checks. */
    public JsonNode readTree(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ManifestException("manifest is not valid JSON: " + e.getOriginalMessage(), e);
        }
    }

    /** Binds an already validated JSON tree to the model. */
    public ModuleManifest bind(JsonNode tree) {
        try {
            return mapper.treeToValue(tree, ModuleManifest.class);
        } catch (IOException e) {
            throw new ManifestException("manifest cannot be bound to the manifest model: " + e.getMessage(), e);
        }
    }

    /**
     * Reads and binds a manifest without schema validation; the registry validates
     * first.
     */
    public ModuleManifest read(String json) {
        return bind(readTree(json));
    }

    public ModuleManifest read(InputStream in) {
        try {
            return read(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Serialises a manifest (for the registry endpoint and the admin page). */
    public String write(ModuleManifest manifest) {
        try {
            return mapper.writeValueAsString(manifest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    abstract static class ExtensionDeclarationMixin {
        @JsonProperty("class")
        abstract String getClassName();

        @JsonProperty("class")
        abstract void setClassName(String className);
    }

    abstract static class PropertyDeclarationMixin {
        @JsonProperty("default")
        abstract String getDefaultValue();

        @JsonProperty("default")
        abstract void setDefaultValue(String defaultValue);
    }
}
