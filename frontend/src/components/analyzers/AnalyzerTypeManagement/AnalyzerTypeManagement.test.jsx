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
import AnalyzerTypeManagement from "./AnalyzerTypeManagement";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
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
    window.history.replaceState({}, "", "/analyzers/types");
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      expect(endpoint).toBe("/rest/analyzer-types");
      callback(catalog);
    });
    postToOpenElisServerJsonResponse.mockImplementation(
      (endpoint, payload, callback) => {
        callback({ profile: { profileId: "site.created" } });
      },
    );
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

  it("creates a site profile only after explicit control-recognition affirmation", async () => {
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
    await userEvent.type(
      within(dialog).getByRole("textbox", { name: "Manufacturer" }),
      "Sysmex",
    );
    await userEvent.type(
      within(dialog).getByRole("textbox", { name: "Model" }),
      "XN",
    );

    const publish = within(dialog).getByRole("button", {
      name: "Create Profile",
    });
    expect(publish).toBeDisabled();
    await userEvent.click(
      within(dialog).getByRole("checkbox", {
        name: "I confirm this interface does not transmit control results",
      }),
    );
    expect(publish).toBeEnabled();
    await userEvent.click(publish);

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [endpoint, rawPayload] =
      postToOpenElisServerJsonResponse.mock.calls[0];
    expect(endpoint).toBe("/rest/analyzer-types");
    const payload = JSON.parse(rawPayload);
    expect(payload.profile).toMatchObject({
      schemaVersion: "1.0",
      profileId: "site.sysmex-xn-series",
      displayName: "Sysmex XN Series",
      protocol: "ASTM",
      capabilities: {
        inboundResults: true,
        outboundOrders: false,
        connectionTest: false,
      },
      identity: {
        manufacturer: "Sysmex",
        model: "XN",
      },
      tests: [],
      controlResultRecognition: {
        mode: "NONE",
        affirmedNoControlResults: true,
      },
    });
    expect(payload.profile).not.toHaveProperty("pluginClassName");
    expect(payload.profile).not.toHaveProperty("identifierPattern");
  });

  it("duplicates an active profile with a unique identity and visible lineage source", async () => {
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

    const [endpoint, rawPayload] =
      postToOpenElisServerJsonResponse.mock.calls[0];
    expect(endpoint).toBe("/rest/analyzer-types/shipped.genexpert/duplicate");
    expect(JSON.parse(rawPayload)).toEqual({
      sourceRevision: 2,
      targetProfileId: "shipped.genexpert-1",
      displayName: "Cepheid GeneXpert MTB/RIF -1",
    });
  });

  it("deactivates a site profile through a confirmation dialog and exposes no delete action", async () => {
    renderPage();
    await screen.findByText("Mindray BC-5380");

    await userEvent.click(
      screen.getByRole("button", {
        name: "Actions for Mindray BC-5380",
      }),
    );
    await userEvent.click(screen.getByRole("menuitem", { name: "Deactivate" }));

    const dialog = screen.getByRole("dialog", {
      name: "Deactivate Mindray BC-5380",
    });
    expect(within(dialog).queryByText(/delete/i)).not.toBeInTheDocument();
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Deactivate" }),
    );

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/analyzer-types/site.mindray/deactivate",
      "{}",
      expect.any(Function),
    );
    expect(screen.queryByRole("menuitem", { name: /delete/i })).toBeNull();
  });

  it("loads revision history through the profile history action", async () => {
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/analyzer-types") {
        callback(catalog);
        return;
      }
      expect(endpoint).toBe("/rest/analyzer-types/site.mindray/history");
      callback([
        {
          profile: {
            profileId: "site.mindray",
            revision: 1,
            displayName: "Mindray BC-5380",
            status: "ACTIVE",
          },
          publication: {
            action: "DUPLICATED",
            actor: "17",
            publishedAt: "2026-08-18T13:00:00Z",
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
  });
});
