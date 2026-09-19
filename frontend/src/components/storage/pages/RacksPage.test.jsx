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
    deleteFromOpenElisServerFullResponse: vi.fn(),
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
  Utils.deleteFromOpenElisServerFullResponse.mockReset();
  notifyCtx.setNotificationVisible.mockReset();
  notifyCtx.addNotification.mockReset();
});

describe("RacksPage — table search", () => {
  const twelveRacks = () =>
    Array.from({ length: 12 }, (_, n) => ({
      id: 900 + n,
      label: `Rack ${n}`,
      code: `RK-${n}`,
      active: true,
    }));

  // The level listings return every row and take no page or size parameter, so
  // the page has to be cut client-side or the table renders the whole set.
  it("renders one page of racks, highest id first, not the whole listing", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(twelveRacks()),
    );
    renderPage();

    expect(await screen.findByText("Rack 11")).toBeInTheDocument();
    expect(document.querySelectorAll("table tbody tr")).toHaveLength(5);
    expect(screen.queryByText("Rack 6")).not.toBeInTheDocument();
    expect(screen.getByText(/of 12 items/i)).toBeInTheDocument();

    const before = Utils.getFromOpenElisServer.mock.calls.length;
    fireEvent.click(screen.getByLabelText("Next page"));

    expect(await screen.findByText("Rack 6")).toBeInTheDocument();
    expect(screen.queryByText("Rack 11")).not.toBeInTheDocument();
    expect(Utils.getFromOpenElisServer.mock.calls.length).toBe(before);
  });

  // Five rows a page is the requested page size, so on any real site the
  // listing runs past one page and a new rack has to be findable anyway.
  it("shows the rack just created, from whichever page the user was on", async () => {
    const racks = twelveRacks();
    const created = {
      id: 999,
      label: "Rack New",
      code: "RK-NEW",
      active: true,
    };
    let listed = racks;
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/shelves")) cb([{ id: 1, label: "Shelf A" }]);
      else cb(listed);
    });
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        listed = [...racks, created];
        cb({ id: created.id });
      },
    );
    renderPage();

    await screen.findByText("Rack 11");
    fireEvent.click(screen.getByLabelText("Next page"));
    await screen.findByText("Rack 6");

    fireEvent.click(screen.getByText("Add"));
    fireEvent.change(await screen.findByLabelText(/^label$/i), {
      target: { value: "Rack New" },
    });
    fireEvent.click(
      document.querySelector('#storage-add-modal-parent [role="combobox"]'),
    );
    fireEvent.click(await screen.findByRole("option", { name: "Shelf A" }));
    fireEvent.click(screen.getByText("Create").closest("button"));

    expect(await screen.findByText("Rack New")).toBeInTheDocument();
  });

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

  // Reception alone pins one point on the boundary: a gate rewritten to
  // exclude Reception rather than admit admins passes that case and hands
  // Add, Edit and Delete to every other role. Results is one of those.
  it("offers a Results user no row menu and no column for one", async () => {
    listOneRack();
    renderPage(["Results"]);

    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
    expect(
      document.querySelector('[data-testid^="storage-row-actions-"]'),
    ).toBeNull();
    expect(headerCount()).toBe(4);
    expect(screen.queryByRole("button", { name: "Add" })).toBeNull();
  });

  // These two pin the shape of the condition rather than another role. A gate
  // written as an exclusion has to let Reception through to pass the case
  // above, and then it denies an administrator who also holds Reception —
  // which leaves a blocklist nowhere to stand, however many roles it names.
  it("offers no controls to a user holding every non-admin role at once", async () => {
    listOneRack();
    renderPage(
      Object.values(Utils.Roles).filter(
        (role) => role !== Utils.Roles.GLOBAL_ADMIN,
      ),
    );

    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
    expect(
      document.querySelector('[data-testid^="storage-row-actions-"]'),
    ).toBeNull();
    expect(headerCount()).toBe(4);
    expect(screen.queryByRole("button", { name: "Add" })).toBeNull();
  });

  it("offers an administrator the controls even when they also hold Reception", async () => {
    listOneRack();
    renderPage([Utils.Roles.GLOBAL_ADMIN, Utils.Roles.RECEPTION]);

    expect(await screen.findByText("Rack R1")).toBeInTheDocument();
    expect(
      document.querySelector('[data-testid="storage-row-actions-1"]'),
    ).toBeInTheDocument();
    expect(headerCount()).toBe(5);
    expect(screen.getByRole("button", { name: "Add" })).toBeInTheDocument();
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

describe("RacksPage — feedback on delete", () => {
  // The last page can hold a single row, and deleting it leaves the slice
  // empty: the table then renders nothing while the other racks still exist.
  it("keeps the surviving racks on screen after the last page's only row goes", async () => {
    const racks = Array.from({ length: 6 }, (_, n) => ({
      id: 900 + n,
      label: `Rack ${n}`,
      code: `RK-${n}`,
      active: true,
    }));
    let listed = racks;
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("cascade-delete-summary")) {
        cb({ childLocationCount: 0, childLocationType: "box", sampleCount: 0 });
      } else {
        cb(listed);
      }
    });
    Utils.deleteFromOpenElisServerFullResponse.mockImplementation((url, cb) => {
      listed = racks.filter((rack) => rack.id !== 900);
      cb({ status: 204 });
    });
    renderPage();

    await screen.findByText("Rack 5");
    fireEvent.click(screen.getByLabelText("Next page"));
    await screen.findByText("Rack 0");

    fireEvent.click(
      document.querySelector('[data-testid="storage-row-actions-900"]'),
    );
    const deleteItem = await waitFor(() => {
      const item = [
        ...document.querySelectorAll(".cds--overflow-menu-options button"),
      ].find((button) => button.textContent === "Delete");
      expect(item).toBeDefined();
      return item;
    });
    fireEvent.click(deleteItem);
    fireEvent.click(await screen.findByLabelText(/^I confirm that I want/i));
    fireEvent.click(document.querySelector(".cds--modal .cds--btn--danger"));

    await waitFor(() =>
      expect(document.querySelectorAll("table tbody tr")).toHaveLength(5),
    );
    expect(screen.getByText("Rack 5")).toBeInTheDocument();
    expect(notifyCtx.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Rack deleted" }),
    );
  });
});
