package org.openelisglobal.module.api.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * The module's configuration surface: the {@code modules.<id>.<key>} properties
 * it reads, with their types and defaults. Values come from the application's
 * property sources; a property marked {@code exposeToFrontend} is also served
 * to the module's pages through the registry.
 */
public class ConfigurationSection {

    private List<PropertyDeclaration> properties = new ArrayList<>();

    public List<PropertyDeclaration> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDeclaration> properties) {
        this.properties = properties == null ? new ArrayList<>() : properties;
    }

    public static class PropertyDeclaration {

        private String key;
        private String type = "string";
        private String defaultValue;
        private String description;
        private boolean exposeToFrontend;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        /** {@code string}, {@code integer}, {@code boolean} or {@code url}. */
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        /**
         * Serialised as {@code default} in the manifest; {@code null} when the property
         * has no default.
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isExposeToFrontend() {
            return exposeToFrontend;
        }

        public void setExposeToFrontend(boolean exposeToFrontend) {
            this.exposeToFrontend = exposeToFrontend;
        }
    }
}
