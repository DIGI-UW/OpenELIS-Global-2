import React from "react";
import { act, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";

import PaymentPanel from "../PaymentPanel";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

const SAMPLE_LOOKUP = "/rest/sample/all-by-accession/ACC-1";
const SUMMARY = "/rest/payments/sample/42/summary";
const PAYMENTS = "/rest/payments/sample/42";

const summary = (overrides = {}) => ({
  sampleId: "42",
  subtotalRupees: "500.00",
  gstRupees: "90.00",
  amountDueRupees: "590.00",
  status: "NOT_REQUESTED",
  paid: false,
  shortUrl: null,
  unpricedTestNames: [],
  ...overrides,
});

/** Answers each GET from the given map; unknown URLs get undefined. */
const serveGets = (responses) => {
  getFromOpenElisServer.mockImplementation((url, cb) => cb(responses[url]));
};

const renderPanel = ({
  roles = ["Reception"],
  accessionNumber = "ACC-1",
  watchForAutoLink = false,
} = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles } }}
      >
        <PaymentPanel
          accessionNumber={accessionNumber}
          watchForAutoLink={watchForAutoLink}
        />
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

describe("PaymentPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders nothing when the lab number has no sample", async () => {
    serveGets({});
    const { container } = renderPanel();
    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        SAMPLE_LOOKUP,
        expect.any(Function),
      ),
    );
    expect(container).toBeEmptyDOMElement();
  });

  test("renders nothing when the user may not see payments (403)", async () => {
    // ControllerSetup's error body: its numeric status must not be read as a
    // payment status.
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: { timestamp: "2026-11-17", status: 403, error: "Forbidden" },
      [PAYMENTS]: { timestamp: "2026-11-17", status: 403, error: "Forbidden" },
    });
    const { container } = renderPanel();
    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        SUMMARY,
        expect.any(Function),
      ),
    );
    expect(container).toBeEmptyDOMElement();
  });

  test("shows subtotal, GST and total for a priced order", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary(),
      [PAYMENTS]: [],
    });
    renderPanel();
    expect(await screen.findByText("₹500.00")).toBeInTheDocument();
    expect(screen.getByText("₹90.00")).toBeInTheDocument();
    expect(screen.getByText("₹590.00")).toBeInTheDocument();
    expect(screen.getByText("Not requested")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Create payment link" }),
    ).toBeEnabled();
  });

  test("warns about unpriced tests and does not offer a link", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary({
        subtotalRupees: null,
        gstRupees: null,
        amountDueRupees: null,
        unpricedTestNames: ["HIV Viral Load", "CD4"],
      }),
      [PAYMENTS]: [],
    });
    renderPanel();
    expect(await screen.findByText(/HIV Viral Load, CD4/)).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Create payment link" }),
    ).not.toBeInTheDocument();
  });

  test("shows the pay link while a payment is pending", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary({
        status: "PENDING",
        shortUrl: "https://rzp.io/i/abc123",
      }),
      [PAYMENTS]: [],
    });
    renderPanel();
    const link = await screen.findByRole("link", {
      name: "https://rzp.io/i/abc123",
    });
    expect(link).toHaveAttribute("href", "https://rzp.io/i/abc123");
    expect(screen.getByText("Awaiting payment")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Create payment link" }),
    ).not.toBeInTheDocument();
  });

  test("creating a link posts for the sample and reloads the summary", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary(),
      [PAYMENTS]: [],
    });
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({ status: 200 }),
    );
    renderPanel();
    await userEvent.click(
      await screen.findByRole("button", { name: "Create payment link" }),
    );
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/payments/sample/42/link",
      null,
      expect.any(Function),
    );
    await waitFor(() =>
      expect(
        getFromOpenElisServer.mock.calls.filter(([url]) => url === SUMMARY),
      ).toHaveLength(2),
    );
  });

  test("explains when online payment is switched off (503)", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary(),
      [PAYMENTS]: [],
    });
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({ status: 503 }),
    );
    renderPanel();
    await userEvent.click(
      await screen.findByRole("button", { name: "Create payment link" }),
    );
    expect(
      await screen.findByText(/Online payment is not enabled/),
    ).toBeInTheDocument();
  });

  test("hides Refund from users who are not administrators", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary({ status: "PAID", paid: true }),
      [PAYMENTS]: [{ id: "p1", status: "PAID" }],
    });
    renderPanel({ roles: ["Reception"] });
    expect(await screen.findByText("Paid")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Refund$/ }),
    ).not.toBeInTheDocument();
  });

  test("administrator refunds the paid payment after confirming", async () => {
    serveGets({
      [SAMPLE_LOOKUP]: [{ id: 42 }],
      [SUMMARY]: summary({ status: "PAID", paid: true }),
      [PAYMENTS]: [
        { id: "p0", status: "CANCELLED" },
        { id: "p1", status: "PAID" },
      ],
    });
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({ status: 202 }),
    );
    renderPanel({ roles: ["Global Administrator"] });
    await userEvent.click(
      await screen.findByRole("button", { name: /Refund$/ }),
    );
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();

    await userEvent.click(
      screen.getByRole("button", { name: /Refund ₹590.00$/ }),
    );
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/payments/p1/refund",
      null,
      expect.any(Function),
    );
    expect(await screen.findByText(/Refund requested/)).toBeInTheDocument();
  });

  describe("watching for the background payment link", () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });
    afterEach(() => {
      vi.useRealTimers();
    });

    const summaryCalls = () =>
      getFromOpenElisServer.mock.calls.filter(([url]) => url === SUMMARY)
        .length;

    test("hides Create while waiting and shows the link once it appears", () => {
      let current = summary();
      getFromOpenElisServer.mockImplementation((url, cb) =>
        cb(
          {
            [SAMPLE_LOOKUP]: [{ id: 42 }],
            [SUMMARY]: current,
            [PAYMENTS]: [],
          }[url],
        ),
      );
      renderPanel({ watchForAutoLink: true });

      expect(screen.getByText(/Creating the payment link/)).toBeInTheDocument();
      expect(
        screen.queryByRole("button", { name: "Create payment link" }),
      ).not.toBeInTheDocument();

      current = summary({ status: "PENDING", shortUrl: "https://rzp.io/i/q" });
      act(() => {
        vi.advanceTimersByTime(2000);
      });

      expect(
        screen.getByRole("link", { name: "https://rzp.io/i/q" }),
      ).toBeInTheDocument();
      const callsWhenFound = summaryCalls();
      act(() => {
        vi.advanceTimersByTime(20000);
      });
      expect(summaryCalls()).toBe(callsWhenFound);
    });

    test("gives up after 10 seconds and offers Create instead", () => {
      serveGets({
        [SAMPLE_LOOKUP]: [{ id: 42 }],
        [SUMMARY]: summary(),
        [PAYMENTS]: [],
      });
      renderPanel({ watchForAutoLink: true });

      // One act per 2 s tick: React schedules the next check after each render.
      for (let tick = 0; tick < 5; tick++) {
        act(() => {
          vi.advanceTimersByTime(2000);
        });
      }

      expect(
        screen.getByRole("button", { name: "Create payment link" }),
      ).toBeInTheDocument();
      expect(screen.queryByText(/Creating the payment link/)).toBeNull();
      // First load plus five re-checks, then no more.
      expect(summaryCalls()).toBe(6);
      act(() => {
        vi.advanceTimersByTime(20000);
      });
      expect(summaryCalls()).toBe(6);
    });

    test("does not wait when the order cannot be billed", () => {
      serveGets({
        [SAMPLE_LOOKUP]: [{ id: 42 }],
        [SUMMARY]: summary({
          subtotalRupees: null,
          gstRupees: null,
          amountDueRupees: null,
          unpricedTestNames: ["CD4"],
        }),
        [PAYMENTS]: [],
      });
      renderPanel({ watchForAutoLink: true });

      expect(screen.getByText(/CD4/)).toBeInTheDocument();
      expect(screen.queryByText(/Creating the payment link/)).toBeNull();
      act(() => {
        vi.advanceTimersByTime(10000);
      });
      expect(summaryCalls()).toBe(1);
    });

    test("does not poll unless asked to", () => {
      serveGets({
        [SAMPLE_LOOKUP]: [{ id: 42 }],
        [SUMMARY]: summary(),
        [PAYMENTS]: [],
      });
      renderPanel();

      act(() => {
        vi.advanceTimersByTime(10000);
      });
      expect(summaryCalls()).toBe(1);
    });
  });
});
