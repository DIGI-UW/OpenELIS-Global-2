import { describe, expect, test } from "vitest";
import {
  createRunScopedAnalyzerConfig,
  resolveMockSimulatorUrl,
  type AnalyzerTestConfig,
} from "../playwright/helpers/analyzer-test-config";

describe("resolveMockSimulatorUrl", () => {
  test("uses the isolated mock endpoint supplied by the runner", () => {
    expect(
      resolveMockSimulatorUrl({
        MOCK_SIMULATOR_URL: "http://localhost:36665",
      }),
    ).toBe("http://localhost:36665");
  });

  test("uses the standard local harness endpoint when no override is set", () => {
    expect(resolveMockSimulatorUrl({})).toBe("http://localhost:8085");
  });
});

describe("createRunScopedAnalyzerConfig", () => {
  const fileConfig: AnalyzerTestConfig = {
    name: "Demo: QuantStudio 7",
    displayName: "QuantStudio 7",
    profileName: "Thermo Fisher QuantStudio QS5/QS7",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: "http://localhost:8085",
      template: "quantstudio7",
      targetDir: "/data/analyzer-imports/demo--quantstudio-7/incoming",
    },
  };

  test("isolates an analyzer instance and its FILE watch directory per run", () => {
    const scoped = createRunScopedAnalyzerConfig(fileConfig, "run-42");

    expect(scoped.name).toBe("Demo: QuantStudio 7 run-42");
    expect(scoped.push.targetDir).toBe(
      "/data/analyzer-imports/demo-quantstudio-7-run-42/incoming",
    );
    expect(scoped.profileName).toBe(fileConfig.profileName);
    expect(scoped.push.template).toBe(fileConfig.push.template);
  });

  test("isolates a provisioned TCP mock without changing its profile", () => {
    const tcpConfig: AnalyzerTestConfig = {
      ...fileConfig,
      name: "Demo: GeneXpert ASTM",
      profileName: "Cepheid GeneXpert (ASTM Mode)",
      protocol: "ASTM",
      mockAnalyzerName: "demo-genexpert",
      push: {
        protocol: "ASTM",
        simulatorUrl: "http://localhost:8085",
        template: "genexpert_astm",
      },
    };

    const scoped = createRunScopedAnalyzerConfig(tcpConfig, "run-42");

    expect(scoped.name).toBe("Demo: GeneXpert ASTM run-42");
    expect(scoped.mockAnalyzerName).toBe("demo-genexpert-run-42");
    expect(scoped.profileName).toBe(tcpConfig.profileName);
    expect(scoped.push).toEqual(tcpConfig.push);
  });
});
