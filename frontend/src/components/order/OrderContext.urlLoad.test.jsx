import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ConfigurationContext } from "../layout/contexts";
import { OrderProvider, useOrderContext } from "./OrderContext";

// OGC-1192 §3 — loading an order from the URL.
//   * The dashboards push `?labNumber=`, the deep links use `?order=`; both must load.
//   * Backend dates are day-first or month-first depending on the site locale, so the
//     load has to wait for the configuration instead of parsing with a default.

const { getFromOpenElisServerMock } = vi.hoisted(() => ({
  getFromOpenElisServerMock: vi.fn(),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: (...args) => getFromOpenElisServerMock(...args),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

vi.mock("./api/sampleTypeRequestApi", () => ({
  createRequestsForSamples: vi.fn(),
  getRequestsBySample: vi.fn(() => Promise.resolve([])),
  convertRequestsToSamples: vi.fn(() => []),
}));

const orderResponse = (labNumber) => ({
  id: "77",
  labNumber,
  samples: [
    { sampleItemId: "1", sampleTypeId: "2", collectionDate: "03/09/2026" },
  ],
  orderData: {},
  sampleOrderItems: { environmentalFields: { workflowType: "environmental" } },
});

const answerServer = (url, callback) => {
  if (url.startsWith("/rest/order/search?labNumber=")) {
    const labNumber = decodeURIComponent(url.split("labNumber=")[1]);
    callback(orderResponse(labNumber));
  } else if (typeof callback === "function") {
    callback({});
  }
};

const Probe = () => {
  const { labNumber, samples } = useOrderContext();
  return (
    <div>
      <span data-testid="lab">{labNumber || ""}</span>
      <span data-testid="date">{samples?.[0]?.collectionDate || ""}</span>
    </div>
  );
};

const tree = (url, configurationProperties) => (
  <ConfigurationContext.Provider value={{ configurationProperties }}>
    <MemoryRouter initialEntries={[url]}>
      <OrderProvider workflowType="environmental">
        <Probe />
      </OrderProvider>
    </MemoryRouter>
  </ConfigurationContext.Provider>
);

const searchCalls = () =>
  getFromOpenElisServerMock.mock.calls
    .map((c) => c[0])
    .filter((url) => url.startsWith("/rest/order/search"));

describe("OrderContext — loading an order from the URL", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation(answerServer);
  });

  it("loads the order the dashboard addresses with ?labNumber=", async () => {
    render(
      tree("/order/environmental/enter?labNumber=DEV01260000000000655", {
        DEFAULT_DATE_LOCALE: "fr-FR",
      }),
    );
    expect(await screen.findByText("DEV01260000000000655")).toBeTruthy();
    expect(searchCalls()).toEqual([
      "/rest/order/search?labNumber=DEV01260000000000655",
    ]);
  });

  it("still loads the order addressed with ?order=", async () => {
    render(
      tree("/order/environmental/enter?order=DEV-2", {
        DEFAULT_DATE_LOCALE: "fr-FR",
      }),
    );
    expect(await screen.findByText("DEV-2")).toBeTruthy();
  });

  it("reads a day-first backend date as the day it was entered", async () => {
    render(
      tree("/order/environmental/enter?labNumber=DEV-3", {
        DEFAULT_DATE_LOCALE: "fr-FR",
      }),
    );
    expect(await screen.findByText("2026-09-03")).toBeTruthy();
  });

  it("reads a month-first backend date under a month-first locale", async () => {
    render(
      tree("/order/environmental/enter?labNumber=DEV-4", {
        DEFAULT_DATE_LOCALE: "en-US",
      }),
    );
    expect(await screen.findByText("2026-03-09")).toBeTruthy();
  });

  it("waits for the site's date locale before loading, then loads once", async () => {
    const { rerender } = render(
      tree("/order/environmental/enter?labNumber=DEV-5", {}),
    );
    expect(searchCalls()).toEqual([]);

    // The anonymous configuration the layout publishes first has no locale.
    rerender(
      tree("/order/environmental/enter?labNumber=DEV-5", {
        currentDateAsText: "07/09/2026",
      }),
    );
    expect(searchCalls()).toEqual([]);

    rerender(
      tree("/order/environmental/enter?labNumber=DEV-5", {
        DEFAULT_DATE_LOCALE: "fr-FR",
      }),
    );
    expect(await screen.findByText("DEV-5")).toBeTruthy();
    expect(searchCalls()).toEqual(["/rest/order/search?labNumber=DEV-5"]);
    expect(screen.getByTestId("date")).toHaveTextContent("2026-09-03");
  });
});
