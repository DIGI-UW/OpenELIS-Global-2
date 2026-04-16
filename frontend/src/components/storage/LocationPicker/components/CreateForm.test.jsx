/**
 * Phase 2a (RED) — CreateForm component tests.
 *
 * Carbon-correct patterns applied:
 *   - Wrap render in <IntlProvider> (Carbon uses react-intl internally;
 *     matches the existing LocationFilterDropdown.test.jsx pattern in
 *     this repo).
 *   - Carbon @carbon/react 1.15 Dropdown does NOT expose role=combobox
 *     on its trigger — the trigger is a plain <button>. Use
 *     getByLabelText('Room') (associates via the rendered <label for=...>).
 *   - Carbon's `disabled` prop sets aria-disabled="true", NOT the native
 *     disabled attribute. Assert via toHaveAttribute('aria-disabled', ...)
 *     not toBeDisabled().
 *
 * CreateForm renders the 5-level cascading-dropdown UI (Room → Device →
 * Shelf → Rack → Box) plus an inline "Create new {level}" affordance per
 * level. The component is presentational + lightweight async (one fetch
 * per level when its parent is selected). Selection state lives in the
 * useLocationPicker reducer; CreateForm receives `selection` prop and
 * fires `onLevelChange(level, value)` callbacks.
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import CreateForm from "./CreateForm";
import * as Utils from "../../../utils/Utils";

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={{}}>
      {component}
    </IntlProvider>,
  );

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.postToOpenElisServerJsonResponse.mockReset();
});

const mockRoomsApi = (rooms) => {
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/storage/rooms")) cb(rooms);
    else if (url.startsWith("/rest/storage/devices")) cb([]);
    else if (url.startsWith("/rest/storage/shelves")) cb([]);
    else if (url.startsWith("/rest/storage/racks")) cb([]);
    else if (url.startsWith("/rest/storage/boxes")) cb([]);
    else cb([]);
  });
};

describe("CreateForm — cascading dropdowns", () => {
  it("renders 5 dropdowns (Room/Device/Shelf/Rack/Box) by stable id", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    expect(document.querySelector("#location-picker-room")).toBeInTheDocument();
    expect(
      document.querySelector("#location-picker-device"),
    ).toBeInTheDocument();
    expect(
      document.querySelector("#location-picker-shelf"),
    ).toBeInTheDocument();
    expect(document.querySelector("#location-picker-rack")).toBeInTheDocument();
    expect(document.querySelector("#location-picker-box")).toBeInTheDocument();
  });

  // Carbon @carbon/react 1.15 Dropdown's `disabled` prop sets
  // aria-disabled="true" on the inner trigger button, NOT the native
  // disabled attribute. So we query the trigger and assert on aria-disabled.
  const triggerOf = (level) =>
    document
      .querySelector(`#location-picker-${level}`)
      .querySelector("button.cds--list-box__field");

  it("disables descendant dropdowns until their parent is selected", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    expect(triggerOf("room")).toHaveAttribute("aria-disabled", "false");
    expect(triggerOf("device")).toHaveAttribute("aria-disabled", "true");
    expect(triggerOf("shelf")).toHaveAttribute("aria-disabled", "true");
    expect(triggerOf("rack")).toHaveAttribute("aria-disabled", "true");
    expect(triggerOf("box")).toHaveAttribute("aria-disabled", "true");
  });

  it("enables Device dropdown when Room is selected", () => {
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    renderWithIntl(
      <CreateForm
        selection={{ room: { id: 1, name: "Main Lab" } }}
        onLevelChange={jest.fn()}
      />,
    );
    expect(triggerOf("device")).toHaveAttribute("aria-disabled", "false");
    expect(triggerOf("shelf")).toHaveAttribute("aria-disabled", "true");
  });

  it("loads rooms on mount", () => {
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    const calls = Utils.getFromOpenElisServer.mock.calls.map((c) => c[0]);
    expect(calls.some((url) => url.startsWith("/rest/storage/rooms"))).toBe(
      true,
    );
  });

  it("loads devices for the selected room", () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/rooms"))
        cb([{ id: 1, name: "Main Lab" }]);
      else if (url.startsWith("/rest/storage/devices?roomId=1"))
        cb([{ id: 5, name: "Freezer 1" }]);
      else cb([]);
    });
    renderWithIntl(
      <CreateForm
        selection={{ room: { id: 1, name: "Main Lab" } }}
        onLevelChange={jest.fn()}
      />,
    );
    const calls = Utils.getFromOpenElisServer.mock.calls.map((c) => c[0]);
    expect(
      calls.some((url) => url.includes("/rest/storage/devices?roomId=1")),
    ).toBe(true);
  });

  it("dispatches SET_LEVEL via onLevelChange when Room is picked", () => {
    const onLevelChange = jest.fn();
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    // Click the Room dropdown trigger to open the menu, then click the
    // option. Each option in Carbon's listbox has role=option.
    fireEvent.click(triggerOf("room"));
    fireEvent.click(screen.getByRole("option", { name: "Main Lab" }));
    expect(onLevelChange).toHaveBeenCalledWith("room", {
      id: 1,
      name: "Main Lab",
    });
  });
});

describe("CreateForm — inline create", () => {
  it("renders an 'Add new' button per level", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    // 5 levels = 5 "Add new" buttons (only the Room one is enabled when
    // nothing is selected; the rest are disabled)
    expect(
      screen.getAllByRole("button", { name: /add new/i }).length,
    ).toBeGreaterThanOrEqual(5);
  });

  it("opens an inline-create dialog when 'Add new' for Room is clicked", () => {
    mockRoomsApi([]);
    renderWithIntl(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    const addButtons = screen.getAllByRole("button", { name: /add new/i });
    fireEvent.click(addButtons[0]);
    // Carbon Modal renders role=dialog
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByLabelText(/^name$/i)).toBeInTheDocument();
  });

  it("POSTs to the level endpoint and dispatches the new option on submit", () => {
    mockRoomsApi([]);
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        cb({ id: 99, name: "New Room", code: "NEW", active: true });
      },
    );
    const onLevelChange = jest.fn();
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.change(screen.getByLabelText(/^name$/i), {
      target: { value: "New Room" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^create$/i }));
    expect(Utils.postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/storage/rooms",
      expect.stringContaining("New Room"),
      expect.any(Function),
    );
    expect(onLevelChange).toHaveBeenCalledWith("room", {
      id: 99,
      name: "New Room",
    });
  });

  it("Cancel closes the inline-create dialog without dispatching", () => {
    mockRoomsApi([]);
    const onLevelChange = jest.fn();
    renderWithIntl(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.click(screen.getByRole("button", { name: /^cancel$/i }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(onLevelChange).not.toHaveBeenCalled();
  });
});
