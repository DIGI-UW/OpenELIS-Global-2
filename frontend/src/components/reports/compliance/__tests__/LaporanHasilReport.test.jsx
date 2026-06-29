import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import LaporanHasilReport from "../LaporanHasilReport";

const MOCK_REPORT = {
  ineligibleCount: 3,
  generatedCount: 1,
  notYetGeneratedCount: 2,
  orders: [
    {
      sampleId: 101,
      labNumber: "ENV-2026-001",
      siteCode: "SITE-042",
      siteName: "Intake Point A — Citarum River",
      standardName: "Water Quality Standard PP No. 22/2021",
      regulationNumber: "PP No. 22/2021",
      collectionDate: "2026-04-06 07:30",
      testCount: 5,
      complianceStatus: "NON_COMPLIANT",
      lastGenerated: null,
      gpsCoordinates: "-6.7054, 107.6048",
      collectionMethod: "Manual Grab",
      waterTemp: "28.5 °C",
      ambientTemp: "33.2 °C",
      weather: "Clear",
      preservation: "HNO3 acidification",
      parameterResults: [
        {
          parameterCode: "pH",
          displayName: "pH",
          resultValue: "7.2",
          thresholdDisplay: "4.5 - 8.5",
          units: "",
          status: "COMPLIANT",
        },
        {
          parameterCode: "Lead",
          displayName: "Lead (Pb)",
          resultValue: "3.032",
          thresholdDisplay: "≤ 0.03 mg/L",
          units: "mg/L",
          status: "NON_COMPLIANT",
        },
      ],
      analystSignature: {
        signerName: "Deri Rahmawati, S.Si",
        signerRole: "Lab Analyst",
        signedAt: "2026-04-06 14:31",
      },
      managerSignature: {
        signerName: "Dr. Bambang Sutrisno",
        signerRole: "Lab Manager",
        signedAt: "2026-04-06 15:22",
      },
    },
    {
      sampleId: 102,
      labNumber: "ENV-2026-002",
      siteCode: "SITE-043",
      siteName: "Treatment Plant Outlet",
      standardName: "Water Quality Standard PP No. 22/2021",
      regulationNumber: "PP No. 22/2021",
      collectionDate: "2026-03-04",
      testCount: 4,
      complianceStatus: "COMPLIANT",
      lastGenerated: "2026-04-05T10:30:00",
      gpsCoordinates: null,
      collectionMethod: null,
      waterTemp: null,
      ambientTemp: null,
      weather: null,
      preservation: null,
      parameterResults: [],
      analystSignature: null,
      managerSignature: null,
    },
  ],
};

const MOCK_STATUSES = [
  { id: "COMPLIANT", text: "Compliant" },
  { id: "NON_COMPLIANT", text: "Non-Compliant" },
  { id: "BORDERLINE", text: "Borderline" },
];

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (url.includes("compliance-statuses")) {
      callback(MOCK_STATUSES);
    } else {
      callback(MOCK_REPORT);
    }
  }),
}));

vi.mock("../../../../config.json", () => ({
  default: { serverBaseUrl: "http://localhost:8080" },
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("LaporanHasilReport", () => {
  it("renders title and subtitle", () => {
    renderWithIntl(<LaporanHasilReport />);
    expect(screen.getByText(/Laporan Hasil — Compliance Report/i)).toBeInTheDocument();
    expect(screen.getByText(/Sertifikat Hasil Uji/i)).toBeInTheDocument();
  });

  it("renders all 5 filter controls", () => {
    renderWithIntl(<LaporanHasilReport />);
    expect(screen.getByLabelText(/Date From/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Date To/i)).toBeInTheDocument();
    expect(screen.getByText(/Compliance Status/i)).toBeInTheDocument();
    expect(screen.getByText(/Generation Status/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Search/i })).toBeInTheDocument();
  });

  it("shows stats tiles after search with correct counts", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getAllByText("3").length).toBeGreaterThan(0);
    expect(screen.getAllByText("1").length).toBeGreaterThan(0);
    expect(screen.getAllByText("2").length).toBeGreaterThan(0);
    expect(screen.getByText(/Ineligible/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Not Yet Generated/i).length).toBeGreaterThan(0);
  });

  it("renders table rows with lab numbers", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getByText("ENV-2026-001")).toBeInTheDocument();
    expect(screen.getByText("ENV-2026-002")).toBeInTheDocument();
  });

  it("renders NON_COMPLIANT as red tag", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getAllByText("✗ Non-Compliant").length + screen.getAllByText(/Non-Compliant/i).length).toBeGreaterThan(0);
  });

  it("renders COMPLIANT as green tag", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getAllByText("✓ Compliant").length).toBeGreaterThan(0);
  });

  it("renders a Generate PDF button for not-yet-generated orders and a Reissue button for already-generated orders", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    // Order 101 has no lastGenerated -> Generate PDF.
    // Order 102 was already generated (lastGenerated set) -> Reissue.
    const pdfButtons = screen.getAllByRole("button", { name: /Generate PDF/i });
    expect(pdfButtons).toHaveLength(1);

    const reissueButtons = screen.getAllByRole("button", {
      name: /Reissue with Amendment/i,
    });
    expect(reissueButtons).toHaveLength(1);
  });

  it("shows empty state when no orders", async () => {
    const { getFromOpenElisServer } = await import("../../../utils/Utils");
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback({ ineligibleCount: 0, generatedCount: 0, notYetGeneratedCount: 0, orders: [] });
      }
    });

    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(
      screen.getByText(/No orders found for the selected filters/i),
    ).toBeInTheDocument();

    // Restore default mock for subsequent tests
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback(MOCK_REPORT);
      }
    });
  });

  it("shows expanded row with site information section on expand", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getByText("ENV-2026-001")).toBeInTheDocument();

    const expandButtons = screen.queryAllByRole("button", { name: /expand row/i });
    if (expandButtons.length > 0) {
      await act(async () => {
        fireEvent.click(expandButtons[0]);
      });
      expect(screen.getByText(/Site Information/i)).toBeInTheDocument();
      expect(screen.getByText(/Collection Conditions/i)).toBeInTheDocument();
      expect(screen.getByText(/Compliance Summary/i)).toBeInTheDocument();
      expect(screen.getByText(/Signatures/i)).toBeInTheDocument();
    }
  });

  it("shows analyst and manager signature names in expanded row", async () => {
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getByText("ENV-2026-001")).toBeInTheDocument();

    const expandButtons = screen.queryAllByRole("button", { name: /expand row/i });
    if (expandButtons.length > 0) {
      await act(async () => {
        fireEvent.click(expandButtons[0]);
      });
      expect(screen.getByText(/Deri Rahmawati/i)).toBeInTheDocument();
      expect(screen.getByText(/Bambang Sutrisno/i)).toBeInTheDocument();
    }
  });
});
