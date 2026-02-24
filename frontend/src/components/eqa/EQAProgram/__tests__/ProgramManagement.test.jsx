import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ProgramManagement from "../ProgramManagement";
import ProgramForm from "../ProgramForm";

jest.mock("../../../../components/common/PageBreadCrumb", () => {
  const mockReact = require("react");
  return function MockBreadCrumb() {
    return mockReact.createElement("div", { "data-testid": "breadcrumb" });
  };
});

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
  putToOpenElisServer: jest.fn(),
}));

const { getFromOpenElisServer } = require("../../../utils/Utils");

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

const mockPrograms = [
  {
    id: 1,
    name: "Chemistry PT",
    description: "Chemistry proficiency testing",
    providerName: "CAP",
    category: "Chemistry",
    frequency: "Quarterly",
    isActive: true,
  },
  {
    id: 2,
    name: "Hematology PT",
    description: "Hematology proficiency testing",
    providerName: "UKNEQAS",
    category: "Hematology",
    frequency: "Monthly",
    isActive: false,
  },
];

describe("ProgramManagement", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(mockPrograms);
    });
  });

  test("renders title", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Program Administration")).toBeTruthy();
  });

  test("renders add program button", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Add Program")).toBeTruthy();
  });

  test("renders program list from API", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getAllByText("Chemistry PT").length).toBeGreaterThanOrEqual(
      1,
    );
    expect(screen.getAllByText("Hematology PT").length).toBeGreaterThanOrEqual(
      1,
    );
  });

  test("renders provider and category columns", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("CAP")).toBeTruthy();
    expect(screen.getByText("UKNEQAS")).toBeTruthy();
  });

  test("renders active/inactive status tags", () => {
    const { container } = renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Active")).toBeTruthy();
    expect(screen.getByText("Inactive")).toBeTruthy();
    const greenTags = container.querySelectorAll(".cds--tag--green");
    expect(greenTags.length).toBeGreaterThanOrEqual(1);
  });

  test("renders summary tiles", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Active Programs")).toBeTruthy();
    expect(
      screen.getAllByText("Enrolled Participants").length,
    ).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Total Participants")).toBeTruthy();
  });

  test("renders tabs", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getAllByText("EQA Programs").length).toBeGreaterThanOrEqual(
      1,
    );
    expect(screen.getAllByText("Participants").length).toBeGreaterThanOrEqual(
      1,
    );
    expect(
      screen.getAllByText("System Settings").length,
    ).toBeGreaterThanOrEqual(1);
  });

  test("shows empty state when no programs", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([]);
    });

    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("No EQA programs found")).toBeTruthy();
  });

  test("opens create form when button clicked", () => {
    renderWithIntl(<ProgramManagement />);
    fireEvent.click(screen.getByText("Add Program"));
    expect(screen.getByText("Add New EQA Program")).toBeTruthy();
  });
});

describe("ProgramForm", () => {
  test("renders create mode with correct heading", () => {
    renderWithIntl(<ProgramForm program={null} onClose={jest.fn()} />);
    expect(screen.getByText("Add New EQA Program")).toBeTruthy();
  });

  test("renders edit mode with program data", () => {
    const program = {
      id: 1,
      name: "Chemistry PT",
      description: "Test desc",
      providerName: "CAP",
      category: "Chemistry",
      frequency: "Quarterly",
      isActive: true,
    };
    renderWithIntl(<ProgramForm program={program} onClose={jest.fn()} />);
    expect(screen.getByText("Edit EQA Program")).toBeTruthy();
  });

  test("shows validation error when name is empty", () => {
    renderWithIntl(<ProgramForm program={null} onClose={jest.fn()} />);
    fireEvent.click(screen.getByText("Add Program"));
    expect(screen.getByText("Program name is required")).toBeTruthy();
  });

  test("renders provider and category dropdowns", () => {
    renderWithIntl(<ProgramForm program={null} onClose={jest.fn()} />);
    expect(screen.getByText("Provider")).toBeTruthy();
    expect(screen.getByText("Category")).toBeTruthy();
    expect(screen.getByText("Frequency")).toBeTruthy();
  });

  test("renders toggle only in edit mode", () => {
    const { container: createContainer } = renderWithIntl(
      <ProgramForm program={null} onClose={jest.fn()} />,
    );
    expect(createContainer.querySelector("#program-active")).toBeNull();

    const program = {
      id: 1,
      name: "Test",
      description: "",
      isActive: true,
    };
    const { container: editContainer } = renderWithIntl(
      <ProgramForm program={program} onClose={jest.fn()} />,
    );
    expect(editContainer.querySelector("#program-active")).toBeTruthy();
  });

  test("calls onClose when cancel is clicked", () => {
    const onClose = jest.fn();
    renderWithIntl(<ProgramForm program={null} onClose={onClose} />);
    fireEvent.click(screen.getByText("Cancel"));
    expect(onClose).toHaveBeenCalled();
  });
});
