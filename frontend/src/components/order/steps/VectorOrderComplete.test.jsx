import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

const { orderContextValue, locationMock } = vi.hoisted(() => ({
  orderContextValue: {
    orderData: { sampleOrderItems: { labNo: "DEV-NEW" } },
    samples: [],
    resetOrder: () => {},
    labNumber: "DEV-NEW",
    loadOrder: vi.fn(() => Promise.resolve()),
  },
  locationMock: { search: "?labNumber=DEV-DONE" },
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: () => {} }),
  useLocation: () => locationMock,
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
}));

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success" },
}));

vi.mock("../OrderWorkflowLayout", () => ({
  default: ({ children }) => <div>{children}</div>,
}));

import VectorOrderComplete from "./VectorOrderComplete";

// OGC-1657 review: Back to the completion page after New Vector Order looked
// up the unsaved new lab number (a 404) before the order the address names.
describe("VectorOrderComplete", () => {
  it("loads the order its address names, not the unsaved one held", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <VectorOrderComplete />
      </IntlProvider>,
    );

    expect(orderContextValue.loadOrder).toHaveBeenCalledWith("DEV-DONE", false);
    expect(orderContextValue.loadOrder).not.toHaveBeenCalledWith(
      "DEV-NEW",
      false,
    );
  });
});
