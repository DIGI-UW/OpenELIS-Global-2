/**
 * LocationPickerModal tests — Carbon ComposedModal shell.
 *
 * Title flips based on `currentLocation`:
 *   null/undefined → "Assign Storage Location"
 *   present        → "Move Item"
 *
 * onConfirm payload: { selection, position, reason, notes }.
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationPickerModal from "./LocationPickerModal";
import * as Utils from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

// Replace Carbon's ComposedModal with a plain div. The real component
// installs a focus-trap MutationObserver via a React portal, which jsdom
// cannot tear down cleanly — tests then leave open handles and Jest
// hangs waiting for the event loop to empty. Here we only care about
// the modal's *contract* (renders when `open`, shows title, body,
// footer), not its portal/focus-trap implementation.
vi.mock("@carbon/react", async () => {
  const actual = await vi.importActual("@carbon/react");
  return {
    ...actual,
    ComposedModal: ({ open, children, onClose }) =>
      open ? (
        <div role="dialog" data-testid="mock-composed-modal">
          {children}
        </div>
      ) : null,
    ModalHeader: ({ title }) => (
      <header>
        <h2>{title}</h2>
      </header>
    ),
    ModalBody: ({ children }) => <div>{children}</div>,
    ModalFooter: ({ children }) => <footer>{children}</footer>,
  };
});

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={{}}>
      {component}
    </IntlProvider>,
  );

const mockOccupant = {
  label: "DEV0126-001",
  type: "Whole Blood",
  status: "Active",
};

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
  Utils.postToOpenElisServerJsonResponse.mockReset();
});

describe("LocationPickerModal", () => {
  it("renders nothing when isOpen is false", () => {
    const { container } = renderWithIntl(
      <LocationPickerModal
        isOpen={false}
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    // Carbon's ComposedModal still mounts but has no .is-visible class
    expect(container.querySelector(".cds--modal.is-visible")).toBeNull();
  });

  it("shows 'Assign Storage Location' title when no currentLocation", () => {
    renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByText(/assign storage location/i)).toBeInTheDocument();
  });

  it("shows 'Move Item' title when currentLocation is present", () => {
    renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        currentLocation={{
          selection: { room: { id: 1, name: "Main Lab" } },
          position: null,
        }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByText(/move item/i)).toBeInTheDocument();
  });

  it("renders the occupant info (identifier, type, status)", () => {
    renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByText("DEV0126-001")).toBeInTheDocument();
    expect(screen.getByText("Whole Blood")).toBeInTheDocument();
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("shows a 'Reason for move' field only when currentLocation is present", () => {
    const { rerender } = renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.queryByLabelText(/reason for move/i)).toBeNull();

    rerender(
      <IntlProvider locale="en" messages={{}}>
        <LocationPickerModal
          isOpen
          occupant={mockOccupant}
          occupantType="SAMPLE_ITEM"
          currentLocation={{
            selection: { room: { id: 1, name: "Main Lab" } },
            position: null,
          }}
          onConfirm={vi.fn()}
          onCancel={vi.fn()}
        />
      </IntlProvider>,
    );
    expect(screen.getByLabelText(/reason for move/i)).toBeInTheDocument();
  });

  it("Cancel button fires onCancel without invoking onConfirm", () => {
    const onCancel = vi.fn();
    const onConfirm = vi.fn();
    renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: /^cancel$/i }));
    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("clears stale state (mode, notes, reason) when closed and reopened", () => {
    const Wrap = ({ isOpen, currentLocation }) => (
      <IntlProvider locale="en" messages={{}}>
        <LocationPickerModal
          isOpen={isOpen}
          occupant={mockOccupant}
          occupantType="SAMPLE_ITEM"
          currentLocation={currentLocation}
          onConfirm={vi.fn()}
          onCancel={vi.fn()}
        />
      </IntlProvider>
    );
    const { rerender } = render(
      <Wrap
        isOpen
        currentLocation={{
          selection: { room: { id: 1, name: "Main Lab" } },
          position: null,
        }}
      />,
    );

    fireEvent.change(screen.getByLabelText(/reason for move/i), {
      target: { value: "freezer failure" },
    });
    fireEvent.change(screen.getByLabelText(/^notes$/i), {
      target: { value: "moved urgently" },
    });

    // Close and reopen with the same currentLocation.
    rerender(<Wrap isOpen={false} />);
    rerender(
      <Wrap
        isOpen
        currentLocation={{
          selection: { room: { id: 1, name: "Main Lab" } },
          position: null,
        }}
      />,
    );

    // Reason and notes are empty.
    expect(screen.getByLabelText(/reason for move/i)).toHaveValue("");
    expect(screen.getByLabelText(/^notes$/i)).toHaveValue("");
  });

  it("fills in the ancestors and enables deeper levels when a shelf is picked from search", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/locations/search")) {
        cb([
          {
            id: 9,
            type: "shelf",
            label: "Shelf A",
            hierarchicalPath: "Main Lab > Freezer 1 > Shelf A",
            parentDeviceId: 5,
            parentDeviceName: "Freezer 1",
            parentRoomId: 1,
            parentRoomName: "Main Lab",
          },
        ]);
      } else if (url.startsWith("/rest/storage/racks")) {
        cb([{ id: 12, label: "Rack 3" }]);
      } else {
        cb([]);
      }
    });
    renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const rackField = () =>
      document
        .querySelector("#location-picker-rack")
        .querySelector("button.cds--list-box__field");
    expect(rackField()).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/search for a storage location/i), {
      target: { value: "shelf a" },
    });
    fireEvent.click(
      await screen.findByRole("option", {
        name: "Main Lab > Freezer 1 > Shelf A",
      }),
    );

    expect(
      document.querySelector(".storage-location-picker-modal-summary"),
    ).toHaveTextContent("Main Lab > Freezer 1 > Shelf A");
    expect(
      document
        .querySelector("#location-picker-room")
        .querySelector(".cds--list-box__label"),
    ).toHaveTextContent("Main Lab");
    expect(
      document
        .querySelector("#location-picker-device")
        .querySelector(".cds--list-box__label"),
    ).toHaveTextContent("Freezer 1");
    expect(rackField()).toBeEnabled();
    fireEvent.click(rackField());
    expect(screen.getByRole("option", { name: "Rack 3" })).toBeInTheDocument();
  });

  it("Confirm button passes the picker payload to onConfirm", () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/rooms"))
        cb([{ id: 1, name: "Main Lab" }]);
      else cb([]);
    });
    const onConfirm = vi.fn();
    renderWithIntl(
      <LocationPickerModal
        isOpen
        occupant={mockOccupant}
        occupantType="SAMPLE_ITEM"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />,
    );
    const roomTrigger = document
      .querySelector("#location-picker-room")
      .querySelector("button.cds--list-box__field");
    fireEvent.click(roomTrigger);
    fireEvent.click(screen.getByRole("option", { name: "Main Lab" }));
    fireEvent.click(screen.getByRole("button", { name: /^confirm$/i }));
    expect(onConfirm).toHaveBeenCalledWith(
      expect.objectContaining({
        selection: expect.objectContaining({
          room: { id: 1, name: "Main Lab" },
        }),
      }),
    );
  });
});
