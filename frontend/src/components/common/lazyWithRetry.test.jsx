import React, { Suspense } from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import messages from "../../languages/en.json";
import { RouteErrorBoundary } from "./RouteErrorBoundary";
import lazyWithRetry from "./lazyWithRetry";

const staleChunk = () =>
  Promise.reject(
    new TypeError(
      "Failed to fetch dynamically imported module: /assets/Index-Betns73U.js",
    ),
  );

const loadedChunk = () =>
  Promise.resolve({ default: () => <p>Analyzer results page</p> });

const renderRoute = (Component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <RouteErrorBoundary
        titleKey="errorBoundary.route.analyzerResults.title"
        messageKey="errorBoundary.route.analyzerResults.message"
        onReload={vi.fn()}
      >
        <Suspense fallback={<p>Loading route</p>}>
          <Component />
        </Suspense>
      </RouteErrorBoundary>
    </IntlProvider>,
  );

describe("lazyWithRetry", () => {
  const originalLocation = window.location;
  let reload;

  beforeEach(() => {
    window.sessionStorage.clear();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...originalLocation, reload },
    });
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    Object.defineProperty(window, "location", {
      configurable: true,
      value: originalLocation,
    });
    vi.restoreAllMocks();
  });

  it("reloads the page once when a route chunk from an older build cannot be loaded", async () => {
    renderRoute(lazyWithRetry(staleChunk, 1, 0));

    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(
      screen.queryByText("Analyzer results could not be loaded"),
    ).not.toBeInTheDocument();
  });

  it("shows the route error when the chunk still fails after that reload", async () => {
    renderRoute(lazyWithRetry(staleChunk, 1, 0));
    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));

    renderRoute(lazyWithRetry(staleChunk, 1, 0));

    expect(
      await screen.findByText("Analyzer results could not be loaded"),
    ).toBeVisible();
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it("allows another reload after a chunk loads, so a later deploy recovers too", async () => {
    renderRoute(lazyWithRetry(staleChunk, 1, 0));
    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));

    renderRoute(lazyWithRetry(loadedChunk, 1, 0));
    expect(await screen.findByText("Analyzer results page")).toBeVisible();

    renderRoute(lazyWithRetry(staleChunk, 1, 0));
    await waitFor(() => expect(reload).toHaveBeenCalledTimes(2));
  });
});
