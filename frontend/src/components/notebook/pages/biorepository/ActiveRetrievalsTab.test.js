import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import ActiveRetrievalsTab from "./ActiveRetrievalsTab";
import { getFromOpenElisServer } from "../../../utils/Utils";

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn((url, callback) => {
    if (url.includes("/requests/")) {
      callback({
        id: 42,
        requestNumber: "REQ-42",
        items: [
          {
            id: 7,
            itemRole: "REFERENCE",
            status: "AWAITING_FULFILLMENT",
            requestedAccessionNumber: "ACC-7",
            quantityRequested: 1,
            unitOfMeasure: "mL",
          },
        ],
      });
      return;
    }
    callback([
      {
        id: 42,
        requestNumber: "REQ-42",
        requestStatus: "APPROVED",
        items: [{ id: 7, status: "AWAITING_FULFILLMENT", sampleNumber: "S-001" }],
      },
    ]);
  }),
  postToOpenElisServerJsonResponse: jest.fn((url, body, callback) => {
    if (url.includes("/items/suggestions")) {
      callback({
        7: {
          retrievalItemId: 7,
          suggestionStatus: "EXACT_MATCH",
          topCandidate: {
            id: 99,
            accessionNumber: "ACC-7",
            remainingQuantity: 5,
            unitOfMeasure: "mL",
            exactIdentityMatch: true,
          },
          candidates: [
            {
              id: 99,
              accessionNumber: "ACC-7",
              remainingQuantity: 5,
              unitOfMeasure: "mL",
              exactIdentityMatch: true,
            },
          ],
        },
      });
    }
  }),
}));

const renderTab = () =>
  render(
    <IntlProvider locale="en">
      <ActiveRetrievalsTab />
    </IntlProvider>,
  );

describe("ActiveRetrievalsTab", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("loads approved and in-progress retrieval requests on mount", () => {
    renderTab();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/biorepository/retrieval/requests?status=APPROVED,IN_PROGRESS,PARTIALLY_COMPLETED",
      expect.any(Function),
    );
  });
});
