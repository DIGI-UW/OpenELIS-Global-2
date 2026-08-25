import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import ReportReadinessPanel from "../ReportReadinessPanel";
import messages from "../../../languages/en.json";

const renderPanel = (service, props = {}) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <ReportReadinessPanel caseId="case-1" service={service} {...props} />
      </IntlProvider>
    </MemoryRouter>,
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
      getReportProjection: vi.fn().mockResolvedValue({
        reportableContent: true,
        mappingConfigured: true,
        content: "Escherichia coli: Ciprofloxacin S",
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
      getReportProjection: vi.fn().mockResolvedValue({
        reportableContent: true,
        mappingConfigured: true,
        content: "Escherichia coli: Ciprofloxacin S",
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

  it("releases a preliminary report and publishes its projected result targets", async () => {
    const onProjectionLoaded = vi.fn();
    const projection = {
      reportableContent: true,
      mappingConfigured: true,
      content: "Escherichia coli: Ciprofloxacin S",
    };
    const service = {
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: true,
        blockers: [],
      }),
      getWhonetReadiness: vi.fn().mockResolvedValue({
        whonetReady: true,
        blockers: [],
      }),
      getReportProjection: vi
        .fn()
        .mockResolvedValueOnce(projection)
        .mockResolvedValue({
          ...projection,
          projectedResultIds: ["result-1"],
        }),
      releasePreliminaryReport: vi.fn().mockResolvedValue({
        finalReleaseState: "PRELIMINARY_RELEASED",
      }),
    };

    renderPanel(service, { onProjectionLoaded });

    fireEvent.click(
      await screen.findByRole("button", {
        name: "Release preliminary report",
      }),
    );

    await waitFor(() =>
      expect(service.releasePreliminaryReport).toHaveBeenCalledWith("case-1"),
    );
    expect(await screen.findByText("Preliminary Released")).toBeInTheDocument();
    await waitFor(() =>
      expect(onProjectionLoaded).toHaveBeenCalledWith(["result-1"]),
    );
  });

  it("blocks final release when the patient-report mapping is absent", async () => {
    const service = {
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: true,
        blockers: [],
      }),
      getWhonetReadiness: vi.fn().mockResolvedValue({
        whonetReady: true,
        blockers: [],
      }),
      getReportProjection: vi.fn().mockResolvedValue({
        reportableContent: true,
        mappingConfigured: false,
        content: "Escherichia coli: Ciprofloxacin S",
      }),
      releaseFinalReport: vi.fn(),
    };

    renderPanel(service);

    expect(
      await screen.findByText(
        "Patient-report mapping required for final release",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Release final report" }),
    ).toBeDisabled();
  });

  it("exposes projected result targets and a link to visible patient results", async () => {
    const onProjectionLoaded = vi.fn();
    const service = {
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: true,
        blockers: [],
      }),
      getWhonetReadiness: vi.fn().mockResolvedValue({
        whonetReady: true,
        blockers: [],
      }),
      getReportProjection: vi.fn().mockResolvedValue({
        reportableContent: true,
        mappingConfigured: true,
        content: "Escherichia coli: Ciprofloxacin S",
        projectedResultIds: ["result-1"],
      }),
      releaseFinalReport: vi.fn(),
    };

    renderPanel(service, {
      patientId: "patient-1",
      onProjectionLoaded,
    });

    await waitFor(() =>
      expect(onProjectionLoaded).toHaveBeenCalledWith(["result-1"]),
    );
    expect(
      screen.getByRole("link", { name: "View patient results" }),
    ).toHaveAttribute("href", "/PatientResults/patient-1");
  });
});
