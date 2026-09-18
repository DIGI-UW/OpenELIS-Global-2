import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AddLocationModal from "./AddLocationModal";
import * as Utils from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const renderModal = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AddLocationModal
        level="room"
        open
        onClose={vi.fn()}
        onCreated={vi.fn()}
        {...props}
      />
    </IntlProvider>,
  );

const chooseParent = async (text) => {
  const trigger = await waitFor(() => {
    const el = document.querySelector("#storage-add-modal-parent");
    expect(el).toBeInTheDocument();
    return el;
  });
  fireEvent.click(trigger.querySelector('[role="combobox"]'));
  fireEvent.click(await screen.findByRole("option", { name: text }));
};

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.postToOpenElisServerJsonResponse.mockReset();
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/devices/types")) cb(["FREEZER", "REFRIGERATOR"]);
    else cb([{ id: 1, name: "Main Lab", label: "Main Lab" }]);
  });
  Utils.postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
    cb({ id: 99 }),
  );
});

describe("AddLocationModal", () => {
  it("titles itself for the level being added", () => {
    renderModal({ level: "rack" });
    expect(screen.getByText("Add Rack")).toBeInTheDocument();
  });

  it("asks rooms for a name and a description, with no parent", () => {
    renderModal({ level: "room" });

    expect(screen.getByLabelText(/^name$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
    expect(document.querySelector("#storage-add-modal-parent")).toBeNull();
  });

  it("asks levels below room for a label and a parent", async () => {
    renderModal({ level: "shelf" });

    expect(screen.getByLabelText(/^label$/i)).toBeInTheDocument();
    await waitFor(() =>
      expect(
        document.querySelector("#storage-add-modal-parent"),
      ).toBeInTheDocument(),
    );
  });

  it("requires a device type before a device can be created", async () => {
    renderModal({ level: "device" });

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Freezer 1" },
    });
    // Name alone is not enough; the type is still missing.
    expect(screen.getByText("Create").closest("button")).toBeDisabled();
  });

  it("offers a grid for boxes and posts the chosen dimensions", async () => {
    const onCreated = vi.fn();
    renderModal({ level: "box", onCreated });

    expect(
      document.querySelector("#storage-add-modal-grid"),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/^label$/i), {
      target: { value: "Box Alpha" },
    });
    fireEvent.change(screen.getByLabelText(/^code$/i), {
      target: { value: "BX-001" },
    });
    await chooseParent("Main Lab");
    fireEvent.click(screen.getByText("Create").closest("button"));

    await waitFor(() => expect(onCreated).toHaveBeenCalled());
    const [url, body] = Utils.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/storage/boxes");
    const payload = JSON.parse(body);
    expect(payload.label).toBe("Box Alpha");
    expect(payload.rows).toBe(8);
    expect(payload.columns).toBe(12);
  });

  it("keeps Create disabled until a level below room has a parent", async () => {
    renderModal({ level: "rack" });

    fireEvent.change(screen.getByLabelText(/^label$/i), {
      target: { value: "Rack R9" },
    });
    // The backend answers 400 "Parent rack not found" for a parentless rack.
    expect(screen.getByText("Create").closest("button")).toBeDisabled();

    await chooseParent("Main Lab");

    expect(screen.getByText("Create").closest("button")).toBeEnabled();
  });

  it("posts a room to its own endpoint and reports success", async () => {
    const onCreated = vi.fn();
    renderModal({ level: "room", onCreated });

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Main Lab" },
    });
    fireEvent.click(screen.getByText("Create").closest("button"));

    await waitFor(() => expect(onCreated).toHaveBeenCalled());
    expect(Utils.postToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      "/rest/storage/rooms",
    );
  });

  it("keeps the dialog open and shows why when the server rejects it", async () => {
    Utils.postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ error: "Code already exists" }),
    );
    const onCreated = vi.fn();
    renderModal({ level: "room", onCreated });

    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "Main Lab" },
    });
    fireEvent.click(screen.getByText("Create").closest("button"));

    expect(await screen.findByText("Code already exists")).toBeInTheDocument();
    expect(onCreated).not.toHaveBeenCalled();
  });
});
