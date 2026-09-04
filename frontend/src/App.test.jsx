import { waitFor } from "@testing-library/dom";
import { render } from "@testing-library/react";
import { vi } from "vitest";
import App from "./App";

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
