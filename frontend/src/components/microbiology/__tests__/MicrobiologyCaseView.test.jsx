import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import MicrobiologyCaseView from "../MicrobiologyCaseView";
import messages from "../../../languages/en.json";

const baseline = {
  id: "case-1",
  accessionNumber: "M9-001",
  domain: "CLINICAL",
  status: "ACTIVE",
  stage: "RECEIVED",
  finalReleaseState: "NOT_RELEASED",
  priority: "ROUTINE",
  canWrite: true,
  canValidate: true,
  canAmend: true,
  labUnit: "Culture lab",
  labUnitId: "10",
  culturePurpose: "DIAGNOSTIC",
  purposeOptions: ["DIAGNOSTIC", "SCREENING"],
  programs: [],
  samples: [],
  relatedCases: [],
  requiredClinicalFields: ["patientOrigin"],
  orderDetail: {
    patientOrigin: "",
    culturePurpose: "DIAGNOSTIC",
    admissionDate: null,
    clinicalHistory: null,
    numberOfSets: null,
    antibioticExposure: false,
  },
};
const report = {
  initial: [],
  additional: [],
  cultures: [],
  isolates: [],
  notes: [],
  classification: "NOT_APPLICABLE",
  validatedResults: 0,
};
const setup = (search = "", changes = {}) => {
  let saved = { ...baseline, ...changes };
  const service = {
    getCase: vi.fn().mockImplementation(() => Promise.resolve(saved)),
    getTests: vi.fn().mockResolvedValue([]),
    getTimeline: vi.fn().mockResolvedValue([]),
    getIncoming: vi.fn().mockResolvedValue(changes.incoming || []),
    getPatientOrigins: vi.fn().mockResolvedValue({
      options: [{ code: "INPATIENT", label: "Inpatient" }],
    }),
    saveOrderDetail: vi.fn().mockImplementation((id, fields) => {
      saved = { ...saved, orderDetail: fields };
      return Promise.resolve({ orderDetail: fields });
    }),
    getReport: vi.fn().mockResolvedValue(report),
    getReportVersions: vi.fn().mockResolvedValue([]),
    getReadiness: vi.fn().mockImplementation(() =>
      Promise.resolve({
        blockers: saved.orderDetail.patientOrigin
          ? []
          : ["CLINICAL_FIELD_REQUIRED:patientOrigin"],
        finalReleaseReady: false,
      }),
    ),
    getCaseAmendments: vi.fn().mockResolvedValue([]),
    getCaseReportVersions: vi.fn().mockResolvedValue([]),
    openCaseAmendment: vi.fn().mockImplementation(() => {
      saved = {
        ...saved,
        canWrite: true,
        canValidate: true,
        finalReleaseState: "AMENDMENT_IN_PROGRESS",
        amendmentOpen: true,
      };
      return Promise.resolve({ id: "amendment-1" });
    }),
    getCultures: vi.fn().mockResolvedValue([]),
    getCultureOptions: vi.fn().mockResolvedValue({ sources: [], media: [] }),
    getCriticalCommunications: vi.fn().mockResolvedValue([]),
  };
  const history = createMemoryHistory({
    initialEntries: [`/Microbiology/cases/case-1${search}`],
  });
  render(
    <IntlProvider locale="en" messages={messages}>
      <Router history={history}>
        <MicrobiologyCaseView caseId="case-1" service={service} />
      </Router>
    </IntlProvider>,
  );
  return { service, history };
};

describe("canonical case workspace", () => {
  test.each([
    "",
    "?view=workbench",
    "?section=order-detail",
    "?grain=cultures&section=case-info",
  ])("exposes the same missing clinical fields at %s", async (search) => {
    setup(search);
    expect(await screen.findByLabelText("Patient origin")).toHaveValue("");
    expect(screen.getByLabelText("Clinical history")).toHaveValue("");
    expect(screen.getByLabelText("Culture purpose")).toHaveValue("DIAGNOSTIC");
    expect(
      screen.getByText(/Required before final release: Patient origin/),
    ).toBeInTheDocument();
    expect(screen.queryByText("Case info Complete")).not.toBeInTheDocument();
  });
  test("links a missing-field blocker to the editor and clears it after saving", async () => {
    const { service } = setup("?section=reports");
    const blocker = await screen.findByRole("link", { name: /Patient origin/ });
    fireEvent.click(blocker);
    expect(document.activeElement).toHaveAttribute("id", "case-information");
    fireEvent.change(screen.getByLabelText("Patient origin"), {
      target: { value: "INPATIENT" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save order detail" }));
    await waitFor(() =>
      expect(service.saveOrderDetail).toHaveBeenCalledWith(
        "case-1",
        expect.objectContaining({
          patientOrigin: "INPATIENT",
          admissionDate: null,
          culturePurpose: "DIAGNOSTIC",
        }),
      ),
    );
    fireEvent.change(screen.getByLabelText("Testing section"), {
      target: { value: "INITIAL" },
    });
    fireEvent.change(screen.getByLabelText("Testing section"), {
      target: { value: "REPORT" },
    });
    await waitFor(() =>
      expect(
        screen.queryByRole("link", { name: /Patient origin/ }),
      ).not.toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("Patient origin")).toHaveValue("INPATIENT"),
    );
  });
  test("announces waiting incoming results before the clinical fields", async () => {
    const { history } = setup("", {
      incoming: [
        { id: "pending", state: "WAITING" },
        { id: "placed", state: "PLACED" },
      ],
    });
    const pending = await screen.findByRole("status");
    expect(pending).toHaveTextContent("1");
    expect(
      pending.compareDocumentPosition(screen.getByLabelText("Patient origin")) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["microbiology.incoming.open"],
      }),
    );
    expect(history.location.search).toContain("scope=INCOMING");
  });
  test("outside-unit direct links expose context without mutation controls", async () => {
    setup("?view=workbench", {
      canWrite: false,
      canValidate: false,
      canAmend: false,
      readOnlyAccess: true,
    });
    expect(await screen.findByLabelText("Patient origin")).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Save order detail" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Transfer case" }),
    ).not.toBeInTheDocument();
  });
  test("opens a reasoned amendment from the ordinary final case", async () => {
    const { service } = setup("", {
      canWrite: false,
      canValidate: false,
      finalReleaseState: "FINAL_RELEASED",
      stage: "FINAL_RELEASED",
    });
    expect(await screen.findByLabelText("Patient origin")).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Testing section"), {
      target: { value: "AMENDMENT" },
    });
    fireEvent.change(
      screen.getByLabelText(messages["microbiology.amendment.reason"]),
      { target: { value: "Correct identification" } },
    );
    fireEvent.click(
      within(screen.getByTestId("microbiology-amendment-card")).getByRole(
        "button",
        { name: messages["microbiology.amendment.open"] },
      ),
    );
    await waitFor(() =>
      expect(service.openCaseAmendment).toHaveBeenCalledWith("case-1", {
        reason: "Correct identification",
      }),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("Patient origin")).not.toBeDisabled(),
    );
  });
  test("keeps the culture section and lab filter when entering from the bench", async () => {
    const { history } = setup("?grain=bench&labUnitId=10&q=M9&scope=CULTURE");
    expect(await screen.findByLabelText("Testing section")).toHaveValue(
      "CULTURE",
    );
    fireEvent.click(
      screen.getByRole("link", { name: "Microbiology worklist" }),
    );
    expect(history.location.search).toContain("labUnitId=10");
    expect(history.location.search).toContain("q=M9");
  });
});
