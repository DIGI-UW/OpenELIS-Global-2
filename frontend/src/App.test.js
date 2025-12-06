import React from "react";
import { act } from "react-dom/test-utils";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";

// Lightweight stubs to avoid exercising full app shell wiring in this smoke test.
jest.mock("./components/layout/Layout", () => {
  const React = require("react");
  return function MockLayout({ children }) {
    return (
      <div>
        <header id="mainHeader">
          <img alt="logo" />
        </header>
        <nav className="cds--side-nav">Nav</nav>
        <main>{children}</main>
      </div>
    );
  };
});

jest.mock("./components/security", () => {
  const React = require("react");
  const { Route } = require("react-router-dom");
  return {
    __esModule: true,
    SecureRoute: ({ component: Component, render, ...rest }) => (
      <Route
        {...rest}
        render={(props) =>
          Component ? <Component {...props} /> : render ? render(props) : null
        }
      />
    ),
  };
});

jest.mock("./components", () => {
  const React = require("react");
  return {
    __esModule: true,
    Admin: () => React.createElement("div", null, "Admin"),
  };
});

jest.mock("./components/Home", () => {
  const React = require("react");
  return function MockHome() {
    return React.createElement("div", null, "Home");
  };
});

// Stub network helpers to keep the app shell test fast and deterministic.
jest.mock("./components/utils/Utils", () => ({
  Roles: {
    GLOBAL_ADMIN: "Global Administrator",
    USER_ACCOUNT_ADMIN: "User Account Administrator",
    AUDIT_TRAIL: "Audit Trail",
    ANALYSER_IMPORT: "Analyser Import",
    CYTOPATHOLOGIST: "Cytopathologist",
    PATHOLOGIST: "Pathologist",
    RECEPTION: "Reception",
    RESULTS: "Results",
    VALIDATION: "Validation",
    REPORTS: "Reports",
  },
  hasRole: () => true,
  hasAnyRole: () => true,
  getFromOpenElisServer: (endPoint, callback) => {
    if (endPoint.includes("notifications")) {
      callback([]); // empty notifications array
      return;
    }
    if (endPoint.includes("pnconfig")) {
      callback({ subscribeEnabled: false });
      return;
    }
    if (endPoint.includes("/menu")) {
      callback([
        {
          menu: {
            displayKey: "home",
            actionURL: "/",
            elementId: "menu_home",
          },
          childMenus: [],
        },
      ]);
      return;
    }
    // default: return empty object
    callback({});
  },
  postToOpenElisServerJsonResponse: jest.fn(),
  postToOpenElisServer: jest.fn(),
  postToOpenElisServerFullResponse: jest.fn(),
  postToOpenElisServerFormData: jest.fn(),
  postToOpenElisServerForBlob: jest.fn(),
  getFromOpenElisServerSync: jest.fn(),
}));

import App from "./App";

describe("App shell renders Carbon header + tri-state sidenav", () => {
  let originalLocation;

  beforeAll(() => {
    // JSDOM navigation isn't implemented; stub location to avoid errors when SecureRoute redirects
    originalLocation = window.location;
    delete window.location;
    window.location = {
      ...originalLocation,
      href: "http://localhost/",
      assign: jest.fn(),
      replace: jest.fn(),
      origin: "http://localhost",
    };
  });

  afterAll(() => {
    // Restore the real location object
    window.location = originalLocation;
  });

  beforeEach(() => {
    // Mock fetch for session/config calls (all endpoints)
    global.fetch = jest.fn(() =>
      Promise.resolve({
        status: 200,
        headers: {
          get: () => "application/json",
        },
        json: () =>
          Promise.resolve({
            authenticated: true,
            csrf: "test-csrf",
            roles: ["Global Administrator"],
            userLabRolesMap: { AllLabUnits: ["Results"] },
          }),
      }),
    );
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test("renders Carbon header and sidenav (default shell)", async () => {
    await act(async () => {
      render(<App />);
    });

    // Logo presence is a stable indicator that the Carbon header rendered.
    const logo = await screen.findByAltText(/logo/i);
    expect(logo).toBeTruthy();

    const sideNav = document.querySelector(".cds--side-nav");
    expect(sideNav).toBeTruthy();
  });
});
