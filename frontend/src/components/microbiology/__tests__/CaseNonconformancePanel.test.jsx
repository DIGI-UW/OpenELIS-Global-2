import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import CaseNonconformancePanel from "../CaseNonconformancePanel";

const categories = [
  {
    id: "10",
    name: "Pre-analytical",
    types: [
      { id: "11", name: "Specimen lost" },
      { id: "12", name: "Incorrect container" },
    ],
  },
];

const renderPanel = (props = {}) => {
  const service = {
    getNceCategories: vi.fn().mockResolvedValue(categories),
    getNceReportingUnits: vi
      .fn()
      .mockResolvedValue([{ id: "7", value: "Microbiology" }]),
    reportCaseNonconformance: vi.fn().mockResolvedValue({
      nceNumber: "NCE-2026-00001",
      affectedCaseIds: ["case-1"],
    }),
  };
  const onComplete = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseNonconformancePanel
        caseId="case-1"
        mode="report-nce"
        service={service}
        onComplete={onComplete}
        onCancel={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );
  return { service, onComplete };
};

describe("CaseNonconformancePanel", () => {
  it("reports a case NCE through labeled Carbon controls", async () => {
    const user = userEvent.setup();
    const { service, onComplete } = renderPanel();

    expect(await screen.findByLabelText("Category")).toHaveValue("10");
    await user.selectOptions(screen.getByLabelText("Type"), "12");
    await user.selectOptions(screen.getByLabelText("Reporting unit"), "7");
    await user.click(screen.getByRole("radio", { name: "Major" }));
    await user.type(
      screen.getByLabelText("Description"),
      "Container arrived cracked",
    );
    await user.click(screen.getByRole("radio", { name: "Flag only" }));
    await user.click(screen.getByRole("button", { name: "Report NCE" }));

    await waitFor(() =>
      expect(service.reportCaseNonconformance).toHaveBeenCalledWith("case-1", {
        categoryId: "10",
        typeId: "12",
        reportingUnitId: 7,
        severity: "MAJOR",
        title: "",
        description: "Container arrived cracked",
        immediateAction: "",
        disposition: "FLAG_ONLY",
        eventType: "NONCONFORMANCE",
      }),
    );
    expect(onComplete).toHaveBeenCalledWith({
      nceNumber: "NCE-2026-00001",
      affectedCaseIds: ["case-1"],
    });
  });

  it("presets specimen loss and rejects the shared specimen", async () => {
    const user = userEvent.setup();
    const { service } = renderPanel({ mode: "mark-lost" });

    expect(await screen.findByLabelText("Category")).toHaveValue("10");
    expect(screen.getByLabelText("Type")).toHaveValue("11");
    expect(
      screen.getByRole("radio", { name: "Reject affected tests" }),
    ).toBeChecked();
    await user.selectOptions(screen.getByLabelText("Reporting unit"), "7");
    await user.click(screen.getByRole("radio", { name: "Major" }));
    await user.type(
      screen.getByLabelText("Description"),
      "Specimen cannot be located",
    );
    await user.click(screen.getByRole("button", { name: "Mark lost" }));

    await waitFor(() =>
      expect(service.reportCaseNonconformance).toHaveBeenCalledWith(
        "case-1",
        expect.objectContaining({
          categoryId: "10",
          typeId: "11",
          disposition: "REJECT_TEST",
          eventType: "SPECIMEN_LOST",
        }),
      ),
    );
  });

  it("creates a scoped AST retest from an NCE disposition", async () => {
    const user = userEvent.setup();
    const { service } = renderPanel({
      workflowType: "BACTERIOLOGY",
      isolates: [{ id: "iso-1", isolateLabel: "ISO-1" }],
    });
    service.getAstRunsForIsolate = vi.fn().mockResolvedValue([
      {
        id: "run-1",
        status: "REVIEWED",
        technique: "VITEK_2",
        orderedAntibiotics: [{ antibioticId: "abx-2" }],
      },
    ]);
    service.getAntibiotics = vi
      .fn()
      .mockResolvedValue([{ id: "abx-2", label: "Meropenem" }]);

    expect(await screen.findByLabelText("Category")).toHaveValue("10");
    await user.selectOptions(screen.getByLabelText("Type"), "12");
    await user.selectOptions(screen.getByLabelText("Reporting unit"), "7");
    await user.click(screen.getByRole("radio", { name: "Major" }));
    await user.type(
      screen.getByLabelText("Description"),
      "Discordant carbapenem result",
    );
    await user.click(screen.getByRole("radio", { name: "Retest AST" }));
    await user.selectOptions(
      await screen.findByLabelText("AST run to retest"),
      "run-1",
    );
    await user.click(screen.getByRole("radio", { name: "Single antibiotic" }));
    await user.selectOptions(
      screen.getByLabelText("Antibiotic to repeat"),
      "abx-2",
    );
    await user.click(screen.getByRole("button", { name: "Report NCE" }));

    await waitFor(() =>
      expect(service.reportCaseNonconformance).toHaveBeenCalledWith("case-1", {
        categoryId: "10",
        typeId: "12",
        reportingUnitId: 7,
        severity: "MAJOR",
        title: "",
        description: "Discordant carbapenem result",
        immediateAction: "",
        disposition: "RETEST",
        eventType: "NONCONFORMANCE",
        sourceAstRunId: "run-1",
        astTechnique: "VITEK_2",
        orderedAntibioticIds: ["abx-2"],
      }),
    );
  });

  it("names a missing specimen-lost configuration instead of guessing", async () => {
    const service = {
      getNceCategories: vi
        .fn()
        .mockResolvedValue([{ id: "10", name: "Pre-analytical", types: [] }]),
      getNceReportingUnits: vi.fn().mockResolvedValue([]),
      reportCaseNonconformance: vi.fn(),
    };

    renderPanel({ mode: "mark-lost", service });

    expect(
      await screen.findByText(
        "The active NCE configuration does not include a Specimen lost type.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mark lost" })).toBeDisabled();
  });
});
