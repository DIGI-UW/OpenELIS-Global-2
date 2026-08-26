import { writeFile } from "fs/promises";
import type { TestInfo } from "@playwright/test";

export const PERCENTILE_METHOD = "nearest-rank-ceiling";

export interface BrowserMeasurement {
  name: string;
  thresholdMs: number;
  warmupIterations: number;
  measuredIterations: number;
  percentileMethod: string;
  rawSamplesMs: number[];
  p50Ms: number;
  p95Ms: number;
  maxMs: number;
  passed: boolean;
}

export interface BrowserPerformanceEvidence {
  commit: string;
  environment: Record<string, string>;
  dataVolume: Record<string, number>;
  measurements: BrowserMeasurement[];
  passed: boolean;
}

export const nearestRankPercentile = (
  samples: number[],
  percentile: number,
): number => {
  if (samples.length === 0) throw new Error("Performance samples are required");
  if (percentile < 1 || percentile > 100) {
    throw new Error("Percentile must be between 1 and 100");
  }
  const sorted = [...samples].sort((left, right) => left - right);
  const rank = Math.ceil((percentile / 100) * sorted.length);
  return sorted[Math.max(0, rank - 1)];
};

export const measureBrowserOperation = async (
  name: string,
  warmupIterations: number,
  measuredIterations: number,
  thresholdMs: number,
  operation: (iteration: number, warmup: boolean) => Promise<number>,
): Promise<BrowserMeasurement> => {
  for (let index = 0; index < warmupIterations; index += 1) {
    await operation(index, true);
  }
  const rawSamplesMs: number[] = [];
  for (let index = 0; index < measuredIterations; index += 1) {
    rawSamplesMs.push(await operation(index, false));
  }
  const p50Ms = nearestRankPercentile(rawSamplesMs, 50);
  const p95Ms = nearestRankPercentile(rawSamplesMs, 95);
  const maxMs = Math.max(...rawSamplesMs);
  return {
    name,
    thresholdMs,
    warmupIterations,
    measuredIterations,
    percentileMethod: PERCENTILE_METHOD,
    rawSamplesMs,
    p50Ms,
    p95Ms,
    maxMs,
    passed: p95Ms < thresholdMs,
  };
};

const markdownFor = (evidence: BrowserPerformanceEvidence): string => {
  const lines = [
    "# Microbiology browser performance qualification",
    "",
    `- Commit: \`${evidence.commit}\``,
    `- Overall: **${evidence.passed ? "PASS" : "FAIL"}**`,
    `- Percentiles: \`${PERCENTILE_METHOD}\``,
    "",
    "## Environment",
    "",
    ...Object.entries(evidence.environment).map(
      ([key, value]) => `- ${key}: \`${value}\``,
    ),
    "",
    "## Data volume",
    "",
    ...Object.entries(evidence.dataVolume).map(
      ([key, value]) => `- ${key}: ${value}`,
    ),
    "",
    "## Measurements",
    "",
    "| Operation | Threshold (ms) | p50 (ms) | p95 (ms) | max (ms) | Result |",
    "| --- | ---: | ---: | ---: | ---: | --- |",
    ...evidence.measurements.map(
      (measurement) =>
        `| ${measurement.name} | ${measurement.thresholdMs.toFixed(3)} | ${measurement.p50Ms.toFixed(3)} | ${measurement.p95Ms.toFixed(3)} | ${measurement.maxMs.toFixed(3)} | ${measurement.passed ? "PASS" : "FAIL"} |`,
    ),
    "",
    "The JSON attachment retains every measured sample.",
    "",
  ];
  return lines.join("\n");
};

export const attachBrowserPerformanceEvidence = async (
  testInfo: TestInfo,
  evidence: BrowserPerformanceEvidence,
): Promise<void> => {
  const json = `${JSON.stringify(evidence, null, 2)}\n`;
  const markdown = markdownFor(evidence);
  const jsonPath = testInfo.outputPath("microbiology-browser-performance.json");
  const markdownPath = testInfo.outputPath(
    "microbiology-browser-performance.md",
  );
  await Promise.all([
    writeFile(jsonPath, json),
    writeFile(markdownPath, markdown),
  ]);
  await testInfo.attach("microbiology-browser-performance-json", {
    path: jsonPath,
    contentType: "application/json",
  });
  await testInfo.attach("microbiology-browser-performance-markdown", {
    path: markdownPath,
    contentType: "text/markdown",
  });
};
