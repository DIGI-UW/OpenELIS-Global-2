package org.openelisglobal.address.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // Track organization IDs by their full path key to avoid duplicates
        // Key format: "level1Name|level2Name|level3Name", Value: organization ID (as
        // String)
        // Using IDs instead of full objects reduces memory and is safer across
        // entityManager.clear()
        Map<String, String> pathToOrgIdMap = new LinkedHashMap<>();

        // Cache existing org->type links to avoid per-link database queries
        // Key: "orgId:typeId", presence means link exists
        java.util.Set<String> existingTypeLinks = new java.util.HashSet<>();

        // Pre-load existing organizations and their type links to avoid per-row
        // database queries
        preloadExistingOrganizations(pathToOrgIdMap, levelTypeMap, existingTypeLinks);
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration", "Pre-loaded " + pathToOrgIdMap.size()
                + " existing organizations and " + existingTypeLinks.size() + " type links into memory");

        // Batch processing variables
        List<BatchOrganizationInfo> batchToInsert = new ArrayList<>();
        List<OrganizationTypeLinkInfo> typeLinksToCreate = new ArrayList<>();
        int rowsProcessed = 0;
        int organizationsCreated = 0;
        int organizationsSkipped = 0;
        int errors = 0;

        for (String csvLine : allLines) {
            rowsProcessed++;

            try {
                String[] values = parseCsvLine(csvLine);
                BatchProcessingResult result = processHierarchyRowBatch(values, levelNames, levelTypeMap,
                        pathToOrgIdMap, batchToInsert, typeLinksToCreate);

                organizationsCreated += result.newOrganizations;
                organizationsSkipped += result.skippedOrganizations;

                // Flush batch when it reaches the batch size
                if (batchToInsert.size() >= BATCH_SIZE) {
                    flushBatch(batchToInsert, typeLinksToCreate, pathToOrgIdMap, existingTypeLinks);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                            "Flushed batch at row " + rowsProcessed + "/" + totalRows + " ("
                                    + String.format("%.1f", (100.0 * rowsProcessed / totalRows)) + "%), "
                                    + "total orgs: " + pathToOrgIdMap.size());
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
                                    + "unique locations: " + pathToOrgIdMap.size() + ", " + "rate: "
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
            flushBatch(batchToInsert, typeLinksToCreate, pathToOrgIdMap, existingTypeLinks);
        }

        DisplayListService.getInstance().refreshLists();

        long totalTime = System.currentTimeMillis() - startTime;
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "COMPLETED processing " + fileName + " in " + (totalTime / 1000) + " seconds: " + rowsProcessed
                        + " rows processed, " + pathToOrgIdMap.size() + " unique locations created/updated, " + errors
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
     * Helper class to track organizations in batch with their path keys. After
     * flush, we update pathToOrgIdMap with the assigned IDs.
     */
    private static class BatchOrganizationInfo {
        Organization organization;
        String pathKey;

        BatchOrganizationInfo(Organization org, String pathKey) {
            this.organization = org;
            this.pathKey = pathKey;
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
     * Flush the current batch to the database. Uses entityManager.persist()
     * directly for true batch inserts instead of organizationService.insert() which
     * flushes immediately.
     */
    private void flushBatch(List<BatchOrganizationInfo> batchToInsert, List<OrganizationTypeLinkInfo> typeLinksToCreate,
            Map<String, String> pathToOrgIdMap, java.util.Set<String> existingTypeLinks) {
        try {
            // Persist all organizations without flushing - allows Hibernate to batch
            for (BatchOrganizationInfo batchInfo : batchToInsert) {
                try {
                    entityManager.persist(batchInfo.organization);
                } catch (jakarta.persistence.PersistenceException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "flushBatch", "Failed to persist organization '"
                            + batchInfo.organization.getOrganizationName() + "': " + e.getMessage());
                    throw e;
                }
            }

            // Single flush for all organizations - Hibernate can batch the INSERTs
            try {
                entityManager.flush();
            } catch (jakarta.persistence.PersistenceException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "flushBatch", "Database flush failed for batch of "
                        + batchToInsert.size() + " organizations: " + e.getMessage());
                // Log the first few org names to help identify the problem
                StringBuilder orgNames = new StringBuilder("Organizations in failed batch: ");
                int count = 0;
                for (BatchOrganizationInfo batchInfo : batchToInsert) {
                    if (count++ >= 5) {
                        orgNames.append("... and ").append(batchToInsert.size() - 5).append(" more");
                        break;
                    }
                    orgNames.append(batchInfo.organization.getOrganizationName()).append(", ");
                }
                LogEvent.logError(this.getClass().getSimpleName(), "flushBatch", orgNames.toString());
                throw e;
            }

            // After flush, organizations now have IDs - update pathToOrgIdMap
            for (BatchOrganizationInfo batchInfo : batchToInsert) {
                if (batchInfo.organization.getId() != null) {
                    pathToOrgIdMap.put(batchInfo.pathKey, batchInfo.organization.getId());
                }
            }

            // Clear persistence context to free memory
            entityManager.clear();

            // Create type links after organizations have IDs
            // Note: linkOrganizationAndType uses direct SQL, no flush needed
            for (OrganizationTypeLinkInfo linkInfo : typeLinksToCreate) {
                try {
                    ensureOrganizationTypeLink(linkInfo.organization, linkInfo.orgType, existingTypeLinks);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "flushBatch",
                            "Failed to link organization " + linkInfo.organization.getOrganizationName() + " to type "
                                    + linkInfo.orgType.getName() + " - " + e.getMessage());
                }
            }

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
     *
     * Uses entity references (getReference) for parents when possible to reduce
     * memory and avoid issues across entityManager.clear() calls.
     */
    private BatchProcessingResult processHierarchyRowBatch(String[] values, String[] levelNames,
            Map<Integer, OrganizationType> levelTypeMap, Map<String, String> pathToOrgIdMap,
            List<BatchOrganizationInfo> batchToInsert, List<OrganizationTypeLinkInfo> typeLinksToCreate) {

        BatchProcessingResult result = new BatchProcessingResult();
        // Track parent: either an ID (for flushed orgs) or the actual org (for
        // unflushed batch orgs)
        String parentId = null;
        Organization parentOrgInBatch = null;
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

            // Check if we already have this location (either pre-loaded or in current
            // batch)
            if (pathToOrgIdMap.containsKey(pathKey)) {
                // This is a flushed organization with a known ID
                parentId = pathToOrgIdMap.get(pathKey);
                parentOrgInBatch = null;
                result.skippedOrganizations++;
                continue;
            }

            // Check if it's in the current unflushed batch
            BatchOrganizationInfo batchOrg = findInBatch(batchToInsert, pathKey);
            if (batchOrg != null) {
                // Parent is in unflushed batch - reference the actual object
                parentId = null;
                parentOrgInBatch = batchOrg.organization;
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

            // Create new organization (but don't insert yet - add to batch)
            Organization newOrg = new Organization();
            newOrg.setOrganizationName(name);
            newOrg.setCode(code);
            newOrg.setShortName(name.length() > 15 ? name.substring(0, 15) : name);
            newOrg.setIsActive("Y");
            newOrg.setMlsSentinelLabFlag("N");
            newOrg.setSysUserId("1");

            // Set parent using lightweight reference when possible
            if (parentId != null) {
                // Parent has been flushed - use getReference for lightweight proxy
                // Organization uses String as its ID type
                newOrg.setOrganization(entityManager.getReference(Organization.class, parentId));
            } else if (parentOrgInBatch != null) {
                // Parent is in unflushed batch - must reference actual object
                newOrg.setOrganization(parentOrgInBatch);
            }
            // else no parent (top-level)

            // Add to batch with path key for later ID mapping
            batchToInsert.add(new BatchOrganizationInfo(newOrg, pathKey));
            typeLinksToCreate.add(new OrganizationTypeLinkInfo(newOrg, orgType));

            // This new org becomes the parent for next level
            parentId = null;
            parentOrgInBatch = newOrg;
            result.newOrganizations++;
        }

        return result;
    }

    /**
     * Find an organization in the current unflushed batch by its path key.
     */
    private BatchOrganizationInfo findInBatch(List<BatchOrganizationInfo> batch, String pathKey) {
        for (BatchOrganizationInfo info : batch) {
            if (pathKey.equals(info.pathKey)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Pre-load existing address hierarchy organizations into the pathToOrgIdMap.
     * Also pre-loads existing organization-type links to avoid per-link database
     * queries. This trades memory for reduced database queries during batch
     * processing. Organizations are loaded once and their path keys are computed by
     * traversing parent chains. Only IDs are stored (not full objects) to reduce
     * memory.
     */
    private void preloadExistingOrganizations(Map<String, String> pathToOrgIdMap,
            Map<Integer, OrganizationType> levelTypeMap, java.util.Set<String> existingTypeLinks) {
        if (levelTypeMap.isEmpty()) {
            return;
        }

        // Get all organization type IDs for address hierarchy levels
        List<String> hierarchyTypeIds = new ArrayList<>();
        for (OrganizationType orgType : levelTypeMap.values()) {
            if (orgType.getId() != null) {
                hierarchyTypeIds.add(orgType.getId());
            }
        }

        if (hierarchyTypeIds.isEmpty()) {
            return;
        }

        // Collect all org IDs that belong to any hierarchy type (one query per type)
        // Also populate existingTypeLinks with the org:type pairs
        java.util.Set<String> hierarchyOrgIds = new java.util.HashSet<>();
        for (String typeId : hierarchyTypeIds) {
            List<String> orgIds = organizationTypeService.getOrganizationIdsForType(typeId);
            hierarchyOrgIds.addAll(orgIds);
            // Cache these links - format: "orgId:typeId"
            for (String orgId : orgIds) {
                existingTypeLinks.add(orgId + ":" + typeId);
            }
        }

        if (hierarchyOrgIds.isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "preloadExistingOrganizations",
                    "No existing address hierarchy organizations found");
            return;
        }

        // Load all organizations once (needed to build path keys via parent traversal)
        List<Organization> allOrganizations = organizationService.getAllOrganizations();

        // Build a map of org ID to org for parent chain traversal
        Map<String, Organization> orgById = new HashMap<>();
        for (Organization org : allOrganizations) {
            if (org.getId() != null) {
                orgById.put(org.getId(), org);
            }
        }

        // For each organization that belongs to a hierarchy type, compute its path key
        // Store only the ID (not the full object) to reduce memory
        for (String orgId : hierarchyOrgIds) {
            Organization org = orgById.get(orgId);
            if (org == null || !"Y".equals(org.getIsActive())) {
                continue;
            }

            // Build path key by traversing parent chain
            String pathKey = buildPathKeyForOrganization(org, orgById);
            if (pathKey != null && !pathKey.isEmpty()) {
                pathToOrgIdMap.put(pathKey, orgId);
            }
        }
    }

    /**
     * Build the path key for an organization by traversing its parent chain. Path
     * format: "grandparent|parent|child"
     */
    private String buildPathKeyForOrganization(Organization org, Map<String, Organization> orgById) {
        List<String> pathParts = new ArrayList<>();
        Organization current = org;
        int maxDepth = 20; // Safety limit to prevent infinite loops

        while (current != null && maxDepth > 0) {
            pathParts.add(0, current.getOrganizationName()); // Add to front

            // Get parent - might be a proxy, so look up in our map
            Organization parent = current.getOrganization();
            if (parent != null && parent.getId() != null) {
                current = orgById.get(parent.getId());
            } else {
                current = null;
            }
            maxDepth--;
        }

        return String.join("|", pathParts);
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
     * already linked. Uses the pre-cached existingTypeLinks set to avoid database
     * queries.
     */
    private void ensureOrganizationTypeLink(Organization org, OrganizationType orgType,
            java.util.Set<String> existingTypeLinks) {
        if (org.getId() == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "ensureOrganizationTypeLink",
                    "Cannot link organization without ID: " + org.getOrganizationName());
            return;
        }

        // Check if already linked using pre-cached set (avoids database query)
        String linkKey = org.getId() + ":" + orgType.getId();
        if (existingTypeLinks.contains(linkKey)) {
            return; // Already linked
        }

        // Not linked, add the link
        organizationService.linkOrganizationAndType(org, orgType.getId());
        // Add to cache so we don't try to create it again
        existingTypeLinks.add(linkKey);
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
