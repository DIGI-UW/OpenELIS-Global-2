import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";
import DeliveryBundleModal from "./DeliveryBundleModal";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../utils/Utils", () => ({ getFromOpenElisServer }));

const show = (receiptId) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <DeliveryBundleModal receiptId={receiptId} onClose={vi.fn()} />
    </IntlProvider>,
  );

describe("DeliveryBundleModal", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  it("fetches nothing while no delivery is selected", () => {
    show(null);
    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("shows the delivered bundle as the Bridge sent it", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ resourceType: "Bundle", type: "collection" }),
    );
    show("receipt 1");
    expect(await screen.findByTestId("delivery-bundle")).toHaveTextContent(
      '"resourceType": "Bundle"',
    );
    expect(getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/analyzer/deliveries/receipt%201/bundle",
    );
  });

  it("says so when the bundle cannot be loaded", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(undefined),
    );
    show("receipt-1");
    expect(
      await screen.findByText("The delivery bundle could not be loaded."),
    ).toBeInTheDocument();
  });
});
