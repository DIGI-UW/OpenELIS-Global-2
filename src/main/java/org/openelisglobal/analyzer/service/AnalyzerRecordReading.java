package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * How a profile reads what an instrument sends (spec point 13). A test is read
 * by the codes it is sent under, its unit, result type, call component, parts
 * and run-failure values with their translations; a part with its own
 * sub-identity by its code, unit, result type and run-failure values; a
 * declared value by the spellings translated to it, and each spelling by the
 * values it translates to. The type reads every result by its routing, parsing
 * (where a record's parts sit and the number format), record declarations,
 * control recognition and sample flags. Names, hints, codings and the other
 * connection defaults are not reading: a LOINC fix or a new assay changes
 * nothing here.
 */
public final class AnalyzerRecordReading {

    /** The profile sections that decide how every result of a type is read. */
    static final List<String> TYPE_READING_SECTIONS = List.of("identifier_pattern", "msh3_pattern", "column_mapping",
            "sheet_detection", "result_value_order", "test_code_values", "controlResultRecognition", "sample_flags");

    /**
     * The connection defaults the Bridge parses with: where each part of a record
     * sits and how the instrument writes a number.
     */
    static final List<String> TYPE_READING_DEFAULTS = List.of("extractionOverrides", "numberFormat");

    /**
     * The type reading sections that route a message to its analyzer; a result
     * already parsed does not depend on them.
     */
    static final List<String> ROUTING_SECTIONS = List.of("identifier_pattern", "msh3_pattern",
            "connection.codeOverrides");

    private AnalyzerRecordReading() {
    }

    /**
     * The records both profiles declare and read the same way: the record itself
     * and every value or spelling declared by the arrival profile (one). A new
     * value in the current profile (other) is allowed; a removed one is not.
     */
    public static Set<AnalyzerMappingRowKey> readAlike(BridgeAnalyzerProfile one, BridgeAnalyzerProfile other) {
        if (!parsing(typeReading(one)).equals(parsing(typeReading(other)))) {
            return Set.of();
        }
        Map<AnalyzerMappingSourceRow, Object> mine = rowReadings(one);
        Map<AnalyzerMappingSourceRow, Object> theirs = rowReadings(other);
        Set<AnalyzerMappingRowKey> differing = mine.entrySet().stream().filter(
                entry -> entry.getKey().rawValue() != null && !entry.getValue().equals(theirs.get(entry.getKey())))
                .map(entry -> recordOf(entry.getKey())).collect(Collectors.toSet());
        return mine.entrySet().stream()
                .filter(entry -> entry.getKey().rawValue() == null
                        && entry.getValue().equals(theirs.get(entry.getKey())))
                .map(entry -> recordOf(entry.getKey())).filter(record -> !differing.contains(record))
                .collect(Collectors.toSet());
    }

    private static AnalyzerMappingRowKey recordOf(AnalyzerMappingSourceRow row) {
        return new AnalyzerMappingRowKey(row.sourceRowKey(), row.subIdentity());
    }

    /**
     * Each row a mapping decides, a record or one value of a record, with how the
     * profile reads it.
     */
    public static Map<AnalyzerMappingSourceRow, Object> rowReadings(BridgeAnalyzerProfile profile) {
        Map<AnalyzerMappingSourceRow, Object> readings = new LinkedHashMap<>();
        Map<String, JsonNode> documents = testDocuments(profile.document());
        for (var test : profile.testDefinitions()) {
            JsonNode document = documents.getOrDefault(test.analyzerCode(), MissingNode.getInstance());
            List<List<String>> parts = test.components().stream()
                    .map(component -> List.of(component.code(), Objects.toString(component.subIdentity(), "")))
                    .sorted(Comparator.<List<String>, String>comparing(part -> part.get(0))
                            .thenComparing(part -> part.get(1)))
                    .toList();
            readings.put(new AnalyzerMappingSourceRow(test.analyzerCode(), null, ""),
                    List.of(sorted(test.aliases()), Objects.toString(test.unit(), ""),
                            Objects.toString(test.resultType(), ""), Objects.toString(test.callComponent(), ""), parts,
                            runFailures(document)));
            addValues(readings, test.analyzerCode(), "", test.resultValues(), test.translations());
            for (var component : test.recordComponents()) {
                readings.put(new AnalyzerMappingSourceRow(test.analyzerCode(), null, component.subIdentity()),
                        List.of(component.code(), Objects.toString(component.unit(), ""),
                                Objects.toString(component.resultType(), ""),
                                runFailures(componentDocument(document, component.subIdentity()))));
                addValues(readings, test.analyzerCode(), component.subIdentity(), component.resultValues(),
                        component.translations());
            }
        }
        return readings;
    }

    /**
     * How the type reads every result, section by section; an absent section reads
     * as absent.
     */
    public static Map<String, JsonNode> typeReading(BridgeAnalyzerProfile profile) {
        JsonNode document = profile.document();
        Map<String, JsonNode> reading = new TreeMap<>();
        for (String section : TYPE_READING_SECTIONS) {
            if (document.has(section)) {
                reading.put(section, document.get(section));
            }
        }
        JsonNode defaults = document.path("configDefaults");
        for (String setting : TYPE_READING_DEFAULTS) {
            if (defaults.has(setting)) {
                reading.put("configDefaults." + setting, defaults.get(setting));
            }
        }
        return reading;
    }

    /**
     * A declared value is read by the spellings translated to it; a mapping keeps a
     * row for each spelling too, read by the values it translates to.
     */
    /** A type reading without its routing: how it parses a result it was sent. */
    public static Map<String, JsonNode> parsing(Map<String, JsonNode> typeReading) {
        Map<String, JsonNode> parsing = new TreeMap<>(typeReading);
        ROUTING_SECTIONS.forEach(parsing::remove);
        return parsing;
    }

    /** {@link #parsing(Map)} of a type reading kept as JSON. */
    public static Map<String, JsonNode> parsing(JsonNode typeReading) {
        Map<String, JsonNode> reading = new TreeMap<>();
        if (typeReading != null) {
            typeReading.fields().forEachRemaining(entry -> reading.put(entry.getKey(), entry.getValue()));
        }
        return parsing(reading);
    }

    private static void addValues(Map<AnalyzerMappingSourceRow, Object> readings, String code, String subIdentity,
            List<String> values, Map<String, List<String>> translations) {
        Map<String, Set<String>> valuesBySpelling = new TreeMap<>();
        for (String value : values) {
            List<String> spellings = translations.getOrDefault(value, List.of());
            readings.put(new AnalyzerMappingSourceRow(code, value, subIdentity), sorted(spellings));
            spellings
                    .forEach(spelling -> valuesBySpelling.computeIfAbsent(spelling, key -> new TreeSet<>()).add(value));
        }
        valuesBySpelling.forEach((spelling, translated) -> readings
                .putIfAbsent(new AnalyzerMappingSourceRow(code, spelling, subIdentity), List.copyOf(translated)));
    }

    /**
     * The values that mean the run produced nothing, each with the vendor spellings
     * translated to it.
     */
    private static List<List<String>> runFailures(JsonNode owner) {
        List<List<String>> failures = new ArrayList<>();
        for (String value : sorted(texts(owner.path("run_failure_values")))) {
            List<String> failure = new ArrayList<>();
            failure.add(value);
            failure.addAll(sorted(texts(owner.path("translations").path(value))));
            failures.add(failure);
        }
        return failures;
    }

    private static JsonNode componentDocument(JsonNode test, String subIdentity) {
        for (JsonNode component : test.path("components")) {
            if (subIdentity.equals(component.path("sub_identity").asText(null))) {
                return component;
            }
        }
        return MissingNode.getInstance();
    }

    private static Map<String, JsonNode> testDocuments(JsonNode document) {
        Map<String, JsonNode> tests = new LinkedHashMap<>();
        document.path("default_test_mappings").forEach(test -> tests.put(test.path("test_code").asText(), test));
        return tests;
    }

    private static List<String> texts(JsonNode node) {
        List<String> texts = new ArrayList<>();
        node.forEach(value -> texts.add(value.asText()));
        return texts;
    }

    private static List<String> sorted(List<String> values) {
        return values.stream().sorted().toList();
    }
}
