import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";

import HelpViewer from "./HelpViewer";
import messages from "../../languages/en.json";

const manifest = (locale, pages) => ({
  releaseVersion: "3.4.0",
  locale,
  guides: {
    user: pages,
  },
});

const pageHtml = (title, extra = "") =>
  `<h1 id="${title}">${title}</h1><p>Body of ${title}.</p>${extra}`;

const mockFetch = (routes) =>
  vi.fn(async (url) => {
    const handler = routes[url];
    if (!handler) {
      return { ok: false, status: 404, text: async () => "" };
    }
    return typeof handler === "function" ? handler() : handler;
  });

const renderAt = (path) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[path]}>
        <Route path="/help/:slug?">
          <HelpViewer />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );

describe("HelpViewer", () => {
  const originalFetch = global.fetch;
  beforeEach(() => {
    global.fetch = originalFetch;
  });
  afterEach(() => {
    vi.restoreAllMocks();
    global.fetch = originalFetch;
  });

  test("renders the landing page (first user-guide slug) when no slug is in the URL", async () => {
    global.fetch = mockFetch({
      "/help-content/en/manifest.json": {
        ok: true,
        json: async () =>
          manifest("en", [
            { slug: "lab-context", title: "Lab Context", headings: [] },
            {
              slug: "working-with-patients",
              title: "Working with Patients",
              headings: [],
            },
          ]),
      },
      "/help-content/en/user/lab-context.html": {
        ok: true,
        text: async () => pageHtml("Lab Context"),
      },
    });

    renderAt("/help");

    expect(await screen.findByText("Lab Context")).toBeInTheDocument();
    expect(screen.getByTestId("help-viewer-content")).toHaveTextContent(
      "Body of Lab Context",
    );
  });

  test("falls back to English with a Tag when the French page is missing", async () => {
    global.fetch = mockFetch({
      "/help-content/en/manifest.json": {
        ok: true,
        json: async () =>
          manifest("en", [
            { slug: "lab-context", title: "Lab Context", headings: [] },
          ]),
      },
      "/help-content/en/user/lab-context.html": {
        ok: true,
        text: async () => pageHtml("Lab Context"),
      },
    });

    render(
      <IntlProvider locale="fr" messages={messages}>
        <MemoryRouter initialEntries={["/help/lab-context"]}>
          <Route path="/help/:slug?">
            <HelpViewer />
          </Route>
        </MemoryRouter>
      </IntlProvider>,
    );

    expect(await screen.findByText(/English translation/i)).toBeInTheDocument();
    expect(screen.getByTestId("help-viewer-content")).toHaveTextContent(
      "Body of Lab Context",
    );
  });

  test("shows a 'page not found' notice when the slug is unknown", async () => {
    global.fetch = mockFetch({
      "/help-content/en/manifest.json": {
        ok: true,
        json: async () =>
          manifest("en", [
            { slug: "lab-context", title: "Lab Context", headings: [] },
          ]),
      },
    });

    renderAt("/help/does-not-exist");

    expect(await screen.findByText(/Page not found/i)).toBeInTheDocument();
  });

  test("shows an error state when the manifest fetch fails", async () => {
    global.fetch = vi.fn(async () => ({ ok: false, status: 500 }));

    renderAt("/help");

    expect(
      await screen.findByText(/Help content could not be loaded/i),
    ).toBeInTheDocument();
  });
});
