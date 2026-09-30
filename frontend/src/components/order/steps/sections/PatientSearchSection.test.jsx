import React, { useState } from "react";
import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const searchFormProps = vi.hoisted(() => ({ current: null, mounts: 0 }));
vi.mock("../../../patient/SearchPatientForm", async () => {
  const { useEffect } = await import("react");
  return {
    default: (props) => {
      searchFormProps.current = props;
      useEffect(() => {
        searchFormProps.mounts += 1;
      }, []);
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
  };
});
const createFormProps = vi.hoisted(() => ({ history: [] }));
vi.mock("../../../patient/CreatePatientForm", () => ({
  default: (props) => {
    createFormProps.history.push(props.selectedPatient);
    return (
      <div data-testid="create-patient-form">
        {props.selectedPatient?.firstName || "blank"}
      </div>
    );
  },
}));

import PatientSearchSection from "./PatientSearchSection";
import { OrderContext, SaveStatus } from "../../OrderContext";

const Host = ({
  isReadOnly = false,
  initial = {},
  onChange = () => {},
  saveStatus = SaveStatus.SAVED,
  expose = () => {},
}) => {
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
  expose(setOrderData);
  return (
    <IntlProvider locale="en" messages={messages}>
      <OrderContext.Provider value={{ saveStatus }}>
        <PatientSearchSection
          orderData={orderData}
          setOrderData={setOrderData}
          setPhoneValidation={() => {}}
          isReadOnly={isReadOnly}
        />
      </OrderContext.Provider>
    </IntlProvider>
  );
};

describe("PatientSearchSection", () => {
  beforeEach(() => {
    searchFormProps.current = null;
    searchFormProps.mounts = 0;
    createFormProps.history = [];
  });

  it("gives the New Patient form the same blank patient on every render", async () => {
    let setOrderData;
    const user = userEvent.setup();
    render(<Host expose={(set) => (setOrderData = set)} />);
    await user.click(screen.getByRole("button", { name: /New Patient/ }));
    const blank = createFormProps.history.at(-1);

    act(() => {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: { labNo: "DEV01260000000000552" },
      }));
    });

    expect(createFormProps.history.at(-1)).toBe(blank);
    expect(blank.patientPK).toBeUndefined();
  });

  it("shows the patient the order saved on the New Patient tab, and does not follow the form's own writes", async () => {
    let setOrderData;
    const user = userEvent.setup();
    render(<Host expose={(set) => (setOrderData = set)} />);
    await user.click(screen.getByRole("button", { name: /New Patient/ }));
    expect(screen.getByTestId("create-patient-form")).toHaveTextContent(
      "blank",
    );

    act(() => {
      setOrderData((prev) => ({
        ...prev,
        patientProperties: {
          ...prev.patientProperties,
          patientPK: "115",
          firstName: "Nia",
          lastName: "Qadup",
          patientUpdateStatus: "NO_ACTION",
        },
      }));
    });
    expect(screen.getByTestId("create-patient-form")).toHaveTextContent("Nia");
    const held = createFormProps.history.at(-1);

    act(() => {
      setOrderData((prev) => ({
        ...prev,
        patientProperties: {
          ...prev.patientProperties,
          firstName: "Maria",
          patientUpdateStatus: "UPDATE",
        },
      }));
    });

    expect(createFormProps.history.at(-1)).toBe(held);
    expect(screen.getByTestId("create-patient-form")).toHaveTextContent("Nia");
  });

  it("takes the saved record as the patient to compare with once a save completes", async () => {
    let setOrderData;
    const user = userEvent.setup();
    const { rerender } = render(
      <Host
        initial={{
          patientProperties: {
            patientPK: "115",
            firstName: "Nia",
            lastName: "Qadup",
          },
        }}
        saveStatus={SaveStatus.SAVING}
        expose={(set) => (setOrderData = set)}
      />,
    );
    await user.click(screen.getByRole("button", { name: /New Patient/ }));
    act(() => {
      setOrderData((prev) => ({
        ...prev,
        patientProperties: { ...prev.patientProperties, firstName: "Maria" },
      }));
    });
    expect(screen.getByTestId("create-patient-form")).toHaveTextContent("Nia");

    rerender(
      <Host
        initial={{}}
        saveStatus={SaveStatus.SAVED}
        expose={(set) => (setOrderData = set)}
      />,
    );

    expect(screen.getByTestId("create-patient-form")).toHaveTextContent(
      "Maria",
    );
  });

  it("searches with the shared patient search form and leaves toasts to the page", () => {
    render(<Host />);

    expect(
      screen.getByRole("button", { name: "shared search pick" }),
    ).toBeVisible();
    expect(searchFormProps.current.idPrefix).toBe("order-patient-search");
    expect(searchFormProps.current.renderNotifications).toBe(false);
  });

  it("leaves the order's lab number in the URL to the order loader", () => {
    render(<Host />);

    expect(searchFormProps.current.followUrlLabNumber).toBe(false);
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

  it("clearing the selection empties the patient and starts a new search", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<Host onChange={onChange} />);
    await user.click(
      screen.getByRole("button", { name: "shared search pick" }),
    );

    const mountsBeforeClear = searchFormProps.mounts;

    await user.click(screen.getByText("Clear"));

    expect(onChange.mock.calls.at(-1)[0].patientProperties.patientPK).toBe("");
    expect(searchFormProps.mounts).toBe(mountsBeforeClear + 1);
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
