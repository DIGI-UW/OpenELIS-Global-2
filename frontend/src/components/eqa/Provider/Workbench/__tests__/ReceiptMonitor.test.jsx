import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../../languages/en.json";
import ReceiptMonitor from "../ReceiptMonitor";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../../utils/Utils";

vi.mock("../../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

const RECEIPTS = [
  {
    organizationId: 550,
    organizationName: "Mbeya Regional Lab",
    boxCode: "EQA-C9-550",
    shipmentId: 31,
    estimatedDeliveryDate: "2026-08-10 00:00:00.0",
    receivedDate: null,
    repeatOfShipmentId: null,
    receivedTempC: null,
    integrityOk: null,
    overdue: true,
    receiptStatus: "OVERDUE",
  },
  {
    organizationId: 551,
    organizationName: "Iringa District Lab",
    boxCode: "EQA-C9-551",
    shipmentId: 32,
    estimatedDeliveryDate: "2026-08-10 00:00:00.0",
    receivedDate: "2026-08-09 10:00:00.0",
    repeatOfShipmentId: null,
    receivedTempC: 6.5,
    integrityOk: false,
    integrityNotes: "Cold chain broken",
    overdue: false,
    receiptStatus: "EXCEPTION",
  },
];

const SCORES = [
  {
    organizationId: 551,
    organizationName: "Iringa District Lab",
    resultCount: 3,
    acceptableCount: 2,
    questionableCount: 0,
    unacceptableCount: 1,
    worstZScore: 3.4,
  },
];

const jsonResponse = (ok, body) => ({ ok, json: () => Promise.resolve(body) });

const renderTab = (cycleStatus = "SUBMISSIONS_OPEN") =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <ReceiptMonitor
          cycleId="9"
          cycleStatus={cycleStatus}
          onChanged={vi.fn()}
          onNotice={vi.fn()}
        />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("ReceiptMonitor", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(url.endsWith("/scores") ? SCORES : RECEIPTS),
    );
  });

  it("tags an overdue shipment and a damaged arrival differently", async () => {
    renderTab();

    expect(await screen.findByText("Overdue")).toBeInTheDocument();
    expect(screen.getByText("Arrived damaged")).toBeInTheDocument();
    expect(screen.getByText(/Cold chain broken/)).toBeInTheDocument();
    // The delivered row carries its scores; the undelivered one has none yet.
    expect(screen.getByText("1 unacceptable of 3")).toBeInTheDocument();
  });

  it("records a delivery for one participant", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { receiptStatus: "DELIVERED" })),
    );
    renderTab();

    await screen.findByText("Mbeya Regional Lab");
    fireEvent.click(screen.getByRole("button", { name: "Mark received" }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/cycles/9/receipts/550/delivered",
        "{}",
        expect.any(Function),
      ),
    );
  });

  it("sends a repeat with the override note the reserve may require", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { boxCode: "EQA-C9-550-R1" })),
    );
    renderTab();

    await screen.findByText("Mbeya Regional Lab");
    fireEvent.click(screen.getAllByRole("button", { name: "Send repeat" })[0]);
    fireEvent.change(screen.getByLabelText("Override note"), {
      target: { value: "Courier lost the box" },
    });
    fireEvent.click(
      screen.getAllByRole("button", { name: "Send repeat" }).slice(-1)[0],
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/cycles/9/receipts/550/repeat",
        JSON.stringify({ overrideNote: "Courier lost the box" }),
        expect.any(Function),
      ),
    );
  });

  it("reports a refused FHIR return as an error, not as sent", async () => {
    // The endpoint answers 200 with success:false when the store refuses it.
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(
        jsonResponse(true, {
          success: false,
          error: "HTTP 422 : invalid reference",
        }),
      ),
    );
    const onNotice = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter>
          <ReceiptMonitor
            cycleId="9"
            cycleStatus="SCORED"
            onChanged={vi.fn()}
            onNotice={onNotice}
          />
        </MemoryRouter>
      </IntlProvider>,
    );

    await screen.findByText("Iringa District Lab");
    fireEvent.click(screen.getByRole("button", { name: "Send scores" }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith(
        expect.objectContaining({ kind: "error" }),
      ),
    );
  });

  it("offers scoring only while the cycle is open for it", async () => {
    renderTab("SHIPPED");

    await screen.findByText("Mbeya Regional Lab");
    expect(
      screen.queryByRole("button", { name: "Score cycle" }),
    ).not.toBeInTheDocument();
  });

  it("scores the cycle from the tab", async () => {
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { cycleStatus: "SCORED", followupCount: 1 })),
    );
    renderTab();

    await screen.findByText("Mbeya Regional Lab");
    fireEvent.click(screen.getByRole("button", { name: "Score cycle" }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/cycles/9/score",
        "{}",
        expect.any(Function),
      ),
    );
  });
});
