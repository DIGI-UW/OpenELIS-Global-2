import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

// ---------------------------------------------------------------------------
// OGC-1191 — Edit Order regressions.
//
//  B. loadOrderValues must NOT blank the loaded referringSiteName. It mutated
//     the payload's referringSiteName to "", leaving a required field empty in
//     form state while the AutoComplete still displayed it from referringSiteId.
//  D. The validation errors that gate Submit must be rendered — they were
//     computed and never shown, so a required field the user could not see left
//     Submit permanently disabled with no on-screen explanation.
//
// ModifyOrder is a self-contained container: it fetches the order on mount via
// getFromOpenElisServer(loadOrderValues). We mock the server layer (capturing
// that callback so the test supplies a payload) and the heavy children, then
// assert on the real ModifyOrder behaviour.
// ---------------------------------------------------------------------------

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  },
}));

vi.mock("../utils/Utils", () => utilsMock);

vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../addOrder/AddOrder", () => ({ default: () => <div /> }));
vi.mock("./EditSample", () => ({ default: () => <div /> }));
vi.mock("./EditOrderEntryAdditionalQuestions", () => ({
  default: () => <div />,
}));
vi.mock("../addOrder/OrderSuccessMessage", () => ({ default: () => <div /> }));
vi.mock("../common/PatientHeader", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));
vi.mock("../addOrder/Index", () => ({
  sampleObject: { tests: [], sampleXML: {} },
}));

import ModifyOrder from "./ModifyOrder";

/** The order payload the SampleEdit GET returns, with a real referring site. */
const orderPayload = () => ({
  accessionNumber: "DEV01260000000000519",
  sampleOrderItems: {
    labNo: "DEV01260000000000519",
    referringSiteName: "QA_AUTO Referring Clinic",
    referringSiteId: "42",
    providerLastName: "",
    providerFirstName: "",
  },
});

/** Mount ModifyOrder; return a getter for the loadOrderValues callback. */
const mountAndCaptureLoad = () => {
  let loadCb;
  utilsMock.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (typeof url === "string" && url.includes("/rest/SampleEdit")) {
      loadCb = cb;
    }
  });
  render(
    <IntlProvider locale="en" messages={messages}>
      <ModifyOrder />
    </IntlProvider>,
  );
  return () => loadCb;
};

describe("ModifyOrder — loaded referring site is preserved (OGC-1191 B)", () => {
  beforeEach(() => utilsMock.getFromOpenElisServer.mockReset());

  test("loadOrderValues does not blank the payload's referringSiteName", () => {
    const getLoad = mountAndCaptureLoad();
    const loadOrderValues = getLoad();
    expect(
      loadOrderValues,
      "mount fetch should register a callback",
    ).toBeTypeOf("function");

    const payload = orderPayload();
    loadOrderValues(payload);

    // The old code mutated this to "" in place; the required field must survive.
    expect(payload.sampleOrderItems.referringSiteName).toBe(
      "QA_AUTO Referring Clinic",
    );
  });
});

describe("ModifyOrder — gating validation errors are shown (OGC-1191 D)", () => {
  beforeEach(() => utilsMock.getFromOpenElisServer.mockReset());

  test("on the order step, a missing required field renders an error and disables Submit", async () => {
    const getLoad = mountAndCaptureLoad();
    // An order missing the required provider name — the state that used to
    // disable Submit with nothing on screen.
    getLoad()(orderPayload());

    // Advance Program -> Sample -> Order.
    fireEvent.click(screen.getByRole("button", { name: /next/i }));
    fireEvent.click(screen.getByRole("button", { name: /next/i }));

    const errorNodes = await screen.findAllByText(
      /Requester Last Name is required/i,
    );
    expect(errorNodes.length).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: /submit/i })).toBeDisabled();
  });
});

// ---------------------------------------------------------------------------
// OGC-1191 — after a successful reassignment the specimen carries its new
// accession, so the page URL must switch to the new Lab Number (the old one no
// longer exists). reflectReassignmentOnSuccess also moves sampleOrderItems.labNo
// to the new number, which is what OrderSuccessMessage prints labels from.
// ---------------------------------------------------------------------------
describe("ModifyOrder — successful reassignment switches to the new Lab Number (OGC-1191)", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerFullResponse.mockReset();
  });

  const validReassignedPayload = () => ({
    accessionNumber: "OLD01260000000000001",
    newAccessionNumber: "NEW01260000000000009",
    sampleOrderItems: {
      labNo: "OLD01260000000000001",
      referringSiteName: "QA_AUTO Referring Clinic",
      referringSiteId: "42",
      providerLastName: "Doe",
      providerFirstName: "Jane",
    },
  });

  test("on a confirmed save, the URL accessionNumber becomes the new number", async () => {
    window.history.replaceState(
      null,
      "",
      "/ModifyOrder?accessionNumber=OLD01260000000000001",
    );

    let submitCallback;
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, cb) => {
        submitCallback = cb;
      },
    );
    const getLoad = mountAndCaptureLoad();
    getLoad()(validReassignedPayload());

    // Advance Program -> Sample -> Order.
    fireEvent.click(screen.getByRole("button", { name: /next/i }));
    fireEvent.click(screen.getByRole("button", { name: /next/i }));

    // A valid order keeps Submit enabled; submit and let the backend confirm.
    fireEvent.click(screen.getByRole("button", { name: /submit/i }));
    expect(submitCallback, "submit should POST to SampleEdit").toBeTypeOf(
      "function",
    );

    await act(async () => {
      await submitCallback({ ok: true });
    });

    expect(window.location.search).toContain(
      "accessionNumber=NEW01260000000000009",
    );
    expect(window.location.search).not.toContain("OLD01260000000000001");
  });
});
