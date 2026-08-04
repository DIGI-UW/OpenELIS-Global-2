package org.openelisglobal.microbiology.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
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
    private final Map<String, ImportPreview> previews = new ConcurrentHashMap<>();

    public MicroBreakpointImportServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
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
        return toForm(token, parsed, 0, 0);
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

        int imported = 0;
        int unchanged = 0;
        Map<String, MicroBreakpointStandard> standards = new java.util.HashMap<>();
        for (ImportRow row : preview.validRows) {
            String key = row.authority + "\n" + row.version;
            MicroBreakpointStandard standard = standards.computeIfAbsent(key,
                    ignored -> findOrCreateStandard(row.authority, row.version, actorId));
            Optional<MicroBreakpointRule> existingHash = ruleDAO.findBySourceRowHash(row.sourceHash);
            if (existingHash.isPresent()) {
                unchanged++;
                continue;
            }
            Optional<MicroBreakpointRule> existingRule = ruleDAO.findByNaturalKey(standard.getId(), row.organismId,
                    row.organismGroup, row.antibioticId, row.method, row.specimenTypeId, row.breakpointType);
            if (existingRule.isPresent() && existingRule.get().isLocallyCustomized()) {
                preview.errors.add(error(row.rowNumber,
                        "A locally customized breakpoint exists for this organism, antibiotic, and method"));
                continue;
            }
            MicroBreakpointRule rule = toRule(row, standard.getId(), actorId);
            if (existingRule.isPresent()) {
                rule.setId(existingRule.get().getId());
                ruleDAO.update(rule);
            } else {
                ruleDAO.insert(rule);
            }
            imported++;
        }
        return toForm(previewToken, preview, imported, unchanged);
    }

    private ImportPreview parse(String csv) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
                .setTrim(true).build();
        try (CSVParser parser = CSVParser.parse(csv, format)) {
            validateHeaders(parser.getHeaderMap());
            ImportPreview preview = new ImportPreview();
            for (CSVRecord record : parser) {
                preview.totalRows++;
                try {
                    preview.validRows.add(parseRow(record));
                } catch (IllegalArgumentException exception) {
                    preview.errors.add(error((int) record.getRecordNumber() + 1, exception.getMessage()));
                }
            }
            return preview;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read breakpoint CSV", exception);
        }
    }

    private ImportRow parseRow(CSVRecord record) {
        ImportRow row = new ImportRow();
        row.rowNumber = (int) record.getRecordNumber() + 1;
        row.authority = required(record, "publisher").toUpperCase(Locale.ROOT);
        if (!AUTHORITIES.contains(row.authority)) {
            throw new IllegalArgumentException("Unsupported publisher: " + row.authority);
        }
        row.version = required(record, "version");

        String organismValue = required(record, "organism_or_group");
        if (organismValue.regionMatches(true, 0, "group:", 0, "group:".length())) {
            row.organismGroup = requiredText(organismValue.substring("group:".length()), "organism group");
        } else {
            MicroOrganism organism = organismDAO.findByDisplayNameIgnoreCase(organismValue)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown organism: " + organismValue));
            row.organismId = organism.getId();
        }

        String antibioticCode = required(record, "antibiotic_whonet_code").toUpperCase(Locale.ROOT);
        MicroAntibiotic antibiotic = antibioticDAO.findByWhonetCodeIgnoreCase(antibioticCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown antibiotic code: " + antibioticCode));
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
        row.breakpointType = required(record, "breakpoint_type").toUpperCase(Locale.ROOT);
        if (!BREAKPOINT_TYPES.contains(row.breakpointType)) {
            throw new IllegalArgumentException("Unsupported breakpoint type: " + row.breakpointType);
        }
        row.susceptibleValue = decimal(record, "susceptible_value");
        row.intermediateLowerValue = decimal(record, "intermediate_lower_value");
        row.intermediateUpperValue = decimal(record, "intermediate_upper_value");
        row.resistantValue = decimal(record, "resistant_value");
        row.units = required(record, "units");
        row.sourceHash = hash(normalizedRow(row));
        return row;
    }

    private MicroBreakpointStandard findOrCreateStandard(String authority, String version, String actorId) {
        Optional<MicroBreakpointStandard> existing = standardDAO.findByAuthorityAndVersion(authority, version);
        if (existing.isPresent()) {
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

    private MicroBreakpointRule toRule(ImportRow row, String standardId, String actorId) {
        MicroBreakpointRule rule = new MicroBreakpointRule();
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
        rule.setUnits(row.units);
        rule.setSeeded(true);
        rule.setLocallyCustomized(false);
        rule.setSourceRowHash(row.sourceHash);
        rule.setLastUpdatedBy(actorId);
        rule.setIsActive("Y");
        return rule;
    }

    private MicroBreakpointImportPreviewForm toForm(String token, ImportPreview preview, int imported, int unchanged) {
        MicroBreakpointImportPreviewForm form = new MicroBreakpointImportPreviewForm();
        form.previewToken = token;
        form.totalRows = preview.totalRows;
        form.validRows = preview.validRows.size();
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
        return String.join("|", row.authority, row.version, nullToEmpty(row.organismId), nullToEmpty(row.organismGroup),
                row.antibioticId, row.method, nullToEmpty(row.specimenTypeId), row.breakpointType,
                decimalText(row.susceptibleValue), decimalText(row.intermediateLowerValue),
                decimalText(row.intermediateUpperValue), decimalText(row.resistantValue), row.units);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private MicroBreakpointImportErrorForm error(int rowNumber, String message) {
        MicroBreakpointImportErrorForm error = new MicroBreakpointImportErrorForm();
        error.rowNumber = rowNumber;
        error.message = message;
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
        private String sourceHash;
    }
}
