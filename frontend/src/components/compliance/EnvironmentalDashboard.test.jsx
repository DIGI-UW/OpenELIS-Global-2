import React from "react";
import { vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import EnvironmentalDashboard from "./EnvironmentalDashboard";
import { getFromOpenElisServer } from "../utils/Utils";

vi.mock("../utils/Utils", async () => {
  const actual = await vi.importActual("../utils/Utils");
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../layout/Layout", async () => {
  const { createContext } = await import("react");
  return { ConfigurationContext: createContext({}) };
});

vi.mock("./utils/compliancePdfGenerator", () => ({
  generateCompliancePdf: vi.fn(),
}));

vi.mock("@carbon/charts-react", () => ({
  LineChart: () => <div data-testid="line-chart" />,
  SimpleBarChart: () => <div data-testid="bar-chart" />,
}));

const NO_VALUE = "—";

const serveSummary = (summary) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/compliance/dashboard/summary")) {
      callback(summary);
    } else if (url.startsWith("/rest/compliance/dashboard/trend")) {
      callback({ series: [] });
    } else if (url.startsWith("/rest/compliance/dashboard/exceedances")) {
      callback({ items: [], totalCount: 0 });
    } else {
      callback([]);
    }
  });
};

const renderDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider value={{ userSessionDetails: {} }}>
        <EnvironmentalDashboard />
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const rateTile = () =>
  within(screen.getByText("Compliance Rate").closest(".cds--tile"));

describe("EnvironmentalDashboard compliance rate tile", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows no rate and no trend arrow when nothing in the filter was judged", async () => {
    serveSummary({
      totalOrders: 1,
      totalExceedances: 0,
      sitesMonitored: 1,
      trend: { totalOrders: 1, totalExceedances: 0, sitesMonitored: 1 },
    });

    renderDashboard();

    expect(await rateTile().findByText(NO_VALUE)).toBeInTheDocument();
    expect(rateTile().queryByText(/0%/)).not.toBeInTheDocument();
    expect(rateTile().queryByText(/[↑↓]/)).not.toBeInTheDocument();
  });

  it("shows the rate and its change when both periods were judged", async () => {
    serveSummary({
      totalOrders: 4,
      complianceRate: 75,
      totalExceedances: 2,
      sitesMonitored: 2,
      trend: {
        totalOrders: 1,
        complianceRate: -5,
        totalExceedances: 0,
        sitesMonitored: 0,
      },
    });

    renderDashboard();

    expect(await rateTile().findByText("75%")).toBeInTheDocument();
    expect(rateTile().getByText(/↓\s*5%/)).toBeInTheDocument();
  });
});

describe("EnvironmentalDashboard date filter", () => {
  const originalTz = process.env.TZ;

  beforeEach(() => {
    vi.clearAllMocks();
    serveSummary({ totalOrders: 0, totalExceedances: 0, sitesMonitored: 0 });
  });

  afterEach(() => {
    process.env.TZ = originalTz;
  });

  it("asks for the picked day east of UTC, not the day before", async () => {
    process.env.TZ = "Africa/Nairobi";
    renderDashboard();

    const user = userEvent.setup();
    const start = screen.getByLabelText("Date Range");
    await user.clear(start);
    await user.type(start, "2026-09-28{Enter}");

    await vi.waitFor(() =>
      expect(
        getFromOpenElisServer.mock.calls
          .map(([url]) => url)
          .filter((url) => url.startsWith("/rest/compliance/dashboard/summary"))
          .pop(),
      ).toContain("startDate=2026-09-28&"),
    );
  });
});
