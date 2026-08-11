import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

// Stable mocks so each useHistory()/useContext() call returns the same fns
// across renders — these tests assert against history.push.
const { historyMock, notificationMock, orderContextValue } = vi.hoisted(() => ({
  historyMock: { push: () => {}, replace: () => {} },
  notificationMock: {
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  },
  orderContextValue: {
    // Shape must satisfy canSave: lab number + a sampling site + every
    // sample-typed row carrying at least one test.
    orderData: {
      sampleOrderItems: {
        labNo: "ENV-100",
        environmentalFields: {
          workflowType: "environmental",
          samplingSiteName: "Well 3",
        },
      },
      patientProperties: {},
    },
    setOrderData: () => {},
    samples: [{ sampleTypeId: "5", tests: [{ id: "1" }], panels: [] }],
    setSamples: () => {},
    labNumber: "ENV-100",
    saveOrder: () => Promise.resolve({ samples: [] }),
    markStepComplete: () => {},
    isReadOnly: false,
    isEditMode: false,
    resetOrder: () => {},
  },
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => historyMock,
  // ?order= present so the mount guard treats this as an existing order and
  // skips resetOrder(), matching a user who has already saved a draft.
  useLocation: () => ({
    search: "?order=ENV-100",
    pathname: "/order/environmental/enter",
  }),
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext(notificationMock),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../nonconform/common/InlineNceForm", () => ({
  default: () => null,
}));

// The workflow chrome owns the Save & Next button; expose just that control so
// the test drives the same handler the real footer wires up.
vi.mock("../OrderWorkflowLayout", () => ({
  default: ({ children, onSaveAndNext, canProceed }) => (
    <div>
      <button
        type="button"
        disabled={!canProceed}
        onClick={onSaveAndNext}
        data-testid="save-and-next"
      >
        Save and Next
      </button>
      {children}
    </div>
  ),
}));

vi.mock("./sections/VectorSection", () => ({ default: () => null }));
vi.mock("./sections/CollectionConditionsSection", () => ({
  default: () => null,
}));
vi.mock("./sections/ProgramSection", () => ({ default: () => null }));
vi.mock("./sections/RequesterSection", () => ({ default: () => null }));
vi.mock("./sections/SampleTestSection", () => ({ default: () => null }));
vi.mock("./sections/ComplianceStandardsSection", () => ({
  default: () => null,
}));

import EnvironmentalOrderEnter from "./EnvironmentalOrderEnter";

const renderWithIntl = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <EnvironmentalOrderEnter />
    </IntlProvider>,
  );

describe("EnvironmentalOrderEnter Save & Next navigation", () => {
  beforeEach(() => {
    historyMock.push = vi.fn();
    orderContextValue.markStepComplete = vi.fn();
    orderContextValue.saveOrder = vi.fn(() => Promise.resolve({ samples: [] }));
  });

  test("advances to the Collect step, not straight to Label", async () => {
    renderWithIntl();

    fireEvent.click(await screen.findByTestId("save-and-next"));

    await waitFor(() => expect(historyMock.push).toHaveBeenCalled());
    const target = historyMock.push.mock.calls[0][0];
    expect(target).toContain("/order/environmental/collect");
    expect(target).not.toContain("/order/environmental/label");
  });

  test("carries the lab number so the Collect step rehydrates the order", async () => {
    renderWithIntl();

    fireEvent.click(await screen.findByTestId("save-and-next"));

    await waitFor(() =>
      expect(historyMock.push).toHaveBeenCalledWith(
        "/order/environmental/collect?order=ENV-100",
      ),
    );
    expect(orderContextValue.markStepComplete).toHaveBeenCalledWith("enter");
  });
});
