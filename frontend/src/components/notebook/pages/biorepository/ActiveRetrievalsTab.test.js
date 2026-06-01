import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import ActiveRetrievalsTab from "./ActiveRetrievalsTab";
import { getFromOpenElisServer } from "../../../utils/Utils";

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn((url, callback) => {
    callback([
      {
        id: 42,
        requestNumber: "REQ-42",
        requestStatus: "APPROVED",
        items: [{ id: 7, status: "PENDING", sampleNumber: "S-001" }],
      },
    ]);
  }),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

const renderTab = () =>
  render(
    <IntlProvider locale="en">
      <ActiveRetrievalsTab />
    </IntlProvider>,
  );

describe("ActiveRetrievalsTab", () => {
  test("loads approved and in-progress retrieval requests on mount", () => {
    renderTab();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/biorepository/retrieval/requests?status=APPROVED,IN_PROGRESS,PARTIALLY_COMPLETED",
      expect.any(Function),
    );
  });
});
