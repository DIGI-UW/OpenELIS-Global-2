import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import EditLocationModal from "./EditLocationModal";
import * as Utils from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

const RECORDS = {
  "/rest/storage/rooms/12": {
    id: 12,
    name: "Main Lab",
    code: "RM-1",
    description: "Ground floor",
    active: true,
  },
  "/rest/storage/racks/7": {
    id: 7,
    label: "Rack R9",
    code: "RK-9",
    parentShelfId: 3,
    active: true,
  },
  "/rest/storage/boxes/4": {
    id: 4,
    label: "Box Alpha",
    code: "BX-1",
    parentRackId: 7,
    rows: 8,
    columns: 12,
    active: false,
  },
};

const PARENTS = [
  { id: 3, name: "Shelf A", label: "Shelf A" },
  { id: 5, name: "Shelf B", label: "Shelf B" },
];

const renderModal = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <EditLocationModal
        level="room"
        id={12}
        open
        onClose={vi.fn()}
        onUpdated={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

const savedPayload = () =>
  JSON.parse(Utils.putToOpenElisServerFullResponse.mock.calls[0][1]);

const save = () => fireEvent.click(screen.getByText("Save").closest("button"));

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.putToOpenElisServerFullResponse.mockReset();
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/devices/types")) cb(["FREEZER", "REFRIGERATOR"]);
    else if (RECORDS[url]) cb(RECORDS[url]);
    else cb(PARENTS);
  });
  Utils.putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
    cb({ ok: true, json: () => Promise.resolve({}) }),
  );
});

describe("EditLocationModal", () => {
  it("loads the current values of the row being edited", async () => {
    renderModal({ level: "room", id: 12 });

    expect(screen.getByText("Edit Room")).toBeInTheDocument();
    expect(await screen.findByLabelText(/^name$/i)).toHaveValue("Main Lab");
    expect(screen.getByLabelText(/^code$/i)).toHaveValue("RM-1");
    expect(screen.getByLabelText(/description/i)).toHaveValue("Ground floor");
  });

  it("saves a rack to its own endpoint with the parent it was given", async () => {
    const onUpdated = vi.fn();
    renderModal({ level: "rack", id: 7, onUpdated });

    const label = await screen.findByLabelText(/^label$/i);
    expect(label).toHaveValue("Rack R9");
    await waitFor(() =>
      expect(screen.getByText("Shelf A")).toBeInTheDocument(),
    );

    fireEvent.change(label, { target: { value: "Rack R10" } });
    save();

    await waitFor(() => expect(onUpdated).toHaveBeenCalled());
    expect(Utils.putToOpenElisServerFullResponse.mock.calls[0][0]).toBe(
      "/rest/storage/racks/7",
    );
    expect(savedPayload()).toMatchObject({
      label: "Rack R10",
      code: "RK-9",
      parentShelfId: "3",
      active: true,
    });
  });

  it("reparents a rack when a different shelf is picked", async () => {
    renderModal({ level: "rack", id: 7 });

    const dropdown = await waitFor(() => {
      const el = document.querySelector("#storage-edit-modal-parent");
      expect(el).toBeInTheDocument();
      return el;
    });
    fireEvent.click(dropdown.querySelector('[role="combobox"]'));
    fireEvent.click(await screen.findByRole("option", { name: "Shelf B" }));
    save();

    await waitFor(() =>
      expect(Utils.putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(savedPayload().parentShelfId).toBe("5");
  });

  it("saves a box with its grid dimensions and no preset to choose from", async () => {
    renderModal({ level: "box", id: 4 });

    const rows = await screen.findByLabelText(/^rows$/i);
    expect(rows).toHaveValue(8);
    expect(screen.getByLabelText(/^columns$/i)).toHaveValue(12);
    expect(document.querySelector("#storage-edit-modal-grid")).toBeNull();

    fireEvent.change(rows, { target: { value: "5" } });
    save();

    await waitFor(() =>
      expect(Utils.putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(Utils.putToOpenElisServerFullResponse.mock.calls[0][0]).toBe(
      "/rest/storage/boxes/4",
    );
    expect(savedPayload()).toMatchObject({
      label: "Box Alpha",
      rows: 5,
      columns: 12,
    });
  });

  it("round-trips the active flag", async () => {
    renderModal({ level: "box", id: 4 });

    const active = await screen.findByLabelText(/^active$/i);
    expect(active).not.toBeChecked();

    fireEvent.click(active);
    save();

    await waitFor(() =>
      expect(Utils.putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(savedPayload().active).toBe(true);
  });

  // Three of this modal's messages carry ICU placeholders (the heading, the
  // parent picker and the HTTP save error). Formatting one without values
  // renders the pattern verbatim, which is what shipped on the Add modal.
  it("leaves no unsubstituted message placeholder on any level", async () => {
    for (const level of ["room", "device", "shelf", "rack", "box"]) {
      const { unmount } = renderModal({ level });
      await waitFor(() =>
        expect(screen.getByText(/^Edit /)).toBeInTheDocument(),
      );
      expect(document.body.textContent).not.toMatch(
        /\{(type|parent|level|status)\}/,
      );
      unmount();
    }
  });

  it("does not offer to move a box, because the server ignores its rack", async () => {
    renderModal({ level: "box", id: 4 });

    await screen.findByLabelText(/^rows$/i);
    const parent = document.querySelector("#storage-edit-modal-parent");
    expect(parent.querySelector('[role="combobox"]')).toBeDisabled();

    // A rack still moves, so the guard is about boxes and not about the field.
    const { unmount } = renderModal({ level: "rack", id: 7 });
    await screen.findByLabelText(/^label$/i);
    const racks = document.querySelectorAll("#storage-edit-modal-parent");
    expect(
      racks[racks.length - 1].querySelector('[role="combobox"]'),
    ).toBeEnabled();
    unmount();
  });

  it("keeps the dialog open and shows why when the server rejects the save", async () => {
    Utils.putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({
        ok: false,
        status: 409,
        json: () =>
          Promise.resolve({ error: "Rack label must be unique within shelf" }),
      }),
    );
    const onUpdated = vi.fn();
    renderModal({ level: "rack", id: 7, onUpdated });

    await screen.findByLabelText(/^label$/i);
    save();

    expect(
      await screen.findByText("Rack label must be unique within shelf"),
    ).toBeInTheDocument();
    expect(onUpdated).not.toHaveBeenCalled();
  });
});
