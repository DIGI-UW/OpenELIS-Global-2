/* global jest, describe, test, expect, beforeEach, afterEach */
import React from "react";
import { render, screen, fireEvent, wait } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import AutocompleteMode from "./AutocompleteMode";
import { getFromOpenElisServer } from "../../utils/Utils";

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

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
  });

  afterEach(() => {
    console.error.mockRestore();
  });

  test("delegates credential handling and parsing to getFromOpenElisServer", async () => {
    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );
    const input = screen.getByRole("combobox");

    fireEvent.change(input, { target: { value: "room" } });

    await wait(() => {
      expect(getFromOpenElisServer).toHaveBeenCalled();
    });
  });

  test("logs error to console when API call fails", async () => {
    getFromOpenElisServer.mockImplementation(
      (url, success, signal, errorCb) => {
        errorCb(new Error("API Failure"));
      },
    );

    renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "error-test" } });

    await wait(() => {
      expect(console.error).toHaveBeenCalledWith(
        "Error searching storage devices:",
        expect.any(Error),
      );
    });
  });

  test("does not log AbortError to console during unmount", async () => {
    const abortError = new Error("Request Aborted");
    abortError.name = "AbortError";

    getFromOpenElisServer.mockImplementation(
      (url, success, signal, errorCb) => {
        errorCb(abortError);
      },
    );

    const { unmount } = renderWithIntl(
      <AutocompleteMode onLocationChange={mockOnLocationChange} />,
    );
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "abort-test" } });

    unmount();

    await wait(() => {
      expect(console.error).not.toHaveBeenCalled();
    });
  });
});
