import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../addonApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../addonApi")>();
  return {
    ...actual,
    listAddons: vi.fn(),
    getCatalog: vi.fn(),
    installAddon: vi.fn(),
    setAddonEnabled: vi.fn(),
    saveAddonConfig: vi.fn(),
    removeAddon: vi.fn(),
    rotateCredentials: vi.fn(),
    invalidateExtensions: vi.fn(),
  };
});

import {
  getCatalog,
  installAddon,
  listAddons,
  setAddonEnabled,
  type AddonView,
} from "../addonApi";
import Extensions from "../Extensions";

const addon = (p: Partial<AddonView>): AddonView => ({
  id: "hello",
  name: "Hello add-on",
  version: "1.0.0",
  publisher: "example",
  provenance: "UNVERIFIED",
  verifiedPublisher: null,
  image: "registry.example.org/hello@sha256:" + "a".repeat(64),
  enabled: false,
  scopes: ["orders.read"],
  events: ["order.created"],
  extensions: [
    {
      name: "hello-panel",
      slot: "result-lookup-side-panel-slot",
      page: "panel.html",
      title: "Hello",
    },
  ],
  configSchema: [],
  config: {},
  deliveries: {},
  ...p,
});

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <Extensions />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("Extensions page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getCatalog).mockResolvedValue({
      scopes: [],
      events: [],
      slots: [],
    });
  });

  it("names the publisher only when the signature verifies it", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: ["example"],
      addons: [
        addon({
          id: "v",
          provenance: "VERIFIED",
          verifiedPublisher: "example",
        }),
        addon({ id: "u", publisher: "acme" }),
        addon({ id: "m", publisher: "acme", provenance: "MISMATCH" }),
      ],
    });
    renderPage();
    expect(await screen.findByTestId("addon-provenance-v")).toHaveTextContent(
      "Verified publisher: example",
    );

    const unverified = screen.getByTestId("addon-provenance-u");
    expect(unverified).toHaveTextContent("Unverified publisher");
    expect(unverified).not.toHaveTextContent("Verified publisher: acme");

    const mismatch = screen.getByTestId("addon-provenance-m");
    expect(mismatch).toHaveTextContent("Signature does not match the contents");
    expect(mismatch).not.toHaveTextContent("Verified publisher");
  });

  it("shows the permissions in plain words", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [addon({})],
    });
    renderPage();
    const permissions = await screen.findByTestId("addon-permissions-hello");
    expect(permissions).toHaveTextContent(
      "Lab numbers and received dates of the orders it is shown",
    );
    expect(permissions).toHaveTextContent("A new order is saved");
    expect(permissions).toHaveTextContent("Patient results page");
  });

  it("asks for consent before turning an add-on on", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [addon({})],
    });
    vi.mocked(setAddonEnabled).mockResolvedValue(addon({ enabled: true }));
    renderPage();
    fireEvent.click(await screen.findByRole("switch"));
    expect(setAddonEnabled).not.toHaveBeenCalled();
    expect(
      await screen.findByText("Turn on Hello add-on?"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Turn on" }));
    await waitFor(() =>
      expect(setAddonEnabled).toHaveBeenCalledWith("hello", true),
    );
  });

  it("turns an add-on off without asking", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [addon({ enabled: true })],
    });
    vi.mocked(setAddonEnabled).mockResolvedValue(addon({}));
    renderPage();
    fireEvent.click(await screen.findByRole("switch"));
    await waitFor(() =>
      expect(setAddonEnabled).toHaveBeenCalledWith("hello", false),
    );
  });

  it("installs a dropped package and shows the credentials once", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [],
    });
    vi.mocked(installAddon).mockResolvedValue({
      upgraded: false,
      addon: addon({ provenance: "MISMATCH" }),
      credentials: { apiKey: "oeak_secret", webhookSecret: "whsec_secret" },
      composeSnippet: "services:\n  hello:\n",
    });
    renderPage();
    await screen.findByTestId("addon-admin-empty");
    const file = new File(["zip"], "hello.zip", { type: "application/zip" });
    const input = screen
      .getByTestId("addon-admin")
      .querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => expect(installAddon).toHaveBeenCalledWith(file));
    const summary = await screen.findByTestId("addon-installed");
    expect(summary).toHaveTextContent(
      "Hello add-on 1.0.0 is installed and turned off",
    );
    expect(summary).toHaveTextContent("Signature does not match the contents");
    expect(summary).toHaveTextContent("oeak_secret");
    expect(summary).toHaveTextContent("whsec_secret");
  });
});
