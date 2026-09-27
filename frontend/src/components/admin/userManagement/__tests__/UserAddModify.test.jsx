import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import {
  NotificationContext,
  ConfigurationContext,
} from "../../../layout/Layout";
import UserAddModify from "../UserAddModify";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const HEMATOLOGY = "36";
const BIOCHEMISTRY = "56";
const VALIDATION = "10";
const RECEPTION = "4";

// Mirrors what GET /rest/UnifiedSystemUser returns for an existing user whose
// only lab unit grant is Validation on Hematology.
const user = (loginName, selectedTestSectionLabUnits = {}) => ({
  systemUser: { id: "5", loginName, firstName: "Ada", lastName: "Lovelace" },
  systemUserId: "5",
  loginUserId: "5",
  userLoginName: loginName,
  userFirstName: "Ada",
  userLastName: "Lovelace",
  userPassword: "@@@@@@@@@",
  confirmPassword: "@@@@@@@@@",
  expirationDate: "01/01/2035",
  timeout: "480",
  accountActive: "Y",
  accountDisabled: "N",
  accountLocked: "N",
  allowCopyUserRoles: "N",
  selectedRoles: [],
  globalRoles: [],
  labUnitRoles: [
    { elementID: "4", roleId: RECEPTION, roleName: "Reception" },
    { elementID: "7", roleId: VALIDATION, roleName: "Validation" },
  ],
  testSections: [
    { id: HEMATOLOGY, value: "Hematology" },
    { id: BIOCHEMISTRY, value: "Biochemistry" },
  ],
  selectedTestSectionLabUnits,
});

const rowSelect = (key) => document.getElementById(`select-${key}`);

describe("UserAddModify", () => {
  let assign;
  let addNotification;

  const renderAt = (path) =>
    render(
      <MemoryRouter initialEntries={[path]}>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <ConfigurationContext.Provider
              value={{
                reloadConfiguration: vi.fn(),
                configurationProperties: {},
              }}
            >
              <NotificationContext.Provider
                value={{
                  notificationVisible: false,
                  setNotificationVisible: vi.fn(),
                  addNotification,
                }}
              >
                <Route path="/MasterListsPage/userManagement/:ID?">
                  <UserAddModify />
                </Route>
                <Route path="/MasterListsPage/userEdit">
                  <UserAddModify />
                </Route>
                <Route exact path="/MasterListsPage/userManagement">
                  <div>user list</div>
                </Route>
              </NotificationContext.Provider>
            </ConfigurationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const renderExisting = (loginName, grants) => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/UnifiedSystemUser"))
        return callback(user(loginName, grants));
      if (url.startsWith("/rest/users")) return callback([]);
      return callback(undefined);
    });
    return renderAt("/MasterListsPage/userEdit?ID=5-5&startingRecNo=1");
  };

  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServerJsonResponse.mockReset();
    addNotification = vi.fn();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/UnifiedSystemUser"))
        return callback(user("ada"));
      if (url.startsWith("/rest/users")) return callback([]);
      return callback(undefined);
    });
    assign = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload: vi.fn(), assign },
    });
  });

  it("goes back to the user list without leaving the app", async () => {
    renderAt("/MasterListsPage/userManagement/5");
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Exit" })).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByRole("button", { name: "Exit" }));

    expect(await screen.findByText("user list")).toBeInTheDocument();
    // Leaving used to download the whole app again to reach a route the router
    // already serves.
    expect(assign).not.toHaveBeenCalled();
  });

  describe("lab unit rows", () => {
    it("shows the stored lab unit, not the first option", async () => {
      renderExisting("qa_heme", { [HEMATOLOGY]: [VALIDATION] });

      await waitFor(() => expect(rowSelect(HEMATOLOGY)).not.toBeNull());

      // The row's <select> used to be uncontrolled and stranded on option 0
      // ("All Lab Units") because its options arrive after first mount.
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );
      expect(
        within(rowSelect(HEMATOLOGY)).getByRole("option", {
          name: "Hematology",
        }).selected,
      ).toBe(true);
    });

    it("never adds an All Lab Units row from Add New Permission", async () => {
      renderExisting("qa_heme", { [HEMATOLOGY]: [VALIDATION] });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      await userEvent.click(
        screen.getByRole("button", { name: "Add New Permission" }),
      );

      await waitFor(() => expect(rowSelect(BIOCHEMISTRY)).not.toBeNull());
      expect(rowSelect(BIOCHEMISTRY)).toHaveValue(BIOCHEMISTRY);
      expect(rowSelect("AllLabUnits")).toBeNull();
    });

    it("asks before All Lab Units replaces scoped grants, and cancel keeps them", async () => {
      renderExisting("qa_heme", {
        [HEMATOLOGY]: [VALIDATION],
        [BIOCHEMISTRY]: [],
      });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      await userEvent.selectOptions(rowSelect(BIOCHEMISTRY), "AllLabUnits");

      const dialog = await screen.findByRole("dialog", {
        name: /Replace lab unit permissions\?/,
      });
      expect(within(dialog).getByText("Hematology: Validation")).toBeVisible();
      expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY);
      expect(rowSelect(BIOCHEMISTRY)).toHaveValue(BIOCHEMISTRY);

      await userEvent.click(
        within(dialog).getByRole("button", { name: /Cancel/ }),
      );

      await waitFor(() =>
        expect(
          screen.queryByText("Hematology: Validation"),
        ).not.toBeInTheDocument(),
      );
      expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY);
      expect(rowSelect(BIOCHEMISTRY)).toHaveValue(BIOCHEMISTRY);
      expect(rowSelect("AllLabUnits")).toBeNull();
    });

    it("collapses to a single All Lab Units row once confirmed", async () => {
      renderExisting("qa_heme", {
        [HEMATOLOGY]: [VALIDATION],
        [BIOCHEMISTRY]: [],
      });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      await userEvent.selectOptions(rowSelect(BIOCHEMISTRY), "AllLabUnits");
      const dialog = await screen.findByRole("dialog", {
        name: /Replace lab unit permissions\?/,
      });
      await userEvent.click(
        within(dialog).getByRole("button", { name: /Replace permissions/ }),
      );

      await waitFor(() => expect(rowSelect("AllLabUnits")).not.toBeNull());
      expect(rowSelect("AllLabUnits")).toHaveValue("AllLabUnits");
      expect(rowSelect(HEMATOLOGY)).toBeNull();
      expect(rowSelect(BIOCHEMISTRY)).toBeNull();
      expect(
        screen.getByRole("button", { name: "Add New Permission" }),
      ).toBeDisabled();
    });

    it("switches to All Lab Units directly when nothing scoped would be lost", async () => {
      renderExisting("qa_heme", { [HEMATOLOGY]: [VALIDATION] });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      await userEvent.selectOptions(rowSelect(HEMATOLOGY), "AllLabUnits");

      await waitFor(() => expect(rowSelect("AllLabUnits")).not.toBeNull());
      // Carbon keeps a closed Modal in the DOM; visibility is the signal.
      expect(document.querySelector(".cds--modal.is-visible")).toBeNull();
      expect(rowSelect(HEMATOLOGY)).toBeNull();
    });
  });

  describe("login name", () => {
    it("accepts a stored login name with an underscore", async () => {
      renderExisting("qa_recept", {});

      await waitFor(() =>
        expect(document.getElementById("login-name")).toHaveValue("qa_recept"),
      );

      expect(document.getElementById("login-name")).not.toHaveAttribute(
        "aria-invalid",
        "true",
      );
    });

    it("explains why a login name is invalid", async () => {
      renderExisting("qa_recept", {});
      await waitFor(() =>
        expect(document.getElementById("login-name")).toHaveValue("qa_recept"),
      );

      await userEvent.type(document.getElementById("login-name"), "1");

      expect(document.getElementById("login-name")).toHaveAttribute(
        "aria-invalid",
        "true",
      );
      expect(
        screen.getByText(messages["notification.invalid.loginName"]),
      ).toBeVisible();
    });
  });

  describe("save", () => {
    const save = async () => {
      await userEvent.click(screen.getByRole("button", { name: "Save" }));
      await waitFor(() => expect(addNotification).toHaveBeenCalled());
      return addNotification.mock.calls[0][0];
    };

    it("says the user was updated, not added, after modifying", async () => {
      postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
        cb({ forward: "redirect:/UnifiedSystemUser" }),
      );
      renderExisting("qa_heme", { [HEMATOLOGY]: [VALIDATION] });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      const notification = await save();

      expect(notification.kind).toBe("success");
      expect(notification.message).toBe(
        "User information updated successfully.",
      );
    });

    it("does not report success when the server answers with the form instead of the redirect", async () => {
      postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
        cb({ forward: "unifiedSystemUserDefinition" }),
      );
      renderExisting("qa_heme", { [HEMATOLOGY]: [VALIDATION] });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      const notification = await save();

      expect(notification.kind).toBe("error");
      expect(notification.message).toBe(messages["server.error.msg"]);
    });

    it("does not report success when the server refuses the save", async () => {
      postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
        cb({ error: "labUnitRoles.allLabUnitsExclusive", statusCode: 400 }),
      );
      renderExisting("qa_heme", { [HEMATOLOGY]: [VALIDATION] });
      await waitFor(() =>
        expect(rowSelect(HEMATOLOGY)).toHaveValue(HEMATOLOGY),
      );

      const notification = await save();

      expect(notification.kind).toBe("error");
      expect(notification.message).toBe(
        messages["systemuserrole.allLabUnits.exclusive.error"],
      );
    });
  });
});
