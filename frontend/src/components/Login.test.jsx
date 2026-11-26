import React from "react";
import { act, render, screen } from "@testing-library/react";
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
    vi.mocked(getBranding).mockImplementation((callback) =>
      callback(null),
    );
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
    vi.mocked(getBranding).mockImplementation((callback) =>
      callback(null),
    );

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
      JSON.stringify({
        url: "/images/branding/cached-logo.png",
        version: 7,
      }),
    );
    vi.mocked(getBranding).mockImplementation(() => {});

    renderLogin();

    const logos = screen.getAllByAltText(LOGO_ALT);
    logos.forEach((logo) =>
      expect(logo).toHaveAttribute(
        "src",
        expect.stringContaining("/images/branding/cached-logo.png?v=7"),
      ),
    );
  });

  test("does not bump the cache-busting version when branding is unchanged", async () => {
    const branding = {
      loginLogoUrl: "/images/branding/custom-logo.png",
      logoRevision: 1,
    };
    let brandingCallback;
    vi.mocked(getBranding).mockImplementation((callback) => {
      brandingCallback = callback;
      callback(branding);
    });

    renderLogin();

    await waitFor(() => {
      expect(screen.getAllByAltText(LOGO_ALT)[0]).toHaveAttribute(
        "src",
        expect.stringContaining("?v=1"),
      );
    });
    const initialSrc = screen.getAllByAltText(LOGO_ALT)[0].getAttribute("src");

    await act(async () => {
      brandingCallback(branding);
    });

    expect(screen.getAllByAltText(LOGO_ALT)[0].getAttribute("src")).toBe(
      initialSrc,
    );
  });

  test("bumps the cache-busting version when the logo revision changes", async () => {
    let brandingCallback;
    vi.mocked(getBranding).mockImplementation((callback) => {
      brandingCallback = callback;
      callback({
        loginLogoUrl: "/images/branding/custom-logo.png",
        logoRevision: 1,
      });
    });

    renderLogin();

    await waitFor(() => {
      expect(screen.getAllByAltText(LOGO_ALT)[0]).toHaveAttribute(
        "src",
        expect.stringContaining("?v=1"),
      );
    });

    await act(async () => {
      brandingCallback({
        loginLogoUrl: "/images/branding/custom-logo.png",
        logoRevision: 2,
      });
    });

    expect(screen.getAllByAltText(LOGO_ALT)[0]).toHaveAttribute(
      "src",
      expect.stringContaining("?v=2"),
    );
  });
});
