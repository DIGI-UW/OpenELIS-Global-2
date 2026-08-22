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
      siteBindingId: "11",
      testMappings: { mapped: 4, total: 4, state: "COMPLETE" },
      resultMappings: { mapped: 5, total: 6, state: "INCOMPLETE" },
      usedBy: 2,
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
      siteBindingId: "12",
      testMappings: { mapped: 13, total: 13, state: "COMPLETE" },
      resultMappings: { mapped: 0, total: 0, state: "NOT_APPLICABLE" },
      usedBy: 0,
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
      siteBindingId: null,
      testMappings: { mapped: 0, total: 1, state: "INCOMPLETE" },
      resultMappings: { mapped: 0, total: 3, state: "INCOMPLETE" },
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
      callback(recognitionDraft(draftId)),
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

    const summary = screen.getByRole("region", {
      name: "Analyzer type summary",
    });
    expect(summary).toHaveTextContent("Analyzer Types3");
    expect(summary).toHaveTextContent("In Use2");
    expect(summary).toHaveTextContent("Needs Attention1");
    expect(summary).toHaveTextContent("Deactivated1");

    expect(
      screen.getByRole("columnheader", { name: "Analyzer type" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("columnheader", { name: "Protocol" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Tests mapped" }),
    ).toBeVisible();
    expect(
      screen.getByRole("columnheader", { name: "Results mapped" }),
    ).toBeVisible();
    expect(screen.getByRole("columnheader", { name: "Used by" })).toBeVisible();
    expect(screen.getByText("Cepheid GeneXpert MTB/RIF")).toBeVisible();
    expect(screen.getByText("Mindray BC-5380")).toBeVisible();
    expect(screen.queryByText("Tecan Infinite F50")).not.toBeInTheDocument();

    expect(screen.queryByText("Plugin class")).not.toBeInTheDocument();
    expect(screen.queryByText("Identifier pattern")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Create Profile" }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Duplicate Profile" }),
    ).toBeVisible();
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

  it("restores filters from the URL and round-trips changes through browser history", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?q=tecan&source=SHIPPED&protocol=FILE&mapping=INCOMPLETE&showDeactivated=true",
    );
    renderPage();

    expect(await screen.findByText("Tecan Infinite F50")).toBeVisible();
    expect(
      screen.queryByText("Cepheid GeneXpert MTB/RIF"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("searchbox", { name: "Search analyzer types" }),
    ).toHaveValue("tecan");
    expect(screen.getByRole("combobox", { name: "Created" })).toHaveValue(
      "SHIPPED",
    );
    expect(screen.getByRole("combobox", { name: "Protocol" })).toHaveValue(
      "FILE",
    );
    expect(
      screen.getByRole("combobox", { name: "Mapping status" }),
    ).toHaveValue("INCOMPLETE");
    expect(
      screen.getByRole("checkbox", { name: "Show deactivated" }),
    ).toBeChecked();

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

  it("opens the sole shared mapping editor and preserves the filtered return URL", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?source=SHIPPED&mapping=INCOMPLETE",
    );
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Cepheid GeneXpert MTB/RIF",
      }),
    );
    await userEvent.click(
      screen.getByRole("menuitem", { name: "Edit mappings" }),
    );

    await waitFor(() =>
      expect(window.location.pathname).toBe(
        "/analyzers/types/shipped.genexpert/mapping",
      ),
    );
    expect(window.location.search).toContain("revision=2");
    expect(new URLSearchParams(window.location.search).get("returnTo")).toBe(
      "/analyzers/types?source=SHIPPED&mapping=INCOMPLETE",
    );
  });

  it("starts a Bridge-owned site profile draft without fabricating a profile document", async () => {
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(
      screen.getByRole("button", { name: "Create Profile" }),
    );

    await waitFor(() =>
      expect(window.location.search).toContain("action=create"),
    );
    const dialog = screen.getByRole("dialog", { name: "Create Profile" });
    await userEvent.type(
      within(dialog).getByRole("textbox", { name: "Profile name" }),
      "Sysmex XN Series",
    );
    expect(
      within(dialog).queryByRole("textbox", { name: "Manufacturer" }),
    ).not.toBeInTheDocument();
    expect(
      within(dialog).queryByRole("combobox", { name: "Protocol" }),
    ).not.toBeInTheDocument();

    const create = within(dialog).getByRole("button", {
      name: "Create Profile",
    });
    expect(create).toBeEnabled();
    await userEvent.click(create);

    expect(createAnalyzerTypeDraft).toHaveBeenCalledWith(
      "Sysmex XN Series",
      expect.any(Function),
    );
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(window.location.search).toContain("draft=draft-create"),
    );
    expect(screen.getByText("Profile draft created")).toBeVisible();
  });

  it("duplicates and explicitly publishes an active profile without choosing its identity", async () => {
    renderPage();
    await screen.findByText("Cepheid GeneXpert MTB/RIF");

    await userEvent.click(
      screen.getByRole("button", { name: "Duplicate Profile" }),
    );
    const dialog = screen.getByRole("dialog", { name: "Duplicate Profile" });
    await userEvent.selectOptions(
      within(dialog).getByRole("combobox", {
        name: "Source analyzer type",
      }),
      "shipped.genexpert",
    );

    expect(
      within(dialog).getByRole("textbox", { name: "New profile name" }),
    ).toHaveValue("Cepheid GeneXpert MTB/RIF -1");
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Duplicate Profile" }),
    );

    expect(duplicateAnalyzerType).toHaveBeenCalledWith(
      "shipped.genexpert",
      2,
      "Cepheid GeneXpert MTB/RIF -1",
      expect.any(Function),
    );
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(window.location.search).toContain("draft=draft-duplicate"),
    );
    expect(within(dialog).getByText("Ready to publish")).toBeVisible();

    await userEvent.click(
      within(dialog).getByRole("button", { name: "Publish Profile" }),
    );
    expect(publishAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-duplicate",
      expect.any(Function),
    );
    await waitFor(() =>
      expect(window.location.search).not.toContain("action=duplicate"),
    );
  });

  it("restores the exact duplicate draft from a bookmarked URL after reload", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&draft=draft-duplicate",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate Profile",
    });
    expect(getAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-duplicate",
      expect.any(Function),
    );
    expect(within(dialog).getByText("Ready to publish")).toBeVisible();
    expect(
      within(dialog).getByRole("button", { name: "Publish Profile" }),
    ).toBeEnabled();
  });

  it("blocks duplicate publication until recognition changes are saved", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&draft=draft-duplicate",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate Profile",
    });
    expect(
      await within(dialog).findByRole("heading", {
        name: "Control result recognition",
      }),
    ).toBeVisible();
    const publish = within(dialog).getByRole("button", {
      name: "Publish Profile",
    });
    await waitFor(() => expect(publish).toBeEnabled());

    const value = within(dialog).getByRole("textbox", {
      name: "Order field 12 value",
    });
    await userEvent.clear(value);
    await userEvent.type(value, "CONTROL");
    expect(publish).toBeDisabled();

    await userEvent.click(
      within(dialog).getByRole("button", {
        name: "Save control recognition",
      }),
    );
    await waitFor(() => expect(publish).toBeEnabled());
  });

  it("duplicates the exact bookmarked profile revision instead of the latest revision", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=duplicate&profile=shipped.genexpert&revision=1&returnTo=%2Fanalyzers%2Ftypes%2Fshipped.genexpert%2Fmapping%3Frevision%3D1",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Duplicate Profile",
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
      within(dialog).getByRole("button", { name: "Duplicate Profile" }),
    );

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
      name: "Duplicate Profile",
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
      name: "Duplicate Profile",
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

  it("starts an immutable shared-profile update from the exact revision and preserves URL state", async () => {
    renderPage();
    await screen.findByText("Mindray BC-5380");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Mindray BC-5380",
      }),
    );
    await userEvent.click(
      screen.getByRole("menuitem", { name: "Update shared" }),
    );

    const dialog = screen.getByRole("dialog", {
      name: "Update Mindray BC-5380",
    });
    expect(within(dialog).getByText(/0 analyzers/)).toBeVisible();
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Start update" }),
    );

    expect(updateSharedAnalyzerType).toHaveBeenCalledWith(
      "site.mindray",
      1,
      expect.any(Function),
    );
    await waitFor(() =>
      expect(window.location.search).toContain("draft=draft-update"),
    );
    expect(screen.getByText("Profile update draft created")).toBeVisible();
  });

  it("restores the exact shared-profile update draft from a bookmarked URL", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=update&profile=site.mindray&draft=draft-update",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Update Mindray BC-5380",
    });
    expect(getAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-update",
      expect.any(Function),
    );
    expect(
      within(dialog).getByText("Profile update draft created"),
    ).toBeVisible();
    expect(updateSharedAnalyzerType).not.toHaveBeenCalled();
  });

  it("publishes a shared-profile update through the same recognition editor", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/types?action=update&profile=site.mindray&draft=draft-update",
    );

    renderPage();

    const dialog = await screen.findByRole("dialog", {
      name: "Update Mindray BC-5380",
    });
    expect(
      await within(dialog).findByRole("heading", {
        name: "Control result recognition",
      }),
    ).toBeVisible();
    expect(
      within(dialog).getByText(/Existing analyzers remain pinned/),
    ).toBeVisible();

    await userEvent.click(
      within(dialog).getByRole("button", { name: "Publish Profile" }),
    );
    expect(publishAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-update",
      expect.any(Function),
    );
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
    await userEvent.click(
      screen.getByRole("menuitem", { name: "View history" }),
    );

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
