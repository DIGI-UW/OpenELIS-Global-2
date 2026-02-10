import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQASampleEntry from "../EQASampleEntry";

jest.mock("../../utils/Utils", () => ({
  ...jest.requireActual("../../utils/Utils"),
  getFromOpenElisServer: jest.fn(),
  getFromOpenElisServerV2: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("EQASampleEntry", () => {
  const mockSetOrderFormValues = jest.fn();
  const defaultOrderFormValues = {
    sampleOrderItems: {
      isEQASample: false,
      eqaProgramId: "",
      eqaProviderOrganizationId: "",
      eqaProviderSampleId: "",
      eqaParticipantId: "",
      eqaDeadline: "",
      eqaPriority: "STANDARD",
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders EQA checkbox", () => {
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={defaultOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    expect(screen.getByText("EQA Sample")).toBeTruthy();
  });

  test("EQA fields are hidden when checkbox is unchecked", () => {
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={defaultOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    expect(screen.queryByText("EQA Sample Details")).toBeNull();
  });

  test("EQA fields appear when checkbox is checked", () => {
    const eqaOrderFormValues = {
      sampleOrderItems: {
        ...defaultOrderFormValues.sampleOrderItems,
        isEQASample: true,
      },
    };
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={eqaOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    expect(screen.getByText("EQA Sample Details")).toBeTruthy();
    expect(screen.getByText("EQA Program")).toBeTruthy();
    expect(screen.getByText("EQA Provider Organization")).toBeTruthy();
    expect(screen.getByText("Testing Deadline")).toBeTruthy();
    expect(screen.getByText("Priority")).toBeTruthy();
  });

  test("clicking checkbox calls setOrderFormValues with isEQASample toggled", () => {
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={defaultOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    const checkbox =
      screen.getByLabelText("EQA Sample") ||
      screen.getByText("EQA Sample").closest("label")?.querySelector("input");
    if (checkbox) {
      fireEvent.click(checkbox);
      expect(mockSetOrderFormValues).toHaveBeenCalled();
    }
  });

  test("priority selection shows all three options", () => {
    const eqaOrderFormValues = {
      sampleOrderItems: {
        ...defaultOrderFormValues.sampleOrderItems,
        isEQASample: true,
      },
    };
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={eqaOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    expect(screen.getByText("Standard")).toBeTruthy();
    expect(screen.getByText("Urgent")).toBeTruthy();
    expect(screen.getByText("Critical")).toBeTruthy();
  });

  test("provider sample ID and participant ID fields render when EQA active", () => {
    const eqaOrderFormValues = {
      sampleOrderItems: {
        ...defaultOrderFormValues.sampleOrderItems,
        isEQASample: true,
      },
    };
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={eqaOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    expect(screen.getByText("Provider Sample ID")).toBeTruthy();
    expect(screen.getByText("Participant ID")).toBeTruthy();
  });
});
