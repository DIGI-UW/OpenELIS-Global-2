import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import UserAddModify from "./UserAddModify";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import messages from "../../../languages/en.json";

const { getFromOpenElisServerMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServerMock(...args),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

// A lab unit that offers more roles than the four the checkbox used to hardcode.
const LAB_UNIT_ROLES = [
  { elementID: "role-4", roleId: "4", roleName: "Reception" },
  { elementID: "role-5", roleId: "5", roleName: "Reports" },
  { elementID: "role-7", roleId: "7", roleName: "Results" },
  { elementID: "role-10", roleId: "10", roleName: "Validation" },
  { elementID: "role-12", roleId: "12", roleName: "Chemical Analyst" },
  { elementID: "role-15", roleId: "15", roleName: "Data Manager" },
];

const ROLE_NAMES = LAB_UNIT_ROLES.map((role) => role.roleName);

function ContextProviders({ children }) {
  return (
    <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        {children}
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>
  );
}

function userResponse({ labUnitRoles = LAB_UNIT_ROLES, selected = [] } = {}) {
  return {
    accountActive: "Y",
    accountDisabled: "N",
    accountLocked: "N",
    globalRoles: [],
    labUnitRoles: labUnitRoles,
    loginUserId: "1",
    selectedRoles: [],
    selectedTestSectionLabUnits: { 1: selected },
    systemUserId: "1",
    testSections: [{ id: "1", value: "Serology" }],
    userFirstName: "Ada",
    userLastName: "Lovelace",
    userLoginName: "ada",
  };
}

async function renderUserAddModify(response) {
  getFromOpenElisServerMock.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/UnifiedSystemUser")) {
      callback(response);
    } else {
      callback([]);
    }
  });

  render(
    <MemoryRouter initialEntries={["/MasterListsPage/userEdit?ID=1"]}>
      <ContextProviders>
        <IntlProvider locale="en" messages={messages}>
          <UserAddModify />
        </IntlProvider>
      </ContextProviders>
    </MemoryRouter>,
  );

  await waitFor(() => expect(allPermissions()).toBeInTheDocument());
}

function allPermissions() {
  return screen.getByLabelText("All Permissions");
}

function role(roleName) {
  return screen.getByLabelText(roleName);
}

describe("UserAddModify lab unit All Permissions checkbox", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
  });

  test("selects every role the lab unit offers", async () => {
    await renderUserAddModify(userResponse());

    fireEvent.click(allPermissions());

    ROLE_NAMES.forEach((roleName) => expect(role(roleName)).toBeChecked());
    expect(allPermissions()).toBeChecked();
  });

  test("keeps the roles already selected while adding the missing ones", async () => {
    await renderUserAddModify(userResponse({ selected: ["12"] }));

    fireEvent.click(allPermissions());

    ROLE_NAMES.forEach((roleName) => expect(role(roleName)).toBeChecked());
  });

  test("stays unticked while any offered role is unselected", async () => {
    await renderUserAddModify(
      userResponse({ selected: ["4", "5", "7", "10"] }),
    );

    expect(role("Reception")).toBeChecked();
    expect(role("Chemical Analyst")).not.toBeChecked();
    expect(allPermissions()).not.toBeChecked();
  });

  test("clears every offered role when it is unticked", async () => {
    await renderUserAddModify(
      userResponse({ selected: LAB_UNIT_ROLES.map((r) => r.roleId) }),
    );
    expect(allPermissions()).toBeChecked();

    fireEvent.click(allPermissions());

    ROLE_NAMES.forEach((roleName) => expect(role(roleName)).not.toBeChecked());
    expect(allPermissions()).not.toBeChecked();
  });

  test("stays unticked when the lab unit offers no roles", async () => {
    await renderUserAddModify(userResponse({ labUnitRoles: [] }));

    expect(allPermissions()).not.toBeChecked();
  });
});
