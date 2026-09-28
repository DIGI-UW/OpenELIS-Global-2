/**
 * A patient picked on the order's patient step can finish loading after the
 * user has already moved on. The step then skips its own tab and selection
 * state (it is gone), but the order must still receive the patient.
 */
import React from "react";
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

    expect(setOrderFormValues).toHaveBeenCalledWith(
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

    expect(setOrderFormValues).toHaveBeenCalledWith(
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
});
