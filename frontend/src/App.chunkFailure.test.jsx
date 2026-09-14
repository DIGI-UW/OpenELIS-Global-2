import { render, screen } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import "@testing-library/jest-dom";

/**
 * Login and Layout are lazy now, so their chunks can fail the way any chunk
 * fetch can, and React unmounts the whole tree on an uncaught render error.
 * Without a boundary above the routes that is a blank page with nothing on it
 * to click — no message, no reload. Its own file because mocking a route
 * module does not come back out of the module registry.
 */
vi.mock("./components/Login", () => ({
  default: () => {
    throw new Error("Failed to fetch dynamically imported module");
  },
}));

afterEach(() => {
  window.history.pushState({}, "", "/");
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

test("a route chunk that fails outright leaves the user something to click", async () => {
  const { default: App } = await import("./App");
  vi.spyOn(console, "error").mockImplementation(() => undefined);
  const store = {};
  vi.stubGlobal("localStorage", {
    getItem: (key) => store[key] ?? null,
    setItem: (key, value) => {
      store[key] = value;
    },
    removeItem: (key) => delete store[key],
  });
  vi.spyOn(globalThis, "fetch").mockImplementation(() =>
    Promise.resolve(
      new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    ),
  );

  window.history.pushState({}, "", "/login");
  render(<App />);

  expect(
    await screen.findByRole("button", { name: "Reload" }),
  ).toBeInTheDocument();
});
