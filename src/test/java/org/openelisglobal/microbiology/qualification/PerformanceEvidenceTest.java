package org.openelisglobal.microbiology.qualification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PerformanceEvidenceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void nearestRankPercentilesAreDeterministic() {
        List<Double> samples = new ArrayList<>();
        for (int value = 1; value <= 20; value++) {
            samples.add((double) value);
        }

        assertEquals(10.0d, PerformanceEvidence.percentile(samples, 50), 0.0d);
        assertEquals(19.0d, PerformanceEvidence.percentile(samples, 95), 0.0d);
        assertEquals(20.0d, PerformanceEvidence.percentile(samples, 100), 0.0d);
    }

    @Test
    public void measurementSeparatesWarmupsAndRetainsEveryMeasuredSample() throws Exception {
        AtomicInteger calls = new AtomicInteger();

        PerformanceEvidence.Measurement measurement = PerformanceEvidence.measure("case-load", 5, 20, 1000,
                (iteration, warmup) -> calls.incrementAndGet());

        assertEquals(25, calls.get());
        assertEquals(5, measurement.warmupIterations());
        assertEquals(20, measurement.measuredIterations());
        assertEquals(20, measurement.rawSamplesMs().size());
        assertEquals(PerformanceEvidence.PERCENTILE_METHOD, measurement.percentileMethod());
        assertTrue(measurement.passed());
    }

    @Test
    public void writesEquivalentJsonAndMarkdownEvidence() throws Exception {
        PerformanceEvidence.Measurement passed = new PerformanceEvidence.Measurement("worklist-load", 2000, 5, 20,
                PerformanceEvidence.PERCENTILE_METHOD, List.of(10.0d, 20.0d), 10.0d, 20.0d, 20.0d, true);
        PerformanceEvidence.Measurement failed = new PerformanceEvidence.Measurement("worklist-search", 500, 5, 20,
                PerformanceEvidence.PERCENTILE_METHOD, List.of(400.0d, 600.0d), 400.0d, 600.0d, 600.0d, false);
        PerformanceEvidence.Evidence evidence = PerformanceEvidence.evidence("abc123",
                Map.of("browser", "Chromium 1", "database", "PostgreSQL 14"), Map.of("cases", 200),
                List.of(passed, failed));
        Path outputDirectory = temporaryFolder.newFolder("evidence").toPath();

        PerformanceEvidence.OutputPaths paths = PerformanceEvidence.write(outputDirectory, "qualification", evidence);

        JsonNode json = new ObjectMapper().readTree(paths.json().toFile());
        String markdown = Files.readString(paths.markdown());
        assertFalse(json.get("passed").asBoolean());
        assertEquals(2, json.get("measurements").size());
        assertTrue(markdown.contains("Overall: **FAIL**"));
        assertTrue(markdown.contains("| worklist-search | 500.000 | 400.000 | 600.000 | 600.000 | FAIL |"));
    }
}
