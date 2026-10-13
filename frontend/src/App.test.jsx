import { waitFor } from "@testing-library/dom";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { vi } from "vitest";
import App, { ANALYZER_RESULTS_ROLES, ANALYZER_SETUP_ROLES } from "./App";
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

describe("session check while the server is unreachable (OGC-1442)", () => {
  const sessionUrl = (resource) => String(resource).endsWith("/session");
  let sessionAnswers;
  let fetchSpy;

  const otherRequests = () =>
    Promise.resolve(
      new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

  const sessionCalls = () =>
    fetchSpy.mock.calls.filter(([resource]) => sessionUrl(resource)).length;

  beforeEach(() => {
    sessionAnswers = [];
    fetchSpy = vi.spyOn(globalThis, "fetch").mockImplementation((resource) => {
      if (sessionUrl(resource)) {
        const next = sessionAnswers.shift() || "down";
        if (next === "down") {
          return Promise.reject(new TypeError("Failed to fetch"));
        }
        return Promise.resolve(next);
      }
      return otherRequests();
    });
    vi.spyOn(console, "error").mockImplementation(() => {});
    window.history.pushState({}, "", "/login");
  });

  afterEach(() => {
    vi.restoreAllMocks();
    window.history.pushState({}, "", "/");
  });

  test("keeps trying with a non-blocking notice and no System Error, then loads on the same URL", async () => {
    const { unmount } = render(<App />);

    expect(await screen.findByText("Can't reach the server")).toBeVisible();
    expect(screen.getByText(/Trying again in \d+ s/)).toBeVisible();
    expect(screen.queryByText("System Error")).not.toBeInTheDocument();

    for (let attempt = 2; attempt <= 12; attempt += 1) {
      act(() => {
        window.dispatchEvent(new Event("online"));
      });
      await waitFor(() => expect(sessionCalls()).toBe(attempt));
      await screen.findByText(/Trying again in \d+ s/);
    }
    expect(screen.queryByText("System Error")).not.toBeInTheDocument();
    expect(screen.getByText("Can't reach the server")).toBeVisible();

    sessionAnswers.push(
      new Response(JSON.stringify({ authenticated: false }), { status: 200 }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Try now" }));

    await waitFor(() =>
      expect(
        screen.queryByText("Can't reach the server"),
      ).not.toBeInTheDocument(),
    );
    expect(sessionCalls()).toBe(13);
    expect(window.location.pathname).toBe("/login");
    unmount();
  });

  test.each([401, 403])(
    "takes HTTP %i as signed out without retrying",
    async (status) => {
      sessionAnswers.push(new Response("", { status }));
      const { unmount } = render(<App />);

      await waitFor(() => expect(sessionCalls()).toBe(1));
      await act(() => new Promise((resolve) => setTimeout(resolve, 1500)));
      expect(
        screen.queryByText("Can't reach the server"),
      ).not.toBeInTheDocument();
      expect(sessionCalls()).toBe(1);
      unmount();
    },
  );

  test("keeps trying through proxy errors and a page served while the server starts", async () => {
    sessionAnswers.push(
      new Response("Bad Gateway", { status: 502 }),
      new Response("<!DOCTYPE html><html></html>", { status: 200 }),
    );
    const { unmount } = render(<App />);

    expect(await screen.findByText("Can't reach the server")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Try now" }));
    await waitFor(() => expect(sessionCalls()).toBe(2));
    expect(await screen.findByText(/Trying again in \d+ s/)).toBeVisible();

    sessionAnswers.push(
      new Response(JSON.stringify({ authenticated: false }), { status: 200 }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Try now" }));
    await waitFor(() =>
      expect(
        screen.queryByText("Can't reach the server"),
      ).not.toBeInTheDocument(),
    );
    expect(sessionCalls()).toBe(3);
    unmount();
  });

  test("stops trying once the app is closed", async () => {
    const { unmount } = render(<App />);
    expect(await screen.findByText("Can't reach the server")).toBeVisible();

    unmount();
    window.dispatchEvent(new Event("online"));
    await act(() => new Promise((resolve) => setTimeout(resolve, 50)));

    expect(sessionCalls()).toBe(1);
  });
});

test("keeps analyzer setup to global administrators", () => {
  expect(ANALYZER_SETUP_ROLES).toEqual([Roles.GLOBAL_ADMIN]);
});
