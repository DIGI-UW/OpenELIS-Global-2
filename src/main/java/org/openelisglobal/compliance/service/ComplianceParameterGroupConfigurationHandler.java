package org.openelisglobal.compliance.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain configuration handler for compliance <em>parameter groups</em>.
 *
 * Loads {@link ParameterGroup} rows from CSV files in
 * {@code configuration/backend/compliance-standards/} that match the file
 * pattern {@code *-parameter-groups.csv}. Runs after the standards handler
 * (load order 200) so parent {@link ComplianceStandard} rows already exist.
 *
 * Required columns (case-insensitive): {@code regulationNumber, standardName,
 * name}. Optional: {@code description, sortOrder, isMandatory}.
 *
 * Lookup keys:
 * <ul>
 * <li>standard: {@code (regulationNumber, standardName)} — row skipped with a
 * WARN if not found.</li>
 * <li>group: {@code (standardId, name)} — existing rows are updated, new rows
 * inserted (idempotent re-runs).</li>
 * </ul>
 */
@Component
public class ComplianceParameterGroupConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Override
    public String getDomainName() {
        return "compliance-standards";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 210;
    }

    @Override
    public String getFileMatcher() {
        return "*-parameter-groups.csv";
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Skip leading blank/# comment lines so the README's authoring rules
        // apply to the header position too.
        ComplianceCsvUtil.HeaderRead header = ComplianceCsvUtil.readHeaderLine(reader, fileName,
                "Compliance parameter-groups configuration file");

        String[] headers = ComplianceCsvUtil.parseCsvLine(header.line);
        Map<String, Integer> columnIndices = ComplianceCsvUtil.createColumnMap(headers);
        validateHeaders(columnIndices, fileName);

        // Cache standard lookups so a file with N rows for the same standard
        // does not issue N identical DB queries.
        Map<String, ComplianceStandard> standardCache = new HashMap<>();

        int created = 0;
        int updated = 0;
        int skipped = 0;
        String line;
        int lineNumber = header.lineNumber;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (ComplianceCsvUtil.isSkippableLine(line)) {
                continue;
            }

            try {
                String[] values = ComplianceCsvUtil.parseCsvLine(line);
                LineResult result = processLine(values, columnIndices, standardCache, lineNumber, fileName);
                if (result == LineResult.CREATED) {
                    created++;
                } else if (result == LineResult.UPDATED) {
                    updated++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                skipped++;
                LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                        "Skipped row " + lineNumber + " in " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration", "Loaded parameter groups from "
                + fileName + " (created=" + created + ", updated=" + updated + ", skipped=" + skipped + ")");
    }

    private void validateHeaders(Map<String, Integer> columnIndices, String fileName) {
        if (!columnIndices.containsKey("regulationnumber")) {
            throw new IllegalArgumentException("Compliance parameter-groups configuration file " + fileName
                    + " must have a 'regulationNumber' column");
        }
        if (!columnIndices.containsKey("standardname")) {
            throw new IllegalArgumentException("Compliance parameter-groups configuration file " + fileName
                    + " must have a 'standardName' column");
        }
        if (!columnIndices.containsKey("name")) {
            throw new IllegalArgumentException(
                    "Compliance parameter-groups configuration file " + fileName + " must have a 'name' column");
        }
    }

    private LineResult processLine(String[] values, Map<String, Integer> columnIndices,
            Map<String, ComplianceStandard> standardCache, int lineNumber, String fileName) {
        String regulationNumber = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("regulationnumber"));
        String standardName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("standardname"));
        String groupName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("name"));

        if (regulationNumber.isEmpty() || standardName.isEmpty() || groupName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine", "Skipping row " + lineNumber + " in "
                    + fileName + ": missing regulationNumber, standardName, " + "or group name");
            return LineResult.SKIPPED;
        }

        ComplianceStandard standard = resolveStandard(standardCache, regulationNumber, standardName);
        if (standard == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine",
                    "Skipping row " + lineNumber + " in " + fileName + ": no compliance standard matches ("
                            + regulationNumber + ", " + standardName + "). Load the standards CSV first.");
            return LineResult.SKIPPED;
        }

        ParameterGroup existing = findGroupByName(standard.getId(), groupName);
        if (existing != null) {
            applyCsvFields(existing, values, columnIndices);
            parameterGroupService.update(existing);
            return LineResult.UPDATED;
        }

        ParameterGroup group = new ParameterGroup();
        group.setStandard(standard);
        group.setName(groupName);
        applyCsvFields(group, values, columnIndices);
        parameterGroupService.insert(group);
        return LineResult.CREATED;
    }

    private ComplianceStandard resolveStandard(Map<String, ComplianceStandard> cache, String regulationNumber,
            String standardName) {
        String key = regulationNumber + "||" + standardName;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        ComplianceStandard standard = complianceStandardService.getByRegulationNumberAndName(regulationNumber,
                standardName);
        cache.put(key, standard);
        return standard;
    }

    private ParameterGroup findGroupByName(String standardId, String groupName) {
        List<ParameterGroup> groups = parameterGroupService.getGroupsByStandardId(standardId);
        if (groups == null) {
            return null;
        }
        for (ParameterGroup g : groups) {
            if (groupName.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    private void applyCsvFields(ParameterGroup group, String[] values, Map<String, Integer> columnIndices) {
        // name is set by the caller; revisit other columns here.

        // README authoring rule: "Empty cell = null" for nullable fields.
        // If the CSV file declares the column at all, an empty cell means the
        // admin wants to clear the value, so re-imports are idempotent. We
        // only leave the field alone when the column itself is absent from
        // the header (so files that don't bother with this column are
        // non-destructive).
        if (columnIndices.containsKey("description")) {
            String description = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("description"));
            group.setDescription(description.isEmpty() ? null : description);
        }

        String sortOrderStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("sortorder"));
        if (!sortOrderStr.isEmpty()) {
            try {
                group.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "applyCsvFields", "Invalid sortOrder '" + sortOrderStr
                        + "' for group '" + group.getName() + "'; leaving previous value.");
            }
        }

        String isMandatoryStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("ismandatory"));
        if (!isMandatoryStr.isEmpty()) {
            group.setIsMandatory(parseBoolean(isMandatoryStr));
        }

        group.setSysUserId("1");
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private enum LineResult {
        CREATED, UPDATED, SKIPPED
    }
}
