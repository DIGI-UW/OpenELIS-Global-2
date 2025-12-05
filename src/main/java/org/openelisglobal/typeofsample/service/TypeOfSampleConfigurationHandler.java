package org.openelisglobal.typeofsample.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading sample type (TypeOfSample) configuration files. Supports
 * CSV format for defining sample types.
 *
 * Expected CSV format:
 * description,localAbbreviation,domain,isActive,sortOrder,englishName,frenchName
 * Whole Blood,WB,H,Y,1,Whole Blood,Sang Total Serum,SER,H,Y,2,Serum,Sérum
 * Plasma,PLS,H,Y,3,Plasma,Plasma Urine,UR,H,Y,4,Urine,Urine
 *
 * Notes: - First line is the header (required) - description and
 * localAbbreviation are required fields - domain defaults to "H" (Human) if not
 * specified - isActive defaults to "Y" if not specified - sortOrder is optional
 * (auto-assigned if not provided) - englishName is used for localization
 * (defaults to description if not provided) - frenchName is used for French
 * localization (defaults to englishName if not provided) - Existing sample
 * types with matching localAbbreviation and domain will be updated
 */
@Component
public class TypeOfSampleConfigurationHandler implements DomainConfigurationHandler {

    private static final String DEFAULT_DOMAIN = "H"; // Human

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private LocalizationService localizationService;

    @Override
    public String getDomainName() {
        return "sample-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 100; // Base entity - load early
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Sample type configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int descriptionIndex = findColumnIndex(headers, "description");
        int localAbbreviationIndex = findColumnIndex(headers, "localAbbreviation");
        int domainIndex = findColumnIndex(headers, "domain");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int englishNameIndex = findColumnIndex(headers, "englishName");
        int frenchNameIndex = findColumnIndex(headers, "frenchName");

        List<TypeOfSample> processedSampleTypes = new ArrayList<>();
        String line;
        int lineNumber = 1; // Start at 1 since we already read the header
        int nextSortOrder = getNextAvailableSortOrder();

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                TypeOfSample sampleType = processCsvLine(values, descriptionIndex, localAbbreviationIndex, domainIndex,
                        isActiveIndex, sortOrderIndex, englishNameIndex, frenchNameIndex, lineNumber, fileName,
                        nextSortOrder);
                if (sampleType != null) {
                    processedSampleTypes.add(sampleType);
                    nextSortOrder++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        // Clear caches and refresh display lists
        typeOfSampleService.clearCache();
        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedSampleTypes.size() + " sample types from " + fileName);
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV parser that handles quoted fields
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasDescriptionColumn = false;
        boolean hasLocalAbbreviationColumn = false;

        for (String header : headers) {
            if ("description".equalsIgnoreCase(header)) {
                hasDescriptionColumn = true;
            }
            if ("localAbbreviation".equalsIgnoreCase(header)) {
                hasLocalAbbreviationColumn = true;
            }
        }

        if (!hasDescriptionColumn) {
            throw new IllegalArgumentException(
                    "Sample type configuration file " + fileName + " must have a 'description' column");
        }
        if (!hasLocalAbbreviationColumn) {
            throw new IllegalArgumentException(
                    "Sample type configuration file " + fileName + " must have a 'localAbbreviation' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private int getNextAvailableSortOrder() {
        List<TypeOfSample> allSampleTypes = typeOfSampleService.getAllTypeOfSamples();
        int maxSortOrder = 0;
        for (TypeOfSample sampleType : allSampleTypes) {
            if (sampleType.getSortOrder() > maxSortOrder) {
                maxSortOrder = sampleType.getSortOrder();
            }
        }
        return maxSortOrder + 1;
    }

    private TypeOfSample processCsvLine(String[] values, int descriptionIndex, int localAbbreviationIndex,
            int domainIndex, int isActiveIndex, int sortOrderIndex, int englishNameIndex, int frenchNameIndex,
            int lineNumber, String fileName, int defaultSortOrder) {

        // Get required fields
        String description = getValueOrEmpty(values, descriptionIndex);
        String localAbbreviation = getValueOrEmpty(values, localAbbreviationIndex);

        if (description.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing description");
            return null;
        }

        if (localAbbreviation.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing localAbbreviation");
            return null;
        }

        // Get optional fields
        String domain = getValueOrEmpty(values, domainIndex);
        if (domain.isEmpty()) {
            domain = DEFAULT_DOMAIN;
        }

        // Check if sample type already exists by localAbbreviation and domain
        TypeOfSample existingSampleType = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(localAbbreviation,
                domain);

        // If not found by abbreviation, also check by description and domain
        if (existingSampleType == null) {
            existingSampleType = findSampleTypeByDescriptionAndDomain(description, domain);
        }

        if (existingSampleType != null) {
            // Update existing sample type
            updateSampleTypeFromCsv(existingSampleType, values, description, localAbbreviation, isActiveIndex,
                    sortOrderIndex, englishNameIndex, frenchNameIndex, defaultSortOrder);
            typeOfSampleService.update(existingSampleType);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Updated existing sample type: " + description + " (" + localAbbreviation + ")");
            return existingSampleType;
        } else {
            // Create new sample type
            return createSampleType(values, description, localAbbreviation, domain, isActiveIndex, sortOrderIndex,
                    englishNameIndex, frenchNameIndex, defaultSortOrder);
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private TypeOfSample findSampleTypeByDescriptionAndDomain(String description, String domain) {
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription(description);
        searchType.setDomain(domain);
        return typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
    }

    private void updateSampleTypeFromCsv(TypeOfSample sampleType, String[] values, String description,
            String localAbbreviation, int isActiveIndex, int sortOrderIndex, int englishNameIndex, int frenchNameIndex,
            int defaultSortOrder) {

        sampleType.setDescription(description);
        sampleType.setLocalAbbreviation(localAbbreviation);

        // Update active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            sampleType.setActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        }

        // Update sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                sampleType.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateSampleTypeFromCsv",
                        "Invalid sortOrder value: " + sortOrderStr + ", using default");
            }
        }

        // Update localization if provided
        String englishName = getValueOrEmpty(values, englishNameIndex);
        String frenchName = getValueOrEmpty(values, frenchNameIndex);

        if (!englishName.isEmpty() || !frenchName.isEmpty()) {
            Localization localization = sampleType.getLocalization();
            if (localization != null) {
                if (!englishName.isEmpty()) {
                    localization.setEnglish(englishName);
                }
                if (!frenchName.isEmpty()) {
                    localization.setFrench(frenchName);
                }
                localization.setSysUserId("1");
                localizationService.update(localization);
            }
        }

        sampleType.setSysUserId("1"); // System user for configuration loading
    }

    private TypeOfSample createSampleType(String[] values, String description, String localAbbreviation, String domain,
            int isActiveIndex, int sortOrderIndex, int englishNameIndex, int frenchNameIndex, int defaultSortOrder) {

        // Create localization first
        String englishName = getValueOrEmpty(values, englishNameIndex);
        String frenchName = getValueOrEmpty(values, frenchNameIndex);

        // Default englishName to description if not provided
        if (englishName.isEmpty()) {
            englishName = description;
        }
        // Default frenchName to englishName if not provided
        if (frenchName.isEmpty()) {
            frenchName = englishName;
        }

        Localization localization = new Localization();
        localization.setDescription("sampleType name");
        localization.setEnglish(englishName);
        localization.setFrench(frenchName);
        localization.setSysUserId("1");
        localizationService.insert(localization);

        // Create sample type
        TypeOfSample sampleType = new TypeOfSample();
        sampleType.setDescription(description);
        sampleType.setLocalAbbreviation(localAbbreviation);
        sampleType.setDomain(domain);
        sampleType.setLocalization(localization);

        // Set active status
        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            sampleType.setActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        } else {
            sampleType.setActive(true); // Default to active
        }

        // Set sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                sampleType.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createSampleType",
                        "Invalid sortOrder value: " + sortOrderStr + ", using default: " + defaultSortOrder);
                sampleType.setSortOrder(defaultSortOrder);
            }
        } else {
            sampleType.setSortOrder(defaultSortOrder);
        }

        sampleType.setSysUserId("1"); // System user for configuration loading

        String sampleTypeId = typeOfSampleService.insert(sampleType);
        sampleType.setId(sampleTypeId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleType",
                "Created new sample type: " + description + " (" + localAbbreviation + ")");

        return sampleType;
    }
}
