import React from "react";
import { render } from "@testing-library/react";
import { Formik } from "formik";
import { vi } from "vitest";
import PatientFormObserver from "./PatientFormObserver";

/**
 * OGC-1443: once the order's patient is saved the patient form reopens on that
 * patient and restates it into the order. That restatement is not an edit, so
 * it goes through the host's hydrating writer and Prepare Samples opens without
 * "Unsaved changes". A real edit still goes through the ordinary writer.
 */

const savedPatient = {
  patientPK: "9001000",
  firstName: "Ida",
  lastName: "Followup",
  nationalId: "QA1443",
  gender: "F",
  birthDateForDisplay: "05/03/1988",
};

const renderObserver = (values: Record<string, unknown>) => {
  const setOrderFormValues = vi.fn();
  const hydrateOrderFormValues = vi.fn();
  render(
    <Formik initialValues={values} onSubmit={() => {}}>
      <PatientFormObserver
        setOrderFormValues={setOrderFormValues}
        hydrateOrderFormValues={hydrateOrderFormValues}
        formAction="UPDATE"
        selectedPatient={savedPatient}
      />
    </Formik>,
  );
  return { setOrderFormValues, hydrateOrderFormValues };
};

describe("PatientFormObserver writes (OGC-1443)", () => {
  it("restates the patient the order holds without marking the order changed", () => {
    const { setOrderFormValues, hydrateOrderFormValues } =
      renderObserver(savedPatient);

    expect(hydrateOrderFormValues).toHaveBeenCalledTimes(1);
    expect(setOrderFormValues).not.toHaveBeenCalled();
  });

  it("marks the order changed when the form differs from that patient", () => {
    const { setOrderFormValues, hydrateOrderFormValues } = renderObserver({
      ...savedPatient,
      lastName: "Followup-Edited",
    });

    expect(setOrderFormValues).toHaveBeenCalledTimes(1);
    expect(hydrateOrderFormValues).not.toHaveBeenCalled();
  });
});
