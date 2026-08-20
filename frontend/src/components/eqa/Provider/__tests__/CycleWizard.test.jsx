import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";
import CycleWizard from "../CycleWizard";
import {
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  patchToOpenElisServerJsonResponse: vi.fn(),
  toLocalIsoDate: (d) => (d ? "2026-10-15" : ""),
  // The real helper: enrollment dates arrive as epoch millis, not strings.
  formatDateOnly: (v) =>
    v
      ? new Date(v).toISOString().slice(0, 10).split("-").reverse().join("/")
      : "",
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const SCHEMES = [
  {
    id: 3,
    name: "National HIV VL PT",
    schemeType: "REGIONAL_PT",
    provider: "This lab",
    participantCount: 2,
    isActive: true,
  },
  { id: 9, name: "Internal round", schemeType: "IN_HOUSE", isActive: true },
];

const ENROLLMENTS = [
  {
    id: 11,
    organizationName: "Kampala Lab",
    status: "Active",
    // Jackson serialises java.sql.Date as epoch millis — a string helper here
    // crashed the whole wizard in UAT, not just this cell.
    enrollmentDate: 1767484800000,
  },
  { id: 12, organizationName: "Gulu Lab", status: "Withdrawn" },
];

const mockReads = (enrollments = ENROLLMENTS) =>
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/eqa/programs") cb(SCHEMES);
    else if (url === "/rest/eqa/testable-tests") cb(["70"]);
    else if (url === "/rest/test-list")
      cb([
        { id: "70", name: "HIV Viral Load" },
        { id: "71", name: "Not testable" },
      ]);
    else if (url === "/rest/eqa/programs/3/enrollments") cb(enrollments);
    else cb([]);
  });

const renderWizard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/cycles/new?schemeId=3"]}>
        <Route path="/qa/eqa/provider/cycles/new">
          <CycleWizard />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );

const goToStep = (label) => fireEvent.click(screen.getByText(label));

describe("CycleWizard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("in-house schemes are not offered: they have their own wizard", () => {
    mockReads();
    renderWizard();

    expect(screen.getByText("National HIV VL PT (2)")).toBeInTheDocument();
    expect(screen.queryByText(/Internal round/)).not.toBeInTheDocument();
  });

  test("only the tests that carry an analyte can hold a target", () => {
    mockReads();
    renderWizard();
    goToStep("Panel samples & source");

    expect(screen.getByText("HIV Viral Load")).toBeInTheDocument();
    expect(screen.queryByText("Not testable")).not.toBeInTheDocument();
  });

  test("participants are the scheme's active enrolments, not every enrolment", () => {
    mockReads();
    renderWizard();
    goToStep("Participants");

    expect(screen.getByText("Kampala Lab")).toBeInTheDocument();
    // A withdrawn laboratory must not be sent a panel.
    expect(screen.queryByText("Gulu Lab")).not.toBeInTheDocument();
  });

  test("a scheme whose participants have all left says so instead of listing none", () => {
    mockReads([ENROLLMENTS[1]]);
    renderWizard();
    goToStep("Participants");

    expect(screen.getByText("No enrolled participants")).toBeInTheDocument();
  });

  test("confirm refuses to write while an answer is missing, and names each one", () => {
    mockReads();
    renderWizard();
    goToStep("Confirm");

    expect(
      screen.getByText("This cycle cannot be created yet"),
    ).toBeInTheDocument();
    expect(screen.getByText(/Set the submission deadline/)).toBeInTheDocument();
    expect(
      screen.getByText(/Every sample needs a test and a target value/),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Create cycle & panel").closest("button"),
    ).toBeDisabled();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(patchToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  test("the aliquot hint counts one per sample per active participant", () => {
    mockReads();
    renderWizard();
    goToStep("Panel samples & source");

    // One active participant, one sample.
    expect(
      screen.getByText(/1 needed: one per sample per participant/),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByText("Add sample"));

    expect(
      screen.getByText(/2 needed: one per sample per participant/),
    ).toBeInTheDocument();
  });

  test("an enrolment date arriving as epoch millis renders as a date", () => {
    mockReads();
    renderWizard();
    goToStep("Participants");

    expect(screen.getByText("04/01/2026")).toBeInTheDocument();
  });
});
