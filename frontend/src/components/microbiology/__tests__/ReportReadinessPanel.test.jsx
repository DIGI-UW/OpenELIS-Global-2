import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import ReportReadinessPanel from "../ReportReadinessPanel";
import messages from "../../../languages/en.json";

const renderPanel = (service) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReportReadinessPanel caseId="case-1" service={service} />
    </IntlProvider>,
  );

describe("ReportReadinessPanel", () => {
  it("disables final release until readiness passes", async () => {
    const service = {
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
      getWhonetReadiness: vi.fn().mockResolvedValue({
        whonetReady: false,
        blockers: ["ORGANISM_MAPPING_REQUIRED"],
      }),
      releaseFinalReport: vi.fn(),
    };

    renderPanel(service);

    expect(
      await screen.findByText("Final release blocked"),
    ).toBeInTheDocument();
    expect(screen.getByText(/AST Review Required/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Release final report" }),
    ).toBeDisabled();
  });

  it("releases a final report when readiness passes", async () => {
    const service = {
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: true,
        blockers: [],
      }),
      getWhonetReadiness: vi.fn().mockResolvedValue({
        whonetReady: true,
        blockers: [],
      }),
      releaseFinalReport: vi.fn().mockResolvedValue({
        finalReleaseState: "FINAL_RELEASED",
      }),
    };

    renderPanel(service);

    const button = await screen.findByRole("button", {
      name: "Release final report",
    });
    fireEvent.click(button);

    await waitFor(() =>
      expect(service.releaseFinalReport).toHaveBeenCalledWith("case-1"),
    );
    expect(await screen.findByText("Final Released")).toBeInTheDocument();
  });
});
