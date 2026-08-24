import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import InstrumentDetailPage from "./InstrumentDetailPage";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const renderPage = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/qc/instruments/:instrumentId">
          <InstrumentDetailPage />
        </Route>
      </IntlProvider>
    </BrowserRouter>,
  );

describe("InstrumentDetailPage analyzer context", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.replaceState(
      {},
      "",
      "/analyzers/qc/instruments/42?returnTo=%2Fanalyzers%3Fsearch%3Dgene%26status%3DACTIVE",
    );
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(
        url === "/rest/qc/dashboard/instruments/42"
          ? {
              instrumentId: "42",
              instrumentName: "GeneXpert - Main Lab",
              instrumentType: "GeneXpert",
              instrumentLocation: "Molecular Biology",
              complianceColor: "GREEN",
              analyteDetails: [],
            }
          : [],
      );
    });
  });

  it("shows a linkable breadcrumb back to the exact analyzer dashboard state", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "GeneXpert - Main Lab",
      }),
    ).toBeVisible();

    const breadcrumb = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(breadcrumb.getByRole("link", { name: "Analyzers" })).toHaveAttribute(
      "href",
      "/analyzers?search=gene&status=ACTIVE",
    );
    expect(
      breadcrumb.getByRole("link", { name: "Quality Control Dashboard" }),
    ).toHaveAttribute("href", "/analyzers/qc/db");
    expect(
      breadcrumb.getByText("GeneXpert - Main Lab").closest("li"),
    ).toHaveAttribute("aria-current", "page");
  });
});
