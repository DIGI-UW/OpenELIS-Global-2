package org.openelisglobal.microbiology.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroBreakpointImportErrorForm;
import org.openelisglobal.microbiology.form.MicroBreakpointImportPreviewForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBreakpointImportServiceImpl implements MicroBreakpointImportService {

    private static final List<String> REQUIRED_HEADERS = List.of("publisher", "version", "organism_or_group",
            "antibiotic_whonet_code", "method", "specimen_type_id", "breakpoint_type", "susceptible_value",
            "intermediate_lower_value", "intermediate_upper_value", "resistant_value", "units");
    private static final List<String> AUTHORITIES = List.of("CLSI", "EUCAST");
    private static final List<String> METHODS = List.of("MIC", "ZONE", "DISK_DIFFUSION", "ETEST");
    private static final List<String> BREAKPOINT_TYPES = List.of("MIC", "ZONE");

    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroBreakpointStandardDAO standardDAO;
    private final MicroBreakpointRuleDAO ruleDAO;
    private final TypeOfSampleService typeOfSampleService;
    private final Map<String, ImportPreview> previews = new ConcurrentHashMap<>();

    public MicroBreakpointImportServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO,
            TypeOfSampleService typeOfSampleService) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
        this.typeOfSampleService = typeOfSampleService;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointImportPreviewForm preview(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("CSV content is required");
        }
        ImportPreview parsed = parse(csv);
        String token = UUID.randomUUID().toString();
        previews.put(token, parsed);
        return toForm(token, parsed, parsed.validRows.size(), 0, 0);
    }

    @Override
    @Transactional
    public MicroBreakpointImportPreviewForm apply(String previewToken, String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("authenticated actor is required");
        }
        ImportPreview preview = previews.get(previewToken);
        if (preview == null) {
            throw new IllegalArgumentException("Import preview has expired or does not exist");
        }

        return applyRows(preview, previewToken, actorId, null);
    }

    @Override
    @Transactional
    public MicroBreakpointImportPreviewForm applyConfiguration(String csv, String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("Configuration actor is required");
        }
        ImportPreview preview = parse(csv);
        if (!preview.errors.isEmpty() || preview.validRows.isEmpty()) {
            throw new IllegalArgumentException("Invalid configuration breakpoints: "
                    + preview.errors.stream().map(e -> e.message).collect(java.util.stream.Collectors.joining("; ")));
        }
        Map<String, List<String>> editionRows = new LinkedHashMap<>();
        for (ImportRow row : preview.validRows) {
            editionRows.computeIfAbsent(editionKey(row), ignored -> new ArrayList<>()).add(row.sourceHash);
        }
        Map<String, String> configurationHashes = new LinkedHashMap<>();
        editionRows.forEach((key, rows) -> configurationHashes.put(key,
                hash(rows.stream().sorted().collect(java.util.stream.Collectors.joining("\n")))));
        var checkedEditions = new java.util.HashSet<String>();
        for (ImportRow row : preview.validRows) {
            if (!checkedEditions.add(editionKey(row))) {
                continue;
            }
            var existing = standardDAO.findByAuthorityAndVersion(row.authority, row.version);
            if (existing.isPresent()
                    && !configurationHashes.get(editionKey(row)).equals(existing.get().getConfigurationHash())) {
                throw new IllegalArgumentException(
                        "Standard edition already belongs to different content: " + row.version);
            }
        }
        var result = applyRows(preview, null, actorId, configurationHashes);
        if (!result.errors.isEmpty()) {
            throw new IllegalArgumentException("Configuration conflicts with an existing breakpoint");
        }
        return result;
    }

    private MicroBreakpointImportPreviewForm applyRows(ImportPreview preview, String previewToken, String actorId,
            Map<String, String> configurationHashes) {
        int imported = 0;
        int unchanged = 0;
        List<Integer> unchangedLines = new ArrayList<>();
        List<Integer> importedLines = new ArrayList<>();
        Map<String, MicroBreakpointStandard> standards = new java.util.HashMap<>();
        for (ImportRow row : preview.validRows) {
            String key = editionKey(row);
            String configurationHash = configurationHashes == null ? null : configurationHashes.get(key);
            MicroBreakpointStandard standard = standards.computeIfAbsent(key,
                    ignored -> findOrCreateStandard(row.authority, row.version, actorId, configurationHash != null));
            if (standard.getConfigurationHash() != null && !standard.getConfigurationHash().equals(configurationHash)) {
                throw new IllegalArgumentException("Configuration-managed standards cannot be changed by CSV upload");
            }
            if (configurationHash != null && standard.getConfigurationHash() == null) {
                standard.setConfigurationHash(configurationHash);
                standardDAO.update(standard);
            }
            Optional<MicroBreakpointRule> existingHash = ruleDAO.findBySourceRowHash(row.sourceHash);
            if (existingHash.isPresent()) {
                unchanged++;
                unchangedLines.add(row.rowNumber);
                ruleDAO.evict(existingHash.get());
                continue;
            }
            Optional<MicroBreakpointRule> existingRule = ruleDAO.findByNaturalKey(standard.getId(), row.organismId,
                    row.organismGroup, row.antibioticId, row.method, row.specimenTypeId, row.breakpointType);
            if (existingRule.isPresent() && existingRule.get().isLocallyCustomized()) {
                preview.errors.add(error(row.rowNumber,
                        "A locally customized breakpoint exists for this organism, antibiotic, and method",
                        row.sourceRow));
                ruleDAO.evict(existingRule.get());
                continue;
            }
            MicroBreakpointRule rule = toRule(row, standard.getId(), actorId,
                    existingRule.orElseGet(MicroBreakpointRule::new));
            if (existingRule.isPresent()) {
                ruleDAO.update(rule);
            } else {
                ruleDAO.insert(rule);
            }
            // BaseDAO flushes each write. Completed rules must not remain in the
            // persistence context and be dirty-checked again for every later row.
            ruleDAO.evict(rule);
            imported++;
            importedLines.add(row.rowNumber);
        }
        var result = toForm(previewToken, preview, imported + unchanged, imported, unchanged);
        result.importedRowNumbers = importedLines;
        result.unchangedRowNumbers = unchangedLines;
        return result;
    }

    private ImportPreview parse(String csv) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
                .setTrim(true).build();
        try (CSVParser parser = CSVParser.parse(csv, format)) {
            validateHeaders(parser.getHeaderMap());
            ImportPreview preview = new ImportPreview();
            List<ImportRow> parsedRows = new ArrayList<>();
            Map<List<String>, Optional<MicroOrganism>> organisms = new java.util.HashMap<>();
            Map<List<String>, Optional<MicroAntibiotic>> antibiotics = new java.util.HashMap<>();
            for (CSVRecord record : parser) {
                preview.totalRows++;
                try {
                    parsedRows.add(parseRow(record, organisms, antibiotics));
                } catch (IllegalArgumentException exception) {
                    preview.errors.add(error((int) record.getRecordNumber() + 1, exception.getMessage(),
                            CSVFormat.DEFAULT.format(record.values())));
                }
            }
            Map<String, List<ImportRow>> rowsByNaturalKey = new LinkedHashMap<>();
            for (ImportRow row : parsedRows) {
                rowsByNaturalKey.computeIfAbsent(naturalKey(row), ignored -> new ArrayList<>()).add(row);
            }
            for (List<ImportRow> rows : rowsByNaturalKey.values()) {
                if (rows.size() == 1) {
                    preview.validRows.add(rows.get(0));
                } else {
                    for (ImportRow row : rows) {
                        preview.errors.add(error(row.rowNumber, "Duplicate breakpoint key in import", row.sourceRow));
                    }
                }
            }
            return preview;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read breakpoint CSV", exception);
        }
    }

    private ImportRow parseRow(CSVRecord record, Map<List<String>, Optional<MicroOrganism>> organisms,
            Map<List<String>, Optional<MicroAntibiotic>> antibiotics) {
        ImportRow row = new ImportRow();
        row.rowNumber = (int) record.getRecordNumber() + 1;
        row.sourceRow = CSVFormat.DEFAULT.format(record.values());
        row.authority = required(record, "publisher").toUpperCase(Locale.ROOT);
        if (!AUTHORITIES.contains(row.authority)) {
            throw new IllegalArgumentException("Unsupported publisher: " + row.authority);
        }
        row.version = required(record, "version");

        String organismValue = required(record, "organism_or_group");
        if (record.isMapped("organism_code") && !record.get("organism_code").isBlank()) {
            var key = List.of(required(record, "organism_source"), required(record, "organism_code"));
            row.organismId = organisms
                    .computeIfAbsent(key, ignored -> organismDAO.findByReference(key.get(0), key.get(1)))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown organism source code")).getId();
        } else if (organismValue.regionMatches(true, 0, "group:", 0, "group:".length())) {
            String requestedGroup = requiredText(organismValue.substring("group:".length()), "organism group");
            row.organismGroup = organismDAO.getAll().stream().map(MicroOrganism::getOrganismGroup)
                    .filter(group -> group != null && group.equalsIgnoreCase(requestedGroup)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown organism group: " + requestedGroup));
        } else {
            MicroOrganism organism = organismDAO.findByDisplayNameIgnoreCase(organismValue)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown organism: " + organismValue));
            row.organismId = organism.getId();
        }

        String antibioticCode = required(record, "antibiotic_whonet_code").toUpperCase(Locale.ROOT);
        MicroAntibiotic antibiotic;
        if (record.isMapped("antibiotic_code") && !record.get("antibiotic_code").isBlank()) {
            var key = List.of(required(record, "antibiotic_source"), required(record, "antibiotic_code"));
            antibiotic = antibiotics
                    .computeIfAbsent(key, ignored -> antibioticDAO.findByReference(key.get(0), key.get(1)))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown antibiotic source code"));
        } else {
            antibiotic = antibioticDAO.findByWhonetCodeIgnoreCase(antibioticCode)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown antibiotic code: " + antibioticCode));
        }
        row.antibioticId = antibiotic.getId();

        String rawMethod = required(record, "method").toUpperCase(Locale.ROOT);
        if (!METHODS.contains(rawMethod)) {
            throw new IllegalArgumentException("Unsupported method: " + rawMethod);
        }
        row.method = switch (rawMethod) {
        case "DISK_DIFFUSION" -> "ZONE";
        case "ETEST" -> "MIC";
        default -> rawMethod;
        };
        row.specimenTypeId = blankToNull(record.get("specimen_type_id"));
        if (row.specimenTypeId != null && typeOfSampleService.getTypeOfSampleById(row.specimenTypeId) == null) {
            throw new IllegalArgumentException("Unknown specimen type: " + row.specimenTypeId);
        }
        row.breakpointType = required(record, "breakpoint_type").toUpperCase(Locale.ROOT);
        if (!BREAKPOINT_TYPES.contains(row.breakpointType)) {
            throw new IllegalArgumentException("Unsupported breakpoint type: " + row.breakpointType);
        }
        row.susceptibleValue = decimal(record, "susceptible_value");
        row.intermediateLowerValue = decimal(record, "intermediate_lower_value");
        row.intermediateUpperValue = decimal(record, "intermediate_upper_value");
        row.resistantValue = decimal(record, "resistant_value");
        row.units = required(record, "units");
        if (record.isMapped("resistant_inclusive") && !record.get("resistant_inclusive").isBlank()) {
            String inclusive = record.get("resistant_inclusive");
            if (!List.of("true", "false").contains(inclusive)) {
                throw new IllegalArgumentException("resistant_inclusive must be true or false");
            }
            row.resistantInclusive = Boolean.parseBoolean(inclusive);
        }
        row.notes = record.isMapped("notes") ? blankToNull(record.get("notes")) : null;
        validateThresholds(row);
        row.sourceHash = hash(normalizedRow(row));
        return row;
    }

    private void validateThresholds(ImportRow row) {
        if (!row.method.equals(row.breakpointType)) {
            throw new IllegalArgumentException("Method and breakpoint type must agree");
        }
        if (!("MIC".equals(row.method) ? List.of("mg/L", "ug/mL").contains(row.units) : "mm".equals(row.units))) {
            throw new IllegalArgumentException("Unsupported measurement units");
        }
        var values = java.util.stream.Stream
                .of(row.susceptibleValue, row.intermediateLowerValue, row.intermediateUpperValue, row.resistantValue)
                .filter(java.util.Objects::nonNull).toList();
        // An explicit empty rule blocks fallback and yields NO_BREAKPOINT in V2.
        if (!values.isEmpty() && (row.susceptibleValue == null || row.resistantValue == null)) {
            throw new IllegalArgumentException(
                    "Both susceptible and resistant thresholds are required for a nonempty rule");
        }
        for (BigDecimal value : values) {
            if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 4
                    || value.precision() - value.scale() > 8) {
                throw new IllegalArgumentException("Threshold cannot be retained exactly: " + value);
            }
        }
        if (row.susceptibleValue != null && row.resistantValue != null) {
            int comparison = row.susceptibleValue.compareTo(row.resistantValue);
            if (("MIC".equals(row.method) ? comparison > 0 : comparison < 0)
                    || (comparison == 0 && row.resistantInclusive)) {
                throw new IllegalArgumentException("Thresholds are out of order");
            }
        }
        if ((row.intermediateLowerValue == null) != (row.intermediateUpperValue == null)
                || (row.intermediateLowerValue != null
                        && row.intermediateLowerValue.compareTo(row.intermediateUpperValue) > 0)) {
            throw new IllegalArgumentException("Intermediate interval must have ordered lower and upper bounds");
        }
        if (row.intermediateLowerValue != null) {
            boolean mic = "MIC".equals(row.method);
            BigDecimal susceptibleEnd = mic ? row.intermediateLowerValue : row.intermediateUpperValue;
            BigDecimal resistantEnd = mic ? row.intermediateUpperValue : row.intermediateLowerValue;
            if (row.susceptibleValue != null && (mic ? susceptibleEnd.compareTo(row.susceptibleValue) <= 0
                    : susceptibleEnd.compareTo(row.susceptibleValue) >= 0)) {
                throw new IllegalArgumentException("Intermediate interval overlaps susceptible range");
            }
            if (row.resistantValue != null) {
                int comparison = resistantEnd.compareTo(row.resistantValue);
                if ((mic ? comparison > 0 : comparison < 0) || (comparison == 0 && row.resistantInclusive)) {
                    throw new IllegalArgumentException("Intermediate interval overlaps resistant range");
                }
            }
        }
    }

    private MicroBreakpointStandard findOrCreateStandard(String authority, String version, String actorId,
            boolean configurationReload) {
        Optional<MicroBreakpointStandard> existing = standardDAO.findByAuthorityAndVersion(authority, version);
        if (existing.isPresent()) {
            if (!configurationReload && "ARCHIVED".equals(existing.get().getLifecycleStatus())) {
                throw new IllegalArgumentException("Cannot import into archived breakpoint standard");
            }
            return existing.get();
        }
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setAuthority(authority);
        standard.setVersion(version);
        standard.setLifecycleStatus("LOADED");
        standard.setIsActive("N");
        standard.setLastUpdatedBy(actorId);
        standardDAO.insert(standard);
        return standard;
    }

    private MicroBreakpointRule toRule(ImportRow row, String standardId, String actorId, MicroBreakpointRule rule) {
        rule.setStandardId(standardId);
        rule.setOrganismId(row.organismId);
        rule.setOrganismGroup(row.organismGroup);
        rule.setAntibioticId(row.antibioticId);
        rule.setMethod(row.method);
        rule.setSpecimenTypeId(row.specimenTypeId);
        rule.setBreakpointType(row.breakpointType);
        rule.setSusceptibleValue(row.susceptibleValue);
        rule.setIntermediateLowerValue(row.intermediateLowerValue);
        rule.setIntermediateUpperValue(row.intermediateUpperValue);
        rule.setResistantValue(row.resistantValue);
        rule.setResistantInclusive(row.resistantInclusive);
        rule.setNotes(row.notes);
        rule.setUnits(row.units);
        rule.setSeeded(true);
        rule.setLocallyCustomized(false);
        rule.setSourceRowHash(row.sourceHash);
        rule.setLastUpdatedBy(actorId);
        rule.setIsActive("Y");
        return rule;
    }

    private MicroBreakpointImportPreviewForm toForm(String token, ImportPreview preview, int validRows, int imported,
            int unchanged) {
        MicroBreakpointImportPreviewForm form = new MicroBreakpointImportPreviewForm();
        form.previewToken = token;
        form.totalRows = preview.totalRows;
        form.validRows = validRows;
        form.skippedRows = preview.errors.size();
        form.importedRows = imported;
        form.unchangedRows = unchanged;
        form.errors = new ArrayList<>(preview.errors);
        return form;
    }

    private void validateHeaders(Map<String, Integer> headers) {
        List<String> missing = REQUIRED_HEADERS.stream().filter(header -> !headers.containsKey(header)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required CSV columns: " + String.join(", ", missing));
        }
    }

    private String required(CSVRecord record, String name) {
        return requiredText(record.get(name), name);
    }

    private String requiredText(String value, String name) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }

    private BigDecimal decimal(CSVRecord record, String name) {
        String value = blankToNull(record.get(name));
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal in " + name + ": " + value);
        }
    }

    private String normalizedRow(ImportRow row) {
        return CSVFormat.DEFAULT.format(row.authority, row.version, nullToEmpty(row.organismId),
                nullToEmpty(row.organismGroup), row.antibioticId, row.method, nullToEmpty(row.specimenTypeId),
                row.breakpointType, decimalText(row.susceptibleValue), decimalText(row.intermediateLowerValue),
                decimalText(row.intermediateUpperValue), decimalText(row.resistantValue), row.units,
                Boolean.toString(row.resistantInclusive), nullToEmpty(row.notes));
    }

    private String editionKey(ImportRow row) {
        return row.authority + "\n" + row.version;
    }

    private String naturalKey(ImportRow row) {
        return String.join("|", row.authority, row.version, nullToEmpty(row.organismId), nullToEmpty(row.organismGroup),
                row.antibioticId, row.method, nullToEmpty(row.specimenTypeId), row.breakpointType);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private MicroBreakpointImportErrorForm error(int rowNumber, String message, String sourceRow) {
        MicroBreakpointImportErrorForm error = new MicroBreakpointImportErrorForm();
        error.rowNumber = rowNumber;
        error.message = message;
        error.sourceRow = sourceRow;
        return error;
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class ImportPreview {
        private int totalRows;
        private final List<ImportRow> validRows = new ArrayList<>();
        private final List<MicroBreakpointImportErrorForm> errors = new ArrayList<>();
    }

    private static class ImportRow {
        private int rowNumber;
        private String authority;
        private String version;
        private String organismId;
        private String organismGroup;
        private String antibioticId;
        private String method;
        private String specimenTypeId;
        private String breakpointType;
        private BigDecimal susceptibleValue;
        private BigDecimal intermediateLowerValue;
        private BigDecimal intermediateUpperValue;
        private BigDecimal resistantValue;
        private String units;
        private boolean resistantInclusive = true;
        private String notes;
        private String sourceHash;
        private String sourceRow;
    }
}
