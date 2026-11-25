import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, useHistory } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { ConfigurationContext } from "../layout/Layout";
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

const DirtyProbe = () => {
  const { labNumber, isDirty, hydrateOrderData, hydrateSamples, setOrderData } =
    useOrderContext();
  return (
    <div>
      <span data-testid="lab">{labNumber || ""}</span>
      <span data-testid="dirty">{String(isDirty)}</span>
      <button
        onClick={() => {
          hydrateOrderData((prev) => ({ ...prev, hydrated: true }));
          hydrateSamples((prev) => prev);
        }}
      >
        hydrate
      </button>
      <button onClick={() => setOrderData((prev) => ({ ...prev }))}>
        edit
      </button>
    </div>
  );
};

describe("OrderContext — values filled in on load are not edits (OGC-1192)", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation(answerServer);
  });

  it("keeps a loaded order clean after hydrating, and dirty after an edit", async () => {
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
      >
        <MemoryRouter
          initialEntries={["/order/environmental/enter?labNumber=DEV-6"]}
        >
          <OrderProvider workflowType="environmental">
            <DirtyProbe />
          </OrderProvider>
        </MemoryRouter>
      </ConfigurationContext.Provider>,
    );
    expect(await screen.findByText("DEV-6")).toBeTruthy();
    expect(screen.getByTestId("dirty")).toHaveTextContent("false");

    fireEvent.click(screen.getByText("hydrate"));
    expect(screen.getByTestId("dirty")).toHaveTextContent("false");

    fireEvent.click(screen.getByText("edit"));
    expect(screen.getByTestId("dirty")).toHaveTextContent("true");
  });
});

const DatesProbe = () => {
  const { labNumber, orderData } = useOrderContext();
  const items = orderData?.sampleOrderItems || {};
  return (
    <div>
      <span data-testid="lab">{labNumber || ""}</span>
      <span data-testid="received">
        {`${items.receivedDateForDisplay || ""} ${items.receivedTime || ""}`}
      </span>
      <span data-testid="requested">{items.requestDate || ""}</span>
    </div>
  );
};

describe("OrderContext — form defaults arriving after the order (OGC-1192)", () => {
  let answerDefaults;

  beforeEach(() => {
    answerDefaults = null;
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url === "/rest/SamplePatientEntry") {
        answerDefaults = () =>
          callback({
            currentDate: "28/09/2026",
            sampleOrderItems: { receivedTime: "20:45" },
          });
      } else if (url.startsWith("/rest/order/search?labNumber=")) {
        callback({
          ...orderResponse("DEV-7"),
          sampleOrderItems: {
            environmentalFields: { workflowType: "environmental" },
            receivedDateForDisplay: "27/09/2026",
            receivedTime: "09:15",
            requestDate: "26/09/2026",
          },
        });
      } else if (typeof callback === "function") {
        callback({});
      }
    });
  });

  it("keeps the loaded order's received and request dates", async () => {
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
      >
        <MemoryRouter
          initialEntries={["/order/environmental/enter?labNumber=DEV-7"]}
        >
          <OrderProvider workflowType="environmental">
            <DatesProbe />
          </OrderProvider>
        </MemoryRouter>
      </ConfigurationContext.Provider>,
    );
    expect(await screen.findByText("DEV-7")).toBeTruthy();

    act(() => answerDefaults());

    expect(screen.getByTestId("received")).toHaveTextContent(
      "27/09/2026 09:15",
    );
    expect(screen.getByTestId("requested")).toHaveTextContent("26/09/2026");
  });
});

const DefaultsProbe = () => {
  const {
    labNumber,
    isDirty,
    samples,
    fillSampleDefaults,
    updateSampleCollectionDetails,
  } = useOrderContext();
  return (
    <div>
      <span data-testid="lab">{labNumber || ""}</span>
      <span data-testid="dirty">{String(isDirty)}</span>
      <span data-testid="time">{samples?.[0]?.collectionTime || ""}</span>
      <button
        onClick={() => fillSampleDefaults(0, { collectionTime: "08:30" })}
      >
        defaults
      </button>
      <button
        onClick={() =>
          updateSampleCollectionDetails(0, { collectionTime: "09:45" })
        }
      >
        type
      </button>
    </div>
  );
};

describe("OrderContext — a step's own defaults are not edits (OGC-1443)", () => {
  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation(answerServer);
  });

  it("fills a sample's default time without Unsaved changes, and marks a typed time dirty", async () => {
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
      >
        <MemoryRouter
          initialEntries={["/order/environmental/collect?order=DEV-7"]}
        >
          <OrderProvider workflowType="environmental">
            <DefaultsProbe />
          </OrderProvider>
        </MemoryRouter>
      </ConfigurationContext.Provider>,
    );
    expect(await screen.findByText("DEV-7")).toBeTruthy();

    fireEvent.click(screen.getByText("defaults"));
    expect(screen.getByTestId("time")).toHaveTextContent("08:30");
    expect(screen.getByTestId("dirty")).toHaveTextContent("false");

    fireEvent.click(screen.getByText("type"));
    expect(screen.getByTestId("time")).toHaveTextContent("09:45");
    expect(screen.getByTestId("dirty")).toHaveTextContent("true");
  });
});

// OGC-1657 walk: loadOrder cleared isLoading before it set the order, and React
// 17 renders after each update outside an event handler, so a step page saw a
// render with no order and no load running and sent the user back to Enter
// Order. Once a load starts, no render may show it finished without the order.
describe("OrderContext — the load ends with the order in place", () => {
  it("never shows the load over before the order it loaded", async () => {
    const answers = [];
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/order/search?labNumber=")) {
        answers.push(() => callback(orderResponse("DEV-6")));
      } else if (typeof callback === "function") {
        callback({});
      }
    });
    const seen = [];
    const Recorder = () => {
      const { isLoading, labNumber } = useOrderContext();
      seen.push({ isLoading, labNumber });
      return <span data-testid="lab">{labNumber || ""}</span>;
    };
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
      >
        <MemoryRouter
          initialEntries={["/order/environmental/label?order=DEV-6"]}
        >
          <OrderProvider workflowType="environmental">
            <Recorder />
          </OrderProvider>
        </MemoryRouter>
      </ConfigurationContext.Provider>,
    );
    const started = seen.findIndex((state) => state.isLoading);
    expect(started).toBeGreaterThanOrEqual(0);

    answers[0]();

    expect(await screen.findByText("DEV-6")).toBeTruthy();
    expect(
      seen
        .slice(started)
        .filter((state) => !state.isLoading && !state.labNumber),
    ).toEqual([]);
  });
});

// OGC-1657 review: the address was read once per provider mount, so Back to
// Label & Store at ?order= after the dashboard had been reloaded left an
// empty step that never loaded.
describe("OrderContext — an address that changes after the provider mounted", () => {
  let history;
  const Navigator = () => {
    history = useHistory();
    return null;
  };
  const Editor = () => {
    const { setOrderData } = useOrderContext();
    return (
      <button
        type="button"
        onClick={() =>
          setOrderData((prev) => ({
            ...prev,
            sampleOrderItems: { ...prev?.sampleOrderItems, priority: "STAT" },
          }))
        }
      >
        edit
      </button>
    );
  };
  const mountAt = (url) =>
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "fr-FR" } }}
      >
        <MemoryRouter initialEntries={[url]}>
          <OrderProvider workflowType="environmental">
            <Navigator />
            <Editor />
            <Probe />
          </OrderProvider>
        </MemoryRouter>
      </ConfigurationContext.Provider>,
    );

  beforeEach(() => {
    getFromOpenElisServerMock.mockReset();
    getFromOpenElisServerMock.mockImplementation(answerServer);
  });

  it("loads the order a later address names", async () => {
    mountAt("/order/environmental");
    expect(searchCalls()).toEqual([]);

    act(() => history.push("/order/environmental/label?order=DEV-8"));

    expect(await screen.findByText("DEV-8")).toBeTruthy();
  });

  it("moves to the order Back returns to", async () => {
    mountAt("/order/environmental/label?order=DEV-9");
    expect(await screen.findByText("DEV-9")).toBeTruthy();

    act(() => history.push("/order/environmental/label?order=DEV-10"));

    expect(await screen.findByText("DEV-10")).toBeTruthy();
    expect(searchCalls()).toEqual([
      "/rest/order/search?labNumber=DEV-9",
      "/rest/order/search?labNumber=DEV-10",
    ]);
  });

  it("keeps an order with unsaved edits", async () => {
    mountAt("/order/environmental/label?order=DEV-11");
    expect(await screen.findByText("DEV-11")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "edit" }));

    act(() => history.push("/order/environmental/label?order=DEV-12"));

    expect(screen.getByTestId("lab").textContent).toBe("DEV-11");
    expect(searchCalls()).toEqual(["/rest/order/search?labNumber=DEV-11"]);
  });
});
