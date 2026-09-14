import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../languages/en.json";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { ConfigurationContext, NotificationContext } from "./layout/contexts";
import Login from "./Login";

vi.mock("./utils/BrandingUtils", () => ({
  getBranding: vi.fn((callback) => callback(null)),
}));

const renderLogin = ({ enabledLanguages, onChangeLanguage } = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { authenticated: false },
          refresh: vi.fn(),
        }}
      >
        <ConfigurationContext.Provider
          value={{
            configurationProperties: {
              useFormLogin: "true",
              useOauth: "false",
              useSaml: "false",
            },
            enabledLanguages,
          }}
        >
          <NotificationContext.Provider
            value={{
              addNotification: vi.fn(),
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
            }}
          >
            <Login onChangeLanguage={onChangeLanguage} />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

describe("Login", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  // Guards the selector Login carries in its own right, because it renders
  // outside Layout and so cannot reach the header's.
  test("offers the enabled languages and switches to the one picked", async () => {
    const user = userEvent.setup();
    const onChangeLanguage = vi.fn();
    renderLogin({
      enabledLanguages: {
        en: { label: "English" },
        fr: { label: "Français" },
      },
      onChangeLanguage,
    });

    const selector = screen.getByLabelText("Select Locale");
    expect([...selector.options].map((option) => option.value)).toEqual([
      "en",
      "fr",
    ]);

    await user.selectOptions(selector, "fr");

    expect(onChangeLanguage).toHaveBeenCalledWith("fr");
  });

  test("submits the credentials entered in the Carbon login fields", async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText("Username"), "admin");
    await user.type(screen.getByLabelText("Password"), "adminADMIN!");
    await user.click(screen.getByRole("button", { name: /^Login/ }));

    await waitFor(() => expect(fetch).toHaveBeenCalledOnce());
    const [, request] = fetch.mock.calls[0];
    const submitted = new URLSearchParams(request.body);

    expect(submitted.get("loginName")).toBe("admin");
    expect(submitted.get("password")).toBe("adminADMIN!");
    expect(submitted.has("username")).toBe(false);
  });
});
