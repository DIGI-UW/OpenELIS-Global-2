import React from "react";
import { render, screen, act } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQAOrdersPage from "../EQAOrdersPage";

jest.mock("react-router-dom", () => ({
  useHistory: () => ({
    push: jest.fn(),
  }),
}));

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

jest.mock("../../layout/Layout", () => {
  const React = require("react");
  return {
    NotificationContext: React.createContext({
      addNotification: jest.fn(),
    }),
  };
});

jest.mock("../../common/PageBreadCrumb", () => {
  return function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  };
});

const { getFromOpenElisServer } = require("../../utils/Utils");

const renderPage = () => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <EQAOrdersPage />
    </IntlProvider>,
  );
};

describe("EQAOrdersPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/eqa/orders/summary")) {
        callback({
          pending: 5,
          inProgress: 3,
          overdue: 1,
          completedThisMonth: 10,
        });
      } else if (url.includes("/rest/eqa/orders")) {
        callback([
          {
            id: 1,
            labNumber: "EQA-001",
            programName: "Chemistry PT",
            providerName: "WHO",
            status: "PENDING",
            deadline: "2026-03-15T00:00:00.000+00:00",
            priority: "STANDARD",
            dateEntered: "2026-02-01T00:00:00.000+00:00",
          },
          {
            id: 2,
            labNumber: "EQA-002",
            programName: "Hematology PT",
            providerName: "CDC",
            status: "OVERDUE",
            deadline: "2026-01-15T00:00:00.000+00:00",
            priority: "URGENT",
            dateEntered: "2026-01-01T00:00:00.000+00:00",
          },
        ]);
      } else if (url.includes("/rest/eqa/my-programs")) {
        callback([
          { id: 1, programName: "Chemistry PT" },
          { id: 2, programName: "Hematology PT" },
        ]);
      }
    });
  });

  test("renders page title", () => {
    renderPage();
    expect(screen.getByText("EQA Orders")).toBeTruthy();
  });

  test("renders summary tiles with correct values", () => {
    renderPage();
    expect(screen.getByText("5")).toBeTruthy();
    expect(screen.getByText("3")).toBeTruthy();
    expect(screen.getByText("1")).toBeTruthy();
    expect(screen.getByText("10")).toBeTruthy();
  });

  test("renders summary tile labels", () => {
    renderPage();
    expect(screen.getAllByText("Pending").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("In Progress").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Overdue").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Completed This Month")).toBeTruthy();
  });

  test("renders DataTable with order data", () => {
    renderPage();
    expect(screen.getByText("EQA-001")).toBeTruthy();
    expect(screen.getByText("EQA-002")).toBeTruthy();
  });

  test("renders Enter New EQA Test button", () => {
    renderPage();
    expect(screen.getByText("Enter New EQA Test")).toBeTruthy();
  });

  test("renders status filter dropdown", () => {
    renderPage();
    expect(screen.getByText("All Statuses")).toBeTruthy();
  });

  test("renders program filter dropdown", () => {
    renderPage();
    expect(screen.getByText("All Programs")).toBeTruthy();
  });

  test("renders priority filter dropdown", () => {
    renderPage();
    expect(screen.getByText("All Priorities")).toBeTruthy();
  });

  test("renders table column headers", () => {
    renderPage();
    expect(screen.getByText("Lab Number")).toBeTruthy();
    expect(screen.getByText("Program")).toBeTruthy();
    expect(screen.getByText("Provider")).toBeTruthy();
  });

  test("renders breadcrumb navigation", () => {
    renderPage();
    expect(screen.getByTestId("breadcrumb")).toBeTruthy();
  });
});
