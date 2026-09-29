import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer, orderContext } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
  orderContext: { current: { orderId: null } },
}));
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer,
  deleteFromOpenElisServer: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
}));
vi.mock("../../OrderContext", () => ({
  useOrderContext: () => orderContext.current,
}));

import OrderAttachmentsSection from "./OrderAttachmentsSection";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderAttachmentsSection
        labNumber="DEV01260000000000099"
        isReadOnly={false}
      />
    </IntlProvider>,
  );

describe("OrderAttachmentsSection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((_url, cb) =>
      cb([{ id: "4", fileName: "request-form.pdf", fileSizeBytes: 2048 }]),
    );
  });

  it("does not ask the server for attachments of an order that is not saved yet", () => {
    orderContext.current = { orderId: null };

    renderSection();

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("order-attachment-save-first")).toHaveTextContent(
      "Save the order to add attachments.",
    );
    expect(screen.queryByText(/Drag and drop/)).toBeNull();
  });

  it("lists and accepts attachments once the order is saved", () => {
    orderContext.current = { orderId: "41" };

    renderSection();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/order/DEV01260000000000099/attachments",
      expect.any(Function),
    );
    expect(screen.getByText("request-form.pdf")).toBeInTheDocument();
    expect(screen.getByText("2 KB")).toBeInTheDocument();
    expect(screen.queryByTestId("order-attachment-save-first")).toBeNull();
  });
});
