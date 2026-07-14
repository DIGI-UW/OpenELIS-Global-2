import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { ConfigurationContext } from "../layout/Layout";
import SecureRoute from "./SecureRoute";
import { Roles } from "../utils/Utils";

const renderRoute = ({ roles = [], permissions = [], routeProps = {} }) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: true, roles, permissions },
          errorLoadingSessionDetails: false,
          isCheckingLogin: () => false,
          logout: vi.fn(),
        }}
      >
        <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
          <MemoryRouter initialEntries={["/guarded"]}>
            <SecureRoute
              path="/guarded"
              exact
              component={() => <div>GUARDED CONTENT</div>}
              {...routeProps}
            />
          </MemoryRouter>
        </ConfigurationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

describe("SecureRoute permission prop", () => {
  test("grants access via the permission when the role is absent", () => {
    renderRoute({
      permissions: ["qa.view.qms"],
      routeProps: { permission: "qa.view.qms", role: Roles.GLOBAL_ADMIN },
    });
    expect(screen.getByText("GUARDED CONTENT")).toBeInTheDocument();
  });

  test("grants access via the role fallback when the permission is absent", () => {
    renderRoute({
      roles: [Roles.GLOBAL_ADMIN],
      routeProps: { permission: "qa.view.qms", role: Roles.GLOBAL_ADMIN },
    });
    expect(screen.getByText("GUARDED CONTENT")).toBeInTheDocument();
  });

  test("denies access with neither the permission nor the role", () => {
    renderRoute({
      roles: ["Results"],
      permissions: ["qa.view.qc"],
      routeProps: { permission: "qa.view.qms", role: Roles.GLOBAL_ADMIN },
    });
    expect(screen.queryByText("GUARDED CONTENT")).not.toBeInTheDocument();
  });

  test("keeps legacy role-only behavior intact", () => {
    renderRoute({
      roles: ["Results"],
      routeProps: { role: "Results" },
    });
    expect(screen.getByText("GUARDED CONTENT")).toBeInTheDocument();
  });
});
