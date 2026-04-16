/**
 * Phase 3c (RED) — LocationPickerModal tests.
 *
 * Modal shell wraps the picker in a Carbon ComposedModal for the
 * documented modal-exception sites (Result Entry's expandable row,
 * Notebook entry).
 *
 * Title flips based on currentLocation:
 *   - null/undefined → "Assign Storage Location"
 *   - present       → "Move Sample"
 *
 * Layout (top to bottom):
 *   - Sample info (sampleId / accession / type / status)
 *   - Current location section (when present)
 *   - Mode toggle + SearchField/CreateForm (the picker)
 *   - Position input (text/grid toggle)
 *   - Reason field (movement only)
 *   - Notes field (always)
 *   - Cancel + Confirm buttons
 *
 * onConfirm receives the full payload: { selection, position, reason, notes }
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationPickerModal from "./LocationPickerModal";
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

const mockSample = {
  id: "42",
  sampleAccessionNumber: "DEV0126-001",
  sampleType: "Whole Blood",
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
        sample={mockSample}
        onConfirm={jest.fn()}
        onCancel={jest.fn()}
      />,
    );
    // Carbon's ComposedModal still mounts but has no .is-visible class
    expect(container.querySelector(".cds--modal.is-visible")).toBeNull();
  });

  it("shows 'Assign Storage Location' title when no currentLocation", () => {
    renderWithIntl(
      <LocationPickerModal
        isOpen
        sample={mockSample}
        onConfirm={jest.fn()}
        onCancel={jest.fn()}
      />,
    );
    expect(screen.getByText(/assign storage location/i)).toBeInTheDocument();
  });

  it("shows 'Move Sample' title when currentLocation is present", () => {
    renderWithIntl(
      <LocationPickerModal
        isOpen
        sample={mockSample}
        currentLocation={{
          selection: { room: { id: 1, name: "Main Lab" } },
          position: null,
        }}
        onConfirm={jest.fn()}
        onCancel={jest.fn()}
      />,
    );
    expect(screen.getByText(/move sample/i)).toBeInTheDocument();
  });

  it("renders the sample info (accession, type, status)", () => {
    renderWithIntl(
      <LocationPickerModal
        isOpen
        sample={mockSample}
        onConfirm={jest.fn()}
        onCancel={jest.fn()}
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
        sample={mockSample}
        onConfirm={jest.fn()}
        onCancel={jest.fn()}
      />,
    );
    expect(screen.queryByLabelText(/reason for move/i)).toBeNull();

    rerender(
      <IntlProvider locale="en" messages={{}}>
        <LocationPickerModal
          isOpen
          sample={mockSample}
          currentLocation={{
            selection: { room: { id: 1, name: "Main Lab" } },
            position: null,
          }}
          onConfirm={jest.fn()}
          onCancel={jest.fn()}
        />
      </IntlProvider>,
    );
    expect(screen.getByLabelText(/reason for move/i)).toBeInTheDocument();
  });

  it("Cancel button fires onCancel without invoking onConfirm", () => {
    const onCancel = jest.fn();
    const onConfirm = jest.fn();
    renderWithIntl(
      <LocationPickerModal
        isOpen
        sample={mockSample}
        onCancel={onCancel}
        onConfirm={onConfirm}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: /^cancel$/i }));
    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("Confirm button passes the picker payload to onConfirm", () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/rooms"))
        cb([{ id: 1, name: "Main Lab" }]);
      else cb([]);
    });
    const onConfirm = jest.fn();
    renderWithIntl(
      <LocationPickerModal
        isOpen
        sample={mockSample}
        onConfirm={onConfirm}
        onCancel={jest.fn()}
      />,
    );
    // Pick a room via create-mode cascade
    fireEvent.click(
      screen.getByRole("button", { name: /create new location/i }),
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
