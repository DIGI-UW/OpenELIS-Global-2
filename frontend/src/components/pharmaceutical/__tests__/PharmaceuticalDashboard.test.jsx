import React from "react";
import { render, screen, waitFor, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import PharmaceuticalDashboard from "../PharmaceuticalDashboard";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  ...jest.requireActual("../../utils/Utils"),
  getFromOpenElisServer: jest.fn(),
}));

// Mock Carbon Charts to avoid rendering issues in tests
jest.mock("@carbon/charts-react", () => ({
  PieChart: () => <div data-testid="pie-chart">PieChart</div>,
  DonutChart: () => <div data-testid="donut-chart">DonutChart</div>,
  SimpleBarChart: () => <div data-testid="bar-chart">SimpleBarChart</div>,
  StackedBarChart: () => (
    <div data-testid="stacked-bar-chart">StackedBarChart</div>
  ),
}));

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

// Mock dashboard data
const mockDashboardData = {
  totalSamples: 150,
  activeSamples: 120,
  pendingQC: 15,
  qcPassed: 100,
  qcFailed: 5,
  pendingDisposal: 8,
  activeExcursions: 2,
  expiringSoon: 12,
  oosCount: 3,
  avgTAT: 24.5,
  qcPassRate: 95.2,
  slaCompliance: 98.0,
  samplesByLabType: [
    { group: "PHARMA", value: 80 },
    { group: "BIOLOGICAL", value: 40 },
    { group: "ENVIRONMENTAL", value: 30 },
  ],
  excursionsByStatus: [
    { group: "ACTIVE", value: 2 },
    { group: "RESOLVED", value: 15 },
  ],
  assaysByType: [
    { group: "HPLC", value: 45 },
    { group: "GC", value: 30 },
    { group: "UV-Vis", value: 25 },
  ],
};

describe("PharmaceuticalDashboard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("Loading State", () => {
    test("displays loading indicator while fetching data", () => {
      // Arrange - mock API to not call callback immediately
      getFromOpenElisServer.mockImplementation(() => {});

      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert
      expect(
        screen.getByRole("progressbar") || screen.getByText(/loading/i),
      ).toBeTruthy();
    });
  });

  describe("Dashboard Data Display", () => {
    beforeEach(() => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/pharmaceutical/reports/dashboard")) {
          callback(mockDashboardData);
        }
      });
    });

    test("renders dashboard title", async () => {
      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert
      await waitFor(() => {
        expect(
          screen.getByText(messages["pharmaceutical.dashboard.title"]),
        ).toBeInTheDocument();
      });
    });

    test("displays key metrics tiles", async () => {
      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert - wait for data to load and check metrics
      await waitFor(() => {
        // Check for total intake metric
        const totalIntakeText = screen.queryByText(
          messages["pharmaceutical.dashboard.totalIntake"],
        );
        expect(totalIntakeText).toBeInTheDocument();
      });
    });

    test("displays active excursions alert when excursions exist", async () => {
      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert
      await waitFor(() => {
        const activeExcursionsText = screen.queryByText(
          messages["pharmaceutical.dashboard.activeExcursions"],
        );
        expect(activeExcursionsText).toBeInTheDocument();
      });
    });

    test("renders chart components", async () => {
      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert - charts should be rendered (mocked)
      await waitFor(() => {
        const charts = screen.getAllByTestId(/(pie|donut|bar)-chart/);
        expect(charts.length).toBeGreaterThan(0);
      });
    });
  });

  describe("Error Handling", () => {
    test("displays error notification when API fails", async () => {
      // Arrange
      getFromOpenElisServer.mockImplementation((url, callback) => {
        callback(null);
      });

      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert
      await waitFor(() => {
        // Check for error state - either InlineNotification or error message
        const errorElement =
          screen.queryByRole("alert") ||
          screen.queryByText(/failed/i) ||
          screen.queryByText(/error/i);
        expect(errorElement).toBeTruthy();
      });
    });
  });

  describe("Export Functionality", () => {
    beforeEach(() => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/pharmaceutical/reports/dashboard")) {
          callback(mockDashboardData);
        }
      });
    });

    test("renders export reports section", async () => {
      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert
      await waitFor(() => {
        const exportText = screen.queryByText(
          messages["pharmaceutical.dashboard.exportReports"],
        );
        expect(exportText).toBeInTheDocument();
      });
    });
  });

  describe("API Integration", () => {
    test("calls dashboard API on mount", async () => {
      // Arrange
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.includes("/rest/pharmaceutical/reports/dashboard")) {
          callback(mockDashboardData);
        }
      });

      // Act
      renderWithIntl(<PharmaceuticalDashboard />);

      // Assert
      await waitFor(() => {
        expect(getFromOpenElisServer).toHaveBeenCalledWith(
          expect.stringContaining("/rest/pharmaceutical/reports/dashboard"),
          expect.any(Function),
        );
      });
    });
  });
});

describe("PharmaceuticalDashboard Metrics", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/pharmaceutical/reports/dashboard")) {
        callback(mockDashboardData);
      }
    });
  });

  test("displays QC pass rate metric", async () => {
    // Act
    renderWithIntl(<PharmaceuticalDashboard />);

    // Assert
    await waitFor(() => {
      const qcPassRateText = screen.queryByText(
        messages["pharmaceutical.dashboard.qcPassRate"],
      );
      expect(qcPassRateText).toBeInTheDocument();
    });
  });

  test("displays pending disposal count", async () => {
    // Act
    renderWithIntl(<PharmaceuticalDashboard />);

    // Assert
    await waitFor(() => {
      const pendingDisposalText = screen.queryByText(
        messages["pharmaceutical.dashboard.pendingDisposal"],
      );
      expect(pendingDisposalText).toBeInTheDocument();
    });
  });

  test("displays expiring soon count", async () => {
    // Act
    renderWithIntl(<PharmaceuticalDashboard />);

    // Assert
    await waitFor(() => {
      const expiringText = screen.queryByText(
        messages["pharmaceutical.dashboard.expiringSoon"],
      );
      expect(expiringText).toBeInTheDocument();
    });
  });
});
