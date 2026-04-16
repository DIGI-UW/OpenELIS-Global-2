/**
 * Phase 2a (RED) — CreateForm component tests.
 *
 * CreateForm renders the 5-level cascading-dropdown UI (Room → Device →
 * Shelf → Rack → Box) plus an inline "Create new {level}" affordance
 * per level. It's the picker's "create" mode counterpart to SearchField.
 *
 * Architecture:
 *   - 5 Carbon Dropdowns (NOT ComboBox — research.md §3 design)
 *   - Each level fetches options from its parent-id-filtered endpoint
 *     (room → /rest/storage/rooms; device → /rest/storage/devices?roomId=X; …)
 *   - Selecting a level dispatches SET_LEVEL via callback (which clears
 *     descendants in the reducer)
 *   - Inline create per level via small modal: name field + submit;
 *     POSTs to the level's endpoint; the new option becomes selected
 *
 * The component is presentational + lightweight async (one fetch per
 * level when its parent is selected). No selection state — the caller
 * passes `selection` prop and `onLevelChange` callback that wires to
 * the useLocationPicker reducer.
 */

import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import "@testing-library/jest-dom";
import CreateForm from "./CreateForm";
import * as Utils from "../../../utils/Utils";

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

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
  it("renders 5 dropdowns labeled Room / Device / Shelf / Rack / Box", () => {
    mockRoomsApi([]);
    render(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    expect(screen.getByLabelText(/room/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/device/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/shelf/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/rack/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/box/i)).toBeInTheDocument();
  });

  it("disables descendant dropdowns until their parent is selected", () => {
    mockRoomsApi([]);
    render(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    expect(screen.getByLabelText(/room/i)).not.toBeDisabled();
    expect(screen.getByLabelText(/device/i)).toBeDisabled();
    expect(screen.getByLabelText(/shelf/i)).toBeDisabled();
    expect(screen.getByLabelText(/rack/i)).toBeDisabled();
    expect(screen.getByLabelText(/box/i)).toBeDisabled();
  });

  it("enables Device dropdown when Room is selected", () => {
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    render(
      <CreateForm
        selection={{ room: { id: 1, name: "Main Lab" } }}
        onLevelChange={jest.fn()}
      />,
    );
    expect(screen.getByLabelText(/device/i)).not.toBeDisabled();
    expect(screen.getByLabelText(/shelf/i)).toBeDisabled();
  });

  it("loads rooms on mount", () => {
    mockRoomsApi([{ id: 1, name: "Main Lab" }]);
    render(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
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
    render(
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
    render(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    // Carbon Dropdown — find the trigger button by accessible name
    fireEvent.click(screen.getByLabelText(/room/i));
    fireEvent.click(screen.getByText("Main Lab"));
    expect(onLevelChange).toHaveBeenCalledWith("room", {
      id: 1,
      name: "Main Lab",
    });
  });
});

describe("CreateForm — inline create", () => {
  it("renders an 'Add new' button per level", () => {
    mockRoomsApi([]);
    render(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    // Each level row has its own add-new affordance; at least 5 (one per level)
    expect(
      screen.getAllByRole("button", { name: /add new/i }).length,
    ).toBeGreaterThanOrEqual(5);
  });

  it("opens an inline-create dialog when 'Add new room' is clicked", () => {
    mockRoomsApi([]);
    render(<CreateForm selection={{}} onLevelChange={jest.fn()} />);
    // Click the Room-row's "Add new" button
    const addButtons = screen.getAllByRole("button", { name: /add new/i });
    fireEvent.click(addButtons[0]);
    // The dialog has a name input + Cancel/Create buttons
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
  });

  it("POSTs to the level endpoint and dispatches the new option on submit", () => {
    mockRoomsApi([]);
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, body, cb) => {
        cb({ id: 99, name: "New Room", code: "NEW", active: true });
      },
    );
    const onLevelChange = jest.fn();
    render(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.change(screen.getByLabelText(/name/i), {
      target: { value: "New Room" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create/i }));
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
    render(<CreateForm selection={{}} onLevelChange={onLevelChange} />);
    fireEvent.click(screen.getAllByRole("button", { name: /add new/i })[0]);
    fireEvent.click(screen.getByRole("button", { name: /cancel/i }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(onLevelChange).not.toHaveBeenCalled();
  });
});
