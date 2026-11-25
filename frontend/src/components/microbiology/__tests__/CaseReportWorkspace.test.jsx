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
      tested: 3,
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
  data = report(),
) => {
  const service = {
    getReport: vi.fn().mockResolvedValue(data),
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
    screen.getByRole("button", { name: "Release final report (1 missing)" }),
  ).toBeDisabled();
  expect(
    screen.getByRole("button", { name: t("partialRelease") }),
  ).toBeEnabled();
});
it("shows the indication a drug was read for, and nothing when none was stated", async () => {
  const data = report();
  const [ampicillin] = data.isolates[0].susceptibilities;
  data.isolates[0].susceptibilities = [
    { ...ampicillin, clinicalContext: "MENINGITIS" },
    { ...ampicillin, antibioticId: "b", clinicalContext: "ENDOCARDITIS" },
    { ...ampicillin, antibioticId: "c", clinicalContext: "NON_MENINGITIS" },
    { ...ampicillin, antibioticId: "d" },
  ];
  show(undefined, undefined, data);
  const rows = within(
    await screen.findByRole("table", { name: "ISO-1: E. coli" }),
  ).getAllByRole("row");
  expect(rows[1]).toHaveTextContent("CLSI 2026Meningitis");
  expect(rows[2]).toHaveTextContent("CLSI 2026Endocarditis");
  expect(rows[3]).toHaveTextContent(
    "CLSI 2026Neither meningitis nor endocarditis",
  );
  expect(rows[4]).toHaveTextContent(/CLSI 2026$/);
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
it("says the server did not answer when a release gets a gateway error", async () => {
  const service = show({ canWrite: true, canValidate: false });
  service.releaseReport.mockRejectedValue(
    Object.assign(new Error("request failed"), { response: { status: 502 } }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: t("partialRelease") }),
  );
  expect(
    await screen.findByText(
      "The server did not answer, and the change may not be saved. Check the connection, then reload the case before you try again.",
    ),
  ).toBeVisible();
  expect(screen.queryByText(t("saveError"))).not.toBeInTheDocument();
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
    screen.getByRole("button", { name: "Release final report (1 missing)" }),
  ).toBeDisabled();
});

it("asks to confirm the exact waiting incoming results in the final release dialog", async () => {
  const service = show(
    { canWrite: true, canValidate: true },
    {
      finalReleaseReady: true,
      blockers: [],
      pendingIncomingIds: ["incoming-1"],
    },
  );
  service.releaseReport.mockResolvedValue({});
  fireEvent.click(
    await screen.findByRole("button", { name: t("finalRelease") }),
  );
  const dialog = await screen.findByRole("dialog", {
    name: "Release final report?",
  });
  const confirm = await within(dialog).findByRole("button", {
    name: "Release final",
  });
  expect(confirm).toBeDisabled();
  fireEvent.click(
    within(dialog).getByRole("checkbox", {
      name: messages["microbiology.incoming.confirmRelease"],
    }),
  );
  expect(confirm).toBeEnabled();
  fireEvent.click(confirm);
  await waitFor(() =>
    expect(service.releaseReport).toHaveBeenCalledWith("c1", "final", [
      "incoming-1",
    ]),
  );
});

it("names a missing clinical field in a sentence", async () => {
  show(undefined, { blockers: ["CLINICAL_FIELD_REQUIRED:patientOrigin"] });
  expect(
    await screen.findByText("Patient origin is required before final release."),
  ).toBeInTheDocument();
});

it("allows a partial report from no-growth-ready without requiring final release", async () => {
  show(
    { canWrite: true, canValidate: true, stage: "NO_GROWTH_READY" },
    { finalReleaseReady: true, blockers: [] },
  );
  await screen.findByText("Microbiology unit");
  expect(
    screen.getByRole("button", { name: t("partialRelease") }),
  ).toBeEnabled();
  expect(screen.getByRole("button", { name: t("finalRelease") })).toBeEnabled();
});
it("lets keyboard users focus and scroll each isolate's AST table", async () => {
  show();
  expect(
    await screen.findByRole("table", { name: "ISO-1: E. coli" }),
  ).toHaveAttribute("tabindex", "0");
});
it("disables report release while the browser is offline", async () => {
  const onLine = vi
    .spyOn(window.navigator, "onLine", "get")
    .mockReturnValue(false);
  try {
    show({ canWrite: true, canValidate: true }, { finalReleaseReady: true });
    expect(
      await screen.findByRole("button", { name: t("partialRelease") }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: t("finalRelease") }),
    ).toBeDisabled();
  } finally {
    onLine.mockRestore();
  }
});
it("says how many of the isolate's tested antibiotics are reported", async () => {
  show();
  expect(
    await screen.findByText("1 of 3 antibiotics reported"),
  ).toBeInTheDocument();
});
it("prints a rule note under its drug without an author", async () => {
  const data = report();
  data.isolates[0].susceptibilities[0].readingId = "ra";
  data.notes = [
    {
      text: "Consider meropenem",
      fromRule: true,
      targetType: "DRUG",
      targetId: "ra",
    },
  ];
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseReportWorkspace
        detail={{ id: "c1", canWrite: true, canValidate: true }}
        service={{
          getReport: vi.fn().mockResolvedValue(data),
          getReportVersions: vi.fn().mockResolvedValue([]),
          getReadiness: vi
            .fn()
            .mockResolvedValue({ blockers: [], finalReleaseReady: false }),
          getTests: vi.fn().mockResolvedValue([]),
          releaseReport: vi.fn(),
        }}
      />
    </IntlProvider>,
  );
  expect(
    await screen.findByText("Consider meropenem · From rule"),
  ).toBeInTheDocument();
});

it("offers no partial release before any result is validated", async () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseReportWorkspace
        detail={{ id: "c1", canWrite: true, canValidate: true }}
        service={{
          getReport: vi
            .fn()
            .mockResolvedValue({ ...report(), validatedResults: 0 }),
          getReportVersions: vi.fn().mockResolvedValue([]),
          getReadiness: vi
            .fn()
            .mockResolvedValue({ blockers: [], finalReleaseReady: false }),
          getTests: vi.fn().mockResolvedValue([]),
          releaseReport: vi.fn(),
        }}
      />
    </IntlProvider>,
  );
  expect(
    await screen.findByRole("button", { name: t("partialRelease") }),
  ).toBeDisabled();
});
