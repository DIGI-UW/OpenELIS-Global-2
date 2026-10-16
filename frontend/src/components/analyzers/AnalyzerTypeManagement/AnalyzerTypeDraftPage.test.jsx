import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route, useLocation } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getAnalyzerTypeControlRecognition,
  getAnalyzerTypeDraft,
  publishAnalyzerTypeDraft,
  updateAnalyzerTypeControlRecognition,
} from "../../../services/analyzerService";
import AnalyzerTypeDraftPage from "./AnalyzerTypeDraftPage";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzerTypeControlRecognition: vi.fn(),
  getAnalyzerTypeDraft: vi.fn(),
  publishAnalyzerTypeDraft: vi.fn(),
  updateAnalyzerTypeControlRecognition: vi.fn(),
  updateAnalyzerTypeDraft: vi.fn(),
}));

const drafts = {
  "draft-create": {
    draftId: "draft-create",
    kind: "CREATE",
    baseProfileId: null,
    baseRevision: null,
    profile: { profileMeta: { id: "site.new", displayName: "Sysmex XN" } },
    validationIssues: ["protocol is required"],
  },
  "draft-duplicate": {
    draftId: "draft-duplicate",
    kind: "DUPLICATE",
    baseProfileId: "shipped.genexpert",
    baseRevision: 2,
    profile: {
      profileMeta: {
        id: "site.copy",
        displayName: "Cepheid GeneXpert MTB/RIF -1",
      },
    },
    validationIssues: [],
  },
  "draft-update": {
    draftId: "draft-update",
    kind: "UPDATE",
    baseProfileId: "site.mindray",
    baseRevision: 1,
    profile: {
      profileMeta: { id: "site.mindray", displayName: "Mindray BC-5380" },
    },
    validationIssues: [],
  },
};

const recognition = (draftId) => ({
  draftId,
  validationIssues: drafts[draftId].validationIssues,
  recognition: {
    mode: "RULES",
    affirmedNoControlResults: false,
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

// Where the page leads, and what it says it did.
const Destination = () => {
  const location = useLocation();
  return (
    <p data-testid="destination">
      {`${location.pathname}${location.search} ${location.state?.analyzerTypeNotice ?? ""}`}
    </p>
  );
};

const renderDraft = (draftId, returnTo = "/analyzers/types?view=inUse") =>
  render(
    <MemoryRouter
      initialEntries={[
        `/analyzers/types/drafts/${draftId}?returnTo=${encodeURIComponent(returnTo)}`,
      ]}
    >
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/types/drafts/:draftId">
          <AnalyzerTypeDraftPage />
        </Route>
        <Route path="/analyzers/types" exact>
          <Destination />
        </Route>
        <Route path="/analyzers" exact>
          <Destination />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyzerTypeDraftPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerTypeDraft.mockImplementation((draftId, callback) =>
      callback(drafts[draftId]),
    );
    getAnalyzerTypeControlRecognition.mockImplementation((draftId, callback) =>
      callback(recognition(draftId)),
    );
    updateAnalyzerTypeControlRecognition.mockImplementation(
      (draftId, update, callback) =>
        callback({
          ...recognition(draftId),
          recognition: {
            ...recognition(draftId).recognition,
            conditions: update.conditions.map((condition) => ({
              ...condition,
              sourceLabel: "Order field 12",
              description: `Order field 12 equals ${condition.value}`,
              editable: true,
            })),
          },
        }),
    );
    publishAnalyzerTypeDraft.mockImplementation((draftId, callback) =>
      callback({ profile: { catalog: { revision: 1 } } }),
    );
  });

  it("edits a new type's draft on a page with its settings and one bar", async () => {
    renderDraft("draft-create");

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "New type: Sysmex XN",
      }),
    ).toBeVisible();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Protocol" })).toBeVisible();
    const bar = screen.getByTestId("analyzer-type-draft-actions");
    expect(
      within(bar).getByRole("button", { name: "Publish type" }),
    ).toBeDisabled();
    expect(
      within(bar).getByText(
        "Complete the settings and control recognition before publishing.",
      ),
    ).toBeVisible();
  });

  it("publishes a copy and returns to the types list saying it was duplicated", async () => {
    renderDraft("draft-duplicate");

    expect(
      await screen.findByText(/Derived from shipped\.genexpert revision 2/),
    ).toBeVisible();
    const bar = screen.getByTestId("analyzer-type-draft-actions");
    const publish = within(bar).getByRole("button", { name: "Publish type" });
    await waitFor(() => expect(publish).toBeEnabled());
    expect(within(bar).getByText("Ready to publish")).toBeVisible();

    await userEvent.click(publish);

    expect(publishAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-duplicate",
      expect.any(Function),
    );
    expect(await screen.findByTestId("destination")).toHaveTextContent(
      "/analyzers/types duplicate",
    );
  });

  it("goes back to analyzer setup with the published type selected", async () => {
    publishAnalyzerTypeDraft.mockImplementation((_draftId, callback) =>
      callback({
        profile: {
          profileMeta: { id: "site.genexpert-copy" },
          catalog: { revision: 1 },
        },
      }),
    );
    renderDraft(
      "draft-duplicate",
      "/analyzers?setup=instrument&name=GX%20bench&labUnits=7",
    );

    const bar = await screen.findByTestId("analyzer-type-draft-actions");
    const publish = within(bar).getByRole("button", { name: "Publish type" });
    await waitFor(() => expect(publish).toBeEnabled());
    await userEvent.click(publish);

    expect(await screen.findByTestId("destination")).toHaveTextContent(
      "/analyzers?setup=instrument&name=GX+bench&labUnits=7&profile=site.genexpert-copy&revision=1",
    );
  });

  it("keeps Publish shut while control recognition has unsaved changes", async () => {
    renderDraft("draft-duplicate");

    expect(
      await screen.findByRole("heading", {
        name: "Control result recognition",
      }),
    ).toBeVisible();
    const publish = screen.getByRole("button", { name: "Publish type" });
    await waitFor(() => expect(publish).toBeEnabled());

    const value = screen.getByRole("textbox", { name: "Order field 12 value" });
    await userEvent.clear(value);
    await userEvent.type(value, "CONTROL");
    expect(publish).toBeDisabled();
    // The bar's one save follows what is unsaved.
    const bar = screen.getByTestId("analyzer-type-draft-actions");
    expect(within(bar).getByText("Unsaved control recognition")).toBeVisible();
    expect(
      within(bar).queryByRole("button", { name: "Save and validate settings" }),
    ).not.toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "Save control recognition" }),
    );
    await waitFor(() => expect(publish).toBeEnabled());
  });

  it("names a new revision and what stays pinned, then publishes it", async () => {
    renderDraft("draft-update");

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "New revision of Mindray BC-5380",
      }),
    ).toBeVisible();
    expect(screen.getByText(/Existing analyzers remain pinned/)).toBeVisible();

    const publish = screen.getByRole("button", { name: "Publish type" });
    await waitFor(() => expect(publish).toBeEnabled());
    await userEvent.click(publish);

    expect(publishAnalyzerTypeDraft).toHaveBeenCalledWith(
      "draft-update",
      expect.any(Function),
    );
    expect(await screen.findByTestId("destination")).toHaveTextContent(
      "/analyzers/types update",
    );
  });

  it("leaves the draft saved and returns where the action began", async () => {
    renderDraft("draft-create");

    await userEvent.click(
      await screen.findByRole("button", { name: "Finish later" }),
    );

    expect(screen.getByTestId("destination")).toHaveTextContent(
      "/analyzers/types?view=inUse",
    );
    expect(publishAnalyzerTypeDraft).not.toHaveBeenCalled();
  });

  it("says when the draft could not be loaded and tries again", async () => {
    getAnalyzerTypeDraft.mockImplementationOnce((_draftId, callback) =>
      callback({ error: "gone" }),
    );
    renderDraft("draft-update");

    expect(
      await screen.findByText("This draft could not be loaded"),
    ).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "New revision of Mindray BC-5380",
      }),
    ).toBeVisible();
  });
});
