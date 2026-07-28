import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { Route, Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import messages from "../../../languages/en.json";
import AnalyzerSetupReview from "./AnalyzerSetupReview";
import * as analyzerService from "../../../services/analyzerService";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getSetupVerification: vi.fn(),
}));

const renderReview = () => {
  const history = createMemoryHistory({
    initialEntries: [
      "/analyzers/42/review?setup=1&step=review&profile=astm%2Fgx&returnTo=%2Fanalyzers%3Fstatus%3DSETUP",
    ],
  });
  const result = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:id/review">
          <AnalyzerSetupReview />
        </Route>
      </IntlProvider>
    </Router>,
  );
  return { ...result, history };
};

describe("AnalyzerSetupReview", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    analyzerService.getAnalyzer.mockImplementation((id, callback) =>
      callback({
        id,
        name: "Main GeneXpert",
        protocolVersion: "ASTM_LIS2_A2",
        status: "SETUP",
        ipAddress: "10.0.0.4",
        port: 5000,
      }),
    );
    analyzerService.getSetupVerification.mockImplementation((id, callback) =>
      callback({
        verificationState: "STALE",
        currentlyVerified: false,
        readyForActivation: false,
        blockers: ["QC_CHANGED"],
        verifiedBy: "77",
        verifiedAt: "2026-07-28T12:00:00Z",
      }),
    );
  });

  test("shows the final setup summary and explained readiness blockers", async () => {
    renderReview();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Review" }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Main GeneXpert")).toHaveLength(2);
    expect(screen.getByText("QC setup changed")).toBeInTheDocument();
    expect(screen.getByTestId("analyzer-setup-progress")).toHaveAttribute(
      "data-current-step",
      "review",
    );
  });

  test("finishes at the preserved analyzer list state", async () => {
    const { history } = renderReview();
    await screen.findByRole("heading", { level: 1, name: "Review" });

    await userEvent.click(screen.getByRole("button", { name: "Finish setup" }));

    expect(`${history.location.pathname}${history.location.search}`).toBe(
      "/analyzers?status=SETUP",
    );
  });
});
