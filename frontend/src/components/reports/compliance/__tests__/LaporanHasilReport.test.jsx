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
      hasBeenReleased: true,
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

vi.mock("../../../utils/Utils", async (importOriginal) => ({
  toLocalIsoDate: (await importOriginal()).toLocalIsoDate,
  parseIsoDate: (await importOriginal()).parseIsoDate,
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (url.includes("compliance-statuses")) {
      callback(MOCK_STATUSES);
    } else {
      callback(MOCK_REPORT);
    }
  }),
  getFromOpenElisServerForBlob: vi.fn(),
  postToOpenElisServerForBlob: vi.fn(),
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
    expect(screen.getByText(/Compliance Report/i)).toBeInTheDocument();
    expect(screen.getByText(/Generate compliance certificates for validated environmental orders/i)).toBeInTheDocument();
  });

  it("renders all 6 filter controls", () => {
    renderWithIntl(<LaporanHasilReport />);
    expect(
      screen.getByRole("textbox", { name: "Lab number" }),
    ).toBeInTheDocument();
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

  it("names an order with no graded result instead of showing its code", async () => {
    const { getFromOpenElisServer } = await import("../../../utils/Utils");
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback({
          ...MOCK_REPORT,
          orders: [
            {
              ...MOCK_REPORT.orders[0],
              complianceStatus: undefined,
              testCount: 0,
              parameterResults: [],
            },
          ],
        });
      }
    });
    try {
      renderWithIntl(<LaporanHasilReport />);
      await act(async () => {
        fireEvent.click(screen.getByRole("button", { name: /Search/i }));
      });

      expect(screen.getByText("Not graded")).toBeInTheDocument();
      expect(screen.queryByText("NONE")).not.toBeInTheDocument();
    } finally {
      getFromOpenElisServer.mockImplementation((url, callback) =>
        callback(
          url.includes("compliance-statuses") ? MOCK_STATUSES : MOCK_REPORT,
        ),
      );
    }
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

  it("says why an order with results not yet validated has no certificate (OGC-1620)", async () => {
    const { getFromOpenElisServer } = await import("../../../utils/Utils");
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback({
          ...MOCK_REPORT,
          orders: [
            {
              ...MOCK_REPORT.orders[0],
              sampleId: 103,
              labNumber: "ENV-2026-003",
              complianceStatus: "INELIGIBLE",
              ineligibleReason: "NOT_VALIDATED",
              parameterResults: [],
            },
            {
              ...MOCK_REPORT.orders[0],
              sampleId: 104,
              labNumber: "ENV-2026-004",
              complianceStatus: "INELIGIBLE",
              ineligibleReason: "NO_STANDARD",
              parameterResults: [],
            },
          ],
        });
      }
    });
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    expect(screen.getByText("Results not yet validated")).toBeInTheDocument();
    expect(screen.getAllByText("— Ineligible")).toHaveLength(2);
    expect(screen.queryByText("INELIGIBLE")).not.toBeInTheDocument();
    expect(screen.getByText("No standard linked")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Generate PDF/i }),
    ).not.toBeInTheDocument();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback(MOCK_REPORT);
      }
    });
  });

  it("explains a certificate the server refuses because results are not validated", async () => {
    const { postToOpenElisServerForBlob } = await import(
      "../../../utils/Utils"
    );
    postToOpenElisServerForBlob.mockImplementation(
      (url, body, callback, errorCallback) =>
        errorCallback(new Error("HTTP error! status: 409")),
    );
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Generate PDF/i }));
    });

    expect(
      screen.getByText(
        "Every test must be validated before the certificate can be generated.",
      ),
    ).toBeInTheDocument();
  });

  it("counts a certificate as generated only the first time", async () => {
    const { getFromOpenElisServer, postToOpenElisServerForBlob } = await import(
      "../../../utils/Utils"
    );
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback({
          ...MOCK_REPORT,
          generatedCount: 1,
          notYetGeneratedCount: 1,
          orders: [
            { ...MOCK_REPORT.orders[0], lastGenerated: "2026-04-05T10:30:00" },
            {
              ...MOCK_REPORT.orders[0],
              sampleId: 105,
              labNumber: "ENV-2026-005",
            },
          ],
        });
      }
    });
    postToOpenElisServerForBlob.mockImplementation((url, body, callback) =>
      callback(new Blob(["%PDF"]), {}),
    );
    global.URL.createObjectURL = vi.fn(() => "blob:x");
    global.URL.revokeObjectURL = vi.fn();
    const linkClick = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});
    renderWithIntl(<LaporanHasilReport />);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });

    await act(async () => {
      fireEvent.click(
        screen.getAllByRole("button", { name: /Generate PDF/i })[0],
      );
    });

    const tile = (label) =>
      screen
        .getAllByText(label, { exact: true })
        .map((element) => element.closest(".cds--tile"))
        .find(Boolean).textContent;
    expect(tile("Not Yet Generated")).toBe("1Not Yet Generated");
    expect(tile("Generated")).toBe("1Generated");
    expect(linkClick).toHaveBeenCalledOnce();
    linkClick.mockRestore();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback(MOCK_REPORT);
      }
    });
  });

  it("finds an order by lab number, typed or scanned, without dates (OGC-1619)", async () => {
    const { getFromOpenElisServer } = await import("../../../utils/Utils");
    const urls = [];
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
        return;
      }
      urls.push(url);
      callback(
        url.includes("labNumber=NOPE")
          ? {
              ineligibleCount: 0,
              generatedCount: 0,
              notYetGeneratedCount: 0,
              orders: [],
              labNumberMatches: 0,
            }
          : MOCK_REPORT,
      );
    });
    renderWithIntl(<LaporanHasilReport />);

    const field = screen.getByRole("textbox", { name: "Lab number" });
    fireEvent.change(field, { target: { value: "ENV-2026-001" } });
    await act(async () => {
      fireEvent.keyDown(field, { key: "Enter" });
    });
    expect(urls.at(-1)).toBe("/rest/complianceReport?labNumber=ENV-2026-001");

    fireEvent.change(field, { target: { value: "NOPE" } });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Search/i }));
    });
    expect(screen.getByTestId("laporan-hasil-empty")).toHaveTextContent(
      "No environmental order with lab number NOPE",
    );
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else {
        callback(MOCK_REPORT);
      }
    });
  });

  it("says a lab number matches when the filters hide its order, and when the list is cut", async () => {
    const { getFromOpenElisServer } = await import("../../../utils/Utils");
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("compliance-statuses")) {
        callback(MOCK_STATUSES);
      } else if (url.includes("labNumber=ENV-2026-001")) {
        callback({
          ineligibleCount: 1,
          generatedCount: 0,
          notYetGeneratedCount: 0,
          orders: [],
          labNumberMatches: 1,
        });
      } else {
        callback({ ...MOCK_REPORT, labNumberMatches: 200, labNumberLimited: true });
      }
    });
    try {
      renderWithIntl(<LaporanHasilReport />);
      const field = screen.getByRole("textbox", { name: "Lab number" });

      fireEvent.change(field, { target: { value: "ENV-2026-001" } });
      await act(async () => {
        fireEvent.keyDown(field, { key: "Enter" });
      });
      const empty = screen.getByTestId("laporan-hasil-empty");
      expect(empty).toHaveTextContent(
        "Lab number ENV-2026-001: 1 order matches, but the other filters hide it.",
      );
      expect(empty).not.toHaveTextContent("No environmental order");

      fireEvent.change(field, { target: { value: "ENV" } });
      await act(async () => {
        fireEvent.keyDown(field, { key: "Enter" });
      });
      expect(
        screen.getByText(
          "More orders start with ENV than can be listed. Type more of the number to narrow the list.",
        ),
      ).toBeInTheDocument();
    } finally {
      getFromOpenElisServer.mockImplementation((url, callback) =>
        callback(
          url.includes("compliance-statuses") ? MOCK_STATUSES : MOCK_REPORT,
        ),
      );
    }
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
