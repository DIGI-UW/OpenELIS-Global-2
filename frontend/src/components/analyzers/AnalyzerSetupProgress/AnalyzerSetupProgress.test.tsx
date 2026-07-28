import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import AnalyzerSetupProgress from "./AnalyzerSetupProgress";

const messages = {
  "analyzer.setup.step.instrument": "Instrument",
  "analyzer.setup.step.verify": "Verify",
  "analyzer.setup.step.connect": "Connect",
  "analyzer.setup.step.review": "Review",
};

describe("AnalyzerSetupProgress", () => {
  it("renders the four setup steps and marks the current step", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <AnalyzerSetupProgress currentStep="connect" />
      </IntlProvider>,
    );

    expect(screen.getByText("Instrument")).toBeInTheDocument();
    expect(screen.getByText("Verify")).toBeInTheDocument();
    expect(screen.getByText("Connect")).toBeInTheDocument();
    expect(screen.getByText("Review")).toBeInTheDocument();
    expect(screen.getByTestId("analyzer-setup-progress")).toHaveAttribute(
      "data-current-step",
      "connect",
    );
  });
});
