import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../languages/en.json";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { ConfigurationContext, NotificationContext } from "./layout/Layout";
import { getBranding } from "./utils/BrandingUtils";
import Login from "./Login";

vi.mock("./utils/BrandingUtils", () => ({
  getBranding: vi.fn((callback) => callback(null)),
}));

const renderLogin = () =>
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
          }}
        >
          <NotificationContext.Provider
            value={{
              addNotification: vi.fn(),
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
            }}
          >
            <Login />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const LOGO_ALT = "fullsize logo";
const DEFAULT_LOGO_SRC = "images/openelis_logo_full.png";

describe("Login", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    sessionStorage.clear();
    vi.mocked(getBranding).mockImplementation((callback) => callback(null));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.clearAllMocks();
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

  test("does not render the default logo while branding is still loading", () => {
    // Branding fetch never resolves — the flash window the bug lives in
    vi.mocked(getBranding).mockImplementation(() => {});

    renderLogin();

    expect(screen.queryByAltText(LOGO_ALT)).toBeNull();
  });

  test("renders the custom login logo once branding resolves", async () => {
    vi.mocked(getBranding).mockImplementation((callback) =>
      callback({ loginLogoUrl: "/images/branding/custom-logo.png" }),
    );

    renderLogin();

    await waitFor(() => {
      const logos = screen.getAllByAltText(LOGO_ALT);
      expect(logos).toHaveLength(2);
      logos.forEach((logo) =>
        expect(logo).toHaveAttribute(
          "src",
          expect.stringContaining("/images/branding/custom-logo.png"),
        ),
      );
    });
  });

  test("renders the default logo when no custom logo is configured", async () => {
    vi.mocked(getBranding).mockImplementation((callback) => callback(null));

    renderLogin();

    await waitFor(() => {
      const logos = screen.getAllByAltText(LOGO_ALT);
      logos.forEach((logo) =>
        expect(logo).toHaveAttribute("src", DEFAULT_LOGO_SRC),
      );
    });
  });

  test("seeds the logo from sessionStorage cache on first paint", () => {
    sessionStorage.setItem(
      "openelis.loginLogoUrl",
      "/images/branding/cached-logo.png",
    );
    // Branding fetch never resolves — cache must carry the first paint
    vi.mocked(getBranding).mockImplementation(() => {});

    renderLogin();

    const logos = screen.getAllByAltText(LOGO_ALT);
    logos.forEach((logo) =>
      expect(logo).toHaveAttribute(
        "src",
        expect.stringContaining("/images/branding/cached-logo.png"),
      ),
    );
  });

  test("does not bump the cache-busting version when the logo URL is unchanged", async () => {
    const branding = { loginLogoUrl: "/images/branding/custom-logo.png" };
    vi.mocked(getBranding).mockImplementation((callback) => callback(branding));

    const { rerender } = renderLogin();
    await waitFor(() => {
      expect(screen.getAllByAltText(LOGO_ALT)[0]).toHaveAttribute(
        "src",
        expect.stringContaining("?v="),
      );
    });
    const initialSrc = screen.getAllByAltText(LOGO_ALT)[0].getAttribute("src");

    // Re-render (e.g. the 10s session poll) with the same branding response
    rerender(
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
            }}
          >
            <NotificationContext.Provider
              value={{
                addNotification: vi.fn(),
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
              }}
            >
              <Login />
            </NotificationContext.Provider>
          </ConfigurationContext.Provider>
        </UserSessionDetailsContext.Provider>
      </IntlProvider>,
    );

    expect(screen.getAllByAltText(LOGO_ALT)[0].getAttribute("src")).toBe(
      initialSrc,
    );
  });
});
