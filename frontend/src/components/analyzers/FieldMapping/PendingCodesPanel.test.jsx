import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PendingCodesPanel from "./PendingCodesPanel";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";

jest.mock("../../../services/analyzerService", () => ({
  updatePendingCodeStatus: jest.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("PendingCodesPanel", () => {
  test("updates pending code status to MAPPED", async () => {
    const onUpdated = jest.fn();
    analyzerService.updatePendingCodeStatus.mockImplementation(
      (analyzerId, pendingCodeId, status, callback) => {
        callback({ ok: true, status });
      },
    );

    renderWithIntl(
      <PendingCodesPanel
        analyzerId="2013"
        onUpdated={onUpdated}
        pendingCodes={[
          {
            id: "pc-1",
            analyzerTestName: "MTB-RIF",
            seenCount: 2,
            status: "PENDING",
          },
        ]}
      />,
    );

    await userEvent.click(screen.getByTestId("pending-code-map-pc-1"));

    await waitFor(() => {
      expect(analyzerService.updatePendingCodeStatus).toHaveBeenCalledWith(
        "2013",
        "pc-1",
        "MAPPED",
        expect.any(Function),
      );
      expect(onUpdated).toHaveBeenCalled();
    });
  });
});
