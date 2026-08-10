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
  it("renders the search field and the level cascade together", () => {
    renderWithIntl(<LocationPickerInline onChange={vi.fn()} />);
    expect(
      screen.getByLabelText(/search for a storage location/i),
    ).toBeInTheDocument();
    ["room", "device", "shelf", "rack", "box"].forEach((level) => {
      expect(
        document.querySelector(`#location-picker-${level}`),
      ).toBeInTheDocument();
    });
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
