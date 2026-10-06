package org.openelisglobal.analyzer.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How a profile revision reads each record: the parts it lands on, its result
 * type and its unit. A LOINC, a declared value or a translation changes only
 * what OE2's mapping decides, so a record a newer revision changes only that
 * way still reads the same.
 */
public final class AnalyzerRecordReading {

    private AnalyzerRecordReading() {
    }

    /** The records both revisions declare and read the same way. */
    public static Set<AnalyzerMappingRowKey> readAlike(BridgeAnalyzerProfile one, BridgeAnalyzerProfile other) {
        Map<AnalyzerMappingRowKey, List<Object>> theirs = readings(other);
        return readings(one).entrySet().stream().filter(entry -> entry.getValue().equals(theirs.get(entry.getKey())))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private static Map<AnalyzerMappingRowKey, List<Object>> readings(BridgeAnalyzerProfile profile) {
        Map<AnalyzerMappingRowKey, List<Object>> readings = new LinkedHashMap<>();
        for (var test : profile.testDefinitions()) {
            List<List<String>> parts = test.components().stream()
                    .map(component -> List.of(component.code(), Objects.toString(component.subIdentity(), "")))
                    .toList();
            readings.put(AnalyzerMappingRowKey.main(test.analyzerCode()), List.of(Objects.toString(test.unit(), ""),
                    Objects.toString(test.resultType(), ""), Objects.toString(test.callComponent(), ""), parts));
            for (var component : test.recordComponents()) {
                readings.put(new AnalyzerMappingRowKey(test.analyzerCode(), component.subIdentity()),
                        List.of(component.code(), Objects.toString(component.unit(), ""),
                                Objects.toString(component.resultType(), "")));
            }
        }
        return readings;
    }
}
