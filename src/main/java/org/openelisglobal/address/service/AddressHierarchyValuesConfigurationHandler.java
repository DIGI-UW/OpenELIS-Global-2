package org.openelisglobal.address.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for loading address hierarchy values from CSV files. This handler
 * creates Organization entries with parent-child relationships based on the
 * configured hierarchy levels.
 *
 * <p>
 * The CSV format uses a flattened hierarchy where each row contains the
 * complete path. This makes the CSV easier to read and maintain. The header row
 * defines the level names.
 *
 * <p>
 * Expected CSV format:
 *
 * <pre>
 * Country,Department,Commune,Section,Locality
 * Haiti,Ouest,Kenscoff,Sourcailles%115-03,Duplan
 * Haiti,Ouest,Kenscoff,Sourcailles%115-03,Brouette
 * Haiti,Ouest,Kenscoff,Belle Fontaine%115-04,Catno
 * </pre>
 *
 * <p>
 * Optional code format: Use "Name%Code" to specify both name and code. If no
 * code is provided, the name is used as the code.
 *
 * <p>
 * Notes: - First line is the header defining level names - Each column
 * represents a hierarchy level (left to right = top to bottom) - Duplicate
 * entries at any level are automatically deduplicated - Parent-child
 * relationships are inferred from the row structure - Files ending with
 * "-levels.csv" are skipped (handled by AddressHierarchyConfigurationHandler)
 *
 * <p>
 * Performance: Uses batch processing with configurable batch size for large
 * datasets. Progress is logged every batch to help track long-running imports.
 */
@Component
public class AddressHierarchyValuesConfigurationHandler implements DomainConfigurationHandler {

    private static final String NAME_CODE_SEPARATOR = "%";
    private static final int BATCH_SIZE = 500;
    private static final int LOG_INTERVAL = 5000;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationTypeService organizationTypeService;

    @PersistenceContext
    private EntityManager entityManager;

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
        return 55; // After levels are created (50)
    }

    @Override
    public boolean canProcess(String fileName) {
        // Process all CSV files EXCEPT level configuration files
        return !fileName.endsWith("-levels.csv");
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        long startTime = System.currentTimeMillis();
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Processing address hierarchy values file: " + fileName);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read header to get level names
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Address hierarchy values file " + fileName + " is empty");
        }

        String[] levelNames = parseCsvLine(headerLine);
        if (levelNames.length == 0) {
            throw new IllegalArgumentException("Address hierarchy values file " + fileName + " has no columns");
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "CSV has " + levelNames.length + " levels: " + String.join(", ", levelNames));

        // Build map of level number to OrganizationType
        Map<Integer, OrganizationType> levelTypeMap = buildOrCreateLevelTypes(levelNames);
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Built level type map with " + levelTypeMap.size() + " levels");

        // Count total lines for progress reporting
        List<String> allLines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                allLines.add(line);
            }
        }
        int totalRows = allLines.size();
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Total rows to process: " + totalRows);

        // Track created organizations by their full path key to avoid duplicates
        // Key format: "level1Name|level2Name|level3Name"
        Map<String, Organization> pathToOrgMap = new LinkedHashMap<>();

        // Batch processing variables
        List<Organization> batchToInsert = new ArrayList<>();
        List<OrganizationTypeLinkInfo> typeLinksToCreate = new ArrayList<>();
        int rowsProcessed = 0;
        int organizationsCreated = 0;
        int organizationsSkipped = 0;
        int errors = 0;

        for (String csvLine : allLines) {
            rowsProcessed++;

            try {
                String[] values = parseCsvLine(csvLine);
                BatchProcessingResult result = processHierarchyRowBatch(values, levelNames, levelTypeMap, pathToOrgMap,
                        batchToInsert, typeLinksToCreate);

                organizationsCreated += result.newOrganizations;
                organizationsSkipped += result.skippedOrganizations;

                // Flush batch when it reaches the batch size
                if (batchToInsert.size() >= BATCH_SIZE) {
                    flushBatch(batchToInsert, typeLinksToCreate, pathToOrgMap);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                            "Flushed batch at row " + rowsProcessed + "/" + totalRows + " ("
                                    + String.format("%.1f", (100.0 * rowsProcessed / totalRows)) + "%), "
                                    + "total orgs: " + pathToOrgMap.size());
                }

                // Log progress periodically
                if (rowsProcessed % LOG_INTERVAL == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rowsPerSecond = rowsProcessed / (elapsed / 1000.0);
                    int remaining = totalRows - rowsProcessed;
                    double estimatedSecondsRemaining = remaining / rowsPerSecond;

                    LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                            "Progress: " + rowsProcessed + "/" + totalRows + " rows ("
                                    + String.format("%.1f", (100.0 * rowsProcessed / totalRows)) + "%), "
                                    + "unique locations: " + pathToOrgMap.size() + ", " + "rate: "
                                    + String.format("%.0f", rowsPerSecond) + " rows/sec, " + "ETA: "
                                    + String.format("%.0f", estimatedSecondsRemaining) + "s");
                }
            } catch (Exception e) {
                errors++;
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing row " + rowsProcessed + "/" + totalRows + ": " + e.getMessage()
                                + " | Line content: " + truncate(csvLine, 100));
                if (errors > 100) {
                    LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                            "Too many errors (" + errors + "), aborting import");
                    throw new RuntimeException("Aborting due to excessive errors (" + errors + " errors)", e);
                }
            }
        }

        // Flush remaining batch
        if (!batchToInsert.isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                    "Flushing final batch of " + batchToInsert.size() + " organizations");
            flushBatch(batchToInsert, typeLinksToCreate, pathToOrgMap);
        }

        DisplayListService.getInstance().refreshLists();

        long totalTime = System.currentTimeMillis() - startTime;
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "COMPLETED processing " + fileName + " in " + (totalTime / 1000) + " seconds: " + rowsProcessed
                        + " rows processed, " + pathToOrgMap.size() + " unique locations created/updated, " + errors
                        + " errors");
    }

    /**
     * Helper class to track type links that need to be created after organization
     * insert.
     */
    private static class OrganizationTypeLinkInfo {
        Organization organization;
        OrganizationType orgType;

        OrganizationTypeLinkInfo(Organization org, OrganizationType type) {
            this.organization = org;
            this.orgType = type;
        }
    }

    /**
     * Result of processing a single row in batch mode.
     */
    private static class BatchProcessingResult {
        int newOrganizations = 0;
        int skippedOrganizations = 0;
    }

    /**
     * Flush the current batch to the database.
     */
    private void flushBatch(List<Organization> batchToInsert, List<OrganizationTypeLinkInfo> typeLinksToCreate,
            Map<String, Organization> pathToOrgMap) {
        try {
            // Insert all organizations in batch
            for (Organization org : batchToInsert) {
                try {
                    String orgId = organizationService.insert(org);
                    org.setId(orgId);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "flushBatch",
                            "Failed to insert organization: " + org.getOrganizationName() + " - " + e.getMessage());
                    throw e;
                }
            }

            // Flush to database
            entityManager.flush();

            // Create type links after organizations have IDs
            for (OrganizationTypeLinkInfo linkInfo : typeLinksToCreate) {
                try {
                    ensureOrganizationTypeLink(linkInfo.organization, linkInfo.orgType);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "flushBatch",
                            "Failed to link organization " + linkInfo.organization.getOrganizationName() + " to type "
                                    + linkInfo.orgType.getName() + " - " + e.getMessage());
                }
            }

            // Flush type links
            entityManager.flush();

            // Clear persistence context to free memory
            entityManager.clear();

            // Clear batch lists
            batchToInsert.clear();
            typeLinksToCreate.clear();

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "flushBatch",
                    "Batch flush failed with " + batchToInsert.size() + " orgs: " + e.getMessage());
            throw new RuntimeException("Batch flush failed", e);
        }
    }

    /**
     * Build or create OrganizationType entries for each level based on header
     * names.
     */
    private Map<Integer, OrganizationType> buildOrCreateLevelTypes(String[] levelNames) {
        Map<Integer, OrganizationType> levelTypeMap = new HashMap<>();

        // First try to use existing configured hierarchy levels
        Map<Integer, OrganizationType> configuredTypes = buildLevelTypeMap();

        for (int i = 0; i < levelNames.length; i++) {
            int level = i + 1;
            String levelName = levelNames[i].trim();

            if (configuredTypes.containsKey(level)) {
                // Use pre-configured type
                levelTypeMap.put(level, configuredTypes.get(level));
                LogEvent.logInfo(this.getClass().getSimpleName(), "buildOrCreateLevelTypes",
                        "Using configured type for level " + level + ": " + configuredTypes.get(level).getName());
            } else {
                // Try to find or create type by name
                OrganizationType orgType = organizationTypeService.getOrganizationTypeByName(levelName);
                if (orgType == null) {
                    // Create new organization type
                    orgType = new OrganizationType();
                    orgType.setName(levelName);
                    orgType.setDescription("Address Hierarchy Level " + level);
                    orgType.setSysUserId("1");
                    String typeId = organizationTypeService.insert(orgType);
                    orgType.setId(typeId);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "buildOrCreateLevelTypes",
                            "Created organization type: " + levelName + " for level " + level);
                } else {
                    // Update existing type with hierarchy level description if not already set
                    String expectedDescription = "Address Hierarchy Level " + level;
                    if (orgType.getDescription() == null
                            || !orgType.getDescription().startsWith("Address Hierarchy Level ")) {
                        orgType.setDescription(expectedDescription);
                        orgType.setSysUserId("1");
                        organizationTypeService.update(orgType);
                        LogEvent.logInfo(this.getClass().getSimpleName(), "buildOrCreateLevelTypes",
                                "Updated organization type: " + levelName + " with level " + level);
                    }
                }
                levelTypeMap.put(level, orgType);
            }
        }

        return levelTypeMap;
    }

    private Map<Integer, OrganizationType> buildLevelTypeMap() {
        Map<Integer, OrganizationType> levelTypeMap = new HashMap<>();
        List<OrganizationType> allTypes = organizationTypeService.getAllOrganizationTypes();

        for (OrganizationType orgType : allTypes) {
            int level = AddressHierarchyConfigurationHandler.getHierarchyLevel(orgType);
            if (level > 0) {
                levelTypeMap.put(level, orgType);
            }
        }

        return levelTypeMap;
    }

    /**
     * Process a single row of the hierarchy CSV in batch mode. Creates
     * organizations for each level and sets up parent-child relationships.
     * Organizations are added to the batch list for later insertion.
     */
    private BatchProcessingResult processHierarchyRowBatch(String[] values, String[] levelNames,
            Map<Integer, OrganizationType> levelTypeMap, Map<String, Organization> pathToOrgMap,
            List<Organization> batchToInsert, List<OrganizationTypeLinkInfo> typeLinksToCreate) {

        BatchProcessingResult result = new BatchProcessingResult();
        Organization parent = null;
        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < Math.min(values.length, levelNames.length); i++) {
            String cellValue = values[i].trim();
            if (cellValue.isEmpty()) {
                continue;
            }

            int level = i + 1;

            // Parse name and optional code from cell value
            String name;
            String code;
            if (cellValue.contains(NAME_CODE_SEPARATOR)) {
                String[] parts = cellValue.split(NAME_CODE_SEPARATOR, 2);
                name = parts[0].trim();
                code = parts[1].trim();
            } else {
                name = cellValue;
                code = generateCode(cellValue, level, pathBuilder.toString());
            }

            // Build path key for deduplication
            if (pathBuilder.length() > 0) {
                pathBuilder.append("|");
            }
            pathBuilder.append(name);
            String pathKey = pathBuilder.toString();

            // Check if we already processed this location
            if (pathToOrgMap.containsKey(pathKey)) {
                parent = pathToOrgMap.get(pathKey);
                result.skippedOrganizations++;
                continue;
            }

            // Get organization type for this level
            OrganizationType orgType = levelTypeMap.get(level);
            if (orgType == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processHierarchyRowBatch",
                        "No OrganizationType found for level " + level + ". Skipping: " + name);
                continue;
            }

            // Check if organization already exists in database
            Organization existingOrg = findExistingOrganization(name, code, parent);
            if (existingOrg != null) {
                pathToOrgMap.put(pathKey, existingOrg);
                parent = existingOrg;
                result.skippedOrganizations++;
                // Still need to ensure type link exists
                typeLinksToCreate.add(new OrganizationTypeLinkInfo(existingOrg, orgType));
                continue;
            }

            // Create new organization (but don't insert yet - add to batch)
            Organization newOrg = new Organization();
            newOrg.setOrganizationName(name);
            newOrg.setCode(code);
            newOrg.setShortName(name.length() > 15 ? name.substring(0, 15) : name);
            newOrg.setIsActive("Y");
            newOrg.setMlsSentinelLabFlag("N");
            newOrg.setOrganization(parent);
            newOrg.setSysUserId("1");

            // Add to batch
            batchToInsert.add(newOrg);
            pathToOrgMap.put(pathKey, newOrg);
            typeLinksToCreate.add(new OrganizationTypeLinkInfo(newOrg, orgType));
            parent = newOrg;
            result.newOrganizations++;
        }

        return result;
    }

    /**
     * Find an existing organization by code or by name+parent combination.
     */
    private Organization findExistingOrganization(String name, String code, Organization parent) {
        // First try to find by code
        Organization existingOrg = organizationService.getOrganizationByCode(code);
        if (existingOrg != null) {
            return existingOrg;
        }

        // Try to find by name and parent combination
        Organization orgByName = new Organization();
        orgByName.setOrganizationName(name);
        existingOrg = organizationService.getOrganizationByName(orgByName, true);
        if (existingOrg != null) {
            // Check if it has the same parent
            boolean sameParent = (parent == null && existingOrg.getOrganization() == null)
                    || (parent != null && existingOrg.getOrganization() != null && parent.getId() != null
                            && parent.getId().equals(existingOrg.getOrganization().getId()));
            if (sameParent) {
                return existingOrg;
            }
        }

        return null;
    }

    /**
     * Generate a unique code for a location based on its name and position in
     * hierarchy.
     */
    private String generateCode(String name, int level, String parentPath) {
        // Create a code from the name: uppercase, no spaces, max 20 chars
        String baseCode = name.toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0,
                Math.min(name.replaceAll("[^A-Za-z0-9]", "").length(), 15));

        // Add level prefix to ensure uniqueness across levels
        return "L" + level + "_" + baseCode;
    }

    /**
     * Ensure an organization is linked to the specified type. Only links if not
     * already linked.
     */
    private void ensureOrganizationTypeLink(Organization org, OrganizationType orgType) {
        if (org.getId() == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "ensureOrganizationTypeLink",
                    "Cannot link organization without ID: " + org.getOrganizationName());
            return;
        }

        // Check if already linked by querying the database directly
        List<String> existingTypeIds = organizationService.getTypeIdsForOrganizationId(org.getId());
        if (existingTypeIds != null && existingTypeIds.contains(orgType.getId())) {
            return; // Already linked
        }
        // Not linked, add the link
        organizationService.linkOrganizationAndType(org, orgType.getId());
    }

    /**
     * Truncate a string for logging purposes.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
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
}
