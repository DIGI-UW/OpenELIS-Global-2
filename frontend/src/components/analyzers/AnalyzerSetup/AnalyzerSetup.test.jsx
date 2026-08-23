import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";
import AnalyzerSetup from "./AnalyzerSetup";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzerLabUnits: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
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

const renderSetup = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <AnalyzerSetup currentStep="instrument" onClose={vi.fn()} />
      </IntlProvider>
    </BrowserRouter>,
  );

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
    expect(screen.getByRole("combobox", { name: "Lab units" })).toBeVisible();

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
});
