/**
 * OGC-1364 v3: result settings shown as switches grouped by purpose, each group
 * with its own Save.
 */
import React from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../../languages/en.json";
import { NotificationContext } from "../../../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../../utils/Utils";
import ResultValidationSettings from "../ResultValidationSettings";

vi.mock("../../../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
}));

const SETTINGS = [
  {
    id: "1",
    name: "ResultTechnicianName",
    value: "false",
    valueType: "boolean",
    description: "",
    lastupdated: 1700000000000,
  },
  {
    id: "2",
    name: "autoFillTechNameBox",
    value: "false",
    valueType: "boolean",
    description: "",
  },
  {
    id: "3",
    name: "allowBulkReleaseClear",
    value: "true",
    valueType: "boolean",
    description: "",
  },
  {
    id: "4",
    name: "customCriticalMessage",
    value: "Call the ward",
    valueType: "text",
    description: "",
  },
  {
    id: "5",
    name: "validate all results",
    value: "true",
    valueType: "boolean",
    description: "",
  },
];

const renderSettings = (onSaved = vi.fn()) => {
  const addNotification = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ addNotification, setNotificationVisible: vi.fn() }}
      >
        <ResultValidationSettings settings={SETTINGS} onSaved={onSaved} />
      </NotificationContext.Provider>
    </IntlProvider>,
  );
  return addNotification;
};

const toggle = (label) =>
  screen.getByRole("switch", { name: new RegExp(`^${label}`) });

describe("Result & Validation Configuration settings", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ id: url.split("=")[1], paramName: "x", value: "old" }),
    );
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(200),
    );
  });

  it("groups the settings under plain-language names and leaves out the replaced switch", () => {
    renderSettings();
    const entry = screen.getByTestId("settings-group-resultEntry");
    expect(
      within(entry).getByText("Require the technician name on results"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("settings-group-validation")).getByText(
        "Allow bulk signing and release of clear results",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText("validate all results")).toBeNull();
  });

  it("keeps the autofill switch off-limits until the technician name is required", () => {
    renderSettings();
    expect(toggle("Show an autofill box")).toBeDisabled();
    fireEvent.click(toggle("Require the technician name"));
    expect(toggle("Show an autofill box")).toBeEnabled();
  });

  it("saves only the group's changed settings with their new values", async () => {
    const onSaved = vi.fn();
    const addNotification = renderSettings(onSaved);
    const save = screen.getByTestId("save-group-resultEntry");
    expect(save).toBeDisabled();

    fireEvent.click(toggle("Require the technician name"));
    fireEvent.change(screen.getByLabelText("Critical result message"), {
      target: { value: "Phone the doctor" },
    });
    expect(screen.getByLabelText("Critical result message")).toHaveValue(
      "Phone the doctor",
    );
    await act(async () => {
      fireEvent.click(save);
    });
    await waitFor(() => expect(onSaved).toHaveBeenCalled());

    const posted = postToOpenElisServer.mock.calls.map(([url, body]) => [
      url,
      JSON.parse(body).value,
    ]);
    expect(posted).toEqual([
      ["/rest/ResultConfiguration?ID=1", "true"],
      ["/rest/ResultConfiguration?ID=4", "Phone the doctor"],
    ]);
    expect(onSaved).toHaveBeenCalled();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Result entry settings saved." }),
    );
  });

  it("says the group was not saved when the server refuses", async () => {
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(500),
    );
    const onSaved = vi.fn();
    const addNotification = renderSettings(onSaved);

    fireEvent.click(toggle("Allow bulk signing"));
    await act(async () => {
      fireEvent.click(screen.getByTestId("save-group-validation"));
    });

    expect(onSaved).not.toHaveBeenCalled();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message:
          "Allow bulk signing and release of clear results could not be saved. Press Save to try again.",
      }),
    );
  });

  it("sends the version the page loaded, and says when someone else changed the setting", async () => {
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(409),
    );
    const onSaved = vi.fn();
    const addNotification = renderSettings(onSaved);

    fireEvent.click(toggle("Require the technician name"));
    await act(async () => {
      fireEvent.click(screen.getByTestId("save-group-resultEntry"));
    });
    await waitFor(() => expect(addNotification).toHaveBeenCalled());

    expect(JSON.parse(postToOpenElisServer.mock.calls[0][1]).lastupdated).toBe(
      1700000000000,
    );
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message:
          "Require the technician name on results was changed by someone else since this page loaded. The page now shows the latest values: check them and save again.",
      }),
    );
    expect(onSaved).toHaveBeenCalled();
  });

  it("names what was saved when a later setting of the group fails", async () => {
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(url.endsWith("ID=4") ? 503 : 200),
    );
    const onSaved = vi.fn();
    const addNotification = renderSettings(onSaved);

    fireEvent.click(toggle("Require the technician name"));
    fireEvent.change(screen.getByLabelText("Critical result message"), {
      target: { value: "Phone the doctor" },
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId("save-group-resultEntry"));
    });
    await waitFor(() => expect(addNotification).toHaveBeenCalled());

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message:
          "Saved Require the technician name on results. Critical result message could not be saved. Press Save to try again.",
      }),
    );
    expect(onSaved).toHaveBeenCalled();
  });

  it("keeps unsaved edits of other groups when the list reloads", () => {
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification: vi.fn(), setNotificationVisible: vi.fn() }}
        >
          <ResultValidationSettings settings={SETTINGS} />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    fireEvent.click(toggle("Allow bulk signing"));
    fireEvent.click(toggle("Require the technician name"));

    const reloaded = SETTINGS.map((setting) =>
      setting.name === "ResultTechnicianName"
        ? { ...setting, value: "true" }
        : { ...setting },
    );
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification: vi.fn(), setNotificationVisible: vi.fn() }}
        >
          <ResultValidationSettings settings={reloaded} />
        </NotificationContext.Provider>
      </IntlProvider>,
    );

    expect(toggle("Allow bulk signing")).toHaveAttribute(
      "aria-checked",
      "false",
    );
    expect(screen.getByTestId("save-group-validation")).toBeEnabled();
    expect(screen.getByTestId("save-group-resultEntry")).toBeDisabled();
  });

  it("shows the stored value of a setting someone else changed, not the refused edit", async () => {
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(409),
    );
    renderSettings();

    fireEvent.click(toggle("Require the technician name"));
    expect(toggle("Require the technician name")).toBeChecked();
    await act(async () => {
      fireEvent.click(screen.getByTestId("save-group-resultEntry"));
    });

    await waitFor(() =>
      expect(toggle("Require the technician name")).not.toBeChecked(),
    );
  });

  it("names what was saved before a later setting turned out changed by someone else", async () => {
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback(url.endsWith("ID=4") ? 409 : 200),
    );
    const addNotification = renderSettings();

    fireEvent.click(toggle("Require the technician name"));
    fireEvent.change(screen.getByLabelText("Critical result message"), {
      target: { value: "Call the doctor" },
    });
    await act(async () => {
      fireEvent.click(screen.getByTestId("save-group-resultEntry"));
    });

    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          message:
            "Saved Require the technician name on results. Critical result message was changed by someone else since this page loaded, so it was not saved. The page now shows the latest values: check them and save again.",
        }),
      ),
    );
    expect(screen.getByLabelText("Critical result message")).toHaveValue(
      "Call the ward",
    );
  });
});
