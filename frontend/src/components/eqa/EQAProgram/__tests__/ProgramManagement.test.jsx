import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import ProgramManagement from "../ProgramManagement";
import ProgramForm from "../ProgramForm";

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
    isActive: true,
  },
  {
    id: 2,
    name: "Hematology PT",
    description: "Hematology proficiency testing",
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
    expect(screen.getByText("EQA Program Management")).toBeTruthy();
  });

  test("renders create button", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Create Program")).toBeTruthy();
  });

  test("renders program list from API", () => {
    renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Chemistry PT")).toBeTruthy();
    expect(screen.getByText("Hematology PT")).toBeTruthy();
  });

  test("renders active/inactive status tags", () => {
    const { container } = renderWithIntl(<ProgramManagement />);
    expect(screen.getByText("Active")).toBeTruthy();
    expect(screen.getByText("Inactive")).toBeTruthy();
    const greenTags = container.querySelectorAll(".cds--tag--green");
    expect(greenTags.length).toBeGreaterThanOrEqual(1);
  });

  test("renders edit buttons for each program", () => {
    renderWithIntl(<ProgramManagement />);
    const editButtons = screen.getAllByText("Edit Program");
    expect(editButtons.length).toBe(2);
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
    fireEvent.click(screen.getByText("Create Program"));
    expect(screen.getByText("Save Program")).toBeTruthy();
  });
});

describe("ProgramForm", () => {
  test("renders create mode", () => {
    renderWithIntl(<ProgramForm program={null} onClose={jest.fn()} />);
    expect(screen.getAllByText("Create Program").length).toBeGreaterThanOrEqual(
      1,
    );
  });

  test("renders edit mode with program data", () => {
    const program = {
      id: 1,
      name: "Chemistry PT",
      description: "Test desc",
      isActive: true,
    };
    renderWithIntl(<ProgramForm program={program} onClose={jest.fn()} />);
    expect(screen.getAllByText("Edit Program").length).toBeGreaterThanOrEqual(
      1,
    );
  });

  test("shows validation error when name is empty", () => {
    renderWithIntl(<ProgramForm program={null} onClose={jest.fn()} />);
    fireEvent.click(screen.getByText("Save Program"));
    expect(screen.getByText("Program name is required")).toBeTruthy();
  });

  test("renders toggle only in edit mode", () => {
    const { container: createContainer } = renderWithIntl(
      <ProgramForm program={null} onClose={jest.fn()} />,
    );
    expect(createContainer.querySelector("#program-active")).toBeNull();

    const program = { id: 1, name: "Test", description: "", isActive: true };
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
