import React, { Suspense } from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import messages from "../../languages/en.json";
import { RouteErrorBoundary } from "./RouteErrorBoundary";
import lazyWithRetry from "./lazyWithRetry";
import { retryServerNow, subscribeServerWait } from "../utils/serverConnection";

const staleChunk = () =>
  Promise.reject(
    new TypeError(
      "Failed to fetch dynamically imported module: /assets/Index-Betns73U.js",
    ),
  );

const serverDown = () =>
  Promise.resolve(new Response("Bad Gateway", { status: 502 }));

const serverUp = () =>
  Promise.resolve(new Response("<html></html>", { status: 200 }));

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
  let fetchSpy;

  beforeEach(() => {
    window.sessionStorage.clear();
    fetchSpy = vi.spyOn(globalThis, "fetch").mockImplementation(serverUp);
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

  it("waits for a server that is down instead of reloading into the proxy's error page, then loads the route", async () => {
    const waits = [];
    const unsubscribe = subscribeServerWait((wait) => waits.push(wait));
    fetchSpy.mockImplementation(serverDown);
    let chunkUp = false;
    const chunk = vi.fn(() => (chunkUp ? loadedChunk() : staleChunk()));

    renderRoute(lazyWithRetry(chunk, 1, 0));

    await waitFor(() => expect(waits[waits.length - 1]).not.toBeNull());
    expect(reload).not.toHaveBeenCalled();

    retryServerNow();
    await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(waits[waits.length - 1]).not.toBeNull());
    expect(chunk).toHaveBeenCalledTimes(1);

    chunkUp = true;
    fetchSpy.mockImplementation(serverUp);
    retryServerNow();

    expect(await screen.findByText("Analyzer results page")).toBeVisible();
    expect(reload).not.toHaveBeenCalled();
    expect(waits[waits.length - 1]).toBeNull();
    unsubscribe();
  });

  it("reloads once when the server is back but the route's chunk is still missing", async () => {
    fetchSpy.mockImplementationOnce(serverDown);
    const chunk = vi.fn(staleChunk);

    renderRoute(lazyWithRetry(chunk, 1, 0));
    await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(1));
    retryServerNow();

    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(chunk).toHaveBeenCalledTimes(2);
  });

  it("reloads after an outage even when an earlier outage already used the tab's reload", async () => {
    window.sessionStorage.setItem("oe.lazyWithRetry.reloaded", "1");
    fetchSpy.mockImplementationOnce(serverDown);

    renderRoute(lazyWithRetry(staleChunk, 1, 0));
    await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(1));
    retryServerNow();

    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(
      screen.queryByText("Analyzer results could not be loaded"),
    ).not.toBeInTheDocument();
  });
});
