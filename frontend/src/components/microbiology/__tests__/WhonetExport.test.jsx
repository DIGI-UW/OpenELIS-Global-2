import React from "react";
import { act, render, screen } from "@testing-library/react";
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
  afterSpecimen: 1,
  afterOrganism: 1,
  afterPatientOrigin: 1,
  clinicalPurposeCases: 1,
  screeningPurposeCases: 1,
  unspecifiedPurposeCases: 1,
  afterCulturePurpose: 1,
  afterSignificance: 1,
  afterDeduplication: 1,
  exportableIsolates: 1,
  exportedRows: 2,
  excludedRows: 1,
  canGenerate: true,
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
      specimenType: "Blood",
      organismCode: "eco",
      antibioticCode: "CIP",
      interpretation: "S",
      method: "MIC",
    },
    {
      caseId: "case-1",
      isolateId: "isolate-1",
      accessionNumber: "LAB-001",
      specimenType: "Blood",
      organismCode: "eco",
      antibioticCode: "GEN",
      interpretation: "R",
      method: "MIC",
    },
  ],
};

const previewUrl =
  "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&includeScreening=false&includeUnspecified=false&dedup=FIRST_ISOLATE_7_DAY&step=preview&page=1&pageSize=20";

const filterOptions = {
  specimenTypes: [
    { id: "sample-type-blood", label: "Blood" },
    { id: "sample-type-urine", label: "Urine" },
  ],
  organisms: [
    { id: "organism-1", label: "E. coli" },
    { id: "organism-2", label: "S. aureus" },
  ],
  patientOrigins: [
    { id: "INPATIENT", label: "Inpatient" },
    { id: "OUTPATIENT", label: "Outpatient" },
  ],
  significance: [
    { id: "CLINICALLY_SIGNIFICANT", label: "CLINICALLY_SIGNIFICANT" },
    { id: "NORMAL_FLORA", label: "NORMAL_FLORA" },
  ],
};

const createService = (overrides = {}) => ({
  getWhonetFilterOptions: vi.fn().mockResolvedValue(filterOptions),
  getWhonetPreview: vi.fn().mockResolvedValue(preview),
  generateWhonetExport: vi.fn(),
  ...overrides,
});

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
    const service = createService();

    renderExport(service);

    expect(
      await screen.findByRole("heading", { name: "WHONET export" }),
    ).toBeInTheDocument();
    expect(service.getWhonetPreview).toHaveBeenCalledWith({
      from: "2026-07-01",
      to: "2026-07-31",
      specimen: [],
      organism: [],
      origin: [],
      significance: ["CLINICALLY_SIGNIFICANT"],
      includeScreening: false,
      includeUnspecified: false,
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
    expect(
      screen.getByText("Isolates included").previousSibling,
    ).toHaveTextContent("1");
    expect(
      screen.getByText("After de-duplication").previousSibling,
    ).toHaveTextContent("1");
    expect(
      screen.getByText("Mappable isolates").previousSibling,
    ).toHaveTextContent("1");
    expect(
      screen.getByText("Clinical cultures").previousSibling,
    ).toHaveTextContent("1");
    expect(
      screen.getByText("Screening cultures").previousSibling,
    ).toHaveTextContent("1");
    expect(
      screen.getByText("Unspecified cultures").previousSibling,
    ).toHaveTextContent("1");
    expect(
      screen.getByText("Preview ready with 2 eligible rows."),
    ).toHaveAttribute("role", "status");
    expect(screen.getByRole("button", { name: "Generate CSV" })).toBeEnabled();
  });

  it("links an unmapped specimen to its owning editor with the exact preview return", async () => {
    const service = createService({
      getWhonetPreview: vi.fn().mockResolvedValue({
        ...preview,
        warnings: [
          {
            code: "SPECIMEN_MAPPING_REQUIRED",
            resource: "specimen-types",
            resourceId: "sample-type-2",
            itemLabel: "Blood culture",
            excludedRows: 2,
          },
        ],
      }),
    });

    renderExport(service);

    expect(
      await screen.findByRole("link", { name: "Fix specimen mapping" }),
    ).toHaveAttribute(
      "href",
      `/MasterListsPage/SampleTypeEditor/sample-type-2/basic-info?focus=whonet&returnTo=${encodeURIComponent(previewUrl)}`,
    );
  });

  it("updates Carbon controls through canonical URL state before previewing", async () => {
    const user = userEvent.setup();
    const service = createService();

    renderExport(
      service,
      "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&includeScreening=false&includeUnspecified=false&dedup=FIRST_ISOLATE_7_DAY&step=configure&page=1&pageSize=20",
    );

    const specimenFilter = await screen.findByRole("combobox", {
      name: /^Specimen types/,
    });
    await user.click(specimenFilter);
    await user.click(screen.getByRole("option", { name: "Blood" }));
    await user.keyboard("{Escape}");
    const significanceFilter = screen.getByRole("combobox", {
      name: /^Inclusion/,
    });
    await user.click(significanceFilter);
    await user.click(screen.getByRole("option", { name: "Normal flora" }));
    await user.keyboard("{Escape}");
    await user.selectOptions(screen.getByLabelText("De-duplication"), "NONE");
    await user.click(
      screen.getByRole("checkbox", {
        name: "Include active screening or carriage cultures",
      }),
    );
    await user.click(
      screen.getByRole("checkbox", {
        name: "Include historical cultures with unspecified purpose",
      }),
    );
    await user.click(screen.getByRole("button", { name: "Preview export" }));

    await waitFor(() =>
      expect(screen.getByTestId("whonet-current-url")).toHaveTextContent(
        "specimen=sample-type-blood&significance=CLINICALLY_SIGNIFICANT&significance=NORMAL_FLORA&includeScreening=true&includeUnspecified=true&dedup=NONE&step=preview&page=1&pageSize=20",
      ),
    );
    await waitFor(() =>
      expect(service.getWhonetPreview).toHaveBeenCalledWith({
        from: "2026-07-01",
        to: "2026-07-31",
        specimen: ["sample-type-blood"],
        organism: [],
        origin: [],
        significance: ["CLINICALLY_SIGNIFICANT", "NORMAL_FLORA"],
        includeScreening: true,
        includeUnspecified: true,
        dedup: "NONE",
        page: 1,
        pageSize: 20,
      }),
    );
  });

  it("downloads the generated CSV through an intentional user action", async () => {
    const user = userEvent.setup();
    const service = createService({
      generateWhonetExport: vi.fn().mockResolvedValue({
        blob: new Blob(["csv-content"], { type: "text/csv" }),
        filename: "WHONET_2026-07-01_to_2026-07-31.csv",
      }),
    });
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

  it("blocks generation when validation leaves no exportable rows", async () => {
    const blockedPreview = {
      ...preview,
      exportableIsolates: 0,
      exportedRows: 0,
      excludedRows: 2,
      canGenerate: false,
      rows: [],
    };
    const service = createService({
      getWhonetPreview: vi.fn().mockResolvedValue(blockedPreview),
    });

    renderExport(service);

    expect(
      await screen.findByRole("button", { name: "Generate CSV" }),
    ).toBeDisabled();
    expect(service.generateWhonetExport).not.toHaveBeenCalled();
  });

  it("uses Carbon pagination to preserve the preview policy on the next page", async () => {
    const user = userEvent.setup();
    const service = createService({
      getWhonetPreview: vi
        .fn()
        .mockResolvedValue({ ...preview, exportedRows: 42 }),
    });

    renderExport(service);
    await screen.findByRole("cell", { name: "CIP" });
    await user.click(screen.getByRole("button", { name: "Next page" }));

    await waitFor(() =>
      expect(screen.getByTestId("whonet-current-url")).toHaveTextContent(
        "significance=CLINICALLY_SIGNIFICANT&includeScreening=false&includeUnspecified=false&dedup=FIRST_ISOLATE_7_DAY&step=preview&page=2&pageSize=20",
      ),
    );
    await waitFor(() =>
      expect(service.getWhonetPreview).toHaveBeenLastCalledWith({
        from: "2026-07-01",
        to: "2026-07-31",
        specimen: [],
        organism: [],
        origin: [],
        significance: ["CLINICALLY_SIGNIFICANT"],
        includeScreening: false,
        includeUnspecified: false,
        dedup: "FIRST_ISOLATE_7_DAY",
        page: 2,
        pageSize: 20,
      }),
    );
  });
  it("clears a previous preview when a refreshed page request fails", async () => {
    const user = userEvent.setup();
    const service = createService({
      getWhonetPreview: vi
        .fn()
        .mockResolvedValueOnce({ ...preview, exportedRows: 42 })
        .mockRejectedValueOnce({ status: 500 }),
    });

    renderExport(service);
    await screen.findByRole("cell", { name: "CIP" });
    await user.click(screen.getByRole("button", { name: "Next page" }));

    expect(
      await screen.findByText("The export request could not be completed."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("cell", { name: "CIP" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Generate CSV" }),
    ).not.toBeInTheDocument();
  });

  it("clears a filter-options error after a successful Carbon date retry", async () => {
    const user = userEvent.setup();
    const service = createService({
      getWhonetFilterOptions: vi
        .fn()
        .mockRejectedValueOnce(new Error("offline"))
        .mockResolvedValue(filterOptions),
    });

    renderExport(
      service,
      "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&includeScreening=false&includeUnspecified=false&dedup=FIRST_ISOLATE_7_DAY&step=configure&page=1&pageSize=20",
    );

    expect(
      await screen.findByText(
        "The export service could not be reached. Try again when the connection is available.",
      ),
    ).toBeInTheDocument();

    const fromDate = screen.getByRole("textbox", { name: "From" });
    await user.clear(fromDate);
    await user.type(fromDate, "2026-07-02");
    await user.keyboard("{Enter}");

    await waitFor(() =>
      expect(service.getWhonetFilterOptions).toHaveBeenCalledTimes(2),
    );
    await waitFor(() =>
      expect(
        screen.queryByText(
          "The export service could not be reached. Try again when the connection is available.",
        ),
      ).not.toBeInTheDocument(),
    );
  });

  it("does not clear a preview error when filter options finish loading", async () => {
    let resolveFilterOptions;
    const filterOptionsRequest = new Promise((resolve) => {
      resolveFilterOptions = resolve;
    });
    const blockedError = Object.assign(new Error("blocked"), {
      code: "MICROBIOLOGY_WHONET_EXPORT_BLOCKED",
      status: 409,
    });
    const service = createService({
      getWhonetFilterOptions: vi.fn().mockReturnValue(filterOptionsRequest),
      getWhonetPreview: vi.fn().mockRejectedValue(blockedError),
    });

    renderExport(service);

    expect(
      await screen.findByText(
        "No valid rows remain. Resolve the listed readiness issues before generating the CSV.",
      ),
    ).toBeInTheDocument();

    await act(async () => resolveFilterOptions(filterOptions));

    expect(
      screen.getByText(
        "No valid rows remain. Resolve the listed readiness issues before generating the CSV.",
      ),
    ).toBeInTheDocument();
  });
});
