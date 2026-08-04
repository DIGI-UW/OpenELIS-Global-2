import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import WhonetExport from "../WhonetExport";

const preview = {
  from: "2026-07-01",
  to: "2026-07-31",
  significance: "CLINICALLY_SIGNIFICANT",
  dedup: "FIRST_ISOLATE_7_DAY",
  totalCases: 1,
  totalIsolates: 1,
  afterSignificance: 1,
  afterDeduplication: 1,
  exportableIsolates: 1,
  exportedRows: 2,
  excludedRows: 1,
  canGenerate: true,
  page: 1,
  pageSize: 20,
  totalRows: 2,
  warnings: [
    {
      code: "ANTIBIOTIC_MAPPING_REQUIRED",
      resource: "antibiotics",
      resourceId: "antibiotic-2",
      itemLabel: "Gentamicin",
      excludedRows: 1,
    },
  ],
  rows: [
    {
      caseId: "case-1",
      isolateId: "isolate-1",
      accessionNumber: "LAB-001",
      patientIdentifier: "NAT-001",
      patientName: "Lovelace, Ada",
      specimenType: "Blood",
      organismCode: "eco",
      antibioticCode: "CIP",
      interpretation: "S",
      method: "MIC",
      finalizedAt: "2026-07-12T10:00:00Z",
    },
    {
      caseId: "case-1",
      isolateId: "isolate-1",
      accessionNumber: "LAB-001",
      patientIdentifier: "NAT-001",
      patientName: "Lovelace, Ada",
      specimenType: "Blood",
      organismCode: "eco",
      antibioticCode: "GEN",
      interpretation: "R",
      method: "MIC",
      finalizedAt: "2026-07-12T10:00:00Z",
    },
  ],
};

const previewUrl =
  "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&step=preview&page=1&pageSize=20";

const renderExport = (service, initialEntry = previewUrl) =>
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <IntlProvider locale="en" messages={messages}>
        <WhonetExport service={service} now={new Date(2026, 7, 4)} />
        <Route
          render={({ location }) => (
            <output data-testid="whonet-current-url">
              {location.pathname}
              {location.search}
            </output>
          )}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("WhonetExport", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the preview counts, all AST rows, and an exact mapping repair link", async () => {
    const service = {
      getWhonetPreview: vi.fn().mockResolvedValue(preview),
      generateWhonetExport: vi.fn(),
    };

    renderExport(service);

    expect(
      await screen.findByRole("heading", { name: "WHONET export" }),
    ).toBeInTheDocument();
    expect(service.getWhonetPreview).toHaveBeenCalledWith({
      from: "2026-07-01",
      to: "2026-07-31",
      significance: "CLINICALLY_SIGNIFICANT",
      dedup: "FIRST_ISOLATE_7_DAY",
      page: 1,
      pageSize: 20,
    });
    expect(screen.getByText("Gentamicin")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Fix antibiotic mapping" }),
    ).toHaveAttribute(
      "href",
      "/MasterListsPage/MicrobiologyReference/antibiotics?edit=antibiotic-2",
    );
    expect(screen.getByRole("cell", { name: "CIP" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "GEN" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Generate CSV" })).toBeEnabled();
  });

  it("updates Carbon controls through canonical URL state before previewing", async () => {
    const user = userEvent.setup();
    const service = {
      getWhonetPreview: vi.fn().mockResolvedValue(preview),
      generateWhonetExport: vi.fn(),
    };

    renderExport(
      service,
      "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&step=configure&page=1&pageSize=20",
    );

    await user.selectOptions(screen.getByLabelText("Inclusion"), "ALL");
    await user.selectOptions(screen.getByLabelText("De-duplication"), "NONE");
    await user.click(screen.getByRole("button", { name: "Preview export" }));

    await waitFor(() =>
      expect(screen.getByTestId("whonet-current-url")).toHaveTextContent(
        "significance=ALL&dedup=NONE&step=preview&page=1&pageSize=20",
      ),
    );
    await waitFor(() =>
      expect(service.getWhonetPreview).toHaveBeenCalledWith({
        from: "2026-07-01",
        to: "2026-07-31",
        significance: "ALL",
        dedup: "NONE",
        page: 1,
        pageSize: 20,
      }),
    );
  });

  it("downloads the generated CSV through an intentional user action", async () => {
    const user = userEvent.setup();
    const service = {
      getWhonetPreview: vi.fn().mockResolvedValue(preview),
      generateWhonetExport: vi.fn().mockResolvedValue({
        blob: new Blob(["csv-content"], { type: "text/csv" }),
        filename: "WHONET_2026-07-01_to_2026-07-31.csv",
      }),
    };
    const createObjectURL = vi
      .spyOn(URL, "createObjectURL")
      .mockReturnValue("blob:whonet");
    const revokeObjectURL = vi
      .spyOn(URL, "revokeObjectURL")
      .mockImplementation(() => {});
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});

    renderExport(service);
    await screen.findByRole("cell", { name: "CIP" });
    await user.click(screen.getByRole("button", { name: "Generate CSV" }));

    await waitFor(() =>
      expect(service.generateWhonetExport).toHaveBeenCalled(),
    );
    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:whonet");
  });

  it("uses Carbon pagination to preserve the preview policy on the next page", async () => {
    const user = userEvent.setup();
    const service = {
      getWhonetPreview: vi
        .fn()
        .mockResolvedValue({ ...preview, totalRows: 42 }),
      generateWhonetExport: vi.fn(),
    };

    renderExport(service);
    await screen.findByRole("cell", { name: "CIP" });
    await user.click(screen.getByRole("button", { name: "Next page" }));

    await waitFor(() =>
      expect(screen.getByTestId("whonet-current-url")).toHaveTextContent(
        "significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&step=preview&page=2&pageSize=20",
      ),
    );
    await waitFor(() =>
      expect(service.getWhonetPreview).toHaveBeenLastCalledWith({
        from: "2026-07-01",
        to: "2026-07-31",
        significance: "CLINICALLY_SIGNIFICANT",
        dedup: "FIRST_ISOLATE_7_DAY",
        page: 2,
        pageSize: 20,
      }),
    );
  });
});
