import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import FulfillmentReviewBlock from "./FulfillmentReviewBlock";
import { SUGGESTION_STATUS, SUGGESTIONS_LOAD_STATE } from "./biorepoFulfillmentSuggestionHelpers";

const renderBlock = (props) =>
  render(
    <IntlProvider locale="en">
      <FulfillmentReviewBlock
        unresolvedItems={[
          {
            id: 1,
            requestedAccessionNumber: "ACC-100",
            quantityRequested: 2,
            unitOfMeasure: "mL",
          },
        ]}
        suggestionsByItemId={{}}
        suggestionsLoadState={SUGGESTIONS_LOAD_STATE.LOADING}
        onUseSample={jest.fn()}
        onReviewAlternatives={jest.fn()}
        {...props}
      />
    </IntlProvider>,
  );

describe("FulfillmentReviewBlock", () => {
  it("shows row reference while suggestions are loading", () => {
    renderBlock();
    expect(screen.getByText("ACC-100")).toBeTruthy();
    expect(screen.getByTestId("fulfillment-review-row-1")).toBeTruthy();
  });

  it("shows best match and actions when suggestion is loaded", () => {
    renderBlock({
      suggestionsLoadState: SUGGESTIONS_LOAD_STATE.LOADED,
      suggestionsByItemId: {
        1: {
          status: SUGGESTION_STATUS.EXACT_MATCH,
          summary: {
            sampleIdentity: "ACC-100",
            availableQuantity: 5,
            availableUnitOfMeasure: "mL",
            samplePath: "Zn A / FRZ 1 / SH 1 / RK 1 / Box 1 / Pos A1",
            matchReason: "EXACT_ACCESSION",
          },
          topSuggestion: {
            id: 99,
            accessionNumber: "ACC-100",
            remainingQuantity: 5,
            unitOfMeasure: "mL",
            matchReason: "EXACT_ACCESSION",
          },
          results: [{ id: 99 }],
        },
      },
    });
    expect(screen.getByText("Use this sample")).toBeTruthy();
    expect(screen.getByText(/Best match/i)).toBeTruthy();
    expect(screen.getAllByText(/ACC-100/).length).toBeGreaterThan(0);
    expect(screen.getByText(/Zn A/)).toBeTruthy();
  });

  it("fires onUseSample when Use this sample is clicked", () => {
    const onUseSample = jest.fn();
    renderBlock({
      suggestionsLoadState: SUGGESTIONS_LOAD_STATE.LOADED,
      suggestionsByItemId: {
        1: {
          status: SUGGESTION_STATUS.EXACT_MATCH,
          topSuggestion: { id: 99, accessionNumber: "ACC-100" },
          results: [{ id: 99 }],
        },
      },
      onUseSample,
    });
    fireEvent.click(screen.getByText("Use this sample"));
    expect(onUseSample).toHaveBeenCalled();
  });
});
