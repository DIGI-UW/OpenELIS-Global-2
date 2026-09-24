import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../../languages/en.json";
import ReceiptMonitor from "../ReceiptMonitor";
import UserSessionDetailsContext from "../../../../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  patchToOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
} from "../../../../utils/Utils";

vi.mock("../../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    patchToOpenElisServerFullResponse: vi.fn(),
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

const PROVIDER_AND_MANAGER = [
  "qa.view.eqa",
  "qa.eqa.provider",
  "qa.manage.eqa",
];

const renderTab = (
  cycleStatus = "SUBMISSIONS_OPEN",
  { permissions = PROVIDER_AND_MANAGER, onNotice = vi.fn() } = {},
) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: true, roles: [], permissions },
          errorLoadingSessionDetails: false,
          isCheckingLogin: () => false,
          logout: vi.fn(),
        }}
      >
        <MemoryRouter>
          <ReceiptMonitor
            cycleId="9"
            cycleStatus={cycleStatus}
            onChanged={vi.fn()}
            onNotice={onNotice}
          />
        </MemoryRouter>
      </UserSessionDetailsContext.Provider>
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

  // Dispatching from the sibling Shipments tab moves the cycle, and this tab
  // kept reporting the pre-dispatch state until the page was reloaded.
  it("refetches when the cycle moves under it", async () => {
    const { rerender } = renderTab("SHIPPED");
    await screen.findByText("Overdue");
    const readsAfterFirstLoad = getFromOpenElisServer.mock.calls.length;

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter>
          <ReceiptMonitor
            cycleId="9"
            cycleStatus="SUBMISSIONS_OPEN"
            onChanged={vi.fn()}
            onNotice={vi.fn()}
          />
        </MemoryRouter>
      </IntlProvider>,
    );

    expect(getFromOpenElisServer.mock.calls.length).toBeGreaterThan(
      readsAfterFirstLoad,
    );
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
    renderTab("SCORED", { onNotice });

    await screen.findByText("Iringa District Lab");
    fireEvent.click(screen.getByRole("button", { name: "Send scores" }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith(
        expect.objectContaining({ kind: "error" }),
      ),
    );
  });

  // F-27: as a persona with the read umbrella and the participant grant only,
  // every write action on this tab rendered and was enabled, and pressing one
  // answered 403 with the single word "Forbidden".
  it("offers no write action to a persona without either grant", async () => {
    renderTab("SUBMISSIONS_OPEN", {
      permissions: ["qa.view.eqa", "qa.eqa.participant"],
    });

    await screen.findByText("Mbeya Regional Lab");
    for (const action of [
      "Mark received",
      "Send repeat",
      "Enter results",
      "Send scores",
      "Score cycle",
    ]) {
      expect(screen.queryByRole("button", { name: action })).toBeNull();
    }
    // Read value is kept, and the missing actions are explained rather than
    // silently absent -- the pattern the provider scheme board already uses.
    expect(
      screen.getByText("Read-only view of receipts and scores"),
    ).toBeInTheDocument();
    expect(screen.getByText("Iringa District Lab")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Scores CSV" }),
    ).toBeInTheDocument();
  });

  it("offers the provider actions but not scoring to a provider without the manage grant", async () => {
    renderTab("SUBMISSIONS_OPEN", {
      permissions: ["qa.view.eqa", "qa.eqa.provider"],
    });

    await screen.findByText("Mbeya Regional Lab");
    expect(
      screen.getAllByRole("button", { name: "Send repeat" }).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByRole("button", { name: "Score cycle" })).toBeNull();
    expect(
      screen.queryByText("Read-only view of receipts and scores"),
    ).toBeNull();
  });

  it("names the grant when a refusal still reaches the user", async () => {
    // Reachable when a grant is revoked mid-session, which is exactly when
    // "Forbidden" explains least.
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb({ ok: false, status: 403, json: () => Promise.resolve({}) }),
    );
    const onNotice = vi.fn();
    renderTab("SCORED", { onNotice });

    await screen.findByText("Iringa District Lab");
    fireEvent.click(screen.getByRole("button", { name: "Send scores" }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith({
        kind: "error",
        text: "This action needs the qa.eqa.provider permission, which your account does not have. Ask an administrator for it.",
      }),
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

  it("says which results the score left without a verdict", async () => {
    // An analyte with no sealed target on a cycle below the peer floor reaches
    // SCORED with performance_status NULL. The score still succeeds; the point is
    // that the operator is told rather than left with a silent null.
    postToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(
        jsonResponse(true, {
          cycleStatus: "SCORED",
          followupCount: 0,
          unjudgedCount: 3,
          unjudgedTests: ["Haemoglobin"],
        }),
      ),
    );
    const onNotice = vi.fn();
    renderTab("SUBMISSIONS_OPEN", { onNotice });

    await screen.findByText("Mbeya Regional Lab");
    fireEvent.click(screen.getByRole("button", { name: "Score cycle" }));

    await waitFor(() =>
      expect(onNotice).toHaveBeenCalledWith({
        kind: "success",
        text: "Cycle scored, but 3 result(s) got no verdict, on Haemoglobin. Those analytes carry no sealed target and the cycle has too few peers to place them against.",
      }),
    );
  });

  it("closes a scored cycle through the transition endpoint, with the reason", async () => {
    patchToOpenElisServerFullResponse.mockImplementation((_url, _body, cb) =>
      cb(jsonResponse(true, { status: "CLOSED" })),
    );
    renderTab("SCORED");

    await screen.findByText("Iringa District Lab");
    fireEvent.click(screen.getByRole("button", { name: "Close cycle" }));
    fireEvent.change(screen.getByLabelText("Reason"), {
      target: { value: "Round finished, scores returned" },
    });
    // Two buttons carry this label once the modal is open: the trigger behind it
    // and the modal's own primary. Scope to the dialog.
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: "Close cycle",
      }),
    );

    await waitFor(() =>
      expect(patchToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/eqa/cycles/9/transition",
        JSON.stringify({
          newState: "CLOSED",
          stateMachine: "PROVIDER",
          reason: "Round finished, scores returned",
        }),
        expect.any(Function),
      ),
    );
  });

  it("offers no close action until the cycle is scored", async () => {
    renderTab("SUBMISSIONS_OPEN");

    await screen.findByText("Mbeya Regional Lab");
    expect(
      screen.queryByRole("button", { name: "Close cycle" }),
    ).not.toBeInTheDocument();
  });

  test("Enter results keys a participant's reported values and posts them per test", async () => {
    renderTab();
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/results?organizationId=550")) {
        cb({
          cycleId: 9,
          organizationId: 550,
          tests: [
            {
              testId: 7,
              testName: "HIV viral load",
              analyteName: "HIV VL",
              reported: null,
            },
            {
              testId: 8,
              testName: "HIV serology",
              analyteName: "HIV Ab",
              reported: "Reactive",
            },
          ],
        });
      } else if (url.includes("/receipts")) cb(RECEIPTS);
      else if (url.includes("/scores")) cb(SCORES);
    });

    const mbeya = (await screen.findByText("Mbeya Regional Lab")).closest("tr");
    fireEvent.click(
      within(mbeya).getByRole("button", { name: "Enter results" }),
    );

    expect(
      await screen.findByText("Results from Mbeya Regional Lab"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("HIV serology")).toHaveValue("Reactive");
    fireEvent.change(screen.getByLabelText("HIV viral load"), {
      target: { value: "250" },
    });
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb(jsonResponse(true, { tests: [] })),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save results" }));

    const [url, body] = postToOpenElisServerFullResponse.mock.calls.at(-1);
    expect(url).toBe("/rest/eqa/cycles/9/results");
    expect(JSON.parse(body)).toEqual({
      organizationId: 550,
      results: [
        { testId: 7, value: "250" },
        { testId: 8, value: "Reactive" },
      ],
    });
  });

  test("Import CSV posts the pasted export bundle and reports what did not map", async () => {
    renderTab();
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/results?organizationId=550")) {
        cb({
          tests: [{ testId: 7, testName: "HIV viral load", reported: null }],
        });
      } else if (url.includes("/receipts")) cb(RECEIPTS);
      else if (url.includes("/scores")) cb(SCORES);
    });
    const mbeya = (await screen.findByText("Mbeya Regional Lab")).closest("tr");
    fireEvent.click(
      within(mbeya).getByRole("button", { name: "Enter results" }),
    );
    await screen.findByText("Results from Mbeya Regional Lab");

    fireEvent.change(
      screen.getByLabelText("Or paste the participant's export bundle (CSV)"),
      {
        target: { value: "analyte_name,result_value\nHIV VL,250\nGhost,1" },
      },
    );
    postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb(
        jsonResponse(true, {
          imported: 1,
          errors: ["Row 3: no test in this scheme reports 'Ghost'"],
          tests: [{ testId: 7, testName: "HIV viral load", reported: 250 }],
        }),
      ),
    );
    fireEvent.click(screen.getByRole("button", { name: "Import CSV" }));

    const [url, body] = postToOpenElisServerFullResponse.mock.calls.at(-1);
    expect(url).toBe("/rest/eqa/cycles/9/results/import");
    expect(JSON.parse(body)).toEqual({
      organizationId: 550,
      csv: "analyte_name,result_value\nHIV VL,250\nGhost,1",
    });
    expect(await screen.findByText("1 values imported.")).toBeInTheDocument();
    expect(
      screen.getByText("Row 3: no test in this scheme reports 'Ghost'"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("HIV viral load")).toHaveValue("250");
  });
});
