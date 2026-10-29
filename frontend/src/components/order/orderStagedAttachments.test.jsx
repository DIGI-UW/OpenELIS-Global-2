import React, { useEffect, useRef } from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { OrderProvider, useOrderContext } from "./OrderContext";
import { NotificationContext } from "../layout/Layout";
import {
  postToOpenElisServerFullResponse,
  postToOpenElisServerFormData,
  getFromOpenElisServer,
} from "../utils/Utils";

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/clinical/enter", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

const LAB_NO = "DEV01260000000000611";
const OTHER_LAB_NO = "DEV01260000000000612";
const referral = new File(["%PDF-1.4"], "referral.pdf", {
  type: "application/pdf",
});

const savedResponse = {
  ok: true,
  clone: () => ({ json: async () => ({ patientProperties: {} }) }),
};

const answerOrderSearch = () =>
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes(`labNumber=${LAB_NO}`)) {
      cb({ id: "172", labNumber: LAB_NO, samples: [] });
    }
    if (url.includes(`labNumber=${OTHER_LAB_NO}`)) {
      cb({ id: "200", labNumber: OTHER_LAB_NO, samples: [] });
    }
  });

/** Holds a file on a new order, then runs `act` once the order is filled in. */
const HoldFileThen = ({ act, contextRef }) => {
  const context = useOrderContext();
  contextRef.current = context;
  const started = useRef(false);
  useEffect(() => {
    context.setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: LAB_NO },
    }));
    context.setSamples([{ sampleTypeId: "3", tests: [{ id: "1" }] }]);
    context.setStagedAttachments([referral]);
  }, []);
  useEffect(() => {
    if (
      started.current ||
      context.stagedAttachments.length === 0 ||
      context.orderData?.sampleOrderItems?.labNo !== LAB_NO
    ) {
      return;
    }
    started.current = true;
    act(context);
  }, [context.stagedAttachments, context.orderData]);
  return null;
};

const renderHolding = (act, notifications = {}) => {
  const contextRef = { current: null };
  render(
    <NotificationContext.Provider
      value={{
        addNotification: vi.fn(),
        setNotificationVisible: vi.fn(),
        ...notifications,
      }}
    >
      <OrderProvider workflowType="clinical">
        <HoldFileThen act={act} contextRef={contextRef} />
      </OrderProvider>
    </NotificationContext.Provider>,
  );
  return contextRef;
};

describe("attachments chosen before the order exists", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    postToOpenElisServerFormData.mockReset();
    getFromOpenElisServer.mockReset();
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb(savedResponse),
    );
    answerOrderSearch();
  });

  it.each(["saveOrderEntry", "saveOrder"])(
    "%s uploads them to the order it saved",
    async (save) => {
      postToOpenElisServerFormData.mockImplementation((url, data, cb) =>
        cb(200),
      );
      const contextRef = renderHolding((context) =>
        context[save]().catch(() => {}),
      );

      await waitFor(() =>
        expect(postToOpenElisServerFormData).toHaveBeenCalledTimes(1),
      );
      const [url, formData] = postToOpenElisServerFormData.mock.calls[0];
      expect(url).toBe(`/rest/order/${LAB_NO}/attachments`);
      expect(formData.getAll("files").map((file) => file.name)).toEqual([
        "referral.pdf",
      ]);
      await waitFor(() =>
        expect(contextRef.current.stagedAttachments).toEqual([]),
      );
    },
  );

  it("does not send them to an order loaded instead", async () => {
    const contextRef = renderHolding((context) =>
      context.loadOrder(OTHER_LAB_NO, false),
    );

    await waitFor(() => expect(contextRef.current.orderId).toBe("200"));
    expect(contextRef.current.stagedAttachments).toEqual([]);
    expect(postToOpenElisServerFormData).not.toHaveBeenCalled();
  });

  it("warns that the order saved without them when the upload fails", async () => {
    const addNotification = vi.fn();
    postToOpenElisServerFormData.mockImplementation((url, data, cb) => cb(413));
    renderHolding((context) => context.saveOrderEntry().catch(() => {}), {
      addNotification,
    });

    await waitFor(() => expect(addNotification).toHaveBeenCalledTimes(1));
    const notification = addNotification.mock.calls[0][0];
    expect(notification.kind).toBe("warning");
    expect(notification.message.props.id).toBe(
      "order.attachment.staged.uploadFailed",
    );
  });
});
