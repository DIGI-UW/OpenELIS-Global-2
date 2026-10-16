import React from "react";
import { act, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import {
  createAnalyzerTypeDraft,
  duplicateAnalyzerType,
  getAnalyzerTypeCatalog,
  getAnalyzerTypeControlRecognition,
  getAnalyzerTypeDraft,
  getAnalyzerTypeRevision,
  publishAnalyzerTypeDraft,
  updateAnalyzerTypeControlRecognition,
  updateSharedAnalyzerType,
} from "../../../services/analyzerService";
import AnalyzerTypeManagement from "./AnalyzerTypeManagement";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));
vi.mock("../../../services/analyzerService", () => ({
  createAnalyzerTypeDraft: vi.fn(),
  duplicateAnalyzerType: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  getAnalyzerTypeControlRecognition: vi.fn(),
  getAnalyzerTypeDraft: vi.fn(),
  updateAnalyzerTypeDraft: vi.fn(),
  getAnalyzerTypeRevision: vi.fn(),
  publishAnalyzerTypeDraft: vi.fn(),
  updateAnalyzerTypeControlRecognition: vi.fn(),
  updateSharedAnalyzerType: vi.fn(),
}));

const catalog = {
  schemaVersion: "1.0",
  catalogFingerprint: "sha256:catalog",
  summary: {
    total: 3,
    inUse: 2,
    needsAttention: 1,
    deactivated: 1,
  },
  types: [
    {
      profileId: "shipped.genexpert",
      revision: 2,
      revisionFingerprint: "sha256:genexpert",
      displayName: "Cepheid GeneXpert MTB/RIF",
      manufacturer: "Cepheid",
      model: "GeneXpert",
      source: "SHIPPED",
      status: "ACTIVE",
      protocol: "ASTM",
      parentProfileId: null,
      parentRevision: null,
      testMappings: { mapped: 3, excluded: 1, total: 4, state: "COMPLETE" },
      resultMappings: {
        mapped: 5,
        excluded: 0,
        total: 6,
        state: "INCOMPLETE",
      },
      usedBy: 2,
      affectedAnalyzers: [
        { id: "501", name: "GeneXpert - Main Lab", active: true },
        { id: "502", name: "GeneXpert - TB Bench", active: true },
      ],
      readiness: "NEEDS_ATTENTION",
      publicationAction: "SHIPPED",
      publicationActor: "system",
      publicationTime: "2026-08-18T12:00:00Z",
    },
    {
      profileId: "site.mindray",
      revision: 1,
      revisionFingerprint: "sha256:mindray",
      displayName: "Mindray BC-5380",
      manufacturer: "Mindray",
      model: "BC-5380",
      source: "SITE",
      status: "ACTIVE",
      protocol: "HL7",
      parentProfileId: "shipped.mindray",
      parentRevision: 3,
      testMappings: { mapped: 13, excluded: 0, total: 13, state: "COMPLETE" },
      resultMappings: {
        mapped: 0,
        excluded: 0,
        total: 0,
        state: "NOT_APPLICABLE",
      },
      usedBy: 2,
      affectedAnalyzers: [
        { id: "601", name: "Mindray - Main Lab", active: true },
        { id: "602", name: "Mindray - Night Bench", active: true },
      ],
      readiness: "READY",
      publicationAction: "DUPLICATED",
      publicationActor: "17",
      publicationTime: "2026-08-18T13:00:00Z",
    },
    {
      profileId: "shipped.tecan",
      revision: 1,
      revisionFingerprint: "sha256:tecan",
      displayName: "Tecan Infinite F50",
      manufacturer: "Tecan",
      model: "Infinite F50",
      source: "SHIPPED",
      status: "INACTIVE",
      protocol: "FILE",
      parentProfileId: null,
      parentRevision: null,
      testMappings: { mapped: 0, excluded: 0, total: 1, state: "INCOMPLETE" },
      resultMappings: { mapped: 0, excluded: 0, total: 3, state: "INCOMPLETE" },
      usedBy: 0,
      readiness: "NEEDS_ATTENTION",
      publicationAction: "DEACTIVATED",
      publicationActor: "17",
      publicationTime: "2026-08-18T14:00:00Z",
    },
  ],
};

const recognitionDraft = (draftId) => ({
  draftId,
  kind: draftId === "draft-update" ? "UPDATE" : "DUPLICATE",
  validationIssues: [],
  recognition: {
    mode: "RULES",
    affirmedNoControlResults: false,
    description: "Any listed condition identifies a control result.",
    conditions: [
      {
        key: "order-action-control",
        kind: "FIELD_VALUE_EQUALS",
        sourceKey: "source-safe-1",
        sourceLabel: "Order field 12",
        description: "Order field 12 equals Q",
        value: "Q",
        editable: true,
        controlLevel: null,
        controlType: null,
      },
    ],
    availableSources: [{ key: "source-safe-1", label: "Order field 12" }],
  },
});

const renderPage = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <AnalyzerTypeManagement />
      </IntlProvider>
    </BrowserRouter>,
  );

describe("AnalyzerTypeManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(Element.prototype, "getBoundingClientRect").mockReturnValue({
      bottom: 40,
      height: 40,
      left: 0,
      right: 160,
      top: 0,
      width: 160,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    });
    window.history.replaceState({}, "", "/analyzers/types");
    getAnalyzerTypeCatalog.mockImplementation((callback) => {
      callback(catalog);
    });
    getAnalyzerTypeRevision.mockImplementation(
      (profileId, revision, callback) => {
        const latest = catalog.types.find(
          (type) => type.profileId === profileId,
        );
        callback(latest ? { ...latest, revision } : undefined);
      },
    );
    createAnalyzerTypeDraft.mockImplementation((displayName, callback) => {
      callback({
        draftId: "draft-create",
        kind: "CREATE",
        profile: {
          profileMeta: {
            id: "site.bridge-generated",
            displayName,
          },
        },
        validationIssues: ["protocol is required"],
      });
    });
    duplicateAnalyzerType.mockImplementation(
      (profileId, sourceRevision, displayName, callback) => {
        callback({
          draftId: "draft-duplicate",
          kind: "DUPLICATE",
          baseProfileId: profileId,
          baseRevision: sourceRevision,
          profile: {
            profileMeta: {
              id: "site.bridge-generated-duplicate",
              displayName,
            },
          },
          validationIssues: [],
        });
      },
    );
    updateSharedAnalyzerType.mockImplementation(
      (profileId, sourceRevision, callback) => {
        callback({
          draftId: "draft-update",
          kind: "UPDATE",
          baseProfileId: profileId,
          baseRevision: sourceRevision,
          profile: {
            profileMeta: {
              id: profileId,
              displayName: "Mindray BC-5380",
            },
          },
          validationIssues: [],
        });
      },
    );
    getAnalyzerTypeDraft.mockImplementation((draftId, callback) => {
      const kind =
        draftId === "draft-create"
          ? "CREATE"
          : draftId === "draft-update"
            ? "UPDATE"
            : "DUPLICATE";
      callback({
        draftId,
        kind,
        baseProfileId:
          kind === "CREATE"
            ? null
            : kind === "UPDATE"
              ? "site.mindray"
              : "shipped.genexpert",
        baseRevision: kind === "CREATE" ? null : kind === "UPDATE" ? 1 : 2,
        profile: {
          profileMeta: {
            id: `site.${draftId}`,
            displayName:
              kind === "CREATE"
                ? "Sysmex XN Series"
                : kind === "UPDATE"
                  ? "Mindray BC-5380"
                  : "Cepheid GeneXpert MTB/RIF -1",
          },
        },
        validationIssues: kind === "CREATE" ? ["protocol is required"] : [],
      });
    });
    publishAnalyzerTypeDraft.mockImplementation((draftId, callback) => {
      callback({
        profile: {
          profileMeta: { id: "site.bridge-generated-duplicate" },
          catalog: { revision: 1, source: "SITE", status: "ACTIVE" },
        },
      });
    });
    getAnalyzerTypeControlRecognition.mockImplementation((draftId, callback) =>
      callback(
        draftId === "draft-create"
          ? {
              ...recognitionDraft(draftId),
              validationIssues: ["protocol is required"],
            }
          : recognitionDraft(draftId),
      ),
    );
    updateAnalyzerTypeControlRecognition.mockImplementation(
      (draftId, update, callback) =>
        callback({
          ...recognitionDraft(draftId),
          recognition: {
            ...recognitionDraft(draftId).recognition,
            mode: update.mode,
            affirmedNoControlResults: update.affirmedNoControlResults,
            conditions: update.conditions.map((condition) => ({
              ...condition,
              sourceLabel: "Order field 12",
              description: `Order field 12 equals ${condition.value}`,
              editable: true,
            })),
          },
        }),
    );
    postToOpenElisServerJsonResponse.mockImplementation(
      (endpoint, payload, callback) => {
        callback({ profile: { profileId: "site.created" } });
      },
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the lab-facing profile catalog and removes developer plugin fields", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Analyzer Types",
      }),
    ).toBeInTheDocument();
    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(breadcrumb).toHaveTextContent("Home");
    expect(breadcrumb).toHaveTextContent("Analyzers");
    expect(breadcrumb).toHaveTextContent("Analyzer Types");
    expect(screen.getByRole("link", { name: "Analyzers" })).toHaveAttribute(
      "href",
      "/analyzers",
    );

    // Counts of the catalog by state, each the view it names; active types first.
    const view = (name) => screen.getByTestId(`analyzer-type-view-${name}`);
    expect(view("all")).toHaveTextContent("2Active");
    expect(view("all")).toHaveAttribute("aria-pressed", "true");
    expect(view("inUse")).toHaveTextContent("2In use");
    expect(view("needsMapping")).toHaveTextContent("1Needs mapping");
    expect(view("deactivated")).toHaveTextContent("1Deactivated");

    expect(
      screen.getByRole("columnheader", { name: "Analyzer type" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Mapping" })).toBeVisible();
    expect(screen.getByRole("columnheader", { name: "Used by" })).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Cepheid GeneXpert MTB/RIF" }),
    ).toHaveAttribute(
      "href",
      expect.stringContaining("/analyzers/types/shipped.genexpert/mapping"),
    );
    expect(screen.getByText("Mindray BC-5380")).toBeVisible();
    expect(screen.getByText("3 of 4 tests · 1 not received")).toBeVisible();
    expect(screen.getByText("5 of 6 values")).toBeVisible();
    // A type whose tests report no coded results has no values line.
    expect(screen.getByText("13 of 13 tests")).toBeVisible();
    expect(screen.queryByText(/of 0 values/)).not.toBeInTheDocument();
    expect(screen.queryByText("Tecan Infinite F50")).not.toBeInTheDocument();

    expect(screen.queryByText("Plugin class")).not.toBeInTheDocument();
    expect(screen.queryByText("Identifier pattern")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create type" })).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Duplicate type" }),
    ).toBeVisible();
  });

  it("keeps rendering when a refreshed catalog removes a displayed profile", async () => {
    let deliverCatalog;
    getAnalyzerTypeCatalog.mockImplementation((callback) => {
      deliverCatalog = callback;
      callback(catalog);
    });

    renderPage();
    expect(await screen.findByText("Cepheid GeneXpert MTB/RIF")).toBeVisible();

    await act(async () => {
      deliverCatalog({
        ...catalog,
        types: catalog.types.filter(
          (type) => type.profileId !== "shipped.genexpert",
        ),
      });
    });

    expect(
      screen.getByRole("heading", { name: "Analyzer Types" }),
    ).toBeVisible();
    expect(
      screen.queryByText("Cepheid GeneXpert MTB/RIF"),
    ).not.toBeInTheDocument();
  });

  it("shows a Carbon action when the catalog fails and retries visibly", async () => {
    getAnalyzerTypeCatalog
      .mockImplementationOnce((callback) => callback(undefined))
      .mockImplementationOnce((callback) => callback(catalog));

    renderPage();

    const retry = await screen.findByRole("button", { name: "Retry" });
    expect(
      screen.getByText("Analyzer types could not be loaded"),
    ).toBeVisible();
    await userEvent.click(retry);

    expect(getAnalyzerTypeCatalog).toHaveBeenCalledTimes(2);
    expect(await screen.findByText("Cepheid GeneXpert MTB/RIF")).toBeVisible();
  });

  it("names each profile file the Bridge set aside and still lists the others", async () => {
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        ...catalog,
        issues: [
          {
            source: "file [/app/analyzer-profiles/broken.json]",
            reason: "Unexpected character",
          },
        ],
      }),
    );

    renderPage();

    expect(await screen.findByText("Cepheid GeneXpert MTB/RIF")).toBeVisible();
    expect(
      screen.getByText("Some analyzer types could not be loaded"),
    ).toBeVisible();
    expect(
      screen.getByText(/file \[\/app\/analyzer-profiles\/broken\.json\]/),
    ).toBeVisible();
    expect(screen.getByText(/Unexpected character/)).toBeVisible();
  });

  it("restores filters from the URL and round-trips changes through browser history", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?q=tecan&source=SHIPPED&protocol=FILE&view=deactivated",
    );
    renderPage();

    expect(await screen.findByText("Tecan Infinite F50")).toBeVisible();
    expect(
      screen.queryByText("Cepheid GeneXpert MTB/RIF"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("searchbox", { name: "Search analyzer types" }),
    ).toHaveValue("tecan");
    expect(screen.getByRole("combobox", { name: "Source" })).toHaveValue(
      "SHIPPED",
    );
    expect(screen.getByRole("combobox", { name: "Protocol" })).toHaveValue(
      "FILE",
    );
    expect(
      screen.getByTestId("analyzer-type-view-deactivated"),
    ).toHaveAttribute("aria-pressed", "true");

    await userEvent.selectOptions(
      screen.getByRole("combobox", { name: "Protocol" }),
      "ASTM",
    );
    await waitFor(() =>
      expect(window.location.search).toContain("protocol=ASTM"),
    );

    await act(async () => {
      window.history.back();
      window.dispatchEvent(new PopStateEvent("popstate"));
    });

    await waitFor(() =>
      expect(screen.getByRole("combobox", { name: "Protocol" })).toHaveValue(
        "FILE",
      ),
    );
    expect(screen.getByText("Tecan Infinite F50")).toBeVisible();
  });

  it("opens the read-only defaults and preserves the filtered return URL", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?source=SHIPPED&view=needsMapping",
    );
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Cepheid GeneXpert MTB/RIF",
      }),
    );
    await userEvent.click(
      screen.getByRole("menuitem", { name: "Default mappings" }),
    );

    await waitFor(() =>
      expect(window.location.pathname).toBe(
        "/analyzers/types/shipped.genexpert/mapping",
      ),
    );
    expect(window.location.search).toContain("revision=2");
    expect(new URLSearchParams(window.location.search).get("returnTo")).toBe(
      "/analyzers/types?source=SHIPPED&view=needsMapping",
    );
  });

  it("names a new type in a dialog and opens its draft on its own page", async () => {
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(screen.getByRole("button", { name: "Create type" }));

    await waitFor(() =>
      expect(window.location.search).toContain("action=create"),
    );
    const dialog = screen.getByRole("dialog", { name: "Create type" });
    await userEvent.type(
      within(dialog).getByRole("textbox", { name: "Type name" }),
      "Sysmex XN Series",
    );
    // The dialog only names the type; the settings are on the draft's page.
    expect(
      within(dialog).queryByRole("combobox", { name: "Protocol" }),
    ).not.toBeInTheDocument();

    await userEvent.click(
      within(dialog).getByRole("button", { name: "Create type" }),
    );

    expect(createAnalyzerTypeDraft).toHaveBeenCalledWith(
      "Sysmex XN Series",
      expect.any(Function),
    );
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(window.location.pathname).toBe(
        "/analyzers/types/drafts/draft-create",
      ),
    );
    expect(new URLSearchParams(window.location.search).get("returnTo")).toBe(
      "/analyzers/types",
    );
  });

  it("duplicates an active type and opens the copy's draft on its own page", async () => {
    window.history.replaceState({}, "", "/analyzers/types?protocol=ASTM");
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(
      screen.getByRole("button", { name: "Duplicate type" }),
    );
    const dialog = screen.getByRole("dialog", { name: "Duplicate type" });
    await userEvent.selectOptions(
      within(dialog).getByRole("combobox", {
        name: "Source analyzer type",
      }),
      "shipped.genexpert",
    );

    expect(
      within(dialog).getByRole("textbox", { name: "Name of the new type" }),
    ).toHaveValue("Cepheid GeneXpert MTB/RIF -1");
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Duplicate type" }),
    );

    expect(duplicateAnalyzerType).toHaveBeenCalledWith(
      "shipped.genexpert",
      2,
      "Cepheid GeneXpert MTB/RIF -1",
      expect.any(Function),
    );
    await waitFor(() =>
      expect(window.location.pathname).toBe(
        "/analyzers/types/drafts/draft-duplicate",
      ),
    );
    // Finishing later returns to the list as it was filtered.
    expect(new URLSearchParams(window.location.search).get("returnTo")).toBe(
      "/analyzers/types?protocol=ASTM",
    );
  });

  it("opens a draft's page from an address saved when drafts were edited in a dialog", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&draft=draft-duplicate",
    );

    renderPage();

    await waitFor(() =>
      expect(window.location.pathname).toBe(
        "/analyzers/types/drafts/draft-duplicate",
      ),
    );
    expect(new URLSearchParams(window.location.search).get("returnTo")).toBe(
      "/analyzers/types",
    );
  });

  it("duplicates the exact bookmarked profile revision instead of the latest revision", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&revision=1&returnTo=%2Fanalyzers%2Ftypes%2Fshipped.genexpert%2Fmapping%3Frevision%3D1",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate type",
    });
    await waitFor(() =>
      expect(getAnalyzerTypeRevision).toHaveBeenCalledWith(
        "shipped.genexpert",
        1,
        expect.any(Function),
      ),
    );
    expect(within(dialog).getByText(/revision 1/)).toBeVisible();

    await userEvent.click(
      within(dialog).getByRole("button", { name: "Duplicate type" }),
    );

    expect(duplicateAnalyzerType).toHaveBeenCalledWith(
      "shipped.genexpert",
      1,
      "Cepheid GeneXpert MTB/RIF -1",
      expect.any(Function),
    );
  });

  it("waits for the exact bookmarked revision before enabling duplication", async () => {
    let finishRevisionLoad;
    getAnalyzerTypeRevision.mockImplementation(
      (_profileId, _revision, callback) => {
        finishRevisionLoad = callback;
      },
    );
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&revision=1",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate type",
    });
    const duplicate = within(dialog).getByRole("button", {
      name: "Duplicate type",
    });
    expect(duplicate).toBeDisabled();

    act(() => {
      finishRevisionLoad({ ...catalog.types[0], revision: 1 });
    });

    await waitFor(() => expect(duplicate).toBeEnabled());
    await userEvent.click(duplicate);
    expect(duplicateAnalyzerType).toHaveBeenCalledWith(
      "shipped.genexpert",
      1,
      "Cepheid GeneXpert MTB/RIF -1",
      expect.any(Function),
    );
  });

  it("returns to the mapping page that launched a profile action", async () => {
    const returnTo =
      "/analyzers/types/shipped.genexpert/mapping?revision=1&returnTo=%2Fanalyzers%2Ftypes%3Fprotocol%3DASTM";
    window.history.replaceState(
      {},
      "",
      `/analyzers/types?action=duplicate&profile=shipped.genexpert&revision=1&returnTo=${encodeURIComponent(
        returnTo,
      )}`,
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate type",
    });
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Cancel" }),
    );

    await waitFor(() =>
      expect(`${window.location.pathname}${window.location.search}`).toBe(
        returnTo,
      ),
    );
  });

  it("rejects an external profile-action return URL", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&returnTo=%2F%2Fevil.example",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate type",
    });
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Cancel" }),
    );

    await waitFor(() => {
      expect(window.location.pathname).toBe("/analyzers/types");
      expect(window.location.search).toBe("");
    });
  });

  it("deactivates a shipped profile through a confirmation dialog and exposes no delete action", async () => {
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Cepheid GeneXpert MTB/RIF",
      }),
    );
    expect(
      document.querySelector(".cds--overflow-menu--flip"),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole("menuitem", { name: "Deactivate" }));

    const dialog = screen.getByRole("dialog", {
      name: "Deactivate Cepheid GeneXpert MTB/RIF",
    });
    expect(within(dialog).queryByText(/delete/i)).not.toBeInTheDocument();
    await userEvent.click(
      within(dialog).getByRole("button", { name: /Deactivate$/ }),
    );

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/analyzer-types/shipped.genexpert/deactivate",
      "{}",
      expect.any(Function),
    );
    expect(screen.queryByRole("menuitem", { name: /delete/i })).toBeNull();
  });

  it("starts a new revision of a shared type from its exact revision and opens its draft", async () => {
    renderPage();
    await screen.findByText("Mindray BC-5380");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Mindray BC-5380",
      }),
    );
    await userEvent.click(
      screen.getByRole("menuitem", { name: "New revision" }),
    );

    const dialog = screen.getByRole("dialog", {
      name: "New revision of Mindray BC-5380",
    });
    expect(within(dialog).getByText(/2 analyzers/)).toBeVisible();
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Start the revision" }),
    );

    expect(updateSharedAnalyzerType).toHaveBeenCalledWith(
      "site.mindray",
      1,
      expect.any(Function),
    );
    await waitFor(() =>
      expect(window.location.pathname).toBe(
        "/analyzers/types/drafts/draft-update",
      ),
    );
  });

  it("names every affected analyzer before starting a shared-profile update", async () => {
    renderPage();
    await screen.findByText("Mindray BC-5380");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Mindray BC-5380",
      }),
    );
    await userEvent.click(
      screen.getByRole("menuitem", { name: "New revision" }),
    );

    const dialog = screen.getByRole("dialog", {
      name: "New revision of Mindray BC-5380",
    });
    expect(within(dialog).getByText("Mindray - Main Lab")).toBeVisible();
    expect(within(dialog).getByText("Mindray - Night Bench")).toBeVisible();
    expect(
      within(dialog).getByText(/remain pinned to their current revisions/),
    ).toBeVisible();
  });

  it("says what a published draft did when its page returns here", async () => {
    window.history.replaceState(
      { state: { analyzerTypeNotice: "update" }, key: "published" },
      "",
      "/analyzers/types",
    );

    renderPage();

    expect(await screen.findByText("Type updated")).toBeVisible();
  });

  it("loads revision history through the profile history action", async () => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      expect(endpoint).toBe("/rest/analyzer-types/site.mindray/history");
      callback([
        {
          profile: {
            profileMeta: {
              id: "site.mindray",
              displayName: "Mindray BC-5380",
            },
            catalog: { revision: 1, status: "ACTIVE" },
          },
          publication: {
            action: "DUPLICATED",
            actor: "17",
            markedAt: "2026-08-18T13:00:00Z",
          },
        },
      ]);
    });
    renderPage();
    await screen.findByText("Mindray BC-5380");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Mindray BC-5380",
      }),
    );
    await userEvent.click(screen.getByRole("menuitem", { name: "History" }));

    const dialog = await screen.findByRole("dialog", {
      name: "Mindray BC-5380 history",
    });
    expect(within(dialog).getByText("Revision 1")).toBeVisible();
    expect(within(dialog).getByText("Duplicated")).toBeVisible();
    expect(within(dialog).getByText("17")).toBeVisible();
    expect(
      within(dialog).getByText(
        new Intl.DateTimeFormat("en", {
          year: "numeric",
          month: "short",
          day: "numeric",
          hour: "numeric",
          minute: "2-digit",
        }).format(new Date("2026-08-18T13:00:00Z")),
      ),
    ).toBeVisible();
  });
});
