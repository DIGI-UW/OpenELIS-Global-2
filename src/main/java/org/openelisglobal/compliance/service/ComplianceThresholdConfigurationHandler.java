package org.openelisglobal.compliance.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain configuration handler for compliance <em>thresholds</em>.
 *
 * Loads {@link ComplianceThreshold} rows from CSV files in
 * {@code configuration/backend/compliance-standards/} that match the file
 * pattern {@code *-thresholds.csv}. Runs after the parameter-groups handler
 * (load order 220) so parent {@link ParameterGroup} rows already exist.
 *
 * Required columns (case-insensitive): {@code regulationNumber, standardName,
 * groupName, parameterCode, displayName, thresholdType}. Optional:
 * {@code minValue, maxValue, targetValue, valueDescriptive, units,
 * methodReference, detectionLimit, isMandatory, sortOrder, validationRules,
 * notes, testName}. {@code testName} pre-links the threshold to a real Test row
 * (matched by {@link TestService#getTestByName}); deployments without a
 * matching test get a WARN and the threshold stays template-level (null
 * {@code test_id}).
 *
 * Lookup keys:
 * <ul>
 * <li>standard: {@code (regulationNumber, standardName)}.</li>
 * <li>group: {@code (standardId, groupName)}.</li>
 * <li>threshold: {@code (groupId, parameterCode, thresholdType)} — matches the
 * multi-limit uniqueness invariant in
 * {@link ComplianceThresholdServiceImpl#save}.</li>
 * </ul>
 *
 * SELECT_MAP rows attach option mappings via the sibling
 * {@link ComplianceThresholdValueMapConfigurationHandler}; this handler only
 * upserts the parent SELECT_MAP threshold row.
 */
@Component
public class ComplianceThresholdConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ParameterGroupService parameterGroupService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    // Optional resolution: when the CSV declares a testName, the loader pre-
    // links the threshold to a real Test from the catalog so the Standards
    // list shows a non-zero Tests count without manual UI work. If the named
    // test isn't in the catalog, we log and leave testName null — the seed
    // file shouldn't fail-fast just because the deployment doesn't ship a
    // matching test row.
    @Autowired
    private TestService testService;

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
        return 220;
    }

    @Override
    public String getFileMatcher() {
        return "*-thresholds.csv";
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Skip leading blank/# comment lines so the README's authoring rules
        // apply to the header position too.
        ComplianceCsvUtil.HeaderRead header = ComplianceCsvUtil.readHeaderLine(reader, fileName,
                "Compliance thresholds configuration file");

        String[] headers = ComplianceCsvUtil.parseCsvLine(header.line);
        Map<String, Integer> columnIndices = ComplianceCsvUtil.createColumnMap(headers);
        validateHeaders(columnIndices, fileName);

        Map<String, ComplianceStandard> standardCache = new HashMap<>();
        Map<String, ParameterGroup> groupCache = new HashMap<>();

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
                LineResult result = processLine(values, columnIndices, standardCache, groupCache, lineNumber, fileName);
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

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration", "Loaded thresholds from " + fileName
                + " (created=" + created + ", updated=" + updated + ", skipped=" + skipped + ")");
    }

    private void validateHeaders(Map<String, Integer> columnIndices, String fileName) {
        for (String required : new String[] { "regulationnumber", "standardname", "groupname", "parametercode",
                "displayname", "thresholdtype" }) {
            if (!columnIndices.containsKey(required)) {
                throw new IllegalArgumentException("Compliance thresholds configuration file " + fileName
                        + " must have a '" + required + "' column");
            }
        }
    }

    private LineResult processLine(String[] values, Map<String, Integer> columnIndices,
            Map<String, ComplianceStandard> standardCache, Map<String, ParameterGroup> groupCache, int lineNumber,
            String fileName) {
        String regulationNumber = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("regulationnumber"));
        String standardName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("standardname"));
        String groupName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("groupname"));
        String parameterCode = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("parametercode"));
        String displayName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("displayname"));
        String thresholdTypeStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("thresholdtype"));

        if (regulationNumber.isEmpty() || standardName.isEmpty() || groupName.isEmpty() || parameterCode.isEmpty()
                || displayName.isEmpty() || thresholdTypeStr.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine",
                    "Skipping row " + lineNumber + " in " + fileName + ": one or more required fields are blank");
            return LineResult.SKIPPED;
        }

        ThresholdType thresholdType;
        try {
            // Use the lenient import-token parser so the FRS aliases the enum
            // documents (HIGH/LOW, MAX/MIN, "Select Mapping", "select-map",
            // "Qualitative", display names) all resolve. valueOf() rejected
            // every one of those silently.
            thresholdType = ThresholdType.fromImportToken(thresholdTypeStr);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine", "Skipping row " + lineNumber + " in "
                    + fileName + ": unknown thresholdType '" + thresholdTypeStr + "'");
            return LineResult.SKIPPED;
        }

        ParameterGroup group = resolveGroup(standardCache, groupCache, regulationNumber, standardName, groupName);
        if (group == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLine",
                    "Skipping row " + lineNumber + " in " + fileName + ": no parameter group matches (" + standardName
                            + " / " + groupName + "). Load standards and groups CSVs first.");
            return LineResult.SKIPPED;
        }

        ComplianceThreshold existing = findThreshold(group.getId(), parameterCode, thresholdType);
        if (existing != null) {
            existing.setDisplayName(displayName);
            applyCsvFields(existing, values, columnIndices);
            complianceThresholdService.update(existing);
            return LineResult.UPDATED;
        }

        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setGroup(group);
        threshold.setParameterCode(parameterCode);
        threshold.setDisplayName(displayName);
        threshold.setThresholdType(thresholdType);
        applyCsvFields(threshold, values, columnIndices);
        complianceThresholdService.insert(threshold);
        return LineResult.CREATED;
    }

    private ParameterGroup resolveGroup(Map<String, ComplianceStandard> standardCache,
            Map<String, ParameterGroup> groupCache, String regulationNumber, String standardName, String groupName) {
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
        if (groupCache.containsKey(groupKey)) {
            return groupCache.get(groupKey);
        }
        ParameterGroup match = null;
        List<ParameterGroup> groups = parameterGroupService.getGroupsByStandardId(standard.getId());
        if (groups != null) {
            for (ParameterGroup g : groups) {
                if (groupName.equals(g.getName())) {
                    match = g;
                    break;
                }
            }
        }
        groupCache.put(groupKey, match);
        return match;
    }

    private ComplianceThreshold findThreshold(String groupId, String parameterCode, ThresholdType type) {
        List<ComplianceThreshold> thresholds = complianceThresholdService.getThresholdsByGroupId(groupId);
        if (thresholds == null) {
            return null;
        }
        for (ComplianceThreshold t : thresholds) {
            if (parameterCode.equals(t.getParameterCode()) && type == t.getThresholdType()) {
                return t;
            }
        }
        return null;
    }

    private void applyCsvFields(ComplianceThreshold threshold, String[] values, Map<String, Integer> columnIndices) {
        threshold.setMinValue(parseBigDecimal(values, columnIndices.get("minvalue")));
        threshold.setMaxValue(parseBigDecimal(values, columnIndices.get("maxvalue")));
        threshold.setTargetValue(parseBigDecimal(values, columnIndices.get("targetvalue")));
        threshold.setDetectionLimit(parseBigDecimal(values, columnIndices.get("detectionlimit")));

        String valueDescriptive = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("valuedescriptive"));
        if (!valueDescriptive.isEmpty()) {
            threshold.setValueDescriptive(valueDescriptive);
        }

        String units = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("units"));
        if (!units.isEmpty()) {
            threshold.setUnits(units);
        }

        String methodReference = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("methodreference"));
        if (!methodReference.isEmpty()) {
            threshold.setMethodReference(methodReference);
        }

        String validationRules = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("validationrules"));
        if (!validationRules.isEmpty()) {
            threshold.setValidationRules(validationRules);
        }

        String notes = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("notes"));
        if (!notes.isEmpty()) {
            threshold.setNotes(notes);
        }

        String isMandatoryStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("ismandatory"));
        if (!isMandatoryStr.isEmpty()) {
            threshold.setIsMandatory(parseBoolean(isMandatoryStr));
        }

        String sortOrderStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("sortorder"));
        if (!sortOrderStr.isEmpty()) {
            try {
                threshold.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "applyCsvFields", "Invalid sortOrder '" + sortOrderStr
                        + "' for threshold '" + threshold.getParameterCode() + "'; leaving previous value.");
            }
        }

        // Optional: resolve the parameter to a real Test in the catalog so
        // the threshold counts toward the Standards-list Tests column on
        // first load. Missing matches log at WARN and leave the threshold
        // as a template (test_id null) — the seed shouldn't fail just
        // because a deployment hasn't loaded a matching test row.
        String testName = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("testname"));
        if (!testName.isEmpty()) {
            Test resolved = testService.getTestByName(testName);
            if (resolved != null) {
                threshold.setTest(resolved);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "applyCsvFields",
                        "Threshold '" + threshold.getParameterCode() + "' references testName '" + testName
                                + "' which is not in the test catalog; leaving threshold unlinked.");
            }
        }

        threshold.setSysUserId("1");
    }

    private BigDecimal parseBigDecimal(String[] values, Integer index) {
        String raw = ComplianceCsvUtil.getValueOrEmpty(values, index);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "parseBigDecimal",
                    "Invalid numeric value '" + raw + "'; treating as null.");
            return null;
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private enum LineResult {
        CREATED, UPDATED, SKIPPED
    }
}
