import React, { useState } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

vi.mock("../../../patient/SearchPatientForm", () => ({
  default: () => <div data-testid="search-patient-form" />,
}));
vi.mock("../../../patient/CreatePatientForm", () => ({
  default: () => <div data-testid="create-patient-form" />,
}));

const { serverGet, postFull } = vi.hoisted(() => ({
  serverGet: vi.fn(),
  postFull: vi.fn(),
}));
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback) => callback(serverGet(url)),
  postToOpenElisServerFullResponse: (...args) => postFull(...args),
}));

import PatientSearchSection from "./PatientSearchSection";
import { OrderContext, SaveStatus } from "../../OrderContext";

const MATCH = {
  id: "77",
  kind: "patient",
  firstName: "Kila",
  lastName: "Morea",
  birthDate: "1985-03-02",
  identifier: "PNG12345",
  matchedOn: ["name"],
};

let latest;
let setOrder;
const Host = () => {
  const [orderData, setOrderDataState] = useState({ patientProperties: {} });
  latest = orderData;
  setOrder = setOrderDataState;
  return (
    <IntlProvider locale="en" messages={messages}>
      <OrderContext.Provider value={{ saveStatus: SaveStatus.SAVED }}>
        <PatientSearchSection
          orderData={orderData}
          setOrderData={setOrderDataState}
          setPhoneValidation={() => {}}
          isReadOnly={false}
        />
      </OrderContext.Provider>
    </IntlProvider>
  );
};

const enterNewPatient = () => {
  render(<Host />);
  fireEvent.click(screen.getByRole("button", { name: "New Patient" }));
  act(() =>
    setOrder((previous) => ({
      ...previous,
      patientProperties: {
        firstName: "Morea",
        lastName: "Kila",
        birthDateForDisplay: "02/03/1985",
        nationalId: "",
      },
    })),
  );
};

describe("Create patient runs the possible-match check (OGC-1424, FR-B6a)", () => {
  beforeEach(() => {
    serverGet.mockReset();
    postFull.mockReset();
  });

  it("is offered once a name is entered", () => {
    render(<Host />);
    fireEvent.click(screen.getByRole("button", { name: "New Patient" }));

    expect(screen.getByTestId("order-create-patient")).toBeDisabled();
    act(() =>
      setOrder((previous) => ({
        ...previous,
        patientProperties: { lastName: "Kila" },
      })),
    );
    expect(screen.getByTestId("order-create-patient")).toBeEnabled();
  });

  it("with no similar patient on record, the new patient is created with the order", async () => {
    serverGet.mockReturnValue({ matches: [] });
    enterNewPatient();

    fireEvent.click(screen.getByTestId("order-create-patient"));

    expect(
      await screen.findByTestId("order-new-patient-confirmed"),
    ).toHaveTextContent("The new patient is created when the order is saved.");
    expect(serverGet).toHaveBeenCalledWith(
      "/rest/possible-matches/patient?firstName=Morea&lastName=Kila&birthDate=02%2F03%2F1985",
    );
    expect(screen.getByTestId("possible-matches-dialog")).not.toHaveClass(
      "is-visible",
    );
  });

  it("Use this one puts the existing patient on the order", async () => {
    serverGet.mockImplementation((url) =>
      url.startsWith("/rest/possible-matches/")
        ? { matches: [MATCH] }
        : {
            patientPK: "77",
            firstName: "Kila",
            lastName: "Morea",
            birthDateForDisplay: "02/03/1985",
          },
    );
    enterNewPatient();

    fireEvent.click(screen.getByTestId("order-create-patient"));
    const row = await screen.findByTestId("possible-match-77");
    expect(row).toHaveTextContent("Morea, Kila");
    expect(row).toHaveTextContent("Matched on: name");

    fireEvent.click(screen.getByTestId("possible-match-use-77"));

    await waitFor(() => expect(latest.patientProperties.patientPK).toBe("77"));
    expect(serverGet).toHaveBeenCalledWith(
      "/rest/patient-details?patientID=77",
    );
    expect(postFull).not.toHaveBeenCalled();
  });

  it("Create new anyway is confirmed and recorded before the new patient is kept", async () => {
    serverGet.mockReturnValue({ matches: [MATCH] });
    postFull.mockImplementation((url, body, callback) =>
      callback({ ok: true, status: 201, json: () => Promise.resolve({}) }),
    );
    enterNewPatient();

    fireEvent.click(screen.getByTestId("order-create-patient"));
    await screen.findByTestId("possible-match-77");
    fireEvent.click(screen.getByText("Create new anyway"));
    fireEvent.click(screen.getByText("Create new record"));

    expect(
      await screen.findByTestId("order-new-patient-confirmed"),
    ).toBeInTheDocument();
    const [url, body] = postFull.mock.calls[0];
    expect(url).toBe("/rest/possible-matches/patient/override");
    expect(JSON.parse(body).matches).toEqual([
      { id: "77", matchedOn: ["name"] },
    ]);
    expect(latest.patientProperties.patientPK).toBeUndefined();
  });

  it("a second check after Create new anyway shows the matches again, not the confirmation", async () => {
    serverGet.mockReturnValue({ matches: [MATCH] });
    postFull.mockImplementation((url, body, callback) =>
      callback({ ok: true, status: 201, json: () => Promise.resolve({}) }),
    );
    enterNewPatient();

    fireEvent.click(screen.getByTestId("order-create-patient"));
    await screen.findByTestId("possible-match-77");
    fireEvent.click(screen.getByText("Create new anyway"));
    fireEvent.click(screen.getByText("Create new record"));
    await screen.findByTestId("order-new-patient-confirmed");

    fireEvent.click(screen.getByTestId("order-create-patient"));

    expect(await screen.findByTestId("possible-match-77")).toBeVisible();
    expect(screen.getByTestId("possible-match-use-77")).toBeInTheDocument();
    expect(screen.queryByTestId("possible-matches-confirm")).toBeNull();
  });
});
