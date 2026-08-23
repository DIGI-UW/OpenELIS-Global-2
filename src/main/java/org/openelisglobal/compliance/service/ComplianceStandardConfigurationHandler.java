package org.openelisglobal.compliance.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain configuration handler for compliance <em>standards</em>.
 *
 * Loads {@link ComplianceStandard} rows from CSV files in
 * {@code configuration/backend/compliance-standards/} that match the file
 * pattern {@code *-standards.csv}. Sibling handlers in the same folder load the
 * rest of the standard's content:
 * <ul>
 * <li>{@code *-parameter-groups.csv} →
 * {@link ComplianceParameterGroupConfigurationHandler}</li>
 * <li>{@code *-thresholds.csv} →
 * {@link ComplianceThresholdConfigurationHandler}</li>
 * <li>{@code *-threshold-value-maps.csv} →
 * {@link ComplianceThresholdValueMapConfigurationHandler}</li>
 * </ul>
 * Load order is enforced via {@link #getLoadOrder()} so each child handler sees
 * its parents persisted before it runs.
 *
 * Supported columns (case-insensitive; only {@code name} and
 * {@code regulationNumber} are required):
 *
 * <pre>
 * name, regulationNumber, issuingBody, version,
 * effectiveDate, expiryDate (or expirationDate),
 * countryRegion, applicableSampleTypes (comma-delimited within the cell),
 * status, description, regulatoryContext, enforcementAuthority,
 * supersededById, isPreSeeded,
 * localization:&lt;locale&gt; (e.g. localization:en, localization:id)
 * </pre>
 *
 * Lines starting with {@code #} and blank lines are ignored. Dates use ISO
 * {@code yyyy-MM-dd}. Per FR-6-003, every row created from a seed file has
 * {@code isPreSeeded} forced to true (CSV value is ignored on creates) so the
 * runtime delete protection applies.
 */
@Component
public class ComplianceStandardConfigurationHandler implements DomainConfigurationHandler {

    private static final String LOCALIZATION_COLUMN_PREFIX = "localization:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

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
        // 200 places this after base entities (sample types, test sections,
        // 100-tier) and before sibling compliance handlers (210/220/230) that
        // depend on the standard row existing.
        return 200;
    }

    @Override
    public String getFileMatcher() {
        // Sibling handlers in this same domain folder match on different
        // suffixes so each CSV file routes to exactly one handler.
        return "*-standards.csv";
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Skip leading blank/# comment lines so the README's authoring rules
        // ("blank lines and # comment lines are skipped") apply to the header
        // position too. Carry the discovered line number forward so error
        // messages downstream stay aligned with the source file.
        ComplianceCsvUtil.HeaderRead header = ComplianceCsvUtil.readHeaderLine(reader, fileName,
                "Compliance standards configuration file");

        String[] headers = ComplianceCsvUtil.parseCsvLine(header.line);
        validateHeaders(headers, fileName);

        Map<String, Integer> columnIndices = ComplianceCsvUtil.createColumnMap(headers);
        Map<String, Integer> localizationColumns = detectLocalizationColumns(headers);

        List<ComplianceStandard> processedStandards = new ArrayList<>();
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
                CsvLineResult lineResult = processCsvLine(values, columnIndices, localizationColumns);
                if (lineResult == null || lineResult.standard == null) {
                    skipped++;
                    continue;
                }
                if (lineResult.created) {
                    created++;
                } else {
                    updated++;
                }
                processedStandards.add(lineResult.standard);
            } catch (Exception e) {
                // FR-6-006: log at WARN and continue - partial loads are
                // acceptable; a single bad row must not poison the whole file.
                skipped++;
                LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                        "Skipped row " + lineNumber + " in " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Loaded " + processedStandards.size() + " compliance standards from " + fileName + " (created="
                        + created + ", updated=" + updated + ", skipped=" + skipped + ")");
    }

    /** Tuple returned by processCsvLine to surface created vs updated counts. */
    private static final class CsvLineResult {
        final ComplianceStandard standard;
        final boolean created;

        CsvLineResult(ComplianceStandard standard, boolean created) {
            this.standard = standard;
            this.created = created;
        }
    }

    /**
     * Detects localization columns from headers
     */
    private Map<String, Integer> detectLocalizationColumns(String[] headers) {
        Map<String, Integer> localizationColumns = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();
            if (header.startsWith(LOCALIZATION_COLUMN_PREFIX)) {
                String locale = header.substring(LOCALIZATION_COLUMN_PREFIX.length());
                if (!locale.isEmpty()) {
                    localizationColumns.put(locale, i);
                }
            }
        }
        return localizationColumns;
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasNameColumn = false;
        boolean hasRegulationNumberColumn = false;

        for (String header : headers) {
            if ("name".equalsIgnoreCase(header.trim())) {
                hasNameColumn = true;
            }
            if ("regulationNumber".equalsIgnoreCase(header.trim())) {
                hasRegulationNumberColumn = true;
            }
        }

        if (!hasNameColumn) {
            throw new IllegalArgumentException(
                    "Compliance standards configuration file " + fileName + " must have a 'name' column");
        }
        if (!hasRegulationNumberColumn) {
            throw new IllegalArgumentException(
                    "Compliance standards configuration file " + fileName + " must have a 'regulationNumber' column");
        }
    }

    private CsvLineResult processCsvLine(String[] values, Map<String, Integer> columnIndices,
            Map<String, Integer> localizationColumns) {

        String name = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("name"));
        String regulationNumber = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("regulationnumber"));

        if (name.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing name");
            return null;
        }

        if (regulationNumber.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row with missing regulationNumber");
            return null;
        }

        // FR-6-005 idempotent loading: skip-or-update if already in the DB
        // (matched by regulationNumber + name) so administrator customizations
        // are preserved across application restarts.
        ComplianceStandard existing = complianceStandardService.getByRegulationNumberAndName(regulationNumber, name);

        if (existing != null) {
            updateStandardFromCsv(existing, values, columnIndices, localizationColumns);
            complianceStandardService.update(existing);
            return new CsvLineResult(existing, false);
        } else {
            ComplianceStandard newStandard = new ComplianceStandard();
            updateStandardFromCsv(newStandard, values, columnIndices, localizationColumns);
            // FR-6-003: anything created from a seed file is flagged as
            // pre-seeded regardless of what the CSV declares so the runtime
            // delete protection (commit 1 baseline) applies.
            newStandard.setIsPreSeeded(true);
            String standardId = complianceStandardService.insert(newStandard);
            newStandard.setId(standardId);
            return new CsvLineResult(newStandard, true);
        }
    }

    private void updateStandardFromCsv(ComplianceStandard standard, String[] values, Map<String, Integer> columnIndices,
            Map<String, Integer> localizationColumns) {

        standard.setName(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("name")));
        standard.setRegulationNumber(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("regulationnumber")));
        standard.setIssuingBody(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("issuingbody")));
        standard.setVersion(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("version")));
        standard.setCountryRegion(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("countryregion")));
        standard.setDescription(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("description")));
        standard.setRegulatoryContext(
                ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("regulatorycontext")));
        standard.setEnforcementAuthority(
                ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("enforcementauthority")));
        // Only touch sampleTypes when the column is actually present in the
        // CSV — calling the setter clears the join-table collection, which
        // would silently wipe out admin-configured sample types on update.
        Integer sampleTypesIdx = columnIndices.get("applicablesampletypes");
        if (sampleTypesIdx != null) {
            standard.setApplicableSampleTypes(ComplianceCsvUtil.getValueOrEmpty(values, sampleTypesIdx));
        }
        standard.setSupersededById(ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("supersededbyid")));

        setDateField(standard, "effectiveDate", values, columnIndices.get("effectivedate"));
        // Accept both column-name conventions: the entity property is expiryDate
        // but historical CSVs (and this handler's older docs) used expirationDate.
        Integer expiryIdx = columnIndices.get("expirydate");
        if (expiryIdx == null) {
            expiryIdx = columnIndices.get("expirationdate");
        }
        setDateField(standard, "expiryDate", values, expiryIdx);

        // Handle status
        String statusStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("status"));
        if (!statusStr.isEmpty()) {
            try {
                standard.setStatus(ComplianceStandard.ComplianceStandardStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateStandardFromCsv",
                        "Invalid status value: " + statusStr + ", defaulting to ACTIVE");
                standard.setStatus(ComplianceStandard.ComplianceStandardStatus.ACTIVE);
            }
        } else {
            standard.setStatus(ComplianceStandard.ComplianceStandardStatus.ACTIVE);
        }

        // Handle isPreSeeded boolean
        String preSeededStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndices.get("ispreseeded"));
        if ("true".equalsIgnoreCase(preSeededStr) || "1".equals(preSeededStr) || "yes".equalsIgnoreCase(preSeededStr)) {
            standard.setIsPreSeeded(true);
        } else {
            standard.setIsPreSeeded(false);
        }

        // Generate UUID if not set
        if (standard.getFhirUuid() == null) {
            standard.setFhirUuid(UUID.randomUUID());
        }

        standard.setSysUserId("1"); // System user for configuration loading

        // Handle localization
        processLocalization(standard, values, localizationColumns);
    }

    private void setDateField(ComplianceStandard standard, String fieldName, String[] values, Integer columnIndex) {
        String dateStr = ComplianceCsvUtil.getValueOrEmpty(values, columnIndex);
        if (!dateStr.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                if ("effectiveDate".equals(fieldName)) {
                    standard.setEffectiveDate(date);
                } else if ("expiryDate".equals(fieldName)) {
                    standard.setExpiryDate(date);
                }
            } catch (DateTimeParseException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "setDateField",
                        "Invalid date format for " + fieldName + ": " + dateStr + ", expected yyyy-MM-dd");
            }
        }
    }

    /**
     * Processes localization columns. Currently a no-op: ComplianceStandard does
     * not yet support localized names. When the entity gains a Localization
     * relationship, populate translations here from {@code localizationColumns}.
     */
    private void processLocalization(ComplianceStandard standard, String[] values,
            Map<String, Integer> localizationColumns) {
        // intentionally empty until ComplianceStandard supports localization
    }
}