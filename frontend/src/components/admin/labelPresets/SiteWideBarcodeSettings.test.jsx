import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import SiteWideBarcodeSettings from "./SiteWideBarcodeSettings";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("../../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      addNotification: () => {},
      notificationVisible: false,
      setNotificationVisible: () => {},
    }),
  };
});

import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";

const API = "/api/siteSettings/barcode";
const ORDER_ENTRY = messages["admin.labelPresets.siteWide.prePrint.orderEntry"];
const SEPARATE = messages["admin.labelPresets.siteWide.prePrint.separate"];
const SAVE = messages["admin.labelPresets.siteWide.save"];

const renderCard = (addNotification = vi.fn()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ addNotification, notificationVisible: false }}
      >
        <SiteWideBarcodeSettings />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const stored = (settings) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === API) callback(settings);
  });

const prefixInput = () =>
  screen.getByLabelText(messages["admin.labelPresets.siteWide.prefix.label"]);

describe("SiteWideBarcodeSettings (OGC-1217)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("shows a stored separate series with its prefix, enabled", async () => {
    stored({
      prePrintUseAltAccession: true,
      prePrintAltAccessionPrefix: "ABCD",
    });
    renderCard();

    expect(
      await screen.findByText(messages["admin.labelPresets.siteWide.title"]),
    ).toBeInTheDocument();
    expect(
      screen.getByText(messages["admin.labelPresets.siteWide.scope"]),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(SEPARATE)).toBeChecked();
    expect(screen.getByLabelText(ORDER_ENTRY)).not.toBeChecked();
    expect(prefixInput()).toBeEnabled();
    expect(prefixInput()).toHaveValue("ABCD");
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      API,
      expect.any(Function),
    );
  });

  test("shows the order-entry pool with the prefix disabled when the flag is off", async () => {
    stored({ prePrintUseAltAccession: false });
    renderCard();

    expect(await screen.findByLabelText(ORDER_ENTRY)).toBeChecked();
    expect(screen.getByLabelText(SEPARATE)).not.toBeChecked();
    expect(prefixInput()).toBeDisabled();
    expect(prefixInput()).toHaveValue("");
  });

  test("choosing the separate series enables the prefix and a short prefix is refused without a request", async () => {
    stored({ prePrintUseAltAccession: false, prePrintAltAccessionPrefix: "" });
    renderCard();
    await screen.findByLabelText(ORDER_ENTRY);

    fireEvent.click(screen.getByLabelText(SEPARATE));
    expect(prefixInput()).toBeEnabled();
    fireEvent.change(prefixInput(), { target: { value: "AB" } });
    fireEvent.click(screen.getByRole("button", { name: SAVE }));

    expect(
      screen.getByText(messages["admin.labelPresets.siteWide.prefix.invalid"]),
    ).toBeInTheDocument();
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });

  test("the prefix input keeps at most 4 characters", async () => {
    stored({ prePrintUseAltAccession: true, prePrintAltAccessionPrefix: "" });
    renderCard();
    await screen.findByLabelText(SEPARATE);

    expect(prefixInput()).toHaveAttribute("maxlength", "4");
  });

  test("saves the separate series with a 4-character prefix and confirms", async () => {
    stored({ prePrintUseAltAccession: false, prePrintAltAccessionPrefix: "" });
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        callback({ status: 200, ok: true, text: async () => payload });
      },
    );
    const addNotification = vi.fn();
    renderCard(addNotification);
    await screen.findByLabelText(ORDER_ENTRY);

    fireEvent.click(screen.getByLabelText(SEPARATE));
    fireEvent.change(prefixInput(), { target: { value: "ab12" } });
    fireEvent.click(screen.getByRole("button", { name: SAVE }));

    await waitFor(() => {
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        API,
        JSON.stringify({
          prePrintUseAltAccession: true,
          prePrintAltAccessionPrefix: "AB12",
        }),
        expect.any(Function),
      );
    });
    expect(addNotification).toHaveBeenCalledWith({
      kind: "success",
      title: messages["admin.labelPresets.siteWide.saved"],
    });
    expect(prefixInput()).toHaveValue("AB12");
  });

  test("sends the order-entry pool as false and keeps the stored prefix for later", async () => {
    stored({
      prePrintUseAltAccession: true,
      prePrintAltAccessionPrefix: "ABCD",
    });
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        callback({ status: 200, ok: true, text: async () => payload });
      },
    );
    renderCard();
    await screen.findByLabelText(SEPARATE);

    fireEvent.click(screen.getByLabelText(ORDER_ENTRY));
    expect(prefixInput()).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: SAVE }));

    await waitFor(() => {
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        API,
        JSON.stringify({
          prePrintUseAltAccession: false,
          prePrintAltAccessionPrefix: "ABCD",
        }),
        expect.any(Function),
      );
    });
  });

  test("shows the server's reason when the save is refused", async () => {
    stored({ prePrintUseAltAccession: false, prePrintAltAccessionPrefix: "" });
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        callback({
          status: 422,
          ok: false,
          text: async () =>
            JSON.stringify({
              fieldErrors: [
                {
                  field: "prePrintAltAccessionPrefix",
                  defaultMessage: "{error.sitesettings.barcode.prefix.format}",
                },
              ],
              globalErrors: [],
            }),
        });
      },
    );
    const addNotification = vi.fn();
    renderCard(addNotification);
    await screen.findByLabelText(ORDER_ENTRY);

    fireEvent.click(screen.getByLabelText(SEPARATE));
    fireEvent.change(prefixInput(), { target: { value: "AB12" } });
    fireEvent.click(screen.getByRole("button", { name: SAVE }));

    await waitFor(() => {
      expect(addNotification).toHaveBeenCalledWith({
        kind: "error",
        title: messages["admin.labelPresets.siteWide.saveFailed"],
        message: expect.stringContaining(
          messages["error.sitesettings.barcode.prefix.format"],
        ),
      });
    });
  });

  test("tells the user when the settings cannot be loaded", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(undefined);
    });
    renderCard();

    expect(
      await screen.findByText(
        messages["admin.labelPresets.siteWide.loadFailed"],
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: SAVE })).toBeDisabled();
  });

  test("Save stays disabled until something changes", async () => {
    stored({
      prePrintUseAltAccession: true,
      prePrintAltAccessionPrefix: "ABCD",
    });
    renderCard();
    await screen.findByLabelText(SEPARATE);

    expect(screen.getByRole("button", { name: SAVE })).toBeDisabled();
    fireEvent.change(prefixInput(), { target: { value: "ABCE" } });
    expect(screen.getByRole("button", { name: SAVE })).toBeEnabled();
  });
});
