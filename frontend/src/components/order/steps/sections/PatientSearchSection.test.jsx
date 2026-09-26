import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const searchFormProps = vi.hoisted(() => ({ current: null }));
vi.mock("../../../patient/SearchPatientForm", () => ({
  default: (props) => {
    searchFormProps.current = props;
    return (
      <button
        type="button"
        onClick={() =>
          props.getSelectedPatient({
            patientPK: "12",
            firstName: "Mary",
            lastName: "Kila",
            birthDateForDisplay: "01/02/1990",
            gender: "F",
            nationalId: "NID-12",
          })
        }
      >
        shared search pick
      </button>
    );
  },
}));
vi.mock("../../../patient/CreatePatientForm", () => ({
  default: (props) => (
    <div data-testid="create-patient-form">
      {props.selectedPatient?.firstName || "blank"}
    </div>
  ),
}));

import PatientSearchSection from "./PatientSearchSection";

const Host = ({ isReadOnly = false, initial = {}, onChange = () => {} }) => {
  const [orderData, setOrderDataState] = useState({
    patientProperties: {},
    ...initial,
  });
  const setOrderData = (update) =>
    setOrderDataState((prev) => {
      const next = typeof update === "function" ? update(prev) : update;
      onChange(next);
      return next;
    });
  return (
    <IntlProvider locale="en" messages={messages}>
      <PatientSearchSection
        orderData={orderData}
        setOrderData={setOrderData}
        setPhoneValidation={() => {}}
        isReadOnly={isReadOnly}
      />
    </IntlProvider>
  );
};

describe("PatientSearchSection", () => {
  beforeEach(() => {
    searchFormProps.current = null;
  });

  it("searches with the shared patient search form and leaves toasts to the page", () => {
    render(<Host />);

    expect(
      screen.getByRole("button", { name: "shared search pick" }),
    ).toBeVisible();
    expect(searchFormProps.current.idPrefix).toBe("order-patient-search");
    expect(searchFormProps.current.renderNotifications).toBe(false);
  });

  it("puts the picked patient on the order and shows the selection card", async () => {
    const onChange = vi.fn();
    render(<Host onChange={onChange} />);

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "shared search pick" }));

    const order = onChange.mock.calls.at(-1)[0];
    expect(order.patientUpdateStatus).toBe("UPDATE");
    expect(order.patientProperties).toEqual(
      expect.objectContaining({
        patientPK: "12",
        patientUpdateStatus: "UPDATE",
      }),
    );
    expect(screen.getByRole("heading", { name: "Mary Kila" })).toBeVisible();
    expect(
      screen.getByRole("button", { name: "shared search pick", hidden: true }),
    ).not.toBeVisible();
  });

  it("clearing the selection brings the search back and empties the patient", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<Host onChange={onChange} />);
    await user.click(
      screen.getByRole("button", { name: "shared search pick" }),
    );

    await user.click(screen.getByText("Clear"));

    expect(onChange.mock.calls.at(-1)[0].patientProperties.patientPK).toBe("");
    expect(
      screen.getByRole("button", { name: "shared search pick" }),
    ).toBeVisible();
    expect(screen.queryByRole("heading", { name: "Mary Kila" })).toBeNull();
  });

  it("opens New Patient pre-filled with the selected patient", async () => {
    const user = userEvent.setup();
    render(<Host />);
    await user.click(
      screen.getByRole("button", { name: "shared search pick" }),
    );

    await user.click(screen.getByRole("button", { name: /New Patient/ }));

    expect(screen.getByTestId("create-patient-form")).toHaveTextContent("Mary");
  });

  it("a read-only order shows its patient without search or clear", () => {
    render(
      <Host
        isReadOnly
        initial={{
          patientProperties: {
            patientPK: "12",
            firstName: "Mary",
            lastName: "Kila",
          },
        }}
      />,
    );

    expect(screen.getByRole("heading", { name: "Mary Kila" })).toBeVisible();
    expect(screen.queryByText("shared search pick")).toBeNull();
    expect(screen.queryByText("Clear")).toBeNull();
  });
});
