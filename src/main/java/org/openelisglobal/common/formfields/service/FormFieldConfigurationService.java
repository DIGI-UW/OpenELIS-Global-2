/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.formfields.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.formfields.AdminFormFields;
import org.openelisglobal.common.formfields.FormField;
import org.openelisglobal.common.formfields.FormFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service to load form field configurations from file-based configuration
 * directory. Reads configuration files from
 * /var/lib/openelis-global/formfields/domains/{domain}/
 */
@Component
public class FormFieldConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormFieldConfigurationService.class);

    private static final String DEFAULT_CONFIG_BASE_PATH = "/var/lib/openelis-global/formfields";
    private static final String DOMAINS_DIR = "domains";
    private static final String DEFAULT_DIR = "default";
    private static final String FORM_FIELDS_FILE = "form-fields.properties";
    private static final String ADMIN_FORM_FIELDS_FILE = "admin-form-fields.properties";

    private final String configBasePath;

    public FormFieldConfigurationService() {
        this(DEFAULT_CONFIG_BASE_PATH);
    }

    public FormFieldConfigurationService(String configBasePath) {
        this.configBasePath = configBasePath;
    }

    /**
     * Loads form fields configuration for a given domain. Returns only
     * domain-specific overrides (not merged with defaults). The calling code is
     * responsible for merging with defaults.
     *
     * @param domainName The domain name (e.g., "CI_GENERAL", "KENYA")
     * @return Map of field configuration overrides, or empty map if not found
     */
    public Map<FormFields.Field, FormField> loadFormFields(String domainName) {
        if (GenericValidator.isBlankOrNull(domainName)) {
            return new HashMap<>();
        }
        // Return only domain-specific overrides
        return loadFormFieldsFromFile(domainName, FORM_FIELDS_FILE);
    }

    /**
     * Loads admin form fields configuration for a given domain. Returns only
     * domain-specific overrides (not merged with defaults). The calling code is
     * responsible for merging with defaults.
     *
     * @param domainName The domain name (e.g., "CI_GENERAL", "KENYA")
     * @return Map of admin field configuration overrides, or empty map if not found
     */
    public Map<AdminFormFields.Field, Boolean> loadAdminFormFields(String domainName) {
        if (GenericValidator.isBlankOrNull(domainName)) {
            return new HashMap<>();
        }
        // Return only domain-specific overrides
        return loadAdminFormFieldsFromFile(domainName, ADMIN_FORM_FIELDS_FILE);
    }

    /**
     * Loads form fields from a properties file. Format:
     * FieldName=inUse,required,labelKey,label Examples: PatientPhone=true,false
     * PatientPhone=true,true,patient.phone.label
     * PatientPhone=true,true,patient.phone.label,Phone Number
     */
    private Map<FormFields.Field, FormField> loadFormFieldsFromFile(String domainName, String fileName) {
        Map<FormFields.Field, FormField> fields = new HashMap<>();
        Path filePath = getConfigFilePath(domainName, fileName);

        if (!Files.exists(filePath)) {
            if (domainName != null) {
                LOGGER.debug("Form fields config file not found for domain {}: {}", domainName, filePath);
            }
            return fields;
        }

        Properties properties = new Properties(); // to read key-value pairs

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            properties.load(reader); // load the properties file into properties object

            for (String key : properties.stringPropertyNames()) {
                try {
                    FormFields.Field field = FormFields.Field.valueOf(key);
                    String value = properties.getProperty(key).trim();

                    FormField formField = parseFormFieldValue(value);
                    fields.put(field, formField);

                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown form field: {} in file {}", key, filePath);
                }
            }

            LOGGER.info("Loaded {} form fields from {}", fields.size(), filePath);

        } catch (IOException e) {
            LOGGER.error("Error loading form fields from file: " + filePath, e);
        }

        return fields;
    }

    /**
     * Loads admin form fields from a properties file. Format: FieldName=true/false
     * Example: DictionaryMenu=true
     */
    private Map<AdminFormFields.Field, Boolean> loadAdminFormFieldsFromFile(String domainName, String fileName) {
        Map<AdminFormFields.Field, Boolean> fields = new HashMap<>();
        Path filePath = getConfigFilePath(domainName, fileName);

        if (!Files.exists(filePath)) {
            if (domainName != null) {
                LOGGER.debug("Admin form fields config file not found for domain {}: {}", domainName, filePath);
            }
            return fields;
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            properties.load(reader);

            for (String key : properties.stringPropertyNames()) {
                try {
                    AdminFormFields.Field field = AdminFormFields.Field.valueOf(key);
                    String value = properties.getProperty(key).trim().toLowerCase();
                    Boolean boolValue = "true".equals(value) || "1".equals(value) || "yes".equals(value);
                    fields.put(field, boolValue);

                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown admin form field: {} in file {}", key, filePath);
                }
            }

            LOGGER.info("Loaded {} admin form fields from {}", fields.size(), filePath);

        } catch (IOException e) {
            LOGGER.error("Error loading admin form fields from file: " + filePath, e);
        }

        return fields;
    }

    /**
     * Parses a form field value string. Format: inUse,required,labelKey,label All
     * parts except inUse are optional.
     */
    private FormField parseFormFieldValue(String value) {
        if (GenericValidator.isBlankOrNull(value)) {
            return new FormField();
        }

        String[] parts = value.split(",", -1);
        FormField field = new FormField();

        // inUse (required)
        if (parts.length > 0 && !GenericValidator.isBlankOrNull(parts[0])) {
            String inUseStr = parts[0].trim().toLowerCase();
            field.setInUse("true".equals(inUseStr) || "1".equals(inUseStr) || "yes".equals(inUseStr));
        }

        // required (optional)
        if (parts.length > 1 && !GenericValidator.isBlankOrNull(parts[1])) {
            String requiredStr = parts[1].trim().toLowerCase();
            field.setRequired("true".equals(requiredStr) || "1".equals(requiredStr) || "yes".equals(requiredStr));
        }

        // labelKey (optional)
        if (parts.length > 2 && !GenericValidator.isBlankOrNull(parts[2])) {
            field.setLabelKey(parts[2].trim());
        }

        // label (optional, overrides labelKey if provided)
        if (parts.length > 3 && !GenericValidator.isBlankOrNull(parts[3])) {
            field.setLabel(parts[3].trim());
        }

        return field;
    }

    /**
     * Gets the configuration file path for a domain. If domainName is null, returns
     * the default config path. Domain names are normalized: converted to lowercase
     * and underscores are preserved.
     */
    private Path getConfigFilePath(String domainName, String fileName) {
        if (domainName == null) {
            // Default configuration
            return Paths.get(configBasePath, DEFAULT_DIR, fileName);
        } else {
            // Domain-specific configuration (case-insensitive domain name matching)
            // Convert to lowercase for directory name, preserve underscores
            String normalizedDomain = domainName.toLowerCase().replace("-", "_");
            return Paths.get(configBasePath, DOMAINS_DIR, normalizedDomain, fileName);
        }
    }

    /**
     * Checks if a configuration file exists for the given domain.
     */
    public boolean hasDomainConfiguration(String domainName) {
        if (GenericValidator.isBlankOrNull(domainName)) {
            return false;
        }
        Path formFieldsPath = getConfigFilePath(domainName, FORM_FIELDS_FILE);
        Path adminFormFieldsPath = getConfigFilePath(domainName, ADMIN_FORM_FIELDS_FILE);
        return Files.exists(formFieldsPath) || Files.exists(adminFormFieldsPath);
    }
}
