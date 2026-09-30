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

const LAB_NO = "DEV01260000000000552";

const patientOf = (call) => JSON.parse(call[1]).patientProperties;

/** The server's answer to a save: the form echoed back with the stored patient id. */
const savedResponse = (patientPK) => ({
  ok: true,
  clone: () => ({
    json: async () => ({ patientProperties: { patientPK } }),
  }),
});

/**
 * Enters a new patient with one sample type, saves, then saves again once the
 * first save has settled, the way Save followed by Save & Next does.
 */
const EnterAndSaveTwice = ({ save }) => {
  const context = useOrderContext();
  const latest = useRef();
  latest.current = context;
  const started = useRef(false);
  useEffect(() => {
    context.setOrderData((prev) => ({
      ...prev,
      patientUpdateStatus: "ADD",
      patientProperties: {
        ...prev.patientProperties,
        firstName: "Nia",
        lastName: "Qadup",
        nationalId: "QA1407N1",
        patientUpdateStatus: "ADD",
      },
      sampleOrderItems: { ...prev.sampleOrderItems, labNo: LAB_NO },
    }));
    context.setSamples([{ sampleTypeId: "3", tests: [{ id: "1" }] }]);
  }, []);
  useEffect(() => {
    if (
      started.current ||
      !context.samples.some((s) => s.sampleTypeId) ||
      context.orderData?.sampleOrderItems?.labNo !== LAB_NO
    ) {
      return;
    }
    started.current = true;
    latest.current[save]().then(() => {
      setTimeout(() => {
        latest.current[save]().catch(() => {});
      }, 0);
    });
  }, [context.samples, context.orderData]);
  return null;
};

describe("saving an order entered with a new patient more than once", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
  });

  it.each(["saveOrderEntry", "saveOrder"])(
    "%s refers to the stored patient by id on the second save",
    async (save) => {
      postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
        cb(savedResponse("115")),
      );
      getFromOpenElisServer.mockImplementation((url, cb) => {
        if (url.includes("/rest/order/search")) {
          cb({
            id: "172",
            labNumber: LAB_NO,
            samples: [{ sampleItemId: "9", sampleTypeId: "3" }],
            patientProperties: { patientPK: "115" },
          });
        }
      });

      render(
        <OrderProvider workflowType="clinical">
          <EnterAndSaveTwice save={save} />
        </OrderProvider>,
      );

      await waitFor(() =>
        expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
      );
      const [first, second] =
        postToOpenElisServerFullResponse.mock.calls.map(patientOf);
      expect(first.patientUpdateStatus).toBe("ADD");
      expect(first.patientPK).toBeFalsy();
      expect(second.patientPK).toBe("115");
      expect(second.patientUpdateStatus).toBe("NO_ACTION");
      expect(second.nationalId).toBe("QA1407N1");
    },
  );

  it("takes the patient id from the save itself when the order cannot be reloaded", async () => {
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb(savedResponse("115")),
    );
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/rest/order/search")) {
        cb(undefined);
      }
    });

    render(
      <OrderProvider workflowType="clinical">
        <EnterAndSaveTwice save="saveOrderEntry" />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );
    const second = patientOf(postToOpenElisServerFullResponse.mock.calls[1]);
    expect(second.patientPK).toBe("115");
    expect(second.patientUpdateStatus).toBe("NO_ACTION");
  });
});
