import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import MyProgramsPage from "../MyProgramsPage";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

vi.mock("../../layout/Layout", () => {
  // Replaced inline React require
  return {
    NotificationContext: React.createContext({
      addNotification: vi.fn(),
    }),
  };
});

vi.mock("../../common/CustomNotification", () => ({
  NotificationKinds: {
    success: "success",
    error: "error",
    info: "info",
    warning: "warning",
  },
}));

vi.mock("../../common/PageBreadCrumb", () => {
  return {
    default: function MockBreadCrumb() {
      return <div data-testid="breadcrumb">breadcrumb</div>;
    },
  };
});

// Replaced inline utils require

const renderPage = () => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MyProgramsPage />
    </IntlProvider>,
  );
};

describe("MyProgramsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/eqa/my-programs") {
        callback([
          {
            id: 1,
            programName: "Chemistry PT",
            provider: "WHO",
            description: "Chemistry proficiency testing",
            isActive: true,
            labUnits: [{ id: 10 }],
            tests: [{ id: 100 }, { id: 101 }],
            panels: [{ id: 200 }],
          },
          {
            id: 2,
            programName: "Hematology PT",
            provider: "CDC",
            description: "Hematology proficiency testing",
            isActive: false,
            labUnits: [],
            tests: [],
            panels: [],
          },
        ]);
      } else if (url === "/rest/eqa/providers") {
        callback(["WHO", "CDC", "PEPFAR"]);
      } else if (url === "/rest/test-sections") {
        callback([{ id: 10, value: "Chemistry" }]);
      } else if (url === "/rest/tests") {
        callback([
          { id: 100, value: "Glucose" },
          { id: 101, value: "Creatinine" },
        ]);
      } else if (url === "/rest/panels") {
        callback([{ id: 200, value: "Basic Metabolic Panel" }]);
      }
    });
  });

  test("renders page title", () => {
    renderPage();
    expect(screen.getByText("My EQA Programs")).toBeTruthy();
  });

  test("renders page subtitle", () => {
    renderPage();
    expect(
      screen.getByText("Programs this laboratory participates in"),
    ).toBeTruthy();
  });

  test("renders DataTable with enrollment data", () => {
    renderPage();
    expect(screen.getByText("Chemistry PT")).toBeTruthy();
    expect(screen.getByText("Hematology PT")).toBeTruthy();
    expect(screen.getByText("WHO")).toBeTruthy();
    expect(screen.getByText("CDC")).toBeTruthy();
  });

  test("renders Enroll in Program button", () => {
    renderPage();
    expect(screen.getByText("Enroll in Program")).toBeTruthy();
  });

  test("renders table column headers", () => {
    renderPage();
    expect(screen.getByText("Program Name")).toBeTruthy();
    expect(screen.getByText("Provider")).toBeTruthy();
    expect(screen.getByText("Lab Unit(s)")).toBeTruthy();
    expect(screen.getByText("Tests")).toBeTruthy();
    expect(screen.getByText("Panels")).toBeTruthy();
    expect(screen.getByText("Status")).toBeTruthy();
    expect(screen.getByText("Actions")).toBeTruthy();
  });

  test("renders Active and Inactive status tags", () => {
    renderPage();
    expect(screen.getByText("Active")).toBeTruthy();
    expect(screen.getByText("Inactive")).toBeTruthy();
  });

  test("renders count tags for enrolled program", () => {
    renderPage();
    // Chemistry PT has: 1 lab unit, 2 tests, 1 panel
    // Multiple elements may render "1" (lab units, panels, etc.)
    const ones = screen.getAllByText("1");
    expect(ones.length).toBeGreaterThanOrEqual(2); // lab units + panels
    expect(screen.getByText("2")).toBeTruthy(); // tests
  });

  test("renders breadcrumb navigation", () => {
    renderPage();
    expect(screen.getByTestId("breadcrumb")).toBeTruthy();
  });

  test("shows inline enrollment form when Enroll in Program is clicked", () => {
    renderPage();
    const enrollButton = screen.getByText("Enroll in Program");
    fireEvent.click(enrollButton);
    expect(screen.getByText("New EQA Program Enrollment")).toBeTruthy();
  });
});
