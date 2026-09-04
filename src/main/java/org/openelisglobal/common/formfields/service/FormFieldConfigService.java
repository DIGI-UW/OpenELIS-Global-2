package org.openelisglobal.common.formfields.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openelisglobal.common.formfields.AdminFormFields;
import org.openelisglobal.common.formfields.FormField;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.stereotype.Service;

@Service
public class FormFieldConfigService {
    private static final Log log = LogFactory.getLog(FormFieldConfigService.class);
    private static final String DEFAULT_CONFIG = "default.json";
    private static final String ADMIN_DEFAULT_CONFIG = "admin_default.json";

    private Map<FormFields.Field, FormField> cachedFields;
    private Map<AdminFormFields.Field, Boolean> cachedAdminFields;

    public Map<FormFields.Field, FormField> getFormFields() {
        if (cachedFields == null) {
            loadFormFields();
        }
        return cachedFields;
    }

    public Map<AdminFormFields.Field, Boolean> getAdminFormFields() {
        if (cachedAdminFields == null) {
            loadAdminFormFields();
        }
        return cachedAdminFields;
    }

    public synchronized void reload() {
        loadFormFields();
        loadAdminFormFields();
    }

    protected String getConfigDir() {
        return System.getenv("OPENELIS_FORMFIELDS_DIR");
    }

    private synchronized void loadFormFields() {
        String configDir = getConfigDir();
        if (configDir == null || configDir.isEmpty()) {
            log.warn("OPENELIS_FORMFIELDS_DIR not set, form fields might not be loaded correctly");
            cachedFields = new HashMap<>();
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            File defaultFile = new File(configDir, DEFAULT_CONFIG);
            Map<FormFields.Field, FormField> fields = new HashMap<>();
            if (defaultFile.exists()) {
                fields = mapper.readValue(defaultFile, new TypeReference<Map<FormFields.Field, FormField>>() {
                });
            }

            String fieldSet = ConfigurationProperties.getInstance().getPropertyValue(Property.FormFieldSet);
            if (fieldSet != null && !fieldSet.isEmpty()) {
                File implementationFile = new File(configDir, fieldSet.toLowerCase() + ".json");
                if (implementationFile.exists()) {
                    Map<FormFields.Field, FormField> overrides = mapper.readValue(implementationFile,
                            new TypeReference<Map<FormFields.Field, FormField>>() {
                            });
                    for (Map.Entry<FormFields.Field, FormField> entry : overrides.entrySet()) {
                        FormFields.Field field = entry.getKey();
                        FormField override = entry.getValue();
                        if (fields.containsKey(field)) {
                            mergeFields(fields.get(field), override);
                        } else {
                            fields.put(field, override);
                        }
                    }
                }
            }
            cachedFields = fields;
        } catch (IOException e) {
            log.error("Error loading form field configurations", e);
            cachedFields = new HashMap<>();
        }
    }

    private synchronized void loadAdminFormFields() {
        String configDir = getConfigDir();
        if (configDir == null || configDir.isEmpty()) {
            cachedAdminFields = new HashMap<>();
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            File defaultFile = new File(configDir, ADMIN_DEFAULT_CONFIG);
            Map<AdminFormFields.Field, Boolean> fields = new HashMap<>();
            if (defaultFile.exists()) {
                fields = mapper.readValue(defaultFile, new TypeReference<Map<AdminFormFields.Field, Boolean>>() {
                });
            }

            String fieldSet = ConfigurationProperties.getInstance().getPropertyValue(Property.FormFieldSet);
            if (fieldSet != null && !fieldSet.isEmpty()) {
                File implementationFile = new File(configDir, fieldSet.toLowerCase() + "_admin.json");
                if (implementationFile.exists()) {
                    Map<AdminFormFields.Field, Boolean> overrides = mapper.readValue(implementationFile,
                            new TypeReference<Map<AdminFormFields.Field, Boolean>>() {
                            });
                    fields.putAll(overrides);
                }
            }
            cachedAdminFields = fields;
        } catch (IOException e) {
            log.error("Error loading admin form field configurations", e);
            cachedAdminFields = new HashMap<>();
        }
    }

    private void mergeFields(FormField base, FormField override) {
        if (override.getInUse() != null) {
            base.setInUse(override.getInUse());
        }
        if (override.getLabelKey() != null) {
            base.setLabelKey(override.getLabelKey());
        }
        if (override.getLabel() != null) {
            base.setLabel(override.getLabel());
        }
        // Always copy required
        base.setRequired(override.getRequired());
    }
}
