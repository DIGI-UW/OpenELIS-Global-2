package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A file-wide configuration transaction: concepts, editions and interpretation
 * evidence succeed together.
 */
@Service
public class MicroReferencePackageServiceImpl implements MicroReferencePackageService {
    static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final String ACTOR = "1";
    private static final Set<String> FILES = Set.of("organisms.csv", "antimicrobials.csv", "breakpoints.csv",
            "interpretation-vectors.csv", "coverage.csv", "source-rows.csv", "scope.json", "NOTICE.txt");
    private static final ObjectMapper mapper = new ObjectMapper();
    private final MicroOrganismDAO organisms;
    private final MicroAntibioticDAO antibiotics;
    private final MicroBreakpointRuleDAO rules;
    private final MicroBreakpointStandardDAO standards;
    private final MicroBreakpointImportService importer;
    private final MicroAstInterpretationService interpreter;

    public MicroReferencePackageServiceImpl(MicroOrganismDAO organisms, MicroAntibioticDAO antibiotics,
            MicroBreakpointRuleDAO rules, MicroBreakpointStandardDAO standards, MicroBreakpointImportService importer,
            MicroAstInterpretationService interpreter) {
        this.organisms = organisms;
        this.antibiotics = antibiotics;
        this.rules = rules;
        this.standards = standards;
        this.importer = importer;
        this.interpreter = interpreter;
    }

    @Override
    @Transactional
    public void load(byte[] artifact) {
        Map<String, byte[]> files = validateArchive(artifact);
        for (CSVRecord row : csv(files.get("organisms.csv")))
            loadOrganism(row);
        for (CSVRecord row : csv(files.get("antimicrobials.csv")))
            loadAntibiotic(row);
        List<CSVRecord> breakpointRows = csv(files.get("breakpoints.csv"));
        Set<String> importedKeys = new HashSet<>();
        for (CSVRecord row : breakpointRows)
            importedKeys.add(vectorKey(row));
        importer.applyConfiguration(new String(files.get("breakpoints.csv"), StandardCharsets.UTF_8), ACTOR,
                sha256(artifact));
        Map<String, MicroBreakpointRule> loadedRules = new HashMap<>();
        for (CSVRecord row : breakpointRows) {
            var organism = organisms.findByReference(row.get("organism_source"), row.get("organism_code"))
                    .orElseThrow();
            var antibiotic = antibiotics.findByReference(row.get("antibiotic_source"), row.get("antibiotic_code"))
                    .orElseThrow();
            var standard = standards.findByAuthorityAndVersion(row.get("publisher"), row.get("version")).orElseThrow();
            loadedRules.put(vectorKey(row), rules.findBestRule(standard.getId(), organism.getId(),
                    organism.getOrganismGroup(), antibiotic.getId(), "MIC", null, "MIC"));
        }
        Set<String> tested = new HashSet<>();
        Map<String, Set<BigDecimal>> testedValues = new HashMap<>();
        for (CSVRecord vector : csv(files.get("interpretation-vectors.csv"))) {
            String key = vectorKey(vector);
            MicroBreakpointRule rule = loadedRules.get(key);
            if (rule == null)
                throw new IllegalArgumentException("Test vector has no imported rule");
            BigDecimal value = new BigDecimal(vector.get("value"));
            if (value.signum() <= 0)
                throw new IllegalArgumentException("Vector value must be positive");
            MicroAstInterpretation expected = switch (vector.get("expected")) {
            case "S" -> MicroAstInterpretation.SUSCEPTIBLE;
            case "I" -> MicroAstInterpretation.INTERMEDIATE;
            case "R" -> MicroAstInterpretation.RESISTANT;
            default -> throw new IllegalArgumentException("Unsupported vector interpretation");
            };
            if (interpreter.interpret(rule, MicroAstMethod.MIC, value) != expected) {
                throw new IllegalArgumentException(
                        "Source interpretation differs from OpenELIS: " + key + " at " + value);
            }
            tested.add(key);
            if (!testedValues.computeIfAbsent(key, ignored -> new HashSet<>()).add(value.stripTrailingZeros())) {
                throw new IllegalArgumentException("Duplicate source test vector");
            }
        }
        if (!tested.equals(importedKeys))
            throw new IllegalArgumentException("Every imported rule requires source test vectors");
        for (var entry : loadedRules.entrySet()) {
            Set<BigDecimal> values = testedValues.get(entry.getKey());
            var rule = entry.getValue();
            if (!values.contains(rule.getSusceptibleValue().stripTrailingZeros())
                    || !values.contains(rule.getResistantValue().stripTrailingZeros())
                    || values.stream().noneMatch(v -> v.compareTo(rule.getResistantValue()) > 0)
                    || values.stream().noneMatch(v -> v.compareTo(rule.getSusceptibleValue()) < 0)) {
                throw new IllegalArgumentException("Missing boundary vectors for " + entry.getKey());
            }
        }
    }

    static Map<String, byte[]> validateArchive(byte[] artifact) {
        Map<String, byte[]> files = readArchive(artifact);
        JsonNode manifest;
        try {
            manifest = mapper.readTree(files.get("manifest.json"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid reference manifest", ex);
        }
        if (manifest == null || !"openelis-amr-reference/v1".equals(manifest.path("format").asText())
                || !manifest.path("packageVersion").asText().matches("[0-9]+(?:\\.[0-9]+){1,2}")
                || !manifest.path("source").path("commit").asText().matches("[a-f0-9]{40}")) {
            throw new IllegalArgumentException("Unsupported reference format or missing source revision");
        }
        if (!("amr-reference-" + manifest.path("packageVersion").asText())
                .equals(manifest.path("packageId").asText())) {
            throw new IllegalArgumentException("Package identity mismatch");
        }
        Set<String> declared = new HashSet<>();
        manifest.path("files").fieldNames().forEachRemaining(declared::add);
        if (!declared.equals(FILES))
            throw new IllegalArgumentException("Unexpected package file inventory");
        for (String name : FILES) {
            JsonNode entry = manifest.path("files").path(name);
            byte[] data = files.get(name);
            if (!sha256(data).equals(entry.path("sha256").asText()) || entry.path("bytes").asLong(-1) != data.length) {
                throw new IllegalArgumentException("Reference package checksum mismatch: " + name);
            }
            if (name.endsWith(".csv") && csv(data).size() != entry.path("rows").asInt(-1)) {
                throw new IllegalArgumentException("Reference package row count mismatch: " + name);
            }
        }
        JsonNode scope;
        try {
            scope = mapper.readTree(files.get("scope.json"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid package scope", ex);
        }
        if (scope == null || !scope.equals(manifest.path("scope"))
                || !strings(scope.path("methods")).equals(Set.of("MIC"))) {
            throw new IllegalArgumentException("Unsupported or inconsistent package scope");
        }
        Set<String> organismCodes = conceptCodes(csv(files.get("organisms.csv")));
        Set<String> antibioticCodes = conceptCodes(csv(files.get("antimicrobials.csv")));
        if (!organismCodes.equals(strings(scope.path("organisms"))))
            throw new IllegalArgumentException("Organism inventory mismatch");
        Map<String, String> editions = new HashMap<>();
        for (JsonNode edition : manifest.path("standards")) {
            String authority = edition.path("publisher").asText();
            String version = edition.path("version").asText();
            String guideline = edition.path("guideline").asText();
            if (!"EUCAST".equals(authority) || !guideline.matches("EUCAST [0-9]{4}")
                    || !version.equals(guideline.substring(7) + "-" + manifest.path("packageVersion").asText())
                    || version.length() > 50
                    || editions.putIfAbsent(authority + "|" + version, edition.path("guideline").asText()) != null) {
                throw new IllegalArgumentException("Invalid or duplicate package edition");
            }
        }
        if (editions.isEmpty() || !new HashSet<>(editions.values()).equals(strings(scope.path("guidelines"))))
            throw new IllegalArgumentException("Edition inventory mismatch");
        List<CSVRecord> breakpointRows = csv(files.get("breakpoints.csv"));
        Set<String> seenEditions = new HashSet<>();
        Set<String> importedKeys = new HashSet<>();
        for (CSVRecord row : breakpointRows) {
            String edition = row.get("publisher") + "|" + row.get("version");
            if (!"AMR".equals(row.get("organism_source")) || !"AMR".equals(row.get("antibiotic_source"))
                    || !organismCodes.contains(row.get("organism_code"))
                    || !antibioticCodes.contains(row.get("antibiotic_code")) || !editions.containsKey(edition)
                    || !"MIC".equals(row.get("method")) || !"MIC".equals(row.get("breakpoint_type"))
                    || !row.get("specimen_type_id").isBlank() || !row.get("intermediate_lower_value").isBlank()
                    || !row.get("intermediate_upper_value").isBlank() || !"false".equals(row.get("resistant_inclusive"))
                    || !"mg/L".equals(row.get("units"))) {
                throw new IllegalArgumentException("Rule outside the supported package contract");
            }
            BigDecimal susceptible = threshold(row.get("susceptible_value"));
            BigDecimal resistant = threshold(row.get("resistant_value"));
            if (susceptible.compareTo(new BigDecimal("0.001")) <= 0 || susceptible.compareTo(resistant) > 0) {
                throw new IllegalArgumentException("Invalid package threshold ordering or unsupported no-S rule");
            }
            seenEditions.add(edition);
            if (!importedKeys.add(vectorKey(row)))
                throw new IllegalArgumentException("Duplicate package rule");
        }
        if (!seenEditions.equals(editions.keySet()))
            throw new IllegalArgumentException("Empty package edition");
        validateCoverage(files, editions, importedKeys, organismCodes, antibioticCodes);
        return files;
    }

    private static Set<String> strings(JsonNode array) {
        if (!array.isArray() || array.isEmpty())
            throw new IllegalArgumentException("Expected a nonempty scope list");
        Set<String> values = new HashSet<>();
        for (JsonNode value : array) {
            if (!value.isTextual() || value.asText().isBlank() || !values.add(value.asText())) {
                throw new IllegalArgumentException("Invalid or duplicate scope entry");
            }
        }
        return values;
    }

    private static Set<String> conceptCodes(List<CSVRecord> rows) {
        Set<String> codes = new HashSet<>();
        for (CSVRecord row : rows) {
            if (!"AMR".equals(row.get("source")) || !codes.add(required(row, "code", 120))) {
                throw new IllegalArgumentException("Invalid or duplicate reference identity");
            }
            required(row, "displayName", 255);
        }
        if (codes.isEmpty())
            throw new IllegalArgumentException("Empty concept inventory");
        return codes;
    }

    private static void validateCoverage(Map<String, byte[]> files, Map<String, String> editions, Set<String> rules,
            Set<String> organisms, Set<String> antibiotics) {
        Map<String, String> versions = new HashMap<>();
        editions.forEach((version, guideline) -> versions.put(guideline, version));
        Map<String, CSVRecord> sourceRows = new HashMap<>();
        Set<String> expectedReferences = new HashSet<>();
        for (CSVRecord row : csv(files.get("source-rows.csv"))) {
            String id = required(row, "source_row", 40);
            if (sourceRows.putIfAbsent(id, row) != null || !versions.containsKey(row.get("guideline"))
                    || !Set.of("SEE_COVERAGE", "OUTSIDE_ORGANISM_SCOPE").contains(row.get("status"))) {
                throw new IllegalArgumentException("Invalid or duplicate source row");
            }
            if ("SEE_COVERAGE".equals(row.get("status")))
                expectedReferences.add(id);
        }
        Set<String> contexts = new HashSet<>(), supported = new HashSet<>(), referenced = new HashSet<>();
        for (CSVRecord row : csv(files.get("coverage.csv"))) {
            String guideline = row.get("guideline");
            String key = String.join("|", versions.getOrDefault(guideline, ""), row.get("organism_code"),
                    row.get("antibiotic_code"), row.get("method"));
            if (!versions.containsKey(guideline) || !contexts.add(key) || !organisms.contains(row.get("organism_code"))
                    || !antibiotics.contains(row.get("antibiotic_code"))) {
                throw new IllegalArgumentException("Invalid or duplicate coverage context");
            }
            for (String id : required(row, "source_rows", MAX_BYTES).split(";", -1)) {
                CSVRecord source = sourceRows.get(id);
                if (source == null || !source.get("guideline").equals(guideline)
                        || !source.get("antibiotic_code").equals(row.get("antibiotic_code"))
                        || !source.get("method").equals(row.get("method"))) {
                    throw new IllegalArgumentException("Coverage does not match its source row");
                }
                referenced.add(id);
            }
            if ("SUPPORTED".equals(row.get("status"))) {
                if (!rules.contains(key) || !row.get("reason").isBlank()) {
                    throw new IllegalArgumentException("Supported coverage requires a rule and no exclusion reason");
                }
                supported.add(key);
            } else if (!"UNSUPPORTED".equals(row.get("status")) || row.get("reason").isBlank() || rules.contains(key)) {
                throw new IllegalArgumentException("Unsupported coverage cannot have a rule");
            }
        }
        if (!supported.equals(rules) || !referenced.equals(expectedReferences)) {
            throw new IllegalArgumentException("Incomplete rule coverage or source accounting");
        }
    }

    private void loadOrganism(CSVRecord row) {
        String source = required(row, "source", 80), code = required(row, "code", 120);
        String name = required(row, "displayName", 255), whonet = optional(row, "whonetCode", 50);
        var existing = organisms.findByReference(source, code);
        if (existing.isPresent())
            return; // Preserve local names, codes, activity and clinical settings.
        var byName = organisms.findByDisplayNameIgnoreCase(name);
        var byCode = whonet == null ? Optional.<MicroOrganism>empty() : organisms.findByWhonetCodeIgnoreCase(whonet);
        if (byName.isPresent() && byCode.isPresent() && !byName.get().getId().equals(byCode.get().getId())) {
            throw new IllegalArgumentException("Conflicting organism name and code: " + code);
        }
        MicroOrganism organism = byCode.or(() -> byName).orElseGet(MicroOrganism::new);
        if (organism.getReferenceCode() != null)
            throw new IllegalArgumentException("Organism already has a different source identity");
        boolean created = byName.isEmpty() && byCode.isEmpty();
        if (created) {
            organism.setDisplayName(name);
            organism.setWhonetCode(whonet);
            organism.setGramStain(optional(row, "gramStain", 20));
            organism.setSeeded(true);
        }
        organism.setReferenceSource(source);
        organism.setReferenceCode(code);
        organism.setLastUpdatedBy(ACTOR);
        organism.setSysUserId(ACTOR);
        if (created)
            organisms.insert(organism);
        else
            organisms.update(organism);
    }

    private void loadAntibiotic(CSVRecord row) {
        String source = required(row, "source", 80), code = required(row, "code", 120);
        String name = required(row, "displayName", 255), whonet = required(row, "whonetCode", 50);
        var existing = antibiotics.findByReference(source, code);
        if (existing.isPresent())
            return;
        var byName = antibiotics.findByDisplayNameIgnoreCase(name);
        var byCode = antibiotics.findByWhonetCodeIgnoreCase(whonet);
        if (byName.isPresent() && byCode.isPresent() && !byName.get().getId().equals(byCode.get().getId())) {
            throw new IllegalArgumentException("Conflicting antimicrobial name and code: " + code);
        }
        MicroAntibiotic antibiotic = byCode.or(() -> byName).orElseGet(MicroAntibiotic::new);
        if (antibiotic.getReferenceCode() != null)
            throw new IllegalArgumentException("Antimicrobial already has a different source identity");
        boolean created = byName.isEmpty() && byCode.isEmpty();
        if (created) {
            antibiotic.setDisplayName(name);
            antibiotic.setWhonetCode(whonet);
            antibiotic.setAntibioticClass(optional(row, "antibioticClass", 255));
            antibiotic.setSeeded(true);
        }
        antibiotic.setReferenceSource(source);
        antibiotic.setReferenceCode(code);
        antibiotic.setLastUpdatedBy(ACTOR);
        antibiotic.setSysUserId(ACTOR);
        if (created)
            antibiotics.insert(antibiotic);
        else
            antibiotics.update(antibiotic);
    }

    private static String required(CSVRecord row, String key, int max) {
        String value = optional(row, key, max);
        if (value == null)
            throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static String optional(CSVRecord row, String key, int max) {
        String value = row.get(key).trim();
        if (value.length() > max)
            throw new IllegalArgumentException(key + " is too long");
        return value.isEmpty() ? null : value;
    }

    private static BigDecimal threshold(String input) {
        BigDecimal value = new BigDecimal(input);
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 4 || value.precision() - value.scale() > 8) {
            throw new IllegalArgumentException("Value cannot be retained exactly: " + input);
        }
        return value;
    }

    private static String vectorKey(CSVRecord row) {
        return String.join("|", row.get("publisher"), row.get("version"), row.get("organism_code"),
                row.get("antibiotic_code"), row.get("method"));
    }

    private static List<CSVRecord> csv(byte[] data) {
        try (CSVParser parser = CSVParser.parse(new String(data, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {
            if (parser.getHeaderNames().isEmpty()
                    || new HashSet<>(parser.getHeaderNames()).size() != parser.getHeaderNames().size()) {
                throw new IllegalArgumentException("Missing or duplicate CSV columns");
            }
            List<CSVRecord> rows = parser.getRecords();
            if (rows.stream().anyMatch(row -> !row.isConsistent()))
                throw new IllegalArgumentException("Malformed CSV row");
            return rows;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid package CSV", ex);
        }
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Map<String, byte[]> readArchive(byte[] artifact) {
        if (artifact == null || artifact.length > MAX_BYTES)
            throw new IllegalArgumentException("Invalid package size");
        Map<String, byte[]> files = new HashMap<>();
        int remaining = MAX_BYTES;
        try (var archive = new ZipInputStream(new ByteArrayInputStream(artifact), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                String name = entry.getName();
                if ((!FILES.contains(name) && !name.equals("manifest.json")) || files.containsKey(name)
                        || entry.isDirectory()) {
                    throw new IllegalArgumentException("Unexpected or duplicate package entry: " + name);
                }
                byte[] content = archive.readNBytes(remaining + 1);
                remaining -= content.length;
                if (remaining < 0)
                    throw new IllegalArgumentException("Expanded package exceeds size limit");
                files.put(name, content);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid package archive", ex);
        }
        Set<String> expected = new HashSet<>(FILES);
        expected.add("manifest.json");
        if (!files.keySet().equals(expected))
            throw new IllegalArgumentException("Missing package files");
        return files;
    }
}
