import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";

/**
 * Manage Methods is the only method page: it lists every method with both
 * names and its status, adds one, and renames one. A method added or renamed
 * shows without a page reload, and a name another method already has is
 * refused on the name field instead of reported as saved.
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

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <MemoryRouter>
          <ManageMethod />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const modal = () => screen.getByRole("dialog");
const cell = (text) => screen.queryByText(text, { selector: "td" });

const fill = (label, value) =>
  fireEvent.change(within(modal()).getByLabelText(label), {
    target: { value },
  });

describe("Manage Methods", () => {
  beforeEach(() => {
    methods = [
      {
        id: "1",
        nameEnglish: "EIA",
        nameFrench: "EIA FR",
        code: "",
        active: "true",
      },
      {
        id: "2",
        nameEnglish: "Flow cytometry",
        nameFrench: "Cytométrie",
        code: "",
        active: "false",
      },
    ];
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ methods: methods.map((m) => ({ ...m })) }),
    );
    utilsMock.postToOpenElisServerFullResponse.mockReset();
  });

  it("lists each method with both names and its status", () => {
    renderPage();
    expect(cell("EIA")).toBeInTheDocument();
    expect(cell("EIA FR")).toBeInTheDocument();
    const inactiveRow = cell("Flow cytometry").closest("tr");
    expect(within(inactiveRow).getByText("Inactive")).toBeInTheDocument();
  });

  it("narrows the list as a name is typed, and opens the match for edit", () => {
    renderPage();

    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "flow" },
    });

    expect(cell("Flow cytometry")).toBeInTheDocument();
    expect(cell("EIA")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: /Edit/ }));
    expect(within(modal()).getByLabelText(/English/)).toHaveValue(
      "Flow cytometry",
    );
  });

  it("adds a method and lists it", async () => {
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) => {
        const sent = JSON.parse(body);
        methods.push({
          id: "3",
          nameEnglish: sent.methodEnglishName,
          nameFrench: sent.methodFrenchName,
          active: "false",
        });
        callback({ status: 200 });
      },
    );
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: "Add New Method" }));
    fill(/English/, "PCR");
    fill(/French/, "PCR FR");
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(utilsMock.postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/MethodCreate",
      JSON.stringify({ methodEnglishName: "PCR", methodFrenchName: "PCR FR" }),
      expect.any(Function),
    );
    await waitFor(() => expect(cell("PCR")).toBeInTheDocument());
  });

  it("renames a method in place", async () => {
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) => {
        const sent = JSON.parse(body);
        methods[0] = {
          ...methods[0],
          nameEnglish: sent.nameEnglish,
          nameFrench: sent.nameFrench,
        };
        callback({ status: 200 });
      },
    );
    renderPage();

    fireEvent.click(screen.getAllByRole("button", { name: /Edit/ })[0]);
    expect(within(modal()).getByLabelText(/English/)).toHaveValue("EIA");
    expect(within(modal()).getByLabelText(/French/)).toHaveValue("EIA FR");
    fill(/English/, "ELISA");
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(utilsMock.postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/MethodRenameEntry",
      JSON.stringify({
        methodId: "1",
        nameEnglish: "ELISA",
        nameFrench: "EIA FR",
      }),
      expect.any(Function),
    );
    await waitFor(() => expect(cell("ELISA")).toBeInTheDocument());
  });

  it("refuses a name another method has, on the name field", () => {
    utilsMock.postToOpenElisServerFullResponse.mockImplementation(
      (url, body, callback) => callback({ status: 409 }),
    );
    renderPage();

    fireEvent.click(screen.getAllByRole("button", { name: /Edit/ })[0]);
    fill(/English/, "Flow cytometry");
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(
      within(modal()).getByText(
        "A method with this name already exists. Use a different name.",
      ),
    ).toBeInTheDocument();
    expect(cell("EIA")).toBeInTheDocument();
  });

  it("does not send a method without both names", () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: "Add New Method" }));
    fill(/English/, "PCR");
    fireEvent.click(within(modal()).getByRole("button", { name: "Save" }));

    expect(utilsMock.postToOpenElisServerFullResponse).not.toHaveBeenCalled();
    expect(
      within(modal()).getByText("This field is required"),
    ).toBeInTheDocument();
  });
});
