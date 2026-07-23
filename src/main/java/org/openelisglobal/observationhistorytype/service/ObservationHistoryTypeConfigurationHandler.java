package org.openelisglobal.observationhistorytype.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for loading observation history type configuration files. Supports
 * CSV format with observation types and their localized descriptions.
 *
 * Expected CSV format: typeName,description,localization:en,localization:fr
 * envWorkflowType,Workflow type: clinical or environmental,Workflow Type,Type
 * de flux envCollectionSiteDescription,Description of collection
 * site,Collection Site Description,Description du site de collecte
 *
 * Notes: - First line is the header (required) - typeName is the unique
 * identifier for the observation type - description is the default description
 * - localization:xx columns provide translations for UI display
 */
@Component
public class ObservationHistoryTypeConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    @Autowired
    private LocalizationService localizationService;

    @Override
    public String getDomainName() {
        return "observation-history-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 50; // Load early as other configurations may depend on observation types
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Observation history type configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        int typeNameIndex = findColumnIndex(headers, "typeName");
        int descriptionIndex = findColumnIndex(headers, "description");

        // Detect localization columns (localization:en, localization:fr, etc.)
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        String line;
        int lineNumber = 1;
        int createdCount = 0;
        int skippedCount = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);

                String typeName = getValueSafe(values, typeNameIndex);
                String description = getValueSafe(values, descriptionIndex);

                if (typeName == null || typeName.trim().isEmpty()) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                            "Skipping line " + lineNumber + " in " + fileName + ": typeName is required");
                    continue;
                }

                // Check if observation type already exists
                ObservationHistoryType existingType = observationHistoryTypeService.getByName(typeName);
                if (existingType != null) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "processConfiguration",
                            "Observation history type '" + typeName + "' already exists, skipping");
                    skippedCount++;
                    continue;
                }

                // Create new observation history type
                ObservationHistoryType newType = new ObservationHistoryType();
                newType.setTypeName(typeName);
                newType.setDescription(description != null ? description : typeName);
                newType.setSysUserId("1"); // System user

                observationHistoryTypeService.insert(newType);
                createdCount++;

                LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                        "Created observation history type: " + typeName);

                // Handle localizations for UI display
                if (!localizationColumns.isEmpty()) {
                    createLocalizations(typeName, values, localizationColumns);
                }

            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
                throw e;
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully processed " + fileName + ": created " + createdCount + ", skipped " + skippedCount);
    }

    private void createLocalizations(String typeName, String[] values, Map<String, Integer> localizationColumns) {
        // Create a localization entry for this observation type's display name
        // The localization key follows the pattern: observationhistorytype.{typeName}
        String localizationKey = "observationhistorytype." + typeName;

        // Build localized values map
        Map<String, String> localizedValues = new HashMap<>();
        String fallbackValue = typeName; // Use typeName as fallback

        for (Map.Entry<String, Integer> entry : localizationColumns.entrySet()) {
            String locale = entry.getKey();
            int columnIndex = entry.getValue();
            String localizedValue = getValueSafe(values, columnIndex);

            if (localizedValue != null && !localizedValue.trim().isEmpty()) {
                localizedValues.put(locale, localizedValue.trim());
                if ("en".equals(locale)) {
                    fallbackValue = localizedValue.trim();
                }
            }
        }

        // Create localization with all locale values
        // Note: We only create localizations for newly created observation types,
        // so duplicates are prevented by the observation type existence check
        if (!localizedValues.isEmpty()) {
            Localization localization = new Localization();
            localization.setDescription(localizationKey);
            localization.setEnglish(localizedValues.getOrDefault("en", fallbackValue));
            localization.setFrench(localizedValues.getOrDefault("fr", fallbackValue));
            localization.setSysUserId("1");

            localizationService.insert(localization);

            LogEvent.logDebug(this.getClass().getSimpleName(), "createLocalizations",
                    "Created localization for observation type: " + typeName);
        }
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasTypeName = false;

        for (String header : headers) {
            if ("typeName".equalsIgnoreCase(header.trim())) {
                hasTypeName = true;
                break;
            }
        }

        if (!hasTypeName) {
            throw new IllegalArgumentException(
                    "Observation history type configuration file " + fileName + " must have a 'typeName' column");
        }
    }

    private Map<String, Integer> detectLocalizationColumns(String[] headers) {
        Map<String, Integer> localizationColumns = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            if (header.startsWith(LOCALIZATION_COLUMN_PREFIX)) {
                String locale = header.substring(LOCALIZATION_COLUMN_PREFIX.length());
                localizationColumns.put(locale, i);
            }
        }
        return localizationColumns;
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private String getValueSafe(String[] values, int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index];
        return value != null ? value.trim() : null;
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV parsing - handles quoted values
        java.util.List<String> values = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }
}
