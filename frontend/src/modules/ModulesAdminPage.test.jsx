import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import messages from "../languages/en.json";
import ModulesAdminPage from "./ModulesAdminPage";
import { ModuleHostContext } from "./ModuleHostProvider";

const registry = {
  openelisVersion: "3.4.0-SNAPSHOT",
  coreApiVersion: "1.0.0",
  spiVersion: "1.0.0",
  frontendContractVersion: "1.0.0",
  modules: [
    {
      id: "specimen-followup",
      name: "Specimen Follow-up",
      version: "1.0.0",
      kind: "module",
      state: "STARTED",
    },
    {
      id: "old-module",
      name: "Old module",
      version: "0.1.0",
      kind: "provider",
      state: "INCOMPATIBLE",
      reason: "requires OpenELIS >=9.0.0 but this is 3.4.0-SNAPSHOT",
    },
  ],
};

function renderPage(value) {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/admin/modules"]}>
        <ModuleHostContext.Provider value={value}>
          <ModulesAdminPage />
        </ModuleHostContext.Provider>
      </MemoryRouter>
    </IntlProvider>,
  );
}

describe("ModulesAdminPage", () => {
  it("lists every registry entry with its state and reason", () => {
    renderPage({
      status: "ready",
      registry,
      modules: [],
      skipped: [],
      routes: [],
      slots: {},
      error: null,
    });

    expect(screen.getByTestId("modules-admin-title")).toHaveTextContent(
      "Modules",
    );
    expect(screen.getByTestId("modules-admin-versions")).toHaveTextContent(
      "OpenELIS 3.4.0-SNAPSHOT",
    );
    expect(
      screen.getByTestId("module-row-specimen-followup"),
    ).toHaveTextContent("STARTED");
    expect(screen.getByTestId("module-row-old-module")).toHaveTextContent(
      "INCOMPATIBLE",
    );
    expect(screen.getByTestId("module-row-old-module")).toHaveTextContent(
      "requires OpenELIS >=9.0.0",
    );
  });

  it("says so when nothing is installed", () => {
    renderPage({
      status: "ready",
      registry: { ...registry, modules: [] },
      modules: [],
      skipped: [],
      routes: [],
      slots: {},
      error: null,
    });

    expect(screen.getByTestId("modules-admin-empty")).toHaveTextContent(
      "No modules are installed.",
    );
  });
});
