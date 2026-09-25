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
  useLocation: () => ({ pathname: "/order/clinical/enter", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

const respond = (status, body = {}) => {
  postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
    cb({ ok: status < 300, status, json: async () => body }),
  );
};

const Probe = ({ onRender }) => {
  const context = useOrderContext();
  onRender(context);
  return null;
};

const SubmitOnMount = ({ samples: requested, save = "requested" }) => {
  const { samples, setSamples, saveOrderEntry, saveOrder } = useOrderContext();
  const submitted = useRef(false);
  useEffect(() => {
    setSamples(requested);
  }, []);
  useEffect(() => {
    if (!submitted.current && samples.some((sample) => sample.sampleTypeId)) {
      submitted.current = true;
      const submit = save === "collected" ? saveOrder : saveOrderEntry;
      submit().catch(() => {});
    }
  }, [samples]);
  return null;
};

const submittedPayload = () =>
  JSON.parse(postToOpenElisServerFullResponse.mock.calls.at(-1)[1]);

describe("what the requested stage submits", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
    respond(200);
  });

  it("sends the requested specimens with the order itself", async () => {
    render(
      <OrderProvider workflowType="clinical">
        <SubmitOnMount
          samples={[
            { sampleTypeId: "3", quantity: "2.5", quantityUnit: "9" },
            { sampleTypeId: "7" },
          ]}
        />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(submittedPayload().requestedSampleTypes).toEqual([
      expect.objectContaining({ typeOfSampleId: "3" }),
      expect.objectContaining({ typeOfSampleId: "7" }),
    ]);
  });

  it("leaves the requested specimens out of a save made from a later stage", async () => {
    render(
      <OrderProvider workflowType="clinical">
        <SubmitOnMount
          samples={[{ sampleTypeId: "3", quantity: "2.5" }]}
          save="collected"
        />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(submittedPayload()).not.toHaveProperty("requestedSampleTypes");
  });

  it("surfaces a blocked save as correctable field errors", async () => {
    respond(400, {
      error: "sampleOrderItems.labNo: must not be blank",
      fieldErrors: [
        {
          field: "sampleOrderItems.labNo",
          defaultMessage: "must not be blank",
        },
        {
          field: "sampleOrderItems.receivedDateForDisplay",
          defaultMessage: "invalid date",
        },
      ],
    });
    let latest;
    render(
      <OrderProvider workflowType="clinical">
        <SubmitOnMount samples={[{ sampleTypeId: "3" }]} />
        <Probe onRender={(context) => (latest = context)} />
      </OrderProvider>,
    );

    await waitFor(() => expect(latest.saveStatus).toBe("error"));
    expect(latest.fieldErrors).toEqual({
      "sampleOrderItems.labNo": "must not be blank",
      "sampleOrderItems.receivedDateForDisplay": "invalid date",
    });
    expect(latest.error).toBe("sampleOrderItems.labNo: must not be blank");
  });

  it("clears the field errors once a save succeeds", async () => {
    respond(400, {
      error: "blocked",
      fieldErrors: [{ field: "sampleOrderItems.labNo", defaultMessage: "x" }],
    });
    let latest;
    render(
      <OrderProvider workflowType="clinical">
        <SubmitOnMount samples={[{ sampleTypeId: "3" }]} />
        <Probe onRender={(context) => (latest = context)} />
      </OrderProvider>,
    );
    await waitFor(() => expect(latest.saveStatus).toBe("error"));

    respond(200);
    await latest.saveOrderEntry();

    await waitFor(() => expect(latest.fieldErrors).toEqual({}));
    expect(latest.saveStatus).toBe("saved");
  });

  it("saves the same order again as an update, never as a second order", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/order/search")) {
        cb({ id: "S1", labNumber: "LAB-9" });
      }
    });
    let latest;
    render(
      <OrderProvider workflowType="clinical">
        <Probe onRender={(context) => (latest = context)} />
      </OrderProvider>,
    );
    latest.setSamples([{ sampleTypeId: "3" }]);
    latest.setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: "LAB-9" },
    }));
    await waitFor(() =>
      expect(latest.orderData.sampleOrderItems.labNo).toBe("LAB-9"),
    );

    await latest.saveOrderEntry();
    await waitFor(() => expect(latest.labNumber).toBe("LAB-9"));
    const firstSave = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls[0][1],
    );
    expect(firstSave.sampleOrderItems.sampleId || "").toBe("");

    await latest.saveOrderEntry();

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2);
    expect(submittedPayload().sampleOrderItems.sampleId).toBe("S1");
  });

  // OGC-1201 R: a 200 was taken as proof the order had been written. The
  // frontend never checked that anything came back, so a save that persisted
  // no specimens still reported success.
  it("refuses to call a save successful when no specimen was stored", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/order/search")) {
        cb({ id: "S1", labNumber: "LAB-9", samples: [] });
      }
    });
    let latest;
    render(
      <OrderProvider workflowType="clinical">
        <Probe onRender={(context) => (latest = context)} />
      </OrderProvider>,
    );
    latest.setSamples([{ sampleTypeId: "3" }]);
    latest.setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: "LAB-9" },
    }));
    await waitFor(() =>
      expect(latest.orderData.sampleOrderItems.labNo).toBe("LAB-9"),
    );

    await expect(latest.saveOrder()).rejects.toThrow("order.save.notPersisted");
    await waitFor(() => expect(latest.saveStatus).toBe("error"));
    expect(latest.error).toBe("order.save.notPersisted");
  });

  it("accepts a save whose specimens come back from the server", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/order/search")) {
        cb({
          id: "S1",
          labNumber: "LAB-9",
          samples: [{ sampleItemId: "SI1", typeOfSampleId: "3" }],
        });
      } else {
        cb([]);
      }
    });
    let latest;
    render(
      <OrderProvider workflowType="clinical">
        <Probe onRender={(context) => (latest = context)} />
      </OrderProvider>,
    );
    latest.setSamples([{ sampleTypeId: "3" }]);
    latest.setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: "LAB-9" },
    }));
    await waitFor(() =>
      expect(latest.orderData.sampleOrderItems.labNo).toBe("LAB-9"),
    );

    await expect(latest.saveOrder()).resolves.toEqual(
      expect.objectContaining({ success: true }),
    );
    expect(latest.saveStatus).toBe("saved");
  });

  // The entry step legitimately stores only sample_type_requests, but the
  // order itself still has to exist.
  it("refuses an entry save the server never turned into an order", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/order/search")) {
        cb({ labNumber: "LAB-9" });
      }
    });
    let latest;
    render(
      <OrderProvider workflowType="clinical">
        <Probe onRender={(context) => (latest = context)} />
      </OrderProvider>,
    );
    latest.setSamples([{ sampleTypeId: "3" }]);
    latest.setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: "LAB-9" },
    }));
    await waitFor(() =>
      expect(latest.orderData.sampleOrderItems.labNo).toBe("LAB-9"),
    );

    await expect(latest.saveOrderEntry()).rejects.toThrow(
      "order.save.notPersisted",
    );
    expect(latest.saveStatus).toBe("error");
  });
});
