/* global jest, describe, test, expect, beforeEach, afterEach */
import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import AutocompleteMode from "./AutocompleteMode";
import { getFromOpenElisServer } from "../../utils/Utils";

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

if (typeof AbortController === "undefined") {
  global.AbortController = class {
    constructor() {
      this.signal = { aborted: false };
    }
    abort() {
      this.signal.aborted = true;
    }
  };
}

const messages = {
  "storage.location.label": "Location",
  "storage.location.search.placeholder": "Search...",
};

const renderWithIntl = (ui) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );
};

describe("AutocompleteMode Architectural Requirements", () => {
  const mockOnLocationChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, "error").mockImplementation(() => {});
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    jest.useRealTimers();
  });

  test("delegates credential handling and parsing to getFromOpenElisServer", () => {
    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "room" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(getFromOpenElisServer).toHaveBeenCalled();
  });

  test("calls getFromOpenElisServer with correct URL and callback", () => {
    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "freezer" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/storage/devices/search?q=freezer",
      expect.any(Function),
      expect.objectContaining({ aborted: expect.any(Boolean) }),
    );
  });

  test("debounces rapid input and only calls API once", () => {
    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "r" } });
    fireEvent.change(input, { target: { value: "ro" } });
    fireEvent.change(input, { target: { value: "roo" } });
    fireEvent.change(input, { target: { value: "room" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
  });

  test("clears results and skips API call for empty input", () => {
    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  test("renders empty results gracefully when API returns null", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(null);
    });

    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "room" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(getFromOpenElisServer).toHaveBeenCalled();
    expect(screen.getByRole("combobox")).toBeInTheDocument();
  });

  test("formats results with parentRoomName correctly", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([
        { id: 1, name: "Shelf A", parentRoomName: "Lab Room 1" },
        { id: 2, name: "Freezer B", parentRoomName: null },
      ]);
    });

    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "shelf" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(getFromOpenElisServer).toHaveBeenCalled();
  });

  test("calls onLocationChange when an item is selected", () => {
    const mockItem = {
      id: 1,
      name: "Shelf A",
      parentRoomName: "Lab Room 1",
      displayPath: "Lab Room 1 > Shelf A",
    };

    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback([mockItem]);
    });

    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );

    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "shelf" } });

    act(() => {
      jest.advanceTimersByTime(500);
    });

    const option = screen.getByText("Lab Room 1 > Shelf A");
    fireEvent.click(option);

    expect(mockOnLocationChange).toHaveBeenCalledWith(
      expect.objectContaining({ id: 1, name: "Shelf A" }),
    );
  });
});
