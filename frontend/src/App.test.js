import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import App from "./App";

describe("App shell rollout to TwoModeLayout", () => {
  beforeEach(() => {
    // Mock fetch for session/config calls
    global.fetch = jest.fn(() =>
      Promise.resolve({
        status: 200,
        json: () =>
          Promise.resolve({
            authenticated: false,
          }),
      }),
    );
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test("renders Carbon header and sidenav (TwoModeLayout shell)", async () => {
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/OpenELIS Global/i)).toBeInTheDocument();
    });

    const sideNav = document.querySelector(".cds--side-nav");
    expect(sideNav).toBeTruthy();
  });
});
