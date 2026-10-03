import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";

/**
 * Manage Methods is the only method page now that the create and rename
 * screens are gone: a method added here must show in the list without a
 * page reload, and a name already taken must say so instead of "saved".
 */
const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);

import ManageMethod from "./ManageMethod";

let methods;
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
          <ManageMethod />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("Manage Methods", () => {
  beforeEach(() => {
    methods = [{ id: "1", value: "EIA" }];
    addNotification = vi.fn();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ existingMethodList: [...methods], inactiveMethodList: [] }),
    );
    utilsMock.postToOpenElisServerFullResponse.mockReset();
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) => {
        methods.push({ id: "2", value: JSON.parse(body).methodEnglishName });
        callback({ status: 200 });
      },
    );
  });

  it("lists a method as soon as it is added", async () => {
    renderPage();
    expect(screen.getByText("EIA")).toBeInTheDocument();

    addMethod("Flow cytometry");

    await waitFor(() =>
      expect(screen.getByText("Flow cytometry")).toBeInTheDocument(),
    );
  });

  it("says a taken name is taken", () => {
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) => callback({ status: 409 }),
    );
    renderPage();

    addMethod("EIA");

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message:
          "A method with this name already exists. Use a different name.",
      }),
    );
  });
});

function addMethod(name) {
  fireEvent.click(screen.getAllByRole("button", { name: "Add New Method" })[0]);
  fireEvent.change(screen.getByLabelText(/English/), {
    target: { value: name },
  });
  fireEvent.change(screen.getByLabelText(/French/), {
    target: { value: name + " FR" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  fireEvent.click(screen.getByRole("button", { name: "Accept" }));
}
