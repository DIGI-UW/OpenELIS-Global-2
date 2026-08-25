import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

const { orderContextValue, programSectionProps } = vi.hoisted(() => ({
  orderContextValue: {
    orderData: {
      patientProperties: {},
      sampleOrderItems: {
        environmentalFields: { workflowType: "clinical" },
      },
    },
    setOrderData: vi.fn(),
    samples: [
      {
        sampleTypeId: "blood",
        tests: [
          {
            id: "culture-test",
            cultureWorkflowType: "BACTERIOLOGY",
          },
        ],
      },
    ],
    setSamples: vi.fn(),
    labNumber: "",
    saveOrderEntry: vi.fn(),
    markStepComplete: vi.fn(),
    isReadOnly: false,
    isEditMode: false,
    resetOrder: vi.fn(),
  },
  programSectionProps: vi.fn(),
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn(), replace: vi.fn() }),
  useLocation: () => ({ pathname: "/order/clinical/enter", search: "" }),
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
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
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../OrderWorkflowLayout", () => ({
  default: ({ children }) => <div>{children}</div>,
}));

vi.mock("./sections/PatientSearchSection", () => ({
  default: () => null,
}));

vi.mock("./sections/ProgramSection", () => ({
  default: (props) => {
    programSectionProps(props);
    return <div data-testid="program-section" />;
  },
}));

vi.mock("./sections/ClinicalInfoSection", () => ({
  default: () => null,
}));

vi.mock("./sections/RequesterSection", () => ({
  default: () => null,
}));

vi.mock("./sections/SampleTestSection", () => ({
  default: () => null,
}));

import ClinicalOrderEnter from "./ClinicalOrderEnter";

describe("ClinicalOrderEnter", () => {
  beforeEach(() => {
    programSectionProps.mockClear();
  });

  it("shares selected samples with the Program section", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(screen.getByTestId("program-section")).toBeInTheDocument();
    expect(programSectionProps).toHaveBeenCalledWith(
      expect.objectContaining({ samples: orderContextValue.samples }),
    );
  });
});
