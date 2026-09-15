/**
 * LocationPickerInline tests — mode toggle between SearchField and
 * CreateForm, plus a compact selection summary. Host receives state
 * changes via onChange.
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationPickerInline from "./LocationPickerInline";
import * as Utils from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
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
    renderWithIntl(<LocationPickerInline onChange={vi.fn()} />);
    // Search mode renders the SearchField (a textbox)
    expect(
      screen.getByLabelText(/search for a storage location/i),
    ).toBeInTheDocument();
    // No CreateForm dropdowns visible
    expect(document.querySelector("#location-picker-room")).toBeNull();
  });

  it("toggles to create mode when 'Create new location' is clicked", () => {
    renderWithIntl(<LocationPickerInline onChange={vi.fn()} />);
    // Toggle button labeled with create-related text
    fireEvent.click(
      screen.getByRole("button", { name: /create new location/i }),
    );
    // CreateForm appears (room dropdown id is stable)
    expect(document.querySelector("#location-picker-room")).toBeInTheDocument();
  });

  it("toggles back to search mode from create mode", () => {
    renderWithIntl(<LocationPickerInline onChange={vi.fn()} />);
    fireEvent.click(
      screen.getByRole("button", { name: /create new location/i }),
    );
    expect(document.querySelector("#location-picker-room")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /back to search/i }));
    expect(document.querySelector("#location-picker-room")).toBeNull();
    expect(
      screen.getByLabelText(/search for a storage location/i),
    ).toBeInTheDocument();
  });

  it("shows the selected hierarchical path when a selection is set", () => {
    renderWithIntl(
      <LocationPickerInline
        initialSelection={{
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
        }}
        onChange={vi.fn()}
      />,
    );
    // The compact summary string composes the selected levels with " > "
    expect(screen.getByText(/Main Lab > Freezer 1/)).toBeInTheDocument();
  });

  it("does not fire onChange when only the parent re-renders with a new callback identity", () => {
    const onChange = vi.fn();
    function Parent({ counter }) {
      // Inline arrow ⇒ a fresh callback identity every render. If Inline's
      // effect depended on `onChange`, the effect would re-fire on every
      // render and onChange would get N extra calls.
      return (
        <IntlProvider locale="en" messages={{}}>
          <LocationPickerInline
            onChange={(state) => onChange(state, counter)}
          />
        </IntlProvider>
      );
    }
    const { rerender } = render(<Parent counter={1} />);
    const initialCallCount = onChange.mock.calls.length;
    rerender(<Parent counter={2} />);
    rerender(<Parent counter={3} />);
    rerender(<Parent counter={4} />);
    // No selection/position change between renders ⇒ onChange should
    // not have been invoked any additional times.
    expect(onChange.mock.calls.length).toBe(initialCallCount);
  });

  it("calls onChange whenever the selection changes", () => {
    const onChange = vi.fn();
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
