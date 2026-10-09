import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import CultureSetSummary from "./CultureSetSummary";

const renderSummary = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CultureSetSummary {...props} />
    </IntlProvider>,
  );

describe("CultureSetSummary", () => {
  it("counts sets and bottles as two separately pluralized phrases", () => {
    renderSummary({
      specimens: [
        { id: 1, collectedInSets: true, cultureSetNumber: 1 },
        { id: 2, collectedInSets: true, cultureSetNumber: 1 },
        { id: 3, collectedInSets: true, cultureSetNumber: 2 },
      ],
    });
    expect(screen.getByText("2 sets, 3 bottles")).toBeInTheDocument();
  });

  it("uses singular forms for one set holding one bottle", () => {
    renderSummary({
      specimens: [{ id: 1, collectedInSets: true, cultureSetNumber: 1 }],
    });
    expect(screen.getByText("1 set, 1 bottle")).toBeInTheDocument();
  });
});
