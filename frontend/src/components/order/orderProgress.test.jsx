import React, { useEffect, useRef } from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { OrderProvider, useOrderContext } from "./OrderContext";
import {
  postToOpenElisServerFullResponse,
  getFromOpenElisServer,
} from "../utils/Utils";

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/clinical/collect", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

vi.mock("./api/sampleAcceptanceApi", () => ({
  getEnforcement: vi.fn().mockResolvedValue({ clinical: "OFF" }),
}));

const LAB_NO = "DEV01260000000000660";

const savedResponse = () => ({
  ok: true,
  clone: () => ({ json: async () => ({}) }),
});

/**
 * Stages a sample with storage notes and the skip-storage decision, then
 * saves the Prepare Samples step as complete, the way Save and next does.
 */
const PrepareAndSave = ({ onContext }) => {
  const context = useOrderContext();
  const latest = useRef();
  latest.current = context;
  onContext(context);
  const started = useRef(false);
  useEffect(() => {
    context.setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: LAB_NO },
    }));
    context.setSamples([
      {
        sampleTypeId: "3",
        tests: [{ id: "1" }],
        storageNotes: "Keep cold & <dry> 'now'",
      },
    ]);
    context.stageStorageSkipped(true);
  }, []);
  useEffect(() => {
    if (
      started.current ||
      !context.samples.some((s) => s.sampleTypeId) ||
      context.orderData?.sampleOrderItems?.labNo !== LAB_NO ||
      context.orderData?.sampleOrderItems?.storageSkipped !== true
    ) {
      return;
    }
    started.current = true;
    latest.current.saveOrder(false, false, null, false, "SAMPLES_PREPARED");
  }, [context.samples, context.orderData]);
  return null;
};

describe("saving the Prepare Samples step", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
  });

  it("sends the step, the storage decision and the notes with the save, then adopts the recorded progress", async () => {
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb(savedResponse()),
    );
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/rest/order/search")) {
        cb({
          id: "172",
          labNumber: LAB_NO,
          samples: [{ sampleItemId: "9", sampleTypeId: "3" }],
          progressStatus: "SAMPLES_PREPARED",
          complete: true,
          progress: {
            enteredAt: "30/09/2026 10:15",
            preparedAt: "30/09/2026 11:02",
          },
        });
      }
    });
    let context;

    render(
      <OrderProvider workflowType="clinical">
        <PrepareAndSave onContext={(value) => (context = value)} />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    const payload = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls[0][1],
    );
    expect(payload.sampleOrderItems.progressStep).toBe("SAMPLES_PREPARED");
    expect(payload.sampleOrderItems.storageSkipped).toBe(true);
    expect(payload.sampleXML).toContain(
      "storageNotes='Keep cold &amp; &lt;dry> &apos;now&apos;'",
    );

    await waitFor(() =>
      expect(context.progress.status).toBe("SAMPLES_PREPARED"),
    );
    expect(context.progress.complete).toBe(true);
    expect(context.progress.preparedAt).toBe("30/09/2026 11:02");
    expect(context.sampleCheckEnabled).toBe(false);
    expect(context.acceptanceMode).toBe("OFF");
  });

  it("keeps the step out of the save when the step is not complete", async () => {
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb(savedResponse()),
    );
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/rest/order/search")) {
        cb({
          id: "172",
          labNumber: LAB_NO,
          samples: [{ sampleItemId: "9", sampleTypeId: "3" }],
        });
      }
    });
    let context;
    const Plain = ({ onContext }) => {
      const value = useOrderContext();
      onContext(value);
      const started = useRef(false);
      useEffect(() => {
        value.setOrderData((prev) => ({
          ...prev,
          sampleOrderItems: { ...prev.sampleOrderItems, labNo: LAB_NO },
        }));
        value.setSamples([{ sampleTypeId: "3", tests: [] }]);
      }, []);
      useEffect(() => {
        if (
          started.current ||
          value.orderData?.sampleOrderItems?.labNo !== LAB_NO ||
          !value.samples.some((s) => s.sampleTypeId)
        ) {
          return;
        }
        started.current = true;
        value.saveOrder();
      }, [value.orderData, value.samples]);
      return null;
    };

    render(
      <OrderProvider workflowType="clinical">
        <Plain onContext={(value) => (context = value)} />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    const payload = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls[0][1],
    );
    expect(payload.sampleOrderItems.progressStep).toBe("");
    await waitFor(() => expect(context.orderId).toBe("172"));
    expect(context.progress.status).toBeNull();
  });
});
