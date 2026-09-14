import { waitFor } from "@testing-library/dom";
import { render, screen } from "@testing-library/react";
import { afterEach, vi } from "vitest";
import "@testing-library/jest-dom";
import App, { ANALYZER_RESULTS_ROLES } from "./App";
import { Roles } from "./components/utils/Utils";

test("renders App component without errors", () => {
  // Just verify the App component renders without throwing errors
  const { container } = render(<App />);
  expect(container).toBeTruthy();
});

test("does not write session credentials to the browser console", async () => {
  const session = {
    authenticated: false,
    sessionId: "sensitive-session-id",
    csrf: "sensitive-csrf-token",
    roles: [],
    userLabRolesMap: {},
  };
  const sessionJson = vi.fn().mockResolvedValue(session);
  const fetchSpy = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation((resource) => {
      if (String(resource).endsWith("/session")) {
        return Promise.resolve({ status: 200, json: sessionJson });
      }
      return Promise.resolve(
        new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    });
  const debugSpy = vi.spyOn(console, "debug").mockImplementation(() => {});

  window.history.pushState({}, "", "/login");
  const { unmount } = render(<App />);

  await waitFor(() => expect(sessionJson).toHaveBeenCalledOnce());
  expect(debugSpy).not.toHaveBeenCalledWith(
    expect.stringContaining(session.sessionId),
  );
  expect(debugSpy).not.toHaveBeenCalledWith(
    expect.stringContaining(session.csrf),
  );

  unmount();
  fetchSpy.mockRestore();
  debugSpy.mockRestore();
  window.history.pushState({}, "", "/");
});

test("allows analyzer operators and global administrators into Analyzer Results", () => {
  expect(ANALYZER_RESULTS_ROLES).toEqual([
    Roles.GLOBAL_ADMIN,
    Roles.ANALYSER_IMPORT,
  ]);
});

/**
 * Which routes render inside Layout. Layout is what carries Header and Footer,
 * and react-router 5's Switch renders only its first match, so moving a route
 * above the Layout-wrapped catch-all silently strips the chrome from it — no
 * error, nothing in the console. /login is the route meant to render bare: that
 * is what keeps Layout, and the component library Header reaches through it,
 * off the login page's first paint.
 */
describe("Layout chrome by route", () => {
  const renderAt = (pathname, { authenticated }) => {
    const store = {};
    vi.stubGlobal("localStorage", {
      getItem: (key) => store[key] ?? null,
      setItem: (key, value) => {
        store[key] = value;
      },
      removeItem: (key) => delete store[key],
    });
    vi.spyOn(globalThis, "fetch").mockImplementation((resource) => {
      if (String(resource).endsWith("/session")) {
        return Promise.resolve({
          status: 200,
          json: async () => ({ authenticated, roles: [] }),
        });
      }
      return Promise.resolve(
        new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    });
    window.history.pushState({}, "", pathname);
    return render(<App />);
  };

  afterEach(() => {
    window.history.pushState({}, "", "/");
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  test("wraps /ChangePasswordLogin, so it keeps a header and a logout", async () => {
    renderAt("/ChangePasswordLogin", { authenticated: true });

    expect(await screen.findByTestId("content-wrapper")).toBeInTheDocument();
  });

  test("wraps /landing, an authenticated route", async () => {
    renderAt("/landing", { authenticated: true });

    expect(await screen.findByTestId("content-wrapper")).toBeInTheDocument();
  });

  test("leaves /login bare, with its own locale selector for the chrome's", async () => {
    renderAt("/login", { authenticated: false });

    expect(await screen.findByLabelText("Select Locale")).toBeInTheDocument();
    expect(screen.queryByTestId("content-wrapper")).not.toBeInTheDocument();
  });
});
