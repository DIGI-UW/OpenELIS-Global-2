import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import CaseReportWorkspace from "../CaseReportWorkspace";
const t = (key) => messages[`microbiology.m6.${key}`];
const report = () => ({
  labUnitName: "Microbiology unit",
  classification: "NOT_APPLICABLE",
  initial: [
    {
      analysisId: "a1",
      name: "Gram stain",
      values: [{ component: "Morphology", value: "Gram negative rods" }],
    },
  ],
  cultures: [],
  isolates: [
    {
      id: "i1",
      label: "ISO-1",
      organism: "E. coli",
      susceptibilities: [
        {
          antibioticId: "a",
          agent: "Ampicillin",
          raw: "4",
          units: "ug/mL",
          interpretation: "SUSCEPTIBLE",
          standard: "CLSI",
          version: "2026",
        },
      ],
    },
  ],
  additional: [],
  notes: [
    {
      text: "Contact clinician",
      author: "Technician",
      at: "2026-10-01T10:00:00Z",
    },
  ],
  validatedResults: 2,
});
const show = (
  rights = { canWrite: true, canValidate: true },
  readiness = {},
) => {
  const service = {
    getReport: vi.fn().mockResolvedValue(report()),
    getReportVersions: vi.fn().mockResolvedValue([]),
    getReadiness: vi.fn().mockResolvedValue({
      blockers: ["CULTURE_OUTCOME_REQUIRED"],
      finalReleaseReady: false,
      ...readiness,
    }),
    getTests: vi.fn().mockResolvedValue([]),
    releaseReport: vi.fn(),
  };
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseReportWorkspace detail={{ id: "c1", ...rights }} service={service} />
    </IntlProvider>,
  );
  return service;
};
it("shows four ordered sections and the chosen raw drug reading with its standard", async () => {
  show();
  await screen.findByText("Microbiology unit");
  const headings = screen
    .getAllByRole("heading", { level: 4 })
    .map((h) => h.textContent);
  expect(
    headings.filter((h) =>
      [t("initial"), t("culture"), t("ast"), t("additional")].includes(h),
    ),
  ).toEqual([t("initial"), t("culture"), t("ast"), t("additional")]);
  const table = screen.getByRole("table", { name: "ISO-1: E. coli" });
  expect(within(table).getByText("4 ug/mL")).toBeInTheDocument();
  expect(within(table).getByText("CLSI 2026")).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: t("finalRelease") }),
  ).toBeDisabled();
  expect(
    screen.getByRole("button", { name: t("partialRelease") }),
  ).toBeEnabled();
});
it("allows partial release by a results technician without final-release rights", async () => {
  const service = show({ canWrite: true, canValidate: false });
  service.releaseReport.mockResolvedValue({});
  service.getReportVersions.mockResolvedValue([
    {
      id: "v1",
      number: 1,
      type: "PRELIMINARY",
      at: "2026-10-01T10:00:00Z",
      report: report(),
    },
  ]);
  fireEvent.click(
    await screen.findByRole("button", { name: t("partialRelease") }),
  );
  await waitFor(() =>
    expect(service.releaseReport).toHaveBeenCalledWith("c1", "preliminary", []),
  );
  expect(
    screen.queryByRole("button", { name: t("finalRelease") }),
  ).not.toBeInTheDocument();
});
it("offers no release or selection controls for direct-link readers", async () => {
  show({ canWrite: false, canValidate: false });
  await screen.findByText("Microbiology unit");
  expect(
    screen.queryByRole("button", { name: t("partialRelease") }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: t("finalRelease") }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
});

it("identifies the program and missing question when final release is blocked", async () => {
  show(undefined, {
    blockers: ["QUESTIONNAIRE_FIELD_REQUIRED:exposure"],
    blockerLabels: {
      "QUESTIONNAIRE_FIELD_REQUIRED:exposure":
        "Tuberculosis: Previous treatment",
    },
  });
  expect(
    await screen.findByText(/Tuberculosis: Previous treatment/),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: t("finalRelease") }),
  ).toBeDisabled();
});

it("requires confirmation of the exact waiting incoming results before final release", async () => {
  const service = show(
    { canWrite: true, canValidate: true },
    {
      finalReleaseReady: true,
      blockers: [],
      pendingIncomingIds: ["incoming-1"],
    },
  );
  await screen.findByText("Microbiology unit");
  const button = screen.getByRole("button", { name: t("finalRelease") });
  expect(button).toBeDisabled();
  fireEvent.click(
    screen.getByRole("checkbox", {
      name: messages["microbiology.incoming.confirmRelease"],
    }),
  );
  expect(button).toBeEnabled();
  fireEvent.click(button);
  await waitFor(() =>
    expect(service.releaseReport).toHaveBeenCalledWith("c1", "final", [
      "incoming-1",
    ]),
  );
});
