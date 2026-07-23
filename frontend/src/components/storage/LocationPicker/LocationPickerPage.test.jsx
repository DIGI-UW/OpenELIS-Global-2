/**
 * LocationPickerPage tests — full-page shell for the
 * /Storage/sample-items/:id/manage-location route.
 *
 * onSave returns the picker payload; the caller navigates away on success.
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationPickerPage from "./LocationPickerPage";
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

describe("LocationPickerPage", () => {
  it("renders the page heading 'Assign Storage Location' (no currentLocation)", () => {
    renderWithIntl(
      <LocationPickerPage
        sample={mockSample}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(
      screen.getByRole("heading", {
        level: 1,
        name: /assign storage location/i,
      }),
    ).toBeInTheDocument();
  });

  it("renders 'Move Sample' heading when currentLocation is present", () => {
    renderWithIntl(
      <LocationPickerPage
        sample={mockSample}
        currentLocation={{
          selection: { room: { id: 1, name: "Main Lab" } },
          position: null,
        }}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(
      screen.getByRole("heading", { level: 1, name: /move sample/i }),
    ).toBeInTheDocument();
  });

  it("renders the sample info", () => {
    renderWithIntl(
      <LocationPickerPage
        sample={mockSample}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByText("DEV0126-001")).toBeInTheDocument();
    expect(screen.getByText("Whole Blood")).toBeInTheDocument();
  });

  it("Cancel button fires onCancel", () => {
    const onCancel = vi.fn();
    renderWithIntl(
      <LocationPickerPage
        sample={mockSample}
        onSave={vi.fn()}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByRole("button", { name: /^cancel$/i }));
    expect(onCancel).toHaveBeenCalled();
  });

  it("Save button passes the payload to onSave", () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.startsWith("/rest/storage/rooms"))
        cb([{ id: 1, name: "Main Lab" }]);
      else cb([]);
    });
    const onSave = vi.fn();
    renderWithIntl(
      <LocationPickerPage
        sample={mockSample}
        onSave={onSave}
        onCancel={vi.fn()}
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
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    expect(onSave).toHaveBeenCalledWith(
      expect.objectContaining({
        selection: expect.objectContaining({
          room: { id: 1, name: "Main Lab" },
        }),
      }),
    );
  });

  it("renders the breadcrumb prop above the heading", () => {
    renderWithIntl(
      <LocationPickerPage
        sample={mockSample}
        onSave={vi.fn()}
        onCancel={vi.fn()}
        breadcrumb={<nav aria-label="Breadcrumb">crumb</nav>}
      />,
    );
    expect(
      screen.getByRole("navigation", { name: /breadcrumb/i }),
    ).toBeInTheDocument();
  });
});
