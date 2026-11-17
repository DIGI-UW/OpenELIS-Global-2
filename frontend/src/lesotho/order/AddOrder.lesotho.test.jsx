// LESOTHO: Add Order shows the Clinical Information section of the MOH request
// form just above Attachments, for a new order only.
import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, test, vi } from "vitest";
import messages from "../../languages/en.json";

vi.mock("../../components/utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));
vi.mock("../../components/layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { restrictFreeTextProviderEntry: "false" },
  }),
}));
vi.mock("../../components/common/CustomNotification", () => ({
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));
vi.mock("../../components/common/AutoComplete", () => ({
  default: () => <div />,
}));
vi.mock("../../components/common/CustomDatePicker", () => ({
  default: () => <div />,
}));
vi.mock("../../components/common/CustomTimePicker", () => ({
  default: () => <div />,
}));
vi.mock("../../components/common/CustomLabNumberInput", () => ({
  default: () => <div />,
}));
vi.mock("../../components/addOrder/OrderResultReporting", () => ({
  default: () => <div />,
}));

import AddOrder from "../../components/addOrder/AddOrder";

const renderAddOrder = (overrides = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AddOrder
        orderFormValues={{
          sampleOrderItems: {
            labNo: "",
            providersList: [],
            paymentOptions: [],
            referringSiteList: [],
            testLocationCodeList: [],
          },
          patientProperties: { gender: "M" },
        }}
        setOrderFormValues={vi.fn()}
        samples={[]}
        error={() => null}
        isModifyOrder={false}
        changed={{}}
        setChanged={vi.fn()}
        stagedAttachments={[]}
        setStagedAttachments={vi.fn()}
        clinicalInformation={{}}
        setClinicalInformation={vi.fn()}
        {...overrides}
      />
    </IntlProvider>,
  );

describe("AddOrder — Lesotho clinical information", () => {
  test("shows the section just above Attachments", () => {
    renderAddOrder();
    const section = screen.getByTestId("order-clinical-info");
    const attachments = screen.getByRole("heading", { name: "Attachments" });
    expect(section).toHaveTextContent("Clinical Information");
    expect(
      section.compareDocumentPosition(attachments) &
        Node.DOCUMENT_POSITION_FOLLOWING,
      "Attachments comes after the clinical information",
    ).toBeTruthy();
  });

  const sampleWith = (...names) => ({
    index: 1,
    sampleTypeId: "3",
    tests: names.map((name, i) => ({ id: String(i + 1), name })),
    panels: [],
    referralItems: [],
    sampleXML: {},
  });

  test("has no form checkboxes, and shows only the shared fields without a linked test", () => {
    renderAddOrder({ samples: [sampleWith("Hemoglobin")] });
    const section = screen.getByTestId("order-clinical-info");
    expect(within(section).queryAllByRole("checkbox")).toHaveLength(0);
    expect(
      within(section).getByLabelText("Clinical history"),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("clinical-info-pcr")).not.toBeInTheDocument();
  });

  test("shows the DNA-PCR fields when HIV DNA PCR is ordered, and saves that form with the section", () => {
    const setClinicalInformation = vi.fn();
    renderAddOrder({
      samples: [sampleWith("Hemoglobin", "HIV DNA PCR")],
      setClinicalInformation,
    });
    expect(screen.getByTestId("clinical-info-pcr")).toBeInTheDocument();
    expect(
      screen.queryByTestId("clinical-info-viral-load"),
    ).not.toBeInTheDocument();
    const update = setClinicalInformation.mock.calls.at(-1)[0];
    expect(update(undefined)).toEqual({ forms: ["DNA_PCR"] });
    expect(update({ clinicalHistory: "x" })).toEqual({
      clinicalHistory: "x",
      forms: ["DNA_PCR"],
    });
  });

  test("shows the blood transfusion request when a cross-match is ordered", () => {
    renderAddOrder({ samples: [sampleWith("ABO Blood Group", "Cross match")] });
    expect(screen.getByTestId("clinical-info-transfusion")).toBeInTheDocument();
    expect(screen.queryByTestId("clinical-info-pcr")).not.toBeInTheDocument();
  });

  test("does not start an empty section for an order with no linked test", () => {
    const setClinicalInformation = vi.fn();
    renderAddOrder({ samples: [sampleWith("CBC")], setClinicalInformation });
    const update = setClinicalInformation.mock.calls.at(-1)[0];
    expect(update(undefined)).toBeUndefined();
    expect(update({ forms: ["DNA_PCR"], pcr: { village: "Ha Abia" } })).toEqual(
      {
        forms: [],
        pcr: { village: "Ha Abia" },
      },
    );
  });

  test("is not shown on Modify Order", () => {
    renderAddOrder({ isModifyOrder: true });
    expect(screen.queryByTestId("order-clinical-info")).not.toBeInTheDocument();
  });
});
