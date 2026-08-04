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
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
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
  previewBreakpointImport,
  saveBreakpointRule,
  setReferenceActive,
} from "./api";
import AstPanelPage from "./AstPanelPage";
import BreakpointPage from "./BreakpointPage";
import { REFERENCE_DEFINITIONS } from "./definitions";
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
      unchangedRows: 1,
      importedRows: 0,
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
    expect(await screen.findByText("1 unchanged")).toBeInTheDocument();
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
