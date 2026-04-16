/**
 * Phase 3a (RED) — LocationPickerInline tests.
 *
 * The Inline shell is the thinnest of the three picker variants. It's
 * embedded directly within a host form (OrderLabel, AddOrder Sample Type).
 * No modal chrome, no breadcrumb, no confirm/cancel — just the picker UI:
 *
 *   - Mode toggle (Search ↔ Create new location)
 *   - When mode=search: <SearchField />
 *   - When mode=create: <CreateForm />
 *   - Optional: a compact summary of the current selection (hierarchical
 *     path string) so the host form sees what was picked
 *
 * The host calls onChange(selection) when the picker's selection changes,
 * giving the host a single value to persist with the rest of the form.
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationPickerInline from "./LocationPickerInline";
import * as Utils from "../../utils/Utils";

jest.mock("../../utils/Utils", () => ({
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
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
});

describe("LocationPickerInline", () => {
  it("renders in search mode by default", () => {
    renderWithIntl(<LocationPickerInline onChange={jest.fn()} />);
    // Search mode renders the SearchField (a textbox)
    expect(screen.getByRole("textbox")).toBeInTheDocument();
    // No CreateForm dropdowns visible
    expect(document.querySelector("#location-picker-room")).toBeNull();
  });

  it("toggles to create mode when 'Create new location' is clicked", () => {
    renderWithIntl(<LocationPickerInline onChange={jest.fn()} />);
    // Toggle button labeled with create-related text
    fireEvent.click(
      screen.getByRole("button", { name: /create new location/i }),
    );
    // CreateForm appears (room dropdown id is stable)
    expect(document.querySelector("#location-picker-room")).toBeInTheDocument();
  });

  it("toggles back to search mode from create mode", () => {
    renderWithIntl(<LocationPickerInline onChange={jest.fn()} />);
    fireEvent.click(
      screen.getByRole("button", { name: /create new location/i }),
    );
    expect(document.querySelector("#location-picker-room")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /back to search/i }));
    expect(document.querySelector("#location-picker-room")).toBeNull();
    expect(screen.getByRole("textbox")).toBeInTheDocument();
  });

  it("shows the selected hierarchical path when a selection is set", () => {
    renderWithIntl(
      <LocationPickerInline
        initialSelection={{
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
        }}
        onChange={jest.fn()}
      />,
    );
    // The compact summary string composes the selected levels with " > "
    expect(screen.getByText(/Main Lab > Freezer 1/)).toBeInTheDocument();
  });

  it("calls onChange whenever the selection changes", () => {
    const onChange = jest.fn();
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/rooms"))
        cb([{ id: 1, name: "Main Lab" }]);
      else cb([]);
    });
    renderWithIntl(<LocationPickerInline onChange={onChange} />);
    fireEvent.click(
      screen.getByRole("button", { name: /create new location/i }),
    );
    // Pick the room from the cascading dropdown
    const roomTrigger = document
      .querySelector("#location-picker-room")
      .querySelector("button.cds--list-box__field");
    fireEvent.click(roomTrigger);
    fireEvent.click(screen.getByRole("option", { name: "Main Lab" }));
    expect(onChange).toHaveBeenCalled();
    // The last call's selection includes the picked room
    const lastCall = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(lastCall.selection.room).toEqual({ id: 1, name: "Main Lab" });
  });
});
