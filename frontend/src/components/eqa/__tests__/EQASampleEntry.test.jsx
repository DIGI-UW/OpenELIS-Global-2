import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import EQASampleEntry from "../EQASampleEntry";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    getFromOpenElisServerV2: vi.fn(),
  };
});

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("EQASampleEntry", () => {
  const mockSetOrderFormValues = vi.fn();
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
    vi.clearAllMocks();
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

  test("checkbox is unchecked by default", () => {
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={defaultOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    const checkbox = screen.getByRole("checkbox");
    expect(checkbox.checked).toBe(false);
  });

  test("checkbox reflects isEQASample state", () => {
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
    const checkbox = screen.getByRole("checkbox");
    expect(checkbox.checked).toBe(true);
  });

  test("clicking checkbox triggers patient search for NULL placeholder", () => {
    // Replaced inline utils require
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={defaultOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    const checkbox = screen.getByRole("checkbox");
    fireEvent.click(checkbox);
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("patient-search-results"),
      expect.any(Function),
    );
  });

  test("unchecking checkbox resets EQA fields and patient properties", () => {
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
    const checkbox = screen.getByRole("checkbox");
    fireEvent.click(checkbox);
    // setOrderFormValues is called with a functional updater
    expect(mockSetOrderFormValues).toHaveBeenCalledWith(expect.any(Function));
    const updater = mockSetOrderFormValues.mock.calls[0][0];
    const result = updater(eqaOrderFormValues);
    expect(result.sampleOrderItems.isEQASample).toBe(false);
    expect(result.sampleOrderItems.eqaProgramId).toBe("");
    expect(result.sampleOrderItems.eqaPriority).toBe("STANDARD");
  });

  test("only renders checkbox, no additional form fields", () => {
    renderWithIntl(
      <EQASampleEntry
        orderFormValues={defaultOrderFormValues}
        setOrderFormValues={mockSetOrderFormValues}
      />,
    );
    expect(screen.queryByText("EQA Sample Details")).toBeNull();
    expect(screen.queryByText("Priority")).toBeNull();
    expect(screen.queryByText("Provider Sample ID")).toBeNull();
  });
});
