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

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const payloadOf = (call) => JSON.parse(call[1]);
const clientKeysIn = (sampleXML) =>
  [...sampleXML.matchAll(/clientKey='([^']*)'/g)].map((match) => match[1]);

/** Saves once the samples are in place, then saves again after it settles. */
const SaveTwice = ({ samples: requested, save, sameTick = false }) => {
  const context = useOrderContext();
  const latestSave = useRef();
  latestSave.current = context[save];
  const started = useRef(false);
  useEffect(() => {
    context.setSamples(requested);
  }, []);
  useEffect(() => {
    if (started.current || !context.samples.some((s) => s.sampleTypeId)) {
      return;
    }
    started.current = true;
    if (sameTick) {
      latestSave.current().catch(() => {});
      latestSave.current().catch(() => {});
      return;
    }
    latestSave
      .current()
      .catch(() => {})
      .finally(() => {
        setTimeout(() => {
          latestSave.current().catch(() => {});
        }, 0);
      });
  }, [context.samples]);
  return null;
};

describe("a save repeated after its reply was lost", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb(undefined),
    );
  });

  it("sends the same order key on the retry of a new order", async () => {
    render(
      <OrderProvider workflowType="clinical">
        <SaveTwice samples={[{ sampleTypeId: "3" }]} save="saveOrderEntry" />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );
    const [first, retry] = postToOpenElisServerFullResponse.mock.calls.map(
      (call) => payloadOf(call).sampleOrderItems,
    );
    expect(first.orderKey).toMatch(UUID_PATTERN);
    expect(retry.orderKey).toBe(first.orderKey);
    expect(first.sampleId).toBeFalsy();
  });

  it("sends the same sample keys on the retry of a collect save", async () => {
    render(
      <OrderProvider workflowType="clinical">
        <SaveTwice
          samples={[{ sampleTypeId: "3" }, { sampleTypeId: "7" }]}
          save="saveOrder"
        />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );
    const [first, retry] = postToOpenElisServerFullResponse.mock.calls.map(
      (call) => clientKeysIn(payloadOf(call).sampleXML),
    );
    expect(first).toHaveLength(2);
    first.forEach((key) => expect(key).toMatch(UUID_PATTERN));
    expect(new Set(first).size).toBe(2);
    expect(retry).toEqual(first);
  });
});

describe("one action, one write", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
  });

  it("sends a single request when save is triggered twice before it settles", async () => {
    postToOpenElisServerFullResponse.mockImplementation(() => {});
    render(
      <OrderProvider workflowType="clinical">
        <SaveTwice
          samples={[{ sampleTypeId: "3" }]}
          save="saveOrder"
          sameTick
        />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
  });
});
