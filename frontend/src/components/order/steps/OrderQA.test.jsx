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

vi.mock("../../layout/contexts", () => ({
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

  // The dashboard reads the presence of this record to mark the order
  // complete, so submission must still write it.
  it("records the QA review against the lab number on submit", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => callback({ success: true }),
    );
    renderQa();

    await screen.getByText("Submit Order").click();

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/qa-checklist",
      JSON.stringify({ labNumber: "DEV01260000000000001" }),
      expect.any(Function),
    );
  });
});
