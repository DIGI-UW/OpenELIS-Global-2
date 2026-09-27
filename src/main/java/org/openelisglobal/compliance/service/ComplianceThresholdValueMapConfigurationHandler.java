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
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ComplianceThresholdValueMap;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain configuration handler for compliance <em>threshold value mappings</em>
 * (the {@code optionValue → ComplianceStatus} table that powers SELECT_MAP
 * thresholds).
 *
 * Loads {@link ComplianceThresholdValueMap} rows from CSV files in
 * {@code configuration/backend/compliance-standards/} that match the file
 * pattern {@code *-threshold-value-maps.csv}. Runs after the thresholds handler
 * (load order 230) so each parent threshold already exists.
 *
 * Required columns (case-insensitive): {@code regulationNumber, standardName,
 * groupName, parameterCode, optionValue, complianceStatus}.
 *
 * The parent threshold is resolved by
 * {@code (groupId, parameterCode, thresholdType=SELECT_MAP)}; rows whose parent
 * threshold is missing or is not a SELECT_MAP type are skipped with a WARN.
 * Mappings persist via the cascade on
 * {@link ComplianceThreshold#getValueMappings()} (cascade=ALL,
 * orphanRemoval=true), so we update the parent threshold after appending or
 * editing entries in its mapping list.
 */
@Component
public class ComplianceThresholdValueMapConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

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
        return 230;
    }

    @Override
    public String getFileMatcher() {
        return "*-threshold-value-maps.csv";
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Skip leading blank/# comment lines so the README's authoring rules
        // apply to the header position too.
        ComplianceCsvUtil.HeaderRead header = ComplianceCsvUtil.readHeaderLine(reader, fileName,
                "Compliance threshold-value-maps configuration file");

        String[] headers = ComplianceCsvUtil.parseCsvLine(header.line);
        Map<String, Integer> columnIndices = ComplianceCsvUtil.createColumnMap(headers);
        validateHeaders(columnIndices, fileName);

        Map<String, ComplianceStandard> standardCache = new HashMap<>();
        Map<String, ParameterGroup> groupCache = new HashMap<>();
        Map<String, ComplianceThreshold> thresholdCache = new HashMap<>();

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
                LineResult result = processLine(values, columnIndices, standardCache, groupCache, thresholdCache,
                        lineNumber, fileName);
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

        // Each entry in thresholdCache had its valueMappings list mutated;
        // persist those changes once per parent threshold so the cascade
        // writes the value-map rows.
        for (ComplianceThreshold threshold : thresholdCache.values()) {
            if (threshold != null) {
                complianceThresholdService.update(threshold);
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Loaded threshold value mappings from " + fileName + " (created=" + created + ", updated=" + updated
                        + ", skipped=" + skipped + ")");
    }

    private void validateHeaders(Map<String, Integer> columnIndices, String fileName) {
        for (String required : new String[] { "regulationnumber", "standardname", "groupname", "parametercode",
                "optionvalue", "compliancestatus" }) {
            if (!columnIndices.containsKey(required)) {
                throw new IllegalArgumentException("Compliance threshold-value-maps configuration file " + fileName
                        + " must have a '" + required + "' column");
            }
        }
    }

    private LineResult processLine(String[] values, Map<String, Integer> columnIndices,
            Map<String, ComplianceStandard> standardCache, Map<String, ParameterGroup> groupCache,
            Map<String, ComplianceThreshold> thresholdCache, int lineNumber, String fileName) {
        String regulationNumber = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("regulationnumber"));
        String standardName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("standardname"));
        String groupName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("groupname"));
        String parameterCode = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("parametercode"));
        String optionValue = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("optionvalue"));
        String statusStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("compliancestatus"));

        if (regulationNumber.isEmpty() || standardName.isEmpty() || groupName.isEmpty() || parameterCode.isEmpty()
                || optionValue.isEmpty() || statusStr.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine",
                    "Skipping row " + lineNumber + " in " + fileName + ": one or more required fields are blank");
            return LineResult.SKIPPED;
        }

        ComplianceStatus status;
        try {
            // Lenient parse: accepts NON_COMPLIANT (enum), Non-Compliant
            // (display name), and the common typed-by-hand variants in
            // between. Strict valueOf() silently dropped valid rows.
            status = ComplianceStatus.parse(statusStr);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine", "Skipping row " + lineNumber + " in "
                    + fileName + ": unknown complianceStatus '" + statusStr + "'");
            return LineResult.SKIPPED;
        }

        ComplianceThreshold threshold = resolveThreshold(standardCache, groupCache, thresholdCache, regulationNumber,
                standardName, groupName, parameterCode);
        if (threshold == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine",
                    "Skipping row " + lineNumber + " in " + fileName + ": no SELECT_MAP threshold matches ("
                            + standardName + " / " + groupName + " / " + parameterCode
                            + "). Ensure a SELECT_MAP threshold exists in the thresholds CSV.");
            return LineResult.SKIPPED;
        }

        ComplianceThresholdValueMap existing = findMapping(threshold, optionValue);
        if (existing != null) {
            existing.setComplianceStatus(status);
            existing.setSysUserId("1");
            return LineResult.UPDATED;
        }

        ComplianceThresholdValueMap mapping = new ComplianceThresholdValueMap(optionValue, status);
        mapping.setThreshold(threshold);
        mapping.setSysUserId("1");
        threshold.getValueMappings().add(mapping);
        return LineResult.CREATED;
    }

    private ComplianceThreshold resolveThreshold(Map<String, ComplianceStandard> standardCache,
            Map<String, ParameterGroup> groupCache, Map<String, ComplianceThreshold> thresholdCache,
            String regulationNumber, String standardName, String groupName, String parameterCode) {
        String standardKey = regulationNumber + "||" + standardName;
        ComplianceStandard standard;
        if (standardCache.containsKey(standardKey)) {
            standard = standardCache.get(standardKey);
        } else {
            standard = complianceStandardService.getByRegulationNumberAndName(regulationNumber, standardName);
            standardCache.put(standardKey, standard);
        }
        if (standard == null) {
            return null;
        }

        String groupKey = standard.getId() + "||" + groupName;
        ParameterGroup group;
        if (groupCache.containsKey(groupKey)) {
            group = groupCache.get(groupKey);
        } else {
            group = null;
            List<ParameterGroup> groups = parameterGroupService.getGroupsByStandardId(standard.getId());
            if (groups != null) {
                for (ParameterGroup g : groups) {
                    if (groupName.equals(g.getName())) {
                        group = g;
                        break;
                    }
                }
            }
            groupCache.put(groupKey, group);
        }
        if (group == null) {
            return null;
        }

        String thresholdKey = group.getId() + "||" + parameterCode;
        if (thresholdCache.containsKey(thresholdKey)) {
            return thresholdCache.get(thresholdKey);
        }
        ComplianceThreshold match = null;
        List<ComplianceThreshold> thresholds = complianceThresholdService.getThresholdsByGroupId(group.getId());
        if (thresholds != null) {
            for (ComplianceThreshold t : thresholds) {
                if (parameterCode.equals(t.getParameterCode()) && t.getThresholdType() == ThresholdType.SELECT_MAP) {
                    match = t;
                    break;
                }
            }
        }
        thresholdCache.put(thresholdKey, match);
        return match;
    }

    private ComplianceThresholdValueMap findMapping(ComplianceThreshold threshold, String optionValue) {
        List<ComplianceThresholdValueMap> mappings = threshold.getValueMappings();
        if (mappings == null) {
            return null;
        }
        for (ComplianceThresholdValueMap m : mappings) {
            if (optionValue.equals(m.getOptionValue())) {
                return m;
            }
        }
        return null;
    }

    private enum LineResult {
        CREATED, UPDATED, SKIPPED
    }
}
