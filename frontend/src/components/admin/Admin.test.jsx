import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import Admin from "./Admin";
import AdminDashboard from "./AdminDashboard";
import messages from "../../languages/en.json";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  getFromOpenElisServerV2: vi.fn(async () => ({})),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteToOpenElisServer: vi.fn(),
}));

const renderAdmin = (route = "/MasterListsPage") => {
  const basePath = route.startsWith("/admin") ? "/admin" : "/MasterListsPage";

  return render(
    <MemoryRouter initialEntries={[route]}>
      <IntlProvider locale="en" messages={messages}>
        <Route path={basePath} component={Admin} />
        <Route
          path="*"
          render={({ location }) => (
            <span data-testid="current-path">{location.pathname}</span>
          )}
        />
      </IntlProvider>
    </MemoryRouter>,
  );
};

describe("Admin", () => {
  test.each(["/MasterListsPage", "/admin"])(
    "renders a dashboard on the base admin route %s",
    (route) => {
      const { container } = renderAdmin(route);

      expect(
        screen.getByText(messages["admin.dashboard.title"]),
      ).toBeInTheDocument();
      expect(
        screen.getByText(messages["unifiedSystemUser.browser.title"]),
      ).toBeInTheDocument();
      expect(
        screen.getByText(messages["role.management.title"]),
      ).toBeInTheDocument();
      expect(
        screen.getByText(messages["organization.main.title"]),
      ).toBeInTheDocument();
      expect(
        screen.getByText(messages["master.lists.page.test.management"]),
      ).toBeInTheDocument();
      // 15, not 14: develop's tile list plus the Role Management tile this
      // branch adds. develop independently dropped role.management.title and
      // renamed sidenav.label.admin.program to admin.programs.title, so both
      // sides carried 14 and the merged list is develop's set plus ours.
      expect(screen.getAllByTestId("admin-dashboard-tile")).toHaveLength(15);
      expect(
        container.querySelectorAll(".admin-dashboard__tile-icon"),
      ).toHaveLength(15);
      expect(
        container.querySelectorAll(".admin-dashboard__tile-arrow"),
      ).toHaveLength(15);
      expect(document.querySelector(".cds--side-nav")).not.toBeInTheDocument();
    },
  );

  test("dashboard links navigate within the current admin route family", () => {
    render(
      <MemoryRouter initialEntries={["/MasterListsPage"]}>
        <IntlProvider locale="en" messages={messages}>
          <AdminDashboard basePath="/MasterListsPage" />
          <Route
            path="*"
            render={({ location }) => (
              <span data-testid="current-path">{location.pathname}</span>
            )}
          />
        </IntlProvider>
      </MemoryRouter>,
    );

    fireEvent.click(
      screen.getByText(messages["unifiedSystemUser.browser.title"]),
    );

    expect(screen.getByTestId("current-path")).toHaveTextContent(
      "/MasterListsPage/userManagement",
    );
  });

  test("the stuck analyzer events tile stays inside the admin route family", () => {
    // Opening it from the admin shell must not drop the reader back to the
    // main navigation, so it has an admin route of its own.
    render(
      <MemoryRouter initialEntries={["/MasterListsPage"]}>
        <IntlProvider locale="en" messages={messages}>
          <AdminDashboard basePath="/MasterListsPage" />
          <Route
            path="*"
            render={({ location }) => (
              <span data-testid="current-path">{location.pathname}</span>
            )}
          />
        </IntlProvider>
      </MemoryRouter>,
    );

    const tile = screen
      .getByText(messages["analyzer.importIssues.events.title"])
      .closest("a");
    expect(tile).toHaveAttribute(
      "href",
      "/MasterListsPage/stuckAnalyzerEvents",
    );

    fireEvent.click(tile);

    expect(screen.getByTestId("current-path")).toHaveTextContent(
      "/MasterListsPage/stuckAnalyzerEvents",
    );
  });
});
