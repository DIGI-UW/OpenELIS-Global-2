import React from "react";
import { render, screen, act, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { ConfigurationContext } from "../layout/Layout";
import SecureRoute from "./SecureRoute";
import { Roles } from "../utils/Utils";

let idleTimerOptions = {};

vi.mock("react-idle-timer", () => ({
  useIdleTimer: (options) => {
    idleTimerOptions = options;
    return {
      activate: vi.fn(),
    };
  },
}));

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

  test("keeps route content rendered when inactivity prompt modal is active", () => {
    renderRoute({
      roles: [Roles.GLOBAL_ADMIN],
      routeProps: { role: Roles.GLOBAL_ADMIN },
    });
    expect(screen.getByText("GUARDED CONTENT")).toBeInTheDocument();

    // Trigger idle prompt (warning timeout reached)
    act(() => {
      idleTimerOptions.onPrompt();
    });

    // Verify modal is displayed
    expect(screen.getByText(messages["stillThere.title"])).toBeInTheDocument();

    // Verify child/route content remains rendered in the document
    expect(screen.getByText("GUARDED CONTENT")).toBeInTheDocument();
  });

  test("preserves entered form state across the inactivity warning and dismissal", () => {
    const FormComponent = () => {
      const [value, setValue] = React.useState("");
      return (
        <input
          aria-label="test-input"
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
      );
    };

    renderRoute({
      roles: [Roles.GLOBAL_ADMIN],
      routeProps: { role: Roles.GLOBAL_ADMIN, component: FormComponent },
    });

    const input = screen.getByLabelText("test-input");
    fireEvent.change(input, { target: { value: "Unsaved Lab Result 123" } });
    expect(input).toHaveValue("Unsaved Lab Result 123");

    const modalHeading = screen.getByText(messages["stillThere.title"]);
    const modalContainer = modalHeading.closest(".cds--modal");

    // Initially modal is not visible
    expect(modalContainer).not.toHaveClass("is-visible");

    // Trigger idle prompt (warning timeout reached)
    act(() => {
      idleTimerOptions.onPrompt();
    });

    // Verify modal is displayed and input retains entered state while modal is open
    expect(modalContainer).toHaveClass("is-visible");
    expect(screen.getByLabelText("test-input")).toHaveValue(
      "Unsaved Lab Result 123",
    );

    // Dismiss modal via onActive
    act(() => {
      idleTimerOptions.onActive();
    });

    // Verify modal is dismissed and form input still preserves the entered state
    expect(modalContainer).not.toHaveClass("is-visible");
    expect(screen.getByLabelText("test-input")).toHaveValue(
      "Unsaved Lab Result 123",
    );
  });
});
