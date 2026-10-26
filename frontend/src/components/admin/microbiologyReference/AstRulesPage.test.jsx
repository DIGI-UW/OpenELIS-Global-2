vi.mock("./api", () => ({
  getReportingRules: vi.fn(),
  saveReportingRule: vi.fn(),
  getReportingRuleHistory: vi.fn(),
  getReportingRuleSettings: vi.fn(),
  saveReportingRuleSettings: vi.fn(),
  getReferenceOptions: vi.fn(),
  getInterpretationRules: vi.fn(),
  saveInterpretationRule: vi.fn(),
  previewReportingRules: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getInterpretationRules,
  getReferenceOptions,
  getReportingRuleHistory,
  getReportingRules,
  getReportingRuleSettings,
  saveReportingRule,
  saveInterpretationRule,
  saveReportingRuleSettings,
  previewReportingRules,
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
  getInterpretationRules.mockResolvedValue([
    {
      id: "i1",
      antibioticId: "amp",
      antibioticName: "Ampicillin",
      organismId: null,
      organismName: null,
      organismGroup: "Klebsiella",
      interpretation: "RESISTANT",
      source: "EUCAST expected phenotypes",
      active: true,
    },
  ]);
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

it("lists only the fields a rule was created with", async () => {
  getReportingRuleHistory.mockResolvedValue([
    {
      revision: 1,
      changedAt: "2026-10-26T10:00:00Z",
      changedBy: "5",
      changedByName: "Kila Lavu",
      before: null,
      after: '{"organismGroup":null,"reportBehavior":"DONT_REPORT"}',
    },
  ]);
  show();
  fireEvent.click(
    await screen.findByRole("button", { name: "History Ceftriaxone" }),
  );
  expect(await screen.findByText("Kila Lavu")).toBeInTheDocument();
  expect(screen.getByText(/reportBehavior/)).toBeInTheDocument();
  expect(screen.queryByText(/organismGroup/)).not.toBeInTheDocument();
});

it("names a local standard the way the picker does", async () => {
  getReportingRuleSettings.mockResolvedValue({
    writtenFor: "LOCAL",
    activeAuthority: "CLSI",
  });
  show();
  expect(
    await screen.findByText(
      "These rules were written for Local, but the active breakpoint standard is CLSI.",
    ),
  ).toBeInTheDocument();
});

it("shows a failed save inside the rule editor", async () => {
  saveReportingRule.mockRejectedValue(new Error("Bad Request"));
  show();
  fireEvent.click(await screen.findByRole("button", { name: "Add rule" }));
  const dialog = await screen.findByRole("dialog");
  fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));
  expect(
    await within(dialog).findByText(
      messages["microbiology.admin.astRules.error"],
    ),
  ).toBeInTheDocument();
});

it("saves a rule note with when it applies", async () => {
  saveReportingRule.mockResolvedValue({});
  show();
  fireEvent.click(await screen.findByRole("button", { name: "Add rule" }));
  const dialog = await screen.findByRole("dialog");
  fireEvent.change(await within(dialog).findByLabelText("Antibiotic"), {
    target: { value: "cro" },
  });
  fireEvent.change(within(dialog).getByLabelText("Note"), {
    target: { value: "Consider meropenem" },
  });
  fireEvent.change(within(dialog).getByLabelText("Note applies"), {
    target: { value: "WHEN_R" },
  });
  fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));
  await waitFor(() =>
    expect(saveReportingRule).toHaveBeenCalledWith(
      expect.objectContaining({
        antibioticId: "cro",
        note: "Consider meropenem",
        noteWhen: "WHEN_R",
      }),
    ),
  );
});

it("shows a rule's note and when it applies", async () => {
  getReportingRules.mockResolvedValue([
    { ...rule, note: "Consider meropenem", noteWhen: "WHEN_R" },
  ]);
  show();
  expect(
    await screen.findByText("When R: Consider meropenem"),
  ).toBeInTheDocument();
});

it("limits a rule note to 1,000 characters and counts them", async () => {
  show();
  fireEvent.click(await screen.findByRole("button", { name: "Add rule" }));
  const dialog = await screen.findByRole("dialog");
  expect(within(dialog).getByLabelText("Note")).toHaveAttribute(
    "maxlength",
    "1000",
  );
  expect(within(dialog).getByText("0/1000")).toBeInTheDocument();
});

it("lists interpretation rules on the same page", async () => {
  show();
  const table = await screen.findByRole("table", {
    name: "Interpretation rules",
  });
  expect(within(table).getByText("Klebsiella")).toBeInTheDocument();
  expect(within(table).getByText("Ampicillin")).toBeInTheDocument();
  expect(within(table).getByText("Always R")).toBeInTheDocument();
  expect(
    within(table).getByText("EUCAST expected phenotypes"),
  ).toBeInTheDocument();
});

it("adds an always resistant rule", async () => {
  saveInterpretationRule.mockResolvedValue({});
  show();
  fireEvent.click(
    await screen.findByRole("button", { name: "Add interpretation rule" }),
  );
  const dialog = await screen.findByRole("dialog");
  fireEvent.change(await within(dialog).findByLabelText("Antibiotic"), {
    target: { value: "amk" },
  });
  fireEvent.change(within(dialog).getByLabelText("Organism group"), {
    target: { value: "Enterobacterales" },
  });
  fireEvent.change(within(dialog).getByLabelText("Source"), {
    target: { value: "Local" },
  });
  fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));
  await waitFor(() =>
    expect(saveInterpretationRule).toHaveBeenCalledWith(
      expect.objectContaining({
        antibioticId: "amk",
        organismGroup: "Enterobacterales",
        interpretation: "RESISTANT",
        source: "Local",
      }),
    ),
  );
  await waitFor(() => expect(getInterpretationRules).toHaveBeenCalledTimes(2));
});

it("asks for an organism or a group and names the interpretation rule in its error", async () => {
  saveInterpretationRule.mockRejectedValue(new Error("Bad Request"));
  show();
  fireEvent.click(
    await screen.findByRole("button", { name: "Add interpretation rule" }),
  );
  const dialog = await screen.findByRole("dialog");
  expect(
    await within(dialog).findByRole("option", { name: "Choose an organism" }),
  ).toBeInTheDocument();
  expect(
    within(dialog).getByRole("option", { name: "Choose a group" }),
  ).toBeInTheDocument();
  fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));
  expect(
    await within(dialog).findByText(
      "The interpretation rule could not be saved.",
    ),
  ).toBeInTheDocument();
});

it("clears an interpretation rule error when its dialog closes", async () => {
  saveInterpretationRule.mockRejectedValue(new Error("Bad Request"));
  show();
  const add = await screen.findByRole("button", {
    name: "Add interpretation rule",
  });
  fireEvent.click(add);
  const dialog = await screen.findByRole("dialog");
  fireEvent.click(within(dialog).getByRole("button", { name: "Save" }));
  await within(dialog).findByText(
    "The interpretation rule could not be saved.",
  );
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  fireEvent.click(add);
  const reopened = await screen.findByRole("dialog");
  expect(
    within(reopened).queryByText("The interpretation rule could not be saved."),
  ).not.toBeInTheDocument();
});

it("previews what an isolate would report and why", async () => {
  previewReportingRules.mockResolvedValue({
    defaultPanelName: "Gram neg stamp",
    rows: [
      {
        antibioticId: "cro",
        antibioticName: "Ceftriaxone",
        measured: "SUSCEPTIBLE",
        interpretation: "RESISTANT",
        expertRule: "Expert rule: Klebsiella, Ceftriaxone always R",
        reportBehavior: "SUPPRESS_UNLESS_RESISTANT",
        reportRuleId: "r1",
        reported: true,
        note: "Consider meropenem",
      },
    ],
  });
  show();
  fireEvent.change(await screen.findByLabelText("Preview organism"), {
    target: { value: "ab" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Add antibiotic" }));
  fireEvent.change(screen.getByLabelText("Antibiotic 1"), {
    target: { value: "cro" },
  });
  fireEvent.change(screen.getByLabelText("Result 1"), {
    target: { value: "SUSCEPTIBLE" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Preview" }));
  await waitFor(() =>
    expect(previewReportingRules).toHaveBeenCalledWith(
      expect.objectContaining({
        organismId: "ab",
        readings: [{ antibioticId: "cro", interpretation: "SUSCEPTIBLE" }],
      }),
    ),
  );
  const result = await screen.findByRole("table", { name: "Preview result" });
  expect(
    within(result).getByText(
      "Acinetobacter baumannii is always resistant to Ceftriaxone",
    ),
  ).toBeInTheDocument();
  expect(
    within(result).queryByText("Expert rule: Klebsiella, Ceftriaxone always R"),
  ).not.toBeInTheDocument();
  expect(within(result).getByText("Consider meropenem")).toBeInTheDocument();
  expect(screen.getByText("Default panel: Gram neg stamp")).toBeInTheDocument();
});

it("previews only once an organism is picked and shows results as S, I or R", async () => {
  previewReportingRules.mockResolvedValue({
    defaultPanelName: null,
    rows: [
      {
        antibioticId: "cro",
        antibioticName: "Ceftriaxone",
        measured: "SUSCEPTIBLE",
        interpretation: "SUSCEPTIBLE",
        reportBehavior: "ALWAYS",
        reported: true,
      },
      {
        antibioticId: "cro",
        antibioticName: "Ceftriaxone",
        measured: "RESISTANT",
        interpretation: "RESISTANT",
        reportBehavior: "ALWAYS",
        reported: true,
      },
    ],
  });
  show();
  const button = await screen.findByRole("button", { name: "Preview" });
  expect(button).toBeDisabled();
  fireEvent.change(screen.getByLabelText("Preview organism"), {
    target: { value: "ab" },
  });
  expect(button).toBeEnabled();
  fireEvent.click(button);
  const result = await screen.findByRole("table", { name: "Preview result" });
  expect(within(result).getAllByText("S")).toHaveLength(2);
  expect(within(result).getAllByText("R")).toHaveLength(2);
});

it("says when a preview could not be run", async () => {
  previewReportingRules.mockRejectedValue(new Error("Bad Request"));
  show();
  fireEvent.change(await screen.findByLabelText("Preview organism"), {
    target: { value: "ab" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Preview" }));
  expect(
    await screen.findByText("The preview could not be run."),
  ).toBeInTheDocument();
});

it("drops an old preview when a new one fails or an input changes", async () => {
  previewReportingRules
    .mockResolvedValueOnce({
      defaultPanelName: null,
      rows: [
        {
          antibioticId: "cro",
          antibioticName: "Ceftriaxone",
          measured: "SUSCEPTIBLE",
          interpretation: "SUSCEPTIBLE",
          reportBehavior: "ALWAYS",
          reported: true,
        },
      ],
    })
    .mockRejectedValueOnce(new Error("Bad Request"))
    .mockResolvedValueOnce({ defaultPanelName: null, rows: [] });
  show();
  fireEvent.change(await screen.findByLabelText("Preview organism"), {
    target: { value: "ab" },
  });
  const button = screen.getByRole("button", { name: "Preview" });
  fireEvent.click(button);
  await screen.findByRole("table", { name: "Preview result" });
  fireEvent.click(button);
  expect(
    await screen.findByText("The preview could not be run."),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("table", { name: "Preview result" }),
  ).not.toBeInTheDocument();
  fireEvent.click(button);
  await screen.findByRole("table", { name: "Preview result" });
  fireEvent.click(screen.getByRole("button", { name: "Add antibiotic" }));
  expect(
    screen.queryByRole("table", { name: "Preview result" }),
  ).not.toBeInTheDocument();
});

it("downloads the rules in the configuration file format", async () => {
  show();
  expect(await screen.findByRole("link", { name: "Export" })).toHaveAttribute(
    "href",
    expect.stringContaining("/rest/microbiology/admin/reporting-rules/export"),
  );
});
