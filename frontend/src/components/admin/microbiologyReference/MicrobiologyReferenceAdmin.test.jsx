vi.mock("./api", () => ({
  getReferencePage: vi.fn(),
  getReferenceItem: vi.fn(),
  getReferenceOptions: vi.fn(),
  saveReference: vi.fn(),
  setReferenceActive: vi.fn(),
  getAstPanel: vi.fn(),
  publishAstPanel: vi.fn(),
  getBreakpointStandards: vi.fn(),
  getBreakpointStandard: vi.fn(),
  getBreakpointRules: vi.fn(),
  getBreakpointRule: vi.fn(),
  saveBreakpointRule: vi.fn(),
  activateBreakpointStandard: vi.fn(),
  archiveBreakpointStandard: vi.fn(),
  previewBreakpointImport: vi.fn(),
  applyBreakpointImport: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  applyBreakpointImport,
  getBreakpointRules,
  getBreakpointRule,
  getBreakpointStandard,
  getBreakpointStandards,
  getReferenceItem,
  getReferenceOptions,
  getReferencePage,
  getAstPanel,
  publishAstPanel,
  previewBreakpointImport,
  saveBreakpointRule,
  saveReference,
  setReferenceActive,
} from "./api";
import AstPanelPage from "./AstPanelPage";
import BreakpointPage from "./BreakpointPage";
import { REFERENCE_DEFINITIONS } from "./definitions";
import MicrobiologyReferenceAdmin from "./MicrobiologyReferenceAdmin";
import ReferenceDataPage from "./ReferenceDataPage";
import { DEFAULT_REFERENCE_QUERY } from "./queryState";

const renderPage = (component) =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );

const query = { ...DEFAULT_REFERENCE_QUERY };

beforeEach(() => {
  vi.clearAllMocks();
  getReferenceItem.mockResolvedValue({});
  getReferenceOptions.mockResolvedValue([]);
});

describe("microbiology reference administration", () => {
  it("offers only workflow identifiers supported by the backend", () => {
    const values = REFERENCE_DEFINITIONS["culture-setups"].fields
      .find((field) => field.key === "workflowType")
      .options.map((option) => option.value);

    expect(values).toEqual(["BACTERIOLOGY", "MYCOBACTERIOLOGY_TB", "MYCOLOGY"]);
  });

  it("loads the complete antibiotic option list for AST panels", async () => {
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });
    getReferenceOptions.mockResolvedValue([]);

    renderPage(<AstPanelPage query={query} setQuery={vi.fn()} />);

    await waitFor(() =>
      expect(getReferenceOptions).toHaveBeenCalledWith(
        "antibiotics",
        expect.any(AbortSignal),
      ),
    );
  });

  it("does not show a breakpoint detail breadcrumb for another section", async () => {
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });
    window.history.pushState(
      {},
      "",
      "/MasterListsPage/MicrobiologyReference/organisms/not-a-breakpoint",
    );

    renderPage(
      <Route path="/MasterListsPage/MicrobiologyReference/:section/:detailId?">
        <MicrobiologyReferenceAdmin />
      </Route>,
    );

    await screen.findByRole("tab", { name: "Organisms" });
    expect(
      screen.queryByText(messages["microbiology.admin.breakpoints.detail"]),
    ).not.toBeInTheDocument();
  });

  it("connects each Carbon tab to an existing tab panel", async () => {
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });
    window.history.pushState(
      {},
      "",
      "/MasterListsPage/MicrobiologyReference/organisms?status=ALL&sort=name&page=1&pageSize=20",
    );

    renderPage(
      <Route path="/MasterListsPage/MicrobiologyReference/:section/:detailId?">
        <MicrobiologyReferenceAdmin />
      </Route>,
    );

    const organismTab = await screen.findByRole("tab", { name: "Organisms" });
    const panelId = organismTab.getAttribute("aria-controls");
    expect(panelId).toBeTruthy();
    expect(document.getElementById(panelId)).toHaveAttribute(
      "role",
      "tabpanel",
    );
  });

  it("renders organism data and writes search state through the URL callback", async () => {
    const user = userEvent.setup();
    const setQuery = vi.fn();
    getReferencePage.mockResolvedValue({
      rows: [
        {
          id: "eco",
          displayName: "Escherichia coli",
          whonetCode: "eco",
          organismGroup: "Enterobacterales",
          initialSignificance: "USUALLY",
          active: true,
        },
      ],
      total: 1,
    });

    renderPage(
      <ReferenceDataPage
        definition={REFERENCE_DEFINITIONS.organisms}
        query={query}
        setQuery={setQuery}
      />,
    );

    expect(await screen.findByText("Escherichia coli")).toBeInTheDocument();
    fireEvent.change(
      screen.getByPlaceholderText(messages["microbiology.admin.search"]),
      { target: { value: "coli" } },
    );
    expect(setQuery).toHaveBeenLastCalledWith({ q: "coli" });
    await user.click(
      screen.getByRole("button", {
        name: messages["microbiology.admin.organisms.add"],
      }),
    );
    expect(setQuery).toHaveBeenLastCalledWith({ edit: "new" });
  });

  it("returns focus to the exact reference command when the editor closes", async () => {
    const user = userEvent.setup();
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });

    const Harness = () => {
      const [currentQuery, setCurrentQuery] = React.useState(query);
      const setQuery = (updates) =>
        setCurrentQuery((current) => ({ ...current, ...updates }));
      return (
        <ReferenceDataPage
          definition={REFERENCE_DEFINITIONS.organisms}
          query={currentQuery}
          setQuery={setQuery}
        />
      );
    };

    renderPage(<Harness />);
    const add = await screen.findByRole("button", {
      name: messages["microbiology.admin.organisms.add"],
    });
    add.focus();
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(document.activeElement?.closest('[role="dialog"]')).not.toBeNull(),
    );

    await user.keyboard("{Escape}");
    await waitFor(() => expect(add).toHaveFocus());

    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(document.activeElement?.closest('[role="dialog"]')).not.toBeNull(),
    );
    const activeDialog = document.activeElement.closest('[role="dialog"]');
    await user.click(
      within(activeDialog).getByRole("button", {
        name: messages["button.save"],
      }),
    );
    await waitFor(() => expect(saveReference).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(add).toHaveFocus());
  });

  it("renders Patient Origins as a read-only Carbon reference list", async () => {
    const setQuery = vi.fn();
    getReferencePage.mockResolvedValue({
      rows: [
        {
          id: "origin-1",
          code: "INPATIENT",
          displayName: "Inpatient",
          whonetCode: "INP",
          active: true,
        },
      ],
      total: 1,
    });

    renderPage(
      <ReferenceDataPage
        definition={REFERENCE_DEFINITIONS["patient-origins"]}
        query={query}
        setQuery={setQuery}
      />,
    );

    expect(await screen.findByText("Inpatient")).toBeInTheDocument();
    expect(screen.getByText("INPATIENT")).toBeInTheDocument();
    expect(screen.getByText("INP")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Add patient origin/i }),
    ).toBeNull();
    expect(screen.queryByRole("button", { name: "Options" })).toBeNull();
  });

  it("reconciles Carbon rows when a reference response becomes filtered", async () => {
    const setQuery = vi.fn();
    getReferencePage.mockImplementation((_resource, requestQuery) =>
      Promise.resolve({
        rows: requestQuery.includes("q=Long-term+Care")
          ? [
              {
                id: "origin-2",
                code: "LONG_TERM_CARE",
                displayName: "Long-term Care",
                whonetCode: "LTC",
                active: true,
              },
            ]
          : [
              {
                id: "origin-1",
                code: "INPATIENT",
                displayName: "Inpatient",
                whonetCode: "INP",
                active: true,
              },
              {
                id: "origin-2",
                code: "LONG_TERM_CARE",
                displayName: "Long-term Care",
                whonetCode: "LTC",
                active: true,
              },
            ],
        total: requestQuery.includes("q=Long-term+Care") ? 1 : 2,
      }),
    );
    const view = renderPage(
      <ReferenceDataPage
        definition={REFERENCE_DEFINITIONS["patient-origins"]}
        query={query}
        setQuery={setQuery}
      />,
    );
    expect(await screen.findByText("Inpatient")).toBeInTheDocument();

    view.rerender(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <ReferenceDataPage
            definition={REFERENCE_DEFINITIONS["patient-origins"]}
            query={{ ...query, q: "Long-term Care" }}
            setQuery={setQuery}
          />
        </IntlProvider>
      </BrowserRouter>,
    );

    expect(await screen.findByText("Long-term Care")).toBeInTheDocument();
    expect(screen.queryByText("Inpatient")).toBeNull();
  });

  it("opens the AST panel editor through canonical edit state", async () => {
    const user = userEvent.setup();
    const setQuery = vi.fn();
    getReferencePage.mockImplementation((resource) =>
      Promise.resolve({ rows: [], total: 0, resource }),
    );

    renderPage(<AstPanelPage query={query} setQuery={setQuery} />);

    await user.click(
      await screen.findByRole("button", {
        name: messages["microbiology.admin.astPanels.add"],
      }),
    );
    expect(setQuery).toHaveBeenCalledWith({ edit: "new" });
  });

  it("returns focus after the AST panel editor closes", async () => {
    const user = userEvent.setup();
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });

    const Harness = () => {
      const [currentQuery, setCurrentQuery] = React.useState(query);
      const setQuery = (updates) =>
        setCurrentQuery((current) => ({ ...current, ...updates }));
      return <AstPanelPage query={currentQuery} setQuery={setQuery} />;
    };

    renderPage(<Harness />);
    const add = await screen.findByRole("button", {
      name: messages["microbiology.admin.astPanels.add"],
    });
    add.focus();
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(document.activeElement?.closest('[role="dialog"]')).not.toBeNull(),
    );
    await user.keyboard("{Escape}");
    await waitFor(() => expect(add).toHaveFocus());
  });

  it("returns focus after the breakpoint import closes", async () => {
    const user = userEvent.setup();
    getBreakpointStandards.mockResolvedValue({ rows: [], total: 0 });

    const Harness = () => {
      const [currentQuery, setCurrentQuery] = React.useState(query);
      const setQuery = (updates) =>
        setCurrentQuery((current) => ({ ...current, ...updates }));
      return (
        <BreakpointPage
          basePath="/MasterListsPage"
          query={currentQuery}
          setQuery={setQuery}
        />
      );
    };

    renderPage(<Harness />);
    const importCsv = await screen.findByRole("button", {
      name: messages["microbiology.admin.breakpoints.import"],
    });
    importCsv.focus();
    await user.keyboard("{Enter}");
    await waitFor(() =>
      expect(document.activeElement?.closest('[role="dialog"]')).not.toBeNull(),
    );
    await user.keyboard("{Escape}");
    await waitFor(() => expect(importCsv).toHaveFocus());
  });

  it("saves structured culture timing through Carbon number inputs", async () => {
    const user = userEvent.setup();
    const setQuery = vi.fn();
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });
    getReferenceOptions.mockResolvedValue([
      { id: "method-1", label: "Routine blood culture" },
    ]);
    saveReference.mockResolvedValue({});

    renderPage(
      <ReferenceDataPage
        definition={REFERENCE_DEFINITIONS["culture-setups"]}
        query={{ ...query, edit: "new" }}
        setQuery={setQuery}
      />,
    );

    await user.selectOptions(
      await screen.findByLabelText(messages["microbiology.admin.field.method"]),
      "method-1",
    );
    await user.type(
      screen.getByLabelText(messages["microbiology.admin.field.name"]),
      "Routine blood culture",
    );
    await user.selectOptions(
      screen.getByLabelText(messages["microbiology.admin.field.workflow"]),
      "BACTERIOLOGY",
    );
    await user.type(
      screen.getByLabelText(
        messages["microbiology.admin.field.incubationHours"],
      ),
      "24",
    );
    await user.type(
      screen.getByLabelText(
        messages["microbiology.admin.field.subcultureAtHours"],
      ),
      "48",
    );
    const maxIncubationInput = screen.getByLabelText(
      messages["microbiology.admin.field.maxIncubationDays"],
    );
    await user.type(maxIncubationInput, "7");
    await user.clear(maxIncubationInput);
    await user.type(maxIncubationInput, "5");
    await user.click(
      screen.getByRole("button", { name: messages["button.save"] }),
    );

    expect(saveReference).toHaveBeenCalledWith(
      "culture-setups",
      expect.objectContaining({
        methodId: "method-1",
        incubationHours: 24,
        subcultureAtHours: 48,
        maxIncubationDays: 5,
      }),
    );
  });

  it("renders the refreshed AST panel rows after publishing a version", async () => {
    const user = userEvent.setup();
    let published = false;
    const antibiotic = {
      id: "cip",
      displayName: "Ciprofloxacin",
      whonetCode: "CIP",
      active: true,
    };
    const version = (id, versionNumber, current) => ({
      id,
      logicalKey: "panel-logical-key",
      name: "Gram negative panel",
      workflowType: "BACTERIOLOGY",
      versionNumber,
      current,
      active: true,
      antibiotics: [
        {
          id: `panel-row-${versionNumber}`,
          antibioticId: antibiotic.id,
          antibioticName: antibiotic.displayName,
          whonetCode: antibiotic.whonetCode,
          tier: 1,
          reportBehavior: "ALWAYS",
        },
      ],
    });
    const firstVersion = version("panel-v1", 1, true);
    const historicalVersion = version("panel-v1", 1, false);
    const secondVersion = version("panel-v2", 2, true);

    getReferencePage.mockImplementation((resource) => {
      if (resource === "antibiotics") {
        return Promise.resolve({ rows: [antibiotic], total: 1 });
      }
      return Promise.resolve({
        rows: published ? [secondVersion, historicalVersion] : [firstVersion],
        total: published ? 2 : 1,
      });
    });
    getAstPanel.mockResolvedValue(firstVersion);
    publishAstPanel.mockImplementation(() => {
      published = true;
      return Promise.resolve(secondVersion);
    });

    const Harness = () => {
      const [currentQuery, setCurrentQuery] = React.useState({
        ...query,
        edit: firstVersion.id,
      });
      const setQuery = (updates) =>
        setCurrentQuery((current) => ({ ...current, ...updates }));
      return <AstPanelPage query={currentQuery} setQuery={setQuery} />;
    };

    renderPage(<Harness />);
    await user.click(
      await screen.findByRole("button", {
        name: messages["microbiology.admin.astPanels.publishVersion"],
      }),
    );
    await user.click(
      await screen.findByRole("button", {
        name: messages["microbiology.admin.astPanels.publishVersion"],
      }),
    );

    expect(
      await screen.findByRole("row", {
        name: /Gram negative panel.*v2.*Current.*Active/,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("row", {
        name: /Gram negative panel.*v1.*Historical.*Active/,
      }),
    ).toBeInTheDocument();
  });

  it("requires impact confirmation before deactivating a referenced organism", async () => {
    const user = userEvent.setup();
    const setQuery = vi.fn();
    getReferencePage.mockResolvedValue({
      rows: [
        {
          id: "eco",
          displayName: "Escherichia coli",
          whonetCode: "eco",
          organismGroup: "Enterobacterales",
          initialSignificance: "USUALLY",
          active: true,
          referenceCount: 3,
        },
      ],
      total: 1,
    });
    setReferenceActive.mockResolvedValue({});
    getReferenceItem.mockResolvedValue({
      id: "eco",
      displayName: "Escherichia coli",
      active: true,
      referenceCount: 3,
    });

    renderPage(
      <ReferenceDataPage
        definition={REFERENCE_DEFINITIONS.organisms}
        query={{ ...query, edit: "deactivate:eco" }}
        setQuery={setQuery}
      />,
    );

    expect(
      await screen.findByText(/3 existing workflow records/),
    ).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", {
        name: /Deactivate$/,
      }),
    );
    expect(setReferenceActive).toHaveBeenCalledWith("organisms", "eco", false);
  });

  it("offers existing Methods when creating culture defaults", async () => {
    const setQuery = vi.fn();
    getReferencePage.mockResolvedValue({ rows: [], total: 0 });
    getReferenceOptions.mockResolvedValue([
      { id: "method-1", label: "Routine culture", code: "CULT" },
    ]);

    renderPage(
      <ReferenceDataPage
        definition={REFERENCE_DEFINITIONS["culture-setups"]}
        query={{ ...query, edit: "new" }}
        setQuery={setQuery}
      />,
    );

    expect(
      await screen.findByRole("option", {
        name: "Routine culture (CULT)",
      }),
    ).toBeInTheDocument();
  });

  it("renders breakpoint lifecycle detail and opens activation state", async () => {
    const user = userEvent.setup();
    const setQuery = vi.fn();
    getBreakpointStandard.mockResolvedValue({
      id: "std-1",
      authority: "CLSI",
      version: "SYNTH-2026",
      lifecycleStatus: "LOADED",
      ruleCount: 1,
    });
    getBreakpointRules.mockResolvedValue({
      rows: [
        {
          id: "rule-1",
          organismName: "Escherichia coli",
          antibioticName: "Ciprofloxacin",
          antibioticCode: "CIP",
          method: "MIC",
          susceptibleValue: 1,
          resistantValue: 4,
          seeded: true,
        },
      ],
      total: 1,
    });

    renderPage(
      <BreakpointPage
        standardId="std-1"
        basePath="/MasterListsPage"
        query={query}
        setQuery={setQuery}
      />,
    );

    expect(await screen.findByText("CLSI SYNTH-2026")).toBeInTheDocument();
    expect(screen.getByText("Escherichia coli")).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", {
        name: messages["microbiology.admin.breakpoints.activate"],
      }),
    );
    expect(setQuery).toHaveBeenCalledWith({ edit: "activate" });
  });

  it("clears an applied import preview when Cancel closes the modal", async () => {
    const user = userEvent.setup();
    const csv = "publisher,version\nCLSI,SYNTH-UAT";
    getBreakpointStandards.mockResolvedValue({ rows: [], total: 0 });
    previewBreakpointImport.mockResolvedValue({
      previewToken: "preview-1",
      validRows: 1,
      skippedRows: 0,
      unchangedRows: 0,
      errors: [],
    });
    applyBreakpointImport.mockResolvedValue({
      previewToken: "preview-1",
      validRows: 1,
      skippedRows: 0,
      unchangedRows: 0,
      importedRows: 1,
      errors: [],
    });

    const Harness = () => {
      const [currentQuery, setCurrentQuery] = React.useState(query);
      const setQuery = (updates) =>
        setCurrentQuery((current) => ({ ...current, ...updates }));
      return (
        <BreakpointPage
          basePath="/MasterListsPage"
          query={currentQuery}
          setQuery={setQuery}
        />
      );
    };

    const { container } = renderPage(<Harness />);
    await user.click(
      await screen.findByRole("button", {
        name: messages["microbiology.admin.breakpoints.import"],
      }),
    );
    const file = new File([csv], "synthetic.csv", { type: "text/csv" });
    if (!file.text) {
      Object.defineProperty(file, "text", {
        value: () => Promise.resolve(csv),
      });
    }
    await user.upload(container.querySelector('input[type="file"]'), file);
    expect(await screen.findByText("1 valid")).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", {
        name: messages["microbiology.admin.breakpoints.applyValid"],
      }),
    );
    expect(await screen.findByText("1 imported")).toBeInTheDocument();
    expect(applyBreakpointImport).toHaveBeenCalledWith("preview-1");

    await user.click(
      screen.getByRole("button", { name: messages["button.cancel"] }),
    );
    await user.click(
      screen.getByRole("button", {
        name: messages["microbiology.admin.breakpoints.import"],
      }),
    );

    expect(screen.queryByText("1 valid")).not.toBeInTheDocument();
    expect(container.querySelector('input[type="file"]')).not.toBeNull();
  });

  it("loads a directly linked breakpoint correction and saves it as local", async () => {
    const user = userEvent.setup();
    const setQuery = vi.fn();
    getBreakpointStandard.mockResolvedValue({
      id: "std-1",
      authority: "CLSI",
      version: "SYNTH-2026",
      lifecycleStatus: "LOADED",
    });
    getBreakpointRules.mockResolvedValue({ rows: [], total: 0 });
    getBreakpointRule.mockResolvedValue({
      id: "rule-1",
      standardId: "std-1",
      organismGroup: "Enterobacterales",
      antibioticId: "cip",
      method: "MIC",
      breakpointType: "MIC",
      susceptibleValue: 1,
      resistantValue: 4,
      active: true,
      locallyCustomized: true,
    });
    getReferenceOptions.mockImplementation((resource) => {
      if (resource === "organism-groups")
        return Promise.resolve([
          { id: "Enterobacterales", label: "Enterobacterales" },
        ]);
      if (resource === "antibiotics")
        return Promise.resolve([{ id: "cip", label: "Ciprofloxacin" }]);
      return Promise.resolve([]);
    });
    saveBreakpointRule.mockResolvedValue({ id: "rule-1" });

    renderPage(
      <BreakpointPage
        standardId="std-1"
        basePath="/MasterListsPage"
        query={{ ...query, edit: "rule:rule-1" }}
        setQuery={setQuery}
      />,
    );

    const notes = await screen.findByLabelText(
      messages["microbiology.admin.field.notes"],
    );
    await user.type(notes, "Local verification");
    await user.click(
      screen.getByRole("button", { name: messages["button.save"] }),
    );

    expect(saveBreakpointRule).toHaveBeenCalledWith(
      "std-1",
      expect.objectContaining({
        id: "rule-1",
        organismGroup: "Enterobacterales",
        antibioticId: "cip",
        notes: "Local verification",
      }),
    );
  });
});
