import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/contexts";
import { getFromOpenElisServer } from "../../utils/Utils";
import LabNumberManagement from "./LabNumberManagement";

vi.mock("../../layout/Layout", () => import("../../layout/contexts"));
vi.mock("../../utils/Utils", async () => ({
  ...(await vi.importActual("../../utils/Utils")),
  getFromOpenElisServer: vi.fn(),
}));

const currentPreviewUrl =
  "/rest/SampleEntryGenerateScanProvider?noIncrement=true";
const legacyPreviewUrl = `${currentPreviewUrl}&format=SITEYEARNUM`;
let previewCallbacks;

const renderForm = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{
            configurationProperties: { AccessionFormat: "SITEYEARNUM" },
            reloadConfiguration: vi.fn(),
            supportedLocales: [],
            enabledLanguages: {},
          }}
        >
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              notifications: [],
              addNotification: vi.fn(),
              removeNotification: vi.fn(),
            }}
          >
            <LabNumberManagement />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

const deliverPreview = (url, response) => {
  const callback = previewCallbacks.get(url);
  if (!callback) throw new Error(`No pending preview request: ${url}`);
  act(() => callback(response));
};

describe("Lab number preview failures", () => {
  beforeEach(() => {
    previewCallbacks = new Map();
    vi.mocked(getFromOpenElisServer).mockReset();
    vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) => {
      if (url === "/rest/labnumbermanagement") {
        callback({
          labNumberType: "ALPHANUM",
          usePrefix: false,
          alphanumPrefix: "",
        });
      } else {
        previewCallbacks.set(url, (response) => callback(response));
      }
    });
  });

  // catchesInversion: removing the current-preview guard throws on the
  // documented callback(undefined) HTTP/network failure contract. This
  // component test covers visible recovery, not backend accession allocation.
  it("retains the current preview after a failed response and accepts a later valid response", () => {
    renderForm();
    deliverPreview(currentPreviewUrl, { status: true, body: "DEV012600001" });
    expect(screen.getByText("Current Format: DEV012600001")).toBeVisible();

    deliverPreview(currentPreviewUrl, undefined);
    expect(screen.getByText("Current Format: DEV012600001")).toBeVisible();
    expect(screen.getByLabelText("Lab Number Type")).toBeEnabled();

    deliverPreview(currentPreviewUrl, { status: true, body: "DEV012600002" });
    expect(screen.getByText("Current Format: DEV012600002")).toBeVisible();
  });

  // catchesInversion: removing the legacy-preview guard crashes when the
  // preview request fails after selecting Legacy, before any form submission.
  it("keeps the legacy form and valid preview usable when its preview request fails", () => {
    renderForm();
    const selector = screen.getByLabelText("Lab Number Type");
    fireEvent.change(selector, { target: { value: "SITEYEARNUM" } });
    deliverPreview(legacyPreviewUrl, { status: true, body: "DEV012600003" });
    expect(screen.getByText("New Format: DEV012600003")).toBeVisible();

    deliverPreview(legacyPreviewUrl, undefined);
    expect(screen.getByText("New Format: DEV012600003")).toBeVisible();
    expect(selector).toHaveValue("SITEYEARNUM");
    expect(screen.getByRole("button", { name: "Submit" })).toBeEnabled();

    deliverPreview(legacyPreviewUrl, { status: true, body: "DEV012600004" });
    expect(screen.getByText("New Format: DEV012600004")).toBeVisible();
    fireEvent.change(selector, { target: { value: "ALPHANUM" } });
    expect(screen.getByRole("textbox", { name: "Prefix" })).toBeVisible();
  });
});
