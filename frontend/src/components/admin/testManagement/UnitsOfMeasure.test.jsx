import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";

/**
 * Units of Measure is where a lab lists its units and corrects one: the test
 * editor only creates units inline. A blank name never leaves the page, and a
 * name another unit already has is refused on the name field.
 */
const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    putToOpenElisServerJsonResponse: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);

import UnitsOfMeasure from "./UnitsOfMeasure";

let units;

let addNotification;

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification,
        }}
      >
        <MemoryRouter>
          <UnitsOfMeasure />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const modal = () => screen.getByRole("dialog");

describe("Units of Measure", () => {
  beforeEach(() => {
    addNotification = vi.fn();
    units = [
      { id: "1", value: "mg/dL", code: "MGDL", ucumCode: "mg/dL" },
      { id: "2", value: "mmol/L", code: "", ucumCode: "mmol/L" },
    ];
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(units.map((u) => ({ ...u }))),
    );
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    utilsMock.putToOpenElisServerJsonResponse.mockReset();
  });

  it("lists every unit with its codes", () => {
    renderPage();
    const rows = screen.getAllByRole("row");
    expect(
      rows.some(
        (r) =>
          within(r).queryAllByText("mg/dL").length &&
          within(r).queryByText("MGDL"),
      ),
    ).toBe(true);
    expect(
      screen.getAllByText("mmol/L", { selector: "td" }).length,
    ).toBeGreaterThan(0);
  });

  it("adds a unit and lists it", async () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => {
        const unit = { id: "3", value: JSON.parse(body).name };
        units.push(unit);
        callback(unit);
      },
    );
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Add Unit/ }));
    fireEvent.change(within(modal()).getByLabelText(/Unit Of Measure Name/), {
      target: { value: "cells/uL" },
    });
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(utilsMock.postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/uom",
      expect.stringContaining('"name":"cells/uL"'),
      expect.any(Function),
    );
    await waitFor(() =>
      expect(
        screen.getByText("cells/uL", { selector: "td" }),
      ).toBeInTheDocument(),
    );
  });

  it("renames a unit in place", async () => {
    utilsMock.putToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => {
        units[0] = { ...units[0], value: JSON.parse(body).name };
        callback({ ...units[0] });
      },
    );
    renderPage();

    fireEvent.click(screen.getAllByRole("button", { name: /Edit/ })[0]);
    const name = within(modal()).getByLabelText(/Unit Of Measure Name/);
    expect(name).toHaveValue("mg/dL");
    fireEvent.change(name, { target: { value: "mg/dl" } });
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(utilsMock.putToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/uom/1",
      expect.stringContaining('"name":"mg/dl"'),
      expect.any(Function),
    );
    await waitFor(() =>
      expect(screen.getByText("mg/dl", { selector: "td" })).toBeInTheDocument(),
    );
  });

  it("refuses a taken name on the name field and keeps the form open", () => {
    utilsMock.putToOpenElisServerJsonResponse.mockImplementation(
      (url, body, callback) => callback({ status: 409 }),
    );
    renderPage();

    fireEvent.click(screen.getAllByRole("button", { name: /Edit/ })[0]);
    fireEvent.change(within(modal()).getByLabelText(/Unit Of Measure Name/), {
      target: { value: "mmol/L" },
    });
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(
      within(modal()).getByText("Unit of Measure already exists"),
    ).toBeInTheDocument();
    expect(
      screen.getAllByText("mg/dL", { selector: "td" }).length,
    ).toBeGreaterThan(0);
  });

  it("does not send a blank name", () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Add Unit/ }));
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(utilsMock.postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(
      within(modal()).getByText("This field is required"),
    ).toBeInTheDocument();
  });

  it("says so when the list cannot be loaded", () => {
    utilsMock.getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(undefined),
    );

    renderPage();

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["server.error.msg"],
      }),
    );
  });
});
