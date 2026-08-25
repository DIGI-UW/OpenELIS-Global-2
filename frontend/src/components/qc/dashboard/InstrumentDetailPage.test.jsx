import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
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
    window.scrollTo = vi.fn();
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
              complianceColor: "NOT_CONFIGURED",
              analyteDetails: [],
              activeControlLots: 0,
            }
          : [],
      );
    });
  });

  it("opens the linked QC detail at the top of the page", async () => {
    renderPage();

    await screen.findByRole("heading", {
      level: 1,
      name: "GeneXpert - Main Lab",
    });

    expect(window.scrollTo).toHaveBeenCalledWith({
      top: 0,
      left: 0,
      behavior: "auto",
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
    const breadcrumbQueries = within(breadcrumb);
    expect(
      breadcrumbQueries.getByRole("link", { name: "Analyzers" }),
    ).toHaveAttribute("href", "/analyzers?search=gene&status=ACTIVE");
    expect(
      breadcrumbQueries.getByRole("link", {
        name: "Quality Control Dashboard",
      }),
    ).toHaveAttribute("href", "/analyzers/qc/db");
    const currentCrumb = breadcrumb.querySelector('[aria-current="page"]');
    expect(currentCrumb).toHaveTextContent("GeneXpert - Main Lab");
  });

  it("restores the selected QC view from the URL and preserves the analyzer return path", async () => {
    window.history.replaceState(
      {},
      "",
      "/analyzers/qc/instruments/42?returnTo=%2Fanalyzers%3Fsearch%3Dgene%26status%3DACTIVE&view=chart",
    );

    renderPage();

    const chartTab = await screen.findByRole("tab", {
      name: "Control Chart",
    });
    const activityTab = screen.getByRole("tab", {
      name: "Activity Timeline",
    });
    expect(chartTab).toHaveAttribute("aria-selected", "true");

    await userEvent.click(activityTab);

    const params = new URLSearchParams(window.location.search);
    expect(params.get("view")).toBe("activity");
    expect(params.get("returnTo")).toBe("/analyzers?search=gene&status=ACTIVE");
    expect(activityTab).toHaveAttribute("aria-selected", "true");
  });

  it("shows an unconfigured operational QC state without exposing a bare database id", async () => {
    renderPage();

    const statusHeader = await screen.findByTestId(
      "instrument-detail-modal-header",
    );
    expect(within(statusHeader).getByText("Not configured")).toBeVisible();
    expect(within(statusHeader).getByText("GeneXpert")).toBeVisible();
    expect(within(statusHeader).getByText("Molecular Biology")).toBeVisible();
    expect(within(statusHeader).queryByText("42")).not.toBeInTheDocument();
    expect(screen.queryByText("In Control")).not.toBeInTheDocument();
    expect(
      screen.getByText(
        "No active control lots or QC results are linked to this analyzer.",
      ),
    ).toBeVisible();
  });
});
