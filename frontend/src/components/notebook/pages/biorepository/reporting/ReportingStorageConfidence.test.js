import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../../../languages/en.json";
import DetailedMetricsTab from "./DetailedMetricsTab";
import OverviewDashboardTab from "./OverviewDashboardTab";

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("Biorepository reporting storage confidence checks", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn((url) => {
      if (url.includes("/dashboard/storage-capacity")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              totalSamplesStored: 12,
              pendingStorage: 2,
              totalDevices: 3,
              averageUtilization: 42.5,
            }),
        });
      }

      if (url.includes("/dashboard/storage-utilization")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              totalDevices: 2,
              capacityDefinedDevices: 2,
              capacityUndefinedDevices: 0,
              averageUtilization: 30.0,
              devices: [
                {
                  deviceName: "Freezer-A",
                  deviceType: "freezer",
                  currentUsage: 20,
                  totalCapacity: 100,
                  utilizationPercent: 20.0,
                  capacitySource: "DEVICE_CAPACITY_LIMIT",
                },
                {
                  deviceName: "Freezer-B",
                  deviceType: "freezer",
                  currentUsage: 10,
                  totalCapacity: 40,
                  utilizationPercent: 25.0,
                  capacitySource: "BOX_GRID_SUM",
                },
              ],
            }),
        });
      }

      if (url.includes("/dashboard/sample-aging")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              expired: 1,
              expiring30Days: 2,
              expiring60Days: 1,
              expiring90Days: 0,
              total: 12,
            }),
        });
      }

      if (url.includes("/dashboard/qc-metrics")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              passRate: 90.0,
              complianceRate: 90.0,
              passedInspections: 9,
              totalInspections: 10,
              failCount: 1,
              failedInspections: 1,
              failTrend: [],
              failTrendBasis: {
                source: "biorepository_qc_inspection.inspection_date",
                granularity: "day",
                windowDays: 30,
              },
              breakdownByFreezer: [],
              breakdownByRack: [],
              breakdownByTechnician: [],
              underInvestigationBoxes: [],
              frequentlyProblematicLocations: [],
              failureResolutionSummary: {
                correctedVsUnresolved: {
                  corrected: 1,
                  unresolved: 0,
                },
              },
              escalationSignals: {
                batchFailRatePercent: 10.0,
                batchFailRateThresholdPercent: 5.0,
                batchFailRateExceeded: true,
                repeatedFailureInSameBoxOrRack: false,
                flaggedFreezers: [],
                triggeredRules: ["BATCH_FAIL_RATE_OVER_5_PERCENT"],
                recommendedActions: ["Notify supervisor"],
              },
            }),
        });
      }

      if (url.includes("/dashboard/qc-discrepancies")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ SAMPLE_MISSING: 1 }),
        });
      }

      if (url.includes("/dashboard/qc-history")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ source: "biorepository_qc_inspection", count: 0, items: [] }),
        });
      }

      if (url.includes("/dashboard/retrieval-stats")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              totalRequests: 3,
              completedRequests: 2,
              pendingRequests: 1,
              overdueReturns: 0,
            }),
        });
      }

      if (url.includes("/dashboard/disposal-stats")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              totalDisposed: 0,
              disposalsByProject: {},
            }),
        });
      }

      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });
  });

  test("DetailedMetricsTab renders storage utilization device table", async () => {
    const view = renderWithIntl(<DetailedMetricsTab />);

    expect(await view.findByText("Storage Utilization by Device")).toBeTruthy();
    expect(await view.findByText("Freezer-A")).toBeTruthy();
    expect(await view.findByText("Freezer-B")).toBeTruthy();
    expect(await view.findByText("DEVICE_CAPACITY_LIMIT")).toBeTruthy();
    expect(await view.findByText("BOX_GRID_SUM")).toBeTruthy();
  });

  test("OverviewDashboardTab renders storage utilization summary line", async () => {
    const view = renderWithIntl(<OverviewDashboardTab />);
    expect(
      await view.findByText("Storage utilization (weighted): 42.5% across 3 active devices"),
    ).toBeTruthy();
  });
});
