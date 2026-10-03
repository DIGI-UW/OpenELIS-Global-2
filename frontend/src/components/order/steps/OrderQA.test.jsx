import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

const { orderContextValue, postToOpenElisServerJsonResponse, layoutProps } =
  vi.hoisted(() => ({
    orderContextValue: {
      orderId: "42",
      orderData: {
        patientProperties: { firstName: "Ada", lastName: "Lovelace" },
        sampleOrderItems: {
          labNo: "DEV01260000000000001",
          environmentalFields: { workflowType: "clinical" },
        },
      },
      samples: [],
      resetOrder: vi.fn(),
      labNumber: "DEV01260000000000001",
      markStepComplete: vi.fn(),
      adoptProgress: vi.fn(),
    },
    postToOpenElisServerJsonResponse: vi.fn(),
    layoutProps: vi.fn(),
  }));

vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn() }),
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
  useWorkflowPrefix: () => "/order/clinical",
  SaveStatus: {
    SAVED: "saved",
    SAVING: "saving",
    ERROR: "error",
    UNSAVED: "unsaved",
  },
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { error: "error", success: "success" },
}));

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse,
}));

vi.mock("../api/sampleAcceptanceApi", () => ({
  getAcceptanceGate: vi.fn().mockResolvedValue({ blocked: false }),
  getEnforcement: vi.fn().mockResolvedValue({ clinical: "ADVISORY" }),
}));

vi.mock("./sections/SampleAcceptanceReview", () => ({
  default: () => <div data-testid="sample-acceptance-review" />,
}));

vi.mock("../../nonconform/common/InlineNceForm", () => ({
  default: () => null,
}));

vi.mock("../SaveFailureNotice", () => ({
  default: () => null,
}));

vi.mock("../OrderWorkflowLayout", () => ({
  default: (props) => {
    layoutProps(props);
    return (
      <div>
        {props.children}
        <button onClick={props.onSaveAndNext}>Submit Order</button>
      </div>
    );
  },
}));

import OrderQA from "./OrderQA";

const renderQa = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderQA />
    </IntlProvider>,
  );

describe("OrderQA", () => {
  beforeEach(() => {
    postToOpenElisServerJsonResponse.mockReset();
    layoutProps.mockClear();
  });

  // OGC-1201 J: a second, entirely unguarded "QA Checklist" tile used to
  // render below the acceptance review, with its own state, config load
  // and POST.
  it("reviews acceptance once, with no second checklist", () => {
    renderQa();

    expect(screen.getByTestId("sample-acceptance-review")).toBeInTheDocument();
    expect(screen.queryByText("QA Checklist")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Verify all items before submitting the order"),
    ).not.toBeInTheDocument();
  });

  it("does not load a checklist configuration of its own", () => {
    renderQa();

    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  // OGC-1266 FR-F3: Release for testing records the Sample check and asks
  // the server to release the order in the same call.
  it("releases the order against the lab number on submit", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => callback({ success: true }),
    );
    renderQa();

    await screen.getByText("Submit Order").click();

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/qa-checklist",
      JSON.stringify({ labNumber: "DEV01260000000000001", release: true }),
      expect.any(Function),
    );
    expect(layoutProps).toHaveBeenCalledWith(
      expect.objectContaining({
        primaryLabelId: "order.sampleCheck.release",
        title: "order.step.sampleCheck",
      }),
    );
  });

  // OGC-1266 FR-F4: under Optional acceptance a release with unanswered items
  // needs a reason; the field appears once the server has asked for it and the
  // next release carries it.
  it("asks for a reason when the server requires one, then sends it", async () => {
    postToOpenElisServerJsonResponse.mockImplementationOnce(
      (_url, _body, callback) =>
        callback({ success: false, error: "order.release.reasonRequired" }),
    );
    renderQa();

    await screen.getByText("Submit Order").click();
    const reason = await screen.findByLabelText(
      messages["order.sampleCheck.proceedReason"],
    );

    postToOpenElisServerJsonResponse.mockImplementationOnce(
      (_url, _body, callback) => callback({ success: true }),
    );
    const { default: userEvent } = await import("@testing-library/user-event");
    await userEvent.setup().type(reason, "Checklist not in use here");
    await screen.getByText("Submit Order").click();

    expect(postToOpenElisServerJsonResponse).toHaveBeenLastCalledWith(
      "/rest/qa-checklist",
      JSON.stringify({
        labNumber: "DEV01260000000000001",
        release: true,
        releaseNote: "Checklist not in use here",
      }),
      expect.any(Function),
    );
  });

  // Found in review: the environmental and vector lanes have no Prepare
  // Samples step, so their Sample check must not wait for one.
  it("lets an environmental order release from Entered", () => {
    orderContextValue.progress = { status: "ENTERED" };
    orderContextValue.orderData.sampleOrderItems.environmentalFields.workflowType =
      "environmental";
    renderQa();

    expect(layoutProps).toHaveBeenLastCalledWith(
      expect.objectContaining({ canProceed: true, toContinue: [] }),
    );
    orderContextValue.orderData.sampleOrderItems.environmentalFields.workflowType =
      "clinical";
    orderContextValue.progress = undefined;
  });

  // The Sample check cannot release an order whose samples are not prepared.
  it("holds the release while Prepare Samples is incomplete", () => {
    orderContextValue.progress = { status: "ENTERED" };
    renderQa();

    expect(layoutProps).toHaveBeenLastCalledWith(
      expect.objectContaining({
        canProceed: false,
        toContinue: [
          expect.objectContaining({
            id: "order.sampleCheck.disabled.incomplete",
          }),
        ],
      }),
    );
    orderContextValue.progress = undefined;
  });
});
