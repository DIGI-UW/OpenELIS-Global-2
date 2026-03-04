package org.openelisglobal.address.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading address hierarchy level configuration from CSV files.
 * This handler creates OrganizationType entries for each hierarchy level and
 * updates site_information with geographic unit labels.
 *
 * <p>
 * Expected CSV format (address-hierarchy-levels.csv):
 *
 * <pre>
 * level,typeName,displayKey,sortOrder,defaultValue
 * 1,Province,address.level.province,1,DKI Jakarta
 * 2,District,address.level.district,2,Kota Jakarta Selatan
 * 3,Sub-District,address.level.subdistrict,3,
 * 4,Village,address.level.village,4,
 * </pre>
 *
 * <p>
 * Notes: - First line is the header (required) - level: The hierarchy level
 * number (1 = top level) - typeName: The OrganizationType name to create -
 * displayKey: i18n key for display (optional) - sortOrder: Display order
 * (optional) - defaultValue: Default value name to pre-select for new patients
 * (optional, must match an organization name at this level) - Only processes
 * files ending with "-levels.csv"
 */
@Component
public class AddressHierarchyConfigurationHandler implements DomainConfigurationHandler {

    private static final String ADDRESS_HIERARCHY_DEFAULT_PREFIX = "AddrHierarchyDefault_";

    @Autowired
    private OrganizationTypeService organizationTypeService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    public String getDomainName() {
        return "address-hierarchy";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 50; // Load before other configs that depend on organizations
    }

    @Override
    public String getFileMatcher() {
        return "*-levels.csv";
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "Address hierarchy levels file " + fileName + " is empty - skipping");
            return;
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int levelIndex = findColumnIndex(headers, "level");
        int typeNameIndex = findColumnIndex(headers, "typeName");
        int displayKeyIndex = findColumnIndex(headers, "displayKey");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");
        int defaultValueIndex = findColumnIndex(headers, "defaultValue");

        List<OrganizationType> processedTypes = new ArrayList<>();
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                OrganizationType orgType = processCsvLine(values, levelIndex, typeNameIndex, displayKeyIndex,
                        sortOrderIndex, defaultValueIndex);
                if (orgType != null) {
                    processedTypes.add(orgType);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        DisplayListService.getInstance().refreshLists();

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedTypes.size() + " address hierarchy levels from " + fileName);
    }

    private String[] parseCsvLine(String line) {
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
        boolean hasLevelColumn = false;
        boolean hasTypeNameColumn = false;

        for (String header : headers) {
            if ("level".equalsIgnoreCase(header)) {
                hasLevelColumn = true;
            }
            if ("typeName".equalsIgnoreCase(header)) {
                hasTypeNameColumn = true;
            }
        }

        if (!hasLevelColumn) {
            throw new IllegalArgumentException(
                    "Address hierarchy levels file " + fileName + " must have a 'level' column");
        }
        if (!hasTypeNameColumn) {
            throw new IllegalArgumentException(
                    "Address hierarchy levels file " + fileName + " must have a 'typeName' column");
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

    private OrganizationType processCsvLine(String[] values, int levelIndex, int typeNameIndex, int displayKeyIndex,
            int sortOrderIndex, int defaultValueIndex) {

        String levelStr = getValueOrEmpty(values, levelIndex);
        String typeName = getValueOrEmpty(values, typeNameIndex);
        String defaultValue = getValueOrEmpty(values, defaultValueIndex);

        if (levelStr.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing level");
            return null;
        }

        if (typeName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing typeName");
            return null;
        }

        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Invalid level value: " + levelStr);
            return null;
        }

        // Check if organization type already exists
        OrganizationType existingType = organizationTypeService.getOrganizationTypeByName(typeName);
        if (existingType != null) {
            // Update existing type with level info in description
            updateOrganizationType(existingType, level, values, displayKeyIndex);
            organizationTypeService.update(existingType);
            updateSiteInformationLabel(level, typeName);
            updateDefaultValue(level, defaultValue);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Updated existing organization type: " + typeName + " at level " + level);
            return existingType;
        } else {
            // Create new organization type
            OrganizationType newType = new OrganizationType();
            newType.setName(typeName);
            updateOrganizationType(newType, level, values, displayKeyIndex);
            newType.setSysUserId("1");
            String typeId = organizationTypeService.insert(newType);
            newType.setId(typeId);
            updateSiteInformationLabel(level, typeName);
            updateDefaultValue(level, defaultValue);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                    "Created new organization type: " + typeName + " at level " + level);
            return newType;
        }
    }

    private void updateOrganizationType(OrganizationType orgType, int level, String[] values, int displayKeyIndex) {
        orgType.setHierarchyLevel(level);
        orgType.setSysUserId("1");
    }

    private void updateSiteInformationLabel(int level, String typeName) {
        // Update GeographicUnit labels for levels 1 and 2 (backward compatibility)
        String siteInfoName = null;
        if (level == 1) {
            siteInfoName = "Geographic Unit 1 label";
        } else if (level == 2) {
            siteInfoName = "Geographic Unit 2 label";
        }

        if (siteInfoName != null) {
            SiteInformation siteInfo = siteInformationService.getSiteInformationByName(siteInfoName);
            if (siteInfo != null) {
                siteInfo.setValue(typeName);
                siteInfo.setSysUserId("1");
                siteInformationService.update(siteInfo);
                LogEvent.logInfo(this.getClass().getSimpleName(), "updateSiteInformationLabel",
                        "Updated " + siteInfoName + " to: " + typeName);
            }
        }
    }

    private void updateDefaultValue(int level, String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return;
        }

        String siteInfoName = ADDRESS_HIERARCHY_DEFAULT_PREFIX + level;

        // Try to find existing site_information entry
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName(siteInfoName);
        if (siteInfo != null) {
            siteInfo.setValue(defaultValue);
            siteInfo.setSysUserId("1");
            siteInformationService.update(siteInfo);
            LogEvent.logInfo(this.getClass().getSimpleName(), "updateDefaultValue",
                    "Updated default for level " + level + " to: " + defaultValue);
        } else {
            // Create new site_information entry
            siteInfo = new SiteInformation();
            siteInfo.setName(siteInfoName);
            siteInfo.setValue(defaultValue);
            siteInfo.setDescription("Default address hierarchy value for level " + level);
            siteInfo.setValueType("text");
            siteInfo.setSysUserId("1");
            siteInformationService.insert(siteInfo);
            LogEvent.logInfo(this.getClass().getSimpleName(), "updateDefaultValue",
                    "Created default for level " + level + ": " + defaultValue);
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    /**
     * Get the hierarchy level for an OrganizationType.
     *
     * @param orgType the organization type to check
     * @return the hierarchy level, or -1 if not an address hierarchy type
     */
    public static int getHierarchyLevel(OrganizationType orgType) {
        if (orgType == null || orgType.getHierarchyLevel() == null) {
            return -1;
        }
        return orgType.getHierarchyLevel();
    }

    /**
     * Check if an OrganizationType is an address hierarchy type.
     *
     * @param orgType the organization type to check
     * @return true if it's an address hierarchy type
     */
    public static boolean isAddressHierarchyType(OrganizationType orgType) {
        return getHierarchyLevel(orgType) > 0;
    }

    /**
     * Get the site_information name for default value at a given level.
     *
     * @param level the hierarchy level number
     * @return the site_information name for storing the default
     */
    public static String getDefaultValueSiteInfoName(int level) {
        return ADDRESS_HIERARCHY_DEFAULT_PREFIX + level;
    }
}
