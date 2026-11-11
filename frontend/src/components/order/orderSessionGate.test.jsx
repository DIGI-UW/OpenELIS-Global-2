import React from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { OrderProvider, useOrderContext } from "./OrderContext";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/clinical/enter", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

const PREFORM = "/rest/SamplePatientEntry";
const ENFORCEMENT = "/rest/sample-acceptance-checklist/enforcement";

const fetched = (url) =>
  getFromOpenElisServer.mock.calls.filter(([called]) => called === url).length;

describe("order entry reference data while the server is unreachable (OGC-1442)", () => {
  let latest;
  const Probe = () => {
    latest = useOrderContext();
    return null;
  };

  const tree = (userSessionDetails) => (
    <UserSessionDetailsContext.Provider value={{ userSessionDetails }}>
      <OrderProvider workflowType="clinical">
        <Probe />
      </OrderProvider>
    </UserSessionDetailsContext.Provider>
  );

  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === PREFORM) {
        cb({
          currentDate: "08/10/2026",
          sampleTypes: [{ id: "2", value: "Serum" }],
          sampleOrderItems: { receivedTime: "09:30" },
        });
      } else if (url === ENFORCEMENT) {
        cb({ clinical: "OFF" });
      }
    });
  });

  it("waits for the session check instead of loading at mount", () => {
    render(tree({}));

    expect(fetched(PREFORM)).toBe(0);
    expect(fetched(ENFORCEMENT)).toBe(0);
    expect(latest.orderData.sampleTypes ?? []).toEqual([]);
  });

  it("loads the lists and the Sample check setting once the server answers signed in", async () => {
    const { rerender } = render(tree({}));

    rerender(tree({ authenticated: true }));

    await waitFor(() =>
      expect(latest.orderData.sampleTypes).toEqual([
        { id: "2", value: "Serum" },
      ]),
    );
    expect(latest.sampleCheckEnabled).toBe(false);
    expect(fetched(PREFORM)).toBe(1);
    expect(fetched(ENFORCEMENT)).toBe(1);
  });

  it("does not load them again when the session is checked again", async () => {
    const { rerender } = render(tree({ authenticated: true }));
    await waitFor(() => expect(fetched(PREFORM)).toBe(1));

    rerender(tree({ authenticated: true, firstName: "Refreshed" }));

    expect(fetched(PREFORM)).toBe(1);
    expect(fetched(ENFORCEMENT)).toBe(1);
  });

  it("does not load them for a signed-out session", () => {
    render(tree({ authenticated: false }));

    expect(fetched(PREFORM)).toBe(0);
    expect(fetched(ENFORCEMENT)).toBe(0);
  });
});
