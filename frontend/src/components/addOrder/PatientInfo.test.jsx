/**
 * A patient picked on the order's patient step can finish loading after the
 * user has already moved on. The step then skips its own tab and selection
 * state (it is gone), but the order must still receive the patient.
 */
import React, { useState } from "react";
import { vi } from "vitest";
import { act, render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import PatientInfo from "./PatientInfo";

let latestSearchProps;
vi.mock("../patient/SearchPatientForm", () => ({
  default: (props) => {
    latestSearchProps = props;
    return <div />;
  },
}));
vi.mock("../patient/CreatePatientForm", () => ({
  default: () => <div />,
}));
vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

const orderFormValues = {
  patientProperties: { firstName: "", guid: "", lastName: "" },
  sampleOrderItems: {},
};

const renderStep = (setOrderFormValues) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PatientInfo
        orderFormValues={orderFormValues}
        setOrderFormValues={setOrderFormValues}
        error={() => null}
        setPhoneValidation={vi.fn()}
      />
    </IntlProvider>,
  );

const handedToOrder = (setOrderFormValues) => {
  const [update] = setOrderFormValues.mock.calls.at(-1);
  return typeof update === "function" ? update(orderFormValues) : update;
};

let orderState;
const StatefulStep = () => {
  const [values, setValues] = useState({
    ...orderFormValues,
    providerFirstName: "",
    providerLastName: "",
    providerId: "",
  });
  orderState = { values, setValues };
  return (
    <IntlProvider locale="en" messages={messages}>
      <PatientInfo
        orderFormValues={values}
        setOrderFormValues={setValues}
        error={() => null}
        setPhoneValidation={vi.fn()}
      />
    </IntlProvider>
  );
};

describe("PatientInfo", () => {
  it("hands the picked patient to the order", () => {
    const setOrderFormValues = vi.fn();
    renderStep(setOrderFormValues);

    act(() => {
      latestSearchProps.getSelectedPatient({
        id: "4",
        lastName: "EARLIER",
        healthRegion: [],
      });
    });

    expect(handedToOrder(setOrderFormValues)).toEqual(
      expect.objectContaining({
        patientUpdateStatus: "NO_ACTION",
        patientProperties: expect.objectContaining({ lastName: "EARLIER" }),
      }),
    );
  });

  it("still hands a patient that arrives after the step closed to the order, without touching the step", () => {
    const setOrderFormValues = vi.fn();
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = renderStep(setOrderFormValues);
    const { getSelectedPatient } = latestSearchProps;
    act(() => {
      unmount();
    });

    getSelectedPatient({ id: "5", lastName: "FIXDOB", healthRegion: [] });

    expect(handedToOrder(setOrderFormValues)).toEqual(
      expect.objectContaining({
        patientProperties: expect.objectContaining({ lastName: "FIXDOB" }),
      }),
    );
    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes("unmounted component"),
      ),
    ).toBe(false);
    errors.mockRestore();
  });

  it("keeps a requester set on the order while the patient was loading", () => {
    render(<StatefulStep />);
    const deliverPatient = latestSearchProps.getSelectedPatient;

    act(() => {
      orderState.setValues((prev) => ({
        ...prev,
        providerFirstName: "Sarah",
        providerLastName: "Okello",
        providerId: "81",
      }));
    });
    act(() => {
      deliverPatient({ id: "6", lastName: "Nakato", healthRegion: [] });
    });

    expect(orderState.values.providerId).toBe("81");
    expect(orderState.values.providerFirstName).toBe("Sarah");
    expect(orderState.values.patientProperties.lastName).toBe("Nakato");
  });
});
