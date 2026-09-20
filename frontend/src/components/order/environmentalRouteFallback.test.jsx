import { waitFor } from "@testing-library/dom";
import { render, screen } from "@testing-library/react";
import { vi } from "vitest";
import App from "../../App";

const memoryStorage = () => {
  const store = new Map();
  return {
    getItem: (key) => (store.has(key) ? store.get(key) : null),
    setItem: (key, value) => store.set(key, String(value)),
    removeItem: (key) => store.delete(key),
  };
};

const renderAppAt = (path) => {
  vi.stubGlobal("localStorage", memoryStorage());
  vi.spyOn(globalThis, "fetch").mockImplementation((resource) => {
    if (String(resource).endsWith("/session")) {
      return Promise.resolve({
        status: 200,
        json: () =>
          Promise.resolve({
            authenticated: true,
            roles: ["Reception"],
          }),
      });
    }
    return Promise.resolve(
      new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
  });

  window.history.pushState({}, "", path);
  return render(<App />);
};

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.pushState({}, "", "/");
});

test(
  "an environmental path with no route of its own lands on the dashboard",
  { timeout: 20000 },
  async () => {
    const { unmount } = renderAppAt("/order/environmental/collect");

    await waitFor(() =>
      expect(window.location.pathname).toBe("/order/environmental"),
    );
    // A non-empty check would pass on SecureRoute's idle-timeout modal text alone.
    await waitFor(
      () =>
        expect(screen.getByRole("main")).toHaveTextContent(/Order Dashboard/i),
      { timeout: 15000 },
    );

    unmount();
  },
);
