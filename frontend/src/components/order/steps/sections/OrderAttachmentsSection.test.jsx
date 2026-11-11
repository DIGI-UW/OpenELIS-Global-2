import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const {
  getFromOpenElisServer,
  deleteFromOpenElisServer,
  postToOpenElisServerFormData,
  orderContext,
} = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
  orderContext: { current: { orderId: null } },
}));
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer,
  deleteFromOpenElisServer,
  postToOpenElisServerFormData,
}));

const referral = new File(["%PDF-1.4"], "referral.pdf", {
  type: "application/pdf",
});
const unsavedOrder = (stagedAttachments) => ({
  orderId: null,
  stagedAttachments,
  setStagedAttachments: vi.fn(),
  attachmentsVersion: 0,
});
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
    deleteFromOpenElisServer.mockReset();
    postToOpenElisServerFormData.mockReset();
    getFromOpenElisServer.mockImplementation((_url, cb) =>
      cb([{ id: "4", fileName: "request-form.pdf", fileSizeBytes: 2048 }]),
    );
  });

  it("holds files chosen before the order is saved instead of uploading them", () => {
    orderContext.current = unsavedOrder([]);
    const { container } = renderSection();

    fireEvent.change(container.querySelector('input[type="file"]'), {
      target: { files: [referral] },
    });

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
    expect(postToOpenElisServerFormData).not.toHaveBeenCalled();
    const { setStagedAttachments } = orderContext.current;
    expect(setStagedAttachments).toHaveBeenCalledTimes(1);
    expect(setStagedAttachments.mock.calls[0][0]([])).toEqual([referral]);
  });

  it("lists held files as waiting for the save and lets the user remove one", () => {
    orderContext.current = unsavedOrder([referral]);
    renderSection();

    expect(screen.getByText("referral.pdf")).toBeInTheDocument();
    expect(
      screen.getByText("Uploads when you save the order"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Remove" }));

    const { setStagedAttachments } = orderContext.current;
    expect(setStagedAttachments.mock.calls[0][0]([referral])).toEqual([]);
  });

  it("lists and accepts attachments once the order is saved", () => {
    orderContext.current = { orderId: "41", stagedAttachments: [] };

    renderSection();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/order/DEV01260000000000099/attachments",
      expect.any(Function),
    );
    expect(screen.getByText("request-form.pdf")).toBeInTheDocument();
    expect(screen.getByText("2 KB")).toBeInTheDocument();
    expect(screen.queryByText("Uploads when you save the order")).toBeNull();
  });

  it("deletes an attachment only after the user confirms", () => {
    orderContext.current = { orderId: "41", stagedAttachments: [] };
    renderSection();

    fireEvent.click(screen.getByRole("button", { name: "Delete attachment" }));
    expect(
      screen.getByText(
        'Are you sure you want to delete "request-form.pdf"? This cannot be undone.',
      ),
    ).toBeVisible();
    expect(deleteFromOpenElisServer).not.toHaveBeenCalled();

    fireEvent.click(screen.getByText("Cancel", { selector: "button" }));
    expect(deleteFromOpenElisServer).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Delete attachment" }));
    fireEvent.click(screen.getByText("Delete", { selector: "button" }));
    expect(deleteFromOpenElisServer).toHaveBeenCalledTimes(1);
    expect(deleteFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/order/attachments/4",
      expect.any(Function),
    );
  });
});
