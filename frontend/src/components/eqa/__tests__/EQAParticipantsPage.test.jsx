import React from "react";
import { render, screen, act, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQAParticipantsPage from "../EQAParticipantsPage";

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
  putToOpenElisServer: jest.fn(),
}));

jest.mock("../../common/PageBreadCrumb", () => {
  return function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  };
});

const { getFromOpenElisServer } = require("../../utils/Utils");

const mockPrograms = [
  { id: 1, name: "Chemistry PT", isActive: true },
  { id: 2, name: "Hematology PT", isActive: true },
  { id: 3, name: "Inactive Prog", isActive: false },
];

const mockEnrollments = [
  {
    id: 10,
    organizationName: "Hospital A",
    organizationCode: "HA-001",
    district: "Central",
    enrollmentDate: "2025-01-15",
    status: "Active",
    isLocal: true,
    organizationId: 100,
  },
  {
    id: 11,
    organizationName: "Clinic B",
    organizationCode: "CB-002",
    district: "Northern",
    enrollmentDate: "2025-02-01",
    status: "Suspended",
    isLocal: false,
    organizationId: 101,
  },
  {
    id: 12,
    organizationName: "Lab C",
    organizationCode: "LC-003",
    district: "Southern",
    enrollmentDate: "2025-03-10",
    status: "Withdrawn",
    isLocal: false,
    organizationId: 102,
  },
];

const renderPage = () => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <EQAParticipantsPage />
    </IntlProvider>,
  );
};

describe("EQAParticipantsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/eqa/programs") {
        callback(mockPrograms);
      } else if (
        url.includes("/rest/eqa/programs/") &&
        url.includes("/enrollments")
      ) {
        callback(mockEnrollments);
      } else if (url.includes("/rest/eqa/eligible-organizations")) {
        callback([]);
      }
    });
  });

  test("renders page title and subtitle", () => {
    renderPage();
    expect(screen.getByText("Participants")).toBeTruthy();
    expect(
      screen.getByText("Manage organization enrollment in EQA programs"),
    ).toBeTruthy();
  });

  test("renders breadcrumb", () => {
    renderPage();
    expect(screen.getByTestId("breadcrumb")).toBeTruthy();
  });

  test("shows prompt when no program is selected", () => {
    renderPage();
    expect(
      screen.getByText("Select a program to view enrollments"),
    ).toBeTruthy();
  });

  test("populates program selector with active programs only", () => {
    renderPage();
    const programSelector = screen.getByTestId("program-selector");
    expect(programSelector).toBeTruthy();
    const options = programSelector.querySelectorAll("option");
    // default + 2 active programs (Inactive Prog filtered out)
    expect(options.length).toBe(3);
  });

  test("shows enrollments table when program is selected", () => {
    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(screen.getByText("Hospital A")).toBeTruthy();
    expect(screen.getByText("Clinic B")).toBeTruthy();
    expect(screen.getByText("Lab C")).toBeTruthy();
  });

  test("renders (This Lab) tag for local organization", () => {
    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(screen.getByText("This Lab")).toBeTruthy();
  });

  test("renders correct column headers", () => {
    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(
      screen.getAllByText("Organization Name").length,
    ).toBeGreaterThanOrEqual(1);
    expect(
      screen.getAllByText("Organization Code").length,
    ).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("District")).toBeTruthy();
    expect(screen.getByText("Enrollment Date")).toBeTruthy();
  });

  test("shows Enroll Participant button when program is selected", () => {
    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(screen.getByTestId("enroll-button")).toBeTruthy();
  });

  test("shows empty message when no enrollments", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/eqa/programs") {
        callback(mockPrograms);
      } else if (url.includes("/enrollments")) {
        callback([]);
      }
    });

    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(screen.getByTestId("empty-message")).toBeTruthy();
  });

  test("renders status tags with correct types", () => {
    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(screen.getAllByText("Active").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Suspended").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Withdrawn").length).toBeGreaterThanOrEqual(1);
  });

  test("renders status filter dropdown", () => {
    renderPage();
    const select = screen.getByTestId("program-selector");
    fireEvent.change(select, { target: { value: "1" } });

    expect(screen.getByTestId("enrollment-status-filter")).toBeTruthy();
  });
});
