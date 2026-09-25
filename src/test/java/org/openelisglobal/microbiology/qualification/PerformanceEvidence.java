package org.openelisglobal.microbiology.qualification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PerformanceEvidence {

    public static final String PERCENTILE_METHOD = "nearest-rank-ceiling";

    private PerformanceEvidence() {
    }

    public static Measurement measure(String name, int warmupIterations, int measuredIterations, double thresholdMs,
            ThrowingOperation operation) throws Exception {
        requireText(name, "name");
        if (warmupIterations < 0) {
            throw new IllegalArgumentException("warmupIterations must not be negative");
        }
        if (measuredIterations < 1) {
            throw new IllegalArgumentException("measuredIterations must be positive");
        }
        if (thresholdMs <= 0) {
            throw new IllegalArgumentException("thresholdMs must be positive");
        }

        for (int index = 0; index < warmupIterations; index++) {
            operation.run(index, true);
        }

        List<Double> samples = new ArrayList<>(measuredIterations);
        for (int index = 0; index < measuredIterations; index++) {
            long startedAt = System.nanoTime();
            operation.run(index, false);
            samples.add((System.nanoTime() - startedAt) / 1_000_000.0d);
        }

        double p50 = percentile(samples, 50);
        double p95 = percentile(samples, 95);
        double max = samples.stream().max(Comparator.naturalOrder()).orElseThrow();
        return new Measurement(name, thresholdMs, warmupIterations, measuredIterations, PERCENTILE_METHOD,
                List.copyOf(samples), p50, p95, max, p95 < thresholdMs);
    }

    public static double percentile(List<Double> samples, int percentile) {
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("samples are required");
        }
        if (percentile < 1 || percentile > 100) {
            throw new IllegalArgumentException("percentile must be between 1 and 100");
        }
        List<Double> sorted = samples.stream().sorted().toList();
        int nearestRank = (int) Math.ceil(percentile / 100.0d * sorted.size());
        return sorted.get(Math.max(0, nearestRank - 1));
    }

    public static Evidence evidence(String commit, Map<String, String> environment, Map<String, Integer> dataVolume,
            List<Measurement> measurements) {
        requireText(commit, "commit");
        if (environment == null || environment.isEmpty()) {
            throw new IllegalArgumentException("environment is required");
        }
        if (dataVolume == null || dataVolume.isEmpty()) {
            throw new IllegalArgumentException("dataVolume is required");
        }
        if (measurements == null || measurements.isEmpty()) {
            throw new IllegalArgumentException("measurements are required");
        }
        boolean passed = measurements.stream().allMatch(Measurement::passed);
        return new Evidence(commit, Map.copyOf(environment), Map.copyOf(dataVolume), List.copyOf(measurements), passed);
    }

    public static OutputPaths write(Path outputDirectory, String baseName, Evidence evidence) throws IOException {
        requireText(baseName, "baseName");
        Files.createDirectories(outputDirectory);
        Path jsonPath = outputDirectory.resolve(baseName + ".json");
        Path markdownPath = outputDirectory.resolve(baseName + ".md");
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(jsonPath.toFile(), evidence);
        Files.writeString(markdownPath, toMarkdown(evidence));
        return new OutputPaths(jsonPath, markdownPath);
    }

    private static String toMarkdown(Evidence evidence) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Microbiology performance qualification\n\n");
        markdown.append("- Commit: `").append(evidence.commit()).append("`\n");
        markdown.append("- Overall: **").append(evidence.passed() ? "PASS" : "FAIL").append("**\n");
        markdown.append("- Percentiles: `").append(PERCENTILE_METHOD).append("`\n\n");
        markdown.append("## Environment\n\n");
        evidence.environment()
                .forEach((key, value) -> markdown.append("- ").append(key).append(": `").append(value).append("`\n"));
        markdown.append("\n## Data volume\n\n");
        evidence.dataVolume()
                .forEach((key, value) -> markdown.append("- ").append(key).append(": ").append(value).append("\n"));
        markdown.append("\n## Measurements\n\n");
        markdown.append("| Operation | Threshold (ms) | p50 (ms) | p95 (ms) | max (ms) | Result |\n");
        markdown.append("| --- | ---: | ---: | ---: | ---: | --- |\n");
        for (Measurement measurement : evidence.measurements()) {
            markdown.append("| ").append(measurement.name()).append(" | ").append(format(measurement.thresholdMs()))
                    .append(" | ").append(format(measurement.p50Ms())).append(" | ").append(format(measurement.p95Ms()))
                    .append(" | ").append(format(measurement.maxMs())).append(" | ")
                    .append(measurement.passed() ? "PASS" : "FAIL").append(" |\n");
        }
        markdown.append("\nEach measurement retains ").append(evidence.measurements().get(0).warmupIterations())
                .append(" warm-up iterations and all raw measured samples in the JSON artifact.\n");
        return markdown.toString();
    }

    public static Map<String, String> currentEnvironment(String databaseVersion) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        environment.put("architecture", System.getProperty("os.arch"));
        environment.put("java", System.getProperty("java.version"));
        environment.put("processors", Integer.toString(Runtime.getRuntime().availableProcessors()));
        environment.put("maxHeapBytes", Long.toString(Runtime.getRuntime().maxMemory()));
        environment.put("database", databaseVersion);
        return environment;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    @FunctionalInterface
    public interface ThrowingOperation {
        void run(int iteration, boolean warmup) throws Exception;
    }

    public record Measurement(String name, double thresholdMs, int warmupIterations, int measuredIterations,
            String percentileMethod, List<Double> rawSamplesMs, double p50Ms, double p95Ms, double maxMs,
            boolean passed) {
    }

    public record Evidence(String commit, Map<String, String> environment, Map<String, Integer> dataVolume,
            List<Measurement> measurements, boolean passed) {
    }

    public record OutputPaths(Path json, Path markdown) {
    }
}
