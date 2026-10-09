import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import CultureSetSummary from "./CultureSetSummary";

const renderSummary = (specimens) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureSetSummary specimens={specimens} />
    </IntlProvider>,
  );

describe("CultureSetSummary count", () => {
  it("counts sets and bottles in the plural", () => {
    renderSummary([
      { collectedInSets: true, cultureSetNumber: 1 },
      { collectedInSets: true, cultureSetNumber: 1 },
      { collectedInSets: true, cultureSetNumber: 2 },
    ]);

    expect(screen.getByText("2 sets, 3 bottles")).toBeInTheDocument();
  });

  it("counts one set and one bottle in the singular", () => {
    renderSummary([{ collectedInSets: true, cultureSetNumber: 1 }]);

    expect(screen.getByText("1 set, 1 bottle")).toBeInTheDocument();
  });
});
