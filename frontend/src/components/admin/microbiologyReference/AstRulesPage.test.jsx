vi.mock("./api", () => ({
  getReportingRules: vi.fn(),
  saveReportingRule: vi.fn(),
  getReportingRuleHistory: vi.fn(),
  getReportingRuleSettings: vi.fn(),
  saveReportingRuleSettings: vi.fn(),
  getReferenceOptions: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getReferenceOptions,
  getReportingRuleHistory,
  getReportingRules,
  getReportingRuleSettings,
  saveReportingRule,
  saveReportingRuleSettings,
} from "./api";
import AstRulesPage from "./AstRulesPage";
import { isKnownSection } from "./sectionConfig";

const rule = {
  id: "r1",
  antibioticId: "cro",
  antibioticName: "Ceftriaxone",
  organismId: "ab",
  organismName: "Acinetobacter baumannii",
  organismGroup: null,
  sampleTypeIds: [],
  sampleTypeNames: [],
  clinicalContext: null,
  reportBehavior: "DONT_REPORT",
  cascadeCondition: null,
  addAutomatically: false,
  active: true,
};
const options = {
  antibiotics: [
    { id: "cro", label: "Ceftriaxone" },
    { id: "amk", label: "Amikacin" },
  ],
  organisms: [{ id: "ab", label: "Acinetobacter baumannii" }],
  "organism-groups": [{ id: "Enterobacterales", label: "Enterobacterales" }],
  "specimen-types": [{ id: "7", label: "Blood" }],
};
const show = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AstRulesPage />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getReportingRules.mockResolvedValue([rule]);
  getReportingRuleSettings.mockResolvedValue({
    writtenFor: "CLSI",
    activeAuthority: "CLSI",
  });
  getReferenceOptions.mockImplementation((resource) =>
    Promise.resolve(options[resource] || []),
  );
});

it("is a section of microbiology reference data", () => {
  expect(isKnownSection("ast-rules")).toBe(true);
});

it("lists rules grouped by organism with their report default", async () => {
  show();
  const table = await screen.findByRole("table", { name: "Reporting rules" });
  expect(
    within(table).getByText("Acinetobacter baumannii"),
  ).toBeInTheDocument();
  expect(within(table).getByText("Ceftriaxone")).toBeInTheDocument();
  expect(within(table).getByText("Don't report")).toBeInTheDocument();
  expect(within(table).getByText("Any sample type")).toBeInTheDocument();
});

it("adds a rule and reloads the table", async () => {
  saveReportingRule.mockResolvedValue({});
  show();
  fireEvent.click(await screen.findByRole("button", { name: "Add rule" }));
  const dialog = await screen.findByRole("dialog");
  fireEvent.change(await within(dialog).findByLabelText("Antibiotic"), {
    target: { value: "amk" },
  });
  fireEvent.change(within(dialog).getByLabelText("Organism group"), {
    target: { value: "Enterobacterales" },
  });
  fireEvent.change(within(dialog).getByLabelText("Report default"), {
    target: { value: "SUPPRESS_UNLESS_RESISTANT" },
  });
  fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));
  await waitFor(() =>
    expect(saveReportingRule).toHaveBeenCalledWith(
      expect.objectContaining({
        antibioticId: "amk",
        organismGroup: "Enterobacterales",
        reportBehavior: "SUPPRESS_UNLESS_RESISTANT",
      }),
    ),
  );
  await waitFor(() => expect(getReportingRules).toHaveBeenCalledTimes(2));
});

it("shows the cascade fields only for a Cascade rule", async () => {
  show();
  fireEvent.click(await screen.findByRole("button", { name: "Add rule" }));
  const dialog = await screen.findByRole("dialog");
  expect(
    within(dialog).queryByLabelText("Cascade condition"),
  ).not.toBeInTheDocument();
  fireEvent.change(await within(dialog).findByLabelText("Report default"), {
    target: { value: "CASCADE" },
  });
  expect(
    within(dialog).getByLabelText("Cascade condition"),
  ).toBeInTheDocument();
});

it("shows who changed a rule", async () => {
  getReportingRuleHistory.mockResolvedValue([
    {
      revision: 2,
      changedAt: "2026-10-26T10:00:00Z",
      changedBy: "5",
      changedByName: "Kila Lavu",
      before: '{"reportBehavior":"ALWAYS"}',
      after: '{"reportBehavior":"DONT_REPORT"}',
    },
  ]);
  show();
  fireEvent.click(
    await screen.findByRole("button", { name: "History Ceftriaxone" }),
  );
  expect(await screen.findByText("Kila Lavu")).toBeInTheDocument();
  expect(getReportingRuleHistory).toHaveBeenCalledWith("r1");
});

it("warns when the rules were written for another standard", async () => {
  getReportingRuleSettings.mockResolvedValue({
    writtenFor: "CLSI",
    activeAuthority: "EUCAST",
  });
  show();
  expect(
    await screen.findByText(
      "These rules were written for CLSI, but the active breakpoint standard is EUCAST.",
    ),
  ).toBeInTheDocument();
});

it("saves which standard the rules were written for", async () => {
  saveReportingRuleSettings.mockResolvedValue({
    writtenFor: "EUCAST",
    activeAuthority: "CLSI",
  });
  show();
  fireEvent.change(await screen.findByLabelText("Written for"), {
    target: { value: "EUCAST" },
  });
  await waitFor(() =>
    expect(saveReportingRuleSettings).toHaveBeenCalledWith({
      writtenFor: "EUCAST",
    }),
  );
});
