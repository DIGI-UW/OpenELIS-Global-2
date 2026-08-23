import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { createMemoryHistory } from "history";
import { BrowserRouter, Router, useLocation } from "react-router-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  createAnalyzer,
  getAnalyzer,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  updateAnalyzer,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";
import AnalyzerSetup from "./AnalyzerSetup";

vi.mock("../../../services/analyzerService", () => ({
  createAnalyzer: vi.fn(),
  getAnalyzer: vi.fn(),
  getAnalyzerLabUnits: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
  updateAnalyzer: vi.fn(),
}));

const activeType = {
  profileId: "shipped.genexpert-astm",
  revision: 3,
  revisionFingerprint: `sha256:${"a".repeat(64)}`,
  displayName: "GeneXpert MTB/RIF",
  manufacturer: "Cepheid",
  model: "GeneXpert",
  source: "SHIPPED",
  status: "ACTIVE",
  protocol: "ASTM",
};

const SetupHarness = () => {
  const location = useLocation();
  const currentStep =
    new URLSearchParams(location.search).get("setup") || "instrument";

  return <AnalyzerSetup currentStep={currentStep} onClose={vi.fn()} />;
};

const renderSetup = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <SetupHarness />
      </IntlProvider>
    </BrowserRouter>,
  );

const renderSetupWithHistory = (initialEntry) => {
  const history = createMemoryHistory({ initialEntries: [initialEntry] });
  render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <SetupHarness />
      </IntlProvider>
    </Router>,
  );
  return history;
};

describe("AnalyzerSetup Instrument step", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState({}, "", "/analyzers?setup=instrument");
    getAnalyzerTypeCatalog.mockImplementation((callback) =>
      callback({
        schemaVersion: "1.0",
        catalogFingerprint: `sha256:${"b".repeat(64)}`,
        summary: {
          total: 2,
          inUse: 0,
          needsAttention: 0,
          deactivated: 1,
        },
        types: [
          activeType,
          {
            ...activeType,
            profileId: "site.inactive",
            displayName: "Inactive site type",
            source: "SITE",
            status: "INACTIVE",
          },
        ],
      }),
    );
    getAnalyzerLabUnits.mockImplementation((callback) =>
      callback([
        { id: "7", name: "Molecular Biology" },
        { id: "8", name: "Hematology" },
      ]),
    );
  });

  it("selects an active Analyzer Type through a searchable, URL-backed lab form", async () => {
    renderSetup();

    expect(
      await screen.findByRole("textbox", { name: "Analyzer name" }),
    ).toBeVisible();
    const typePicker = screen.getByRole("combobox", {
      name: "Analyzer type",
    });
    expect(screen.getByRole("combobox", { name: /^Lab units/ })).toBeVisible();

    await userEvent.click(typePicker);
    await userEvent.type(typePicker, "GeneXpert");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );

    const params = new URLSearchParams(window.location.search);
    expect(params.get("setup")).toBe("instrument");
    expect(params.get("profile")).toBe("shipped.genexpert-astm");
    expect(params.get("revision")).toBe("3");
    expect(screen.queryByText("Inactive site type")).not.toBeInTheDocument();

    const notListed = screen.getByRole("link", {
      name: "Instrument not listed?",
    });
    const notListedUrl = new URL(notListed.href);
    expect(notListedUrl.pathname).toBe("/analyzers/types");
    expect(notListedUrl.searchParams.get("action")).toBe("create");
    expect(notListedUrl.searchParams.get("returnTo")).toBe(
      "/analyzers?setup=instrument",
    );

    expect(screen.queryByLabelText("Status")).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("Communication Mode"),
    ).not.toBeInTheDocument();
    expect(screen.queryByLabelText("IP Address")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Port Number")).not.toBeInTheDocument();
  });

  it("requires a type, name, and lab unit before creating a candidate", async () => {
    renderSetup();

    await userEvent.click(
      await screen.findByRole("button", { name: "Continue to Verify" }),
    );

    expect(screen.getByText("Select an analyzer type")).toBeVisible();
    expect(screen.getByText("Enter an analyzer name")).toBeVisible();
    expect(screen.getByText("Select at least one lab unit")).toBeVisible();
    expect(createAnalyzer).not.toHaveBeenCalled();
  });

  it("persists the selected candidate and advances to URL-backed Verify", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers?search=gene&setup=instrument",
    );
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
    };
    createAnalyzer.mockImplementation((_payload, callback) =>
      callback(candidate),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    renderSetup();

    const typePicker = await screen.findByRole("combobox", {
      name: "Analyzer type",
    });
    await userEvent.click(typePicker);
    await userEvent.type(typePicker, "GeneXpert");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );
    await userEvent.type(
      screen.getByRole("textbox", { name: "Analyzer name" }),
      "GX bench 1",
    );
    const labUnitPicker = screen.getByRole("combobox", {
      name: /^Lab units/,
    });
    await userEvent.click(labUnitPicker);
    await userEvent.type(labUnitPicker, "Molecular");
    await userEvent.click(
      await screen.findByRole("option", { name: "Molecular Biology" }),
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    expect(createAnalyzer).toHaveBeenCalledWith(
      {
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        status: "SETUP",
        testUnitIds: ["7"],
      },
      expect.any(Function),
    );
    const params = new URLSearchParams(window.location.search);
    expect(params.get("search")).toBe("gene");
    expect(params.get("setup")).toBe("verify");
    expect(params.get("analyzerId")).toBe("42");
    expect(params.get("profile")).toBe(activeType.profileId);
    expect(params.get("revision")).toBe("3");
    expect(
      screen.getByRole("heading", { level: 3, name: "Verify" }).closest("li"),
    ).toHaveAttribute("aria-current", "step");
    expect(screen.getByText("GX bench 1")).toBeVisible();
    expect(screen.getByText("Molecular Biology")).toBeVisible();
  });

  it("reloads a bookmarked Verify step from the persisted analyzer", async () => {
    window.history.replaceState(
      {},
      "",
      `/analyzers?setup=verify&analyzerId=42&profile=${activeType.profileId}&revision=3`,
    );
    getAnalyzer.mockImplementation((_id, callback) =>
      callback({
        id: "42",
        name: "GX bench 1",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        testUnitIds: ["7"],
        status: "SETUP",
      }),
    );

    renderSetup();

    expect(await screen.findByText("GX bench 1")).toBeVisible();
    expect(screen.getByText("Molecular Biology")).toBeVisible();
    expect(getAnalyzer).toHaveBeenCalledWith(
      "42",
      expect.any(Function),
      expect.any(AbortSignal),
    );
    expect(createAnalyzer).not.toHaveBeenCalled();
    expect(
      screen.getByRole("heading", { level: 3, name: "Verify" }).closest("li"),
    ).toHaveAttribute("aria-current", "step");
  });

  it("returns to and updates the same candidate through browser history", async () => {
    const candidate = {
      id: "42",
      name: "GX bench 1",
      profileId: activeType.profileId,
      profileRevision: activeType.revision,
      testUnitIds: ["7"],
      status: "SETUP",
    };
    createAnalyzer.mockImplementation((_payload, callback) =>
      callback(candidate),
    );
    getAnalyzer.mockImplementation((_id, callback) => callback(candidate));
    updateAnalyzer.mockImplementation((_id, payload, callback) =>
      callback({ ...candidate, ...payload }),
    );
    const history = renderSetupWithHistory("/analyzers?setup=instrument");

    const typePicker = await screen.findByRole("combobox", {
      name: "Analyzer type",
    });
    await userEvent.click(typePicker);
    await userEvent.type(typePicker, "GeneXpert");
    await userEvent.click(
      await screen.findByRole("option", {
        name: "GeneXpert MTB/RIF · Cepheid · ASTM · revision 3",
      }),
    );
    await userEvent.type(
      screen.getByRole("textbox", { name: "Analyzer name" }),
      "GX bench 1",
    );
    const labUnitPicker = screen.getByRole("combobox", {
      name: /^Lab units/,
    });
    await userEvent.click(labUnitPicker);
    await userEvent.type(labUnitPicker, "Molecular");
    await userEvent.click(
      await screen.findByRole("option", { name: "Molecular Biology" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "verify",
    );
    history.goBack();
    await waitFor(() =>
      expect(new URLSearchParams(history.location.search).get("setup")).toBe(
        "instrument",
      ),
    );
    expect(new URLSearchParams(history.location.search).get("analyzerId")).toBe(
      "42",
    );

    createAnalyzer.mockClear();
    const nameInput = screen.getByRole("textbox", { name: "Analyzer name" });
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "GX bench A");
    await userEvent.click(
      screen.getByRole("button", { name: "Continue to Verify" }),
    );

    expect(createAnalyzer).not.toHaveBeenCalled();
    expect(updateAnalyzer).toHaveBeenCalledWith(
      "42",
      {
        name: "GX bench A",
        profileId: activeType.profileId,
        profileRevision: activeType.revision,
        status: "SETUP",
        testUnitIds: ["7"],
      },
      expect.any(Function),
    );
    expect(new URLSearchParams(history.location.search).get("setup")).toBe(
      "verify",
    );
    expect(new URLSearchParams(history.location.search).get("analyzerId")).toBe(
      "42",
    );
  });
});
