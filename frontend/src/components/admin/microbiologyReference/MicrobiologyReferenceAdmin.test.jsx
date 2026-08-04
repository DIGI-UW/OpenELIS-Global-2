vi.mock("./api", () => ({
  getReferencePage: vi.fn(),
  getReferenceItem: vi.fn(),
  getReferenceOptions: vi.fn(),
  saveReference: vi.fn(),
  setReferenceActive: vi.fn(),
  getAstPanel: vi.fn(),
  publishAstPanel: vi.fn(),
  getBreakpointStandards: vi.fn(),
  getBreakpointRules: vi.fn(),
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
  getBreakpointRules,
  getBreakpointStandards,
  getReferenceItem,
  getReferenceOptions,
  getReferencePage,
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
    getBreakpointStandards.mockResolvedValue({
      rows: [
        {
          id: "std-1",
          authority: "CLSI",
          version: "SYNTH-2026",
          lifecycleStatus: "LOADED",
          ruleCount: 1,
        },
      ],
      total: 1,
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
});
