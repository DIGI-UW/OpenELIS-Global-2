import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import AstEntryPanel from "../AstEntryPanel";
import messages from "../../../languages/en.json";

const isolate = {
  id: "iso-1",
  isolateLabel: "ISO-1",
  significance: "CLINICALLY_SIGNIFICANT",
};

const inProgressRun = {
  id: "run-1",
  isolateId: "iso-1",
  panelId: "panel-1",
  status: "IN_PROGRESS",
  readings: [],
};

const runWithReading = {
  ...inProgressRun,
  readings: [
    {
      id: "reading-1",
      interpretation: "SUSCEPTIBLE",
      method: "MIC",
      rawValue: 4,
    },
  ],
};

const runWithOverride = {
  ...runWithReading,
  readings: [
    {
      ...runWithReading.readings[0],
      overrideInterpretation: "RESISTANT",
    },
  ],
};

const reviewedRun = {
  ...runWithOverride,
  status: "REVIEWED",
};

const renderPanel = (service) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AstEntryPanel
        caseId="case-1"
        workflowType="BACTERIOLOGY"
        isolates={[isolate]}
        service={service}
        saving={false}
      />
    </IntlProvider>,
  );

describe("AstEntryPanel", () => {
  it("records, overrides, and reviews a manual AST run", async () => {
    const service = {
      getAstPanels: vi.fn().mockResolvedValue([
        {
          id: "panel-1",
          label: "Gram negative AST panel",
        },
      ]),
      getAntibiotics: vi.fn().mockResolvedValue([
        {
          id: "abx-1",
          label: "Ciprofloxacin",
        },
      ]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([inProgressRun])
        .mockResolvedValueOnce([runWithReading])
        .mockResolvedValueOnce([runWithOverride])
        .mockResolvedValueOnce([reviewedRun]),
      getCaseReadiness: vi
        .fn()
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: true,
          blockers: [],
        }),
      startAstRun: vi.fn().mockResolvedValue(inProgressRun),
      recordAstReading: vi.fn().mockResolvedValue(runWithReading.readings[0]),
      overrideAstReading: vi
        .fn()
        .mockResolvedValue(runWithOverride.readings[0]),
      reviewAstRun: vi.fn().mockResolvedValue(reviewedRun),
    };

    renderPanel(service);

    expect(await screen.findByText("Manual AST")).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Start AST run" }),
      ).not.toBeDisabled(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Start AST run" }));

    expect(await screen.findByText("In Progress")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Record AST reading" }));

    expect(
      await screen.findByText(
        (_, element) =>
          element?.tagName.toLowerCase() === "strong" &&
          element.textContent === "SUSCEPTIBLE",
      ),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Override reason"), {
      target: { value: "mixed growth confirmed on repeat" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply override" }));

    expect(await screen.findByText(/RESISTANT/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Review AST run" }));

    expect(await screen.findByText("Reviewed")).toBeInTheDocument();
    expect(screen.getByText("Final release ready")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Record AST reading" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Apply override" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Review AST run" }),
    ).toBeDisabled();
    expect(service.recordAstReading).toHaveBeenCalledWith("run-1", {
      antibioticId: "abx-1",
      method: "MIC",
      rawValue: "4",
    });
  });
});
