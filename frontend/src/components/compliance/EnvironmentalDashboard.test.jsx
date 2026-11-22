import React from "react";
import { vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { format } from "date-fns";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import EnvironmentalDashboard from "./EnvironmentalDashboard";
import { getFromOpenElisServer } from "../utils/Utils";
import { ConfigurationContext as SiteConfigurationContext } from "../layout/contexts";
import { generateCompliancePdf } from "./utils/compliancePdfGenerator";

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

const renderDashboard = (configurationProperties = {}) =>
  render(
    <SiteConfigurationContext.Provider value={{ configurationProperties }}>
      <IntlProvider locale="en" messages={messages}>
        <UserSessionDetailsContext.Provider value={{ userSessionDetails: {} }}>
          <EnvironmentalDashboard />
        </UserSessionDetailsContext.Provider>
      </IntlProvider>
    </SiteConfigurationContext.Provider>,
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
    await user.type(start, "09/28/2026{Enter}");

    await vi.waitFor(() =>
      expect(lastSummaryUrl()).toContain("startDate=2026-09-28&"),
    );
  });
});

const lastSummaryUrl = () =>
  getFromOpenElisServer.mock.calls
    .map(([url]) => url)
    .filter((url) => url.startsWith("/rest/compliance/dashboard/summary"))
    .pop();

describe("EnvironmentalDashboard dates follow the site date format (OGC-1657)", () => {
  const dayFirst = { DEFAULT_DATE_LOCALE: "fr-FR" };
  const exceedance = {
    date: "2026-03-05 00:00:00.0",
    labNumber: "ENV26000001",
    siteName: "Bekasi Riverside",
    parameter: "Lead",
    result: "0.9",
    threshold: "<= 0.5",
    status: "FAIL",
  };

  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/compliance/dashboard/summary")) {
        callback({ totalOrders: 1, totalExceedances: 1, sitesMonitored: 1 });
      } else if (url.startsWith("/rest/compliance/dashboard/trend")) {
        callback({ series: [] });
      } else if (url.startsWith("/rest/compliance/dashboard/exceedances")) {
        callback({ items: [exceedance], totalCount: 1 });
      } else {
        callback([]);
      }
    });
  });

  it("filters show and read dates day first on a dd/MM/yyyy site", async () => {
    renderDashboard(dayFirst);
    const start = screen.getByLabelText("Date Range");
    const end = screen.getByLabelText("End date");

    expect(start).toHaveAttribute("placeholder", "dd/mm/yyyy");
    expect(end.value).toBe(format(new Date(), "dd/MM/yyyy"));
    expect(lastSummaryUrl()).toContain(
      `endDate=${format(new Date(), "yyyy-MM-dd")}`,
    );

    const user = userEvent.setup();
    await user.clear(start);
    await user.type(start, "05/03/2026{Enter}");

    await vi.waitFor(() =>
      expect(lastSummaryUrl()).toContain("startDate=2026-03-05&"),
    );
    expect(start.value).toBe("05/03/2026");
  });

  it("the exceedance table and the PDF show their dates day first", async () => {
    renderDashboard(dayFirst);

    expect(await screen.findByText("05/03/2026")).toBeInTheDocument();
    expect(screen.queryByText(exceedance.date)).not.toBeInTheDocument();

    await userEvent.setup().click(screen.getByText("Export PDF"));

    await vi.waitFor(() => expect(generateCompliancePdf).toHaveBeenCalled());
    const [data, meta] = generateCompliancePdf.mock.calls[0];
    expect(data.exceedances.items[0].date).toBe("05/03/2026");
    expect(meta.period).toMatch(
      new RegExp(
        `^\\d{2}/\\d{2}/\\d{4} – ${format(new Date(), "dd/MM/yyyy")}$`,
      ),
    );
    expect(meta.generatedAt).toMatch(
      new RegExp(`^${format(new Date(), "dd/MM/yyyy")} \\d{2}:\\d{2}$`),
    );
  });
});
