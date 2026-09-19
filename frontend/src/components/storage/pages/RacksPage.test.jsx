import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import RacksPage from "./RacksPage";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { NotificationContext } from "../../layout/Layout";
import * as Utils from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    putToOpenElisServerFullResponse: vi.fn(),
  };
});

const notifyCtx = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

const renderPage = (roles = ["Global Administrator"]) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notifyCtx}>
        <UserSessionDetailsContext.Provider
          value={{ userSessionDetails: { roles }, logout: vi.fn() }}
        >
          <MemoryRouter initialEntries={["/Storage/racks"]}>
            <RacksPage />
          </MemoryRouter>
        </UserSessionDetailsContext.Provider>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.postToOpenElisServerJsonResponse.mockReset();
  Utils.putToOpenElisServerFullResponse.mockReset();
  notifyCtx.setNotificationVisible.mockReset();
  notifyCtx.addNotification.mockReset();
});

describe("RacksPage — table search", () => {
  it("lists racks from the list endpoint by default", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([{ id: 1, label: "Rack R1", code: "RKR1", active: true }]),
    );
    renderPage();

    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
    expect(Utils.getFromOpenElisServer.mock.calls[0][0]).toContain(
      "/rest/storage/racks?",
    );
  });

  it("queries the search endpoint once a term is typed", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/racks/search")) {
        cb([{ id: 2, label: "Rack R2", code: "RKR2", active: true }]);
      } else {
        cb([{ id: 1, label: "Rack R1", code: "RKR1", active: true }]);
      }
    });
    renderPage();
    await screen.findByText("Rack R1");

    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "R2" },
    });

    expect(await screen.findByText("Rack R2")).toBeInTheDocument();
    const searched = Utils.getFromOpenElisServer.mock.calls.some(([url]) =>
      url.includes("/rest/storage/racks/search?q=R2"),
    );
    expect(searched).toBe(true);
  });

  it("returns to the full list when the search is cleared", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/racks/search")) cb([]);
      else cb([{ id: 1, label: "Rack R1", code: "RKR1", active: true }]);
    });
    renderPage();
    await screen.findByText("Rack R1");

    const box = screen.getByRole("searchbox");
    fireEvent.change(box, { target: { value: "nothing" } });
    await waitFor(() =>
      expect(screen.queryByText("Rack R1")).not.toBeInTheDocument(),
    );

    fireEvent.change(box, { target: { value: "" } });
    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
  });
});

describe("RacksPage — feedback", () => {
  it("raises a notification once a rack is created", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/shelves")) cb([{ id: 1, label: "Shelf A" }]);
      else cb([]);
    });
    Utils.postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ id: 42 }),
    );
    renderPage();

    fireEvent.click(await screen.findByText("Add"));
    fireEvent.change(await screen.findByLabelText(/^label$/i), {
      target: { value: "Rack R9" },
    });
    fireEvent.click(
      document.querySelector('#storage-add-modal-parent [role="combobox"]'),
    );
    fireEvent.click(await screen.findByRole("option", { name: "Shelf A" }));
    fireEvent.click(screen.getByText("Create").closest("button"));

    await waitFor(() => expect(notifyCtx.addNotification).toHaveBeenCalled());
    expect(notifyCtx.setNotificationVisible).toHaveBeenCalledWith(true);
    expect(notifyCtx.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Rack created" }),
    );
  });
});

// The storage POST, PUT and DELETE endpoints carry no role check of their own,
// so this gate is the only role barrier on the create and edit paths. A suite
// that only asserts admins DO see the controls lets the gate be deleted.
describe("RacksPage — who gets Add, Edit and Delete", () => {
  const listOneRack = () =>
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([{ id: 1, label: "Rack R1", code: "RKR1", active: true }]),
    );

  const headerCount = () => document.querySelectorAll("thead th").length;

  it("offers a global administrator a row menu in a column of its own", async () => {
    listOneRack();
    renderPage(["Global Administrator"]);

    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
    expect(
      document.querySelector('[data-testid="storage-row-actions-1"]'),
    ).toBeInTheDocument();
    expect(headerCount()).toBe(5);
    expect(screen.getByRole("button", { name: "Add" })).toBeInTheDocument();
  });

  it("offers a Reception user no row menu and no column for one", async () => {
    listOneRack();
    renderPage(["Reception"]);

    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
    expect(
      document.querySelector('[data-testid^="storage-row-actions-"]'),
    ).toBeNull();
    expect(headerCount()).toBe(4);
    expect(screen.queryByRole("button", { name: "Add" })).toBeNull();
  });
});

describe("RacksPage — feedback on edit", () => {
  it("raises a notification once a rack is updated", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/storage/racks/1") {
        cb({ id: 1, label: "Rack R1", code: "RKR1", parentShelfId: 1 });
      } else if (url.includes("/shelves")) {
        cb([{ id: 1, label: "Shelf A" }]);
      } else {
        cb([{ id: 1, label: "Rack R1", code: "RKR1", active: true }]);
      }
    });
    Utils.putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({ ok: true, json: () => Promise.resolve({}) }),
    );
    renderPage();

    await screen.findByText("Rack R1");
    fireEvent.click(
      document.querySelector('[data-testid="storage-row-actions-1"]'),
    );
    fireEvent.click(await screen.findByText("Edit"));

    const labelInput = await waitFor(() => {
      const el = document.querySelector("#storage-edit-modal-name");
      expect(el).toBeInTheDocument();
      return el;
    });
    fireEvent.change(labelInput, { target: { value: "Rack R2" } });
    fireEvent.click(screen.getByText("Save").closest("button"));

    await waitFor(() => expect(notifyCtx.addNotification).toHaveBeenCalled());
    expect(Utils.putToOpenElisServerFullResponse.mock.calls[0][0]).toBe(
      "/rest/storage/racks/1",
    );
    expect(notifyCtx.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Rack updated" }),
    );
  });
});
