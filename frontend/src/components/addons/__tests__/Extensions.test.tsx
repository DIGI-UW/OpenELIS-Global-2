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
    loadExtensions: vi.fn(() => Promise.resolve([])),
    getAccessLog: vi.fn(),
  };
});

import {
  getAccessLog,
  getCatalog,
  loadExtensions,
  installAddon,
  listAddons,
  saveAddonConfig,
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

describe("Extensions page: identities, access log, settings page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(loadExtensions).mockResolvedValue([]);
    vi.mocked(getCatalog).mockResolvedValue({
      scopes: [],
      events: [],
      slots: [],
      identifyingScopes: ["patient.identity"],
    });
  });

  it("warns when an add-on may see who patients are", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [
        addon({ id: "mcp", scopes: ["orders.search", "patient.identity"] }),
        addon({ id: "plain", scopes: ["orders.read"] }),
      ],
    });
    renderPage();
    expect(
      await screen.findByTestId("addon-identifying-mcp"),
    ).toHaveTextContent("see who patients are");
    expect(screen.queryByTestId("addon-identifying-plain")).toBeNull();
  });

  it("shows the access log with what the add-on reported", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [addon({ id: "mcp", enabled: true })],
    });
    vi.mocked(getAccessLog).mockResolvedValue({
      addon: "mcp",
      retentionDays: 180,
      entries: [
        {
          at: "2026-10-22T08:15:02Z",
          addonId: "mcp",
          credential: "service",
          actor: "agent:a7f3 Claude Code",
          purpose: "get_order",
          method: "GET",
          endpoint: "/orders/{accession}/results",
          subjectType: "order",
          subject: "0221026-0004",
          status: 200,
          rows: 12,
        },
      ],
    });
    renderPage();
    fireEvent.click(await screen.findByTestId("addon-access-log-toggle-mcp"));
    const log = await screen.findByTestId("addon-access-log-mcp");
    await waitFor(() =>
      expect(log).toHaveTextContent("agent:a7f3 Claude Code"),
    );
    expect(log).toHaveTextContent("0221026-0004");
    expect(log).toHaveTextContent("Kept 180 days");
    expect(getAccessLog).toHaveBeenCalledWith("mcp", {
      actor: "",
      subject: "",
    });
  });

  it("offers the settings page only when the add-on has one and is on", async () => {
    vi.mocked(loadExtensions).mockResolvedValue([
      {
        addonId: "mcp",
        addonName: "MCP",
        version: "1.0.0",
        name: "mcp-settings",
        slot: "addon-settings-page-slot",
        page: "admin.html",
        order: 10,
        title: "MCP access",
        height: 600,
      },
    ]);
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [
        addon({ id: "mcp", enabled: true }),
        addon({ id: "hello", enabled: true }),
      ],
    });
    renderPage();
    expect(await screen.findByTestId("addon-settings-open-mcp")).toBeVisible();
    expect(screen.queryByTestId("addon-settings-open-hello")).toBeNull();
  });
});

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

  it("shows where an add-on connects outside OpenELIS, or that it does not", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [
        addon({ id: "net", egress: ["openrouter.ai", "api.openai.com:443"] }),
        addon({ id: "local" }),
      ],
    });
    renderPage();
    expect(await screen.findByTestId("addon-egress-net")).toHaveTextContent(
      "openrouter.ai",
    );
    expect(screen.getByTestId("addon-egress-net")).toHaveTextContent(
      "api.openai.com:443",
    );
    expect(screen.getByTestId("addon-permissions-local")).toHaveTextContent(
      "Nowhere: it needs nothing outside OpenELIS.",
    );
  });

  it("never shows a saved secret and sends it only when it was typed", async () => {
    vi.mocked(listAddons).mockResolvedValue({
      trustedPublishers: [],
      addons: [
        addon({
          configSchema: [
            {
              key: "model",
              type: "string",
              label: "Model",
              required: false,
              defaultValue: "m1",
              options: [],
              min: null,
              max: null,
            },
            {
              key: "apiKey",
              type: "secret",
              label: "API key",
              required: false,
              defaultValue: null,
              options: [],
              min: null,
              max: null,
            },
          ],
          config: { model: "m1" },
          secretsSet: ["apiKey"],
        }),
      ],
    });
    vi.mocked(saveAddonConfig).mockResolvedValue({ model: "m1" });
    renderPage();
    const key = (await screen.findByLabelText("API key")) as HTMLInputElement;
    expect(key.value).toBe("");
    expect(
      screen.getByText("A value is saved. Leave empty to keep it."),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(saveAddonConfig).toHaveBeenCalledWith("hello", { model: "m1" }),
    );

    fireEvent.change(key, { target: { value: "sk-new" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(saveAddonConfig).toHaveBeenLastCalledWith("hello", {
        model: "m1",
        apiKey: "sk-new",
      }),
    );

    fireEvent.click(
      screen.getByRole("button", { name: "Clear the saved value" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(saveAddonConfig).toHaveBeenLastCalledWith("hello", {
        model: "m1",
        apiKey: "",
      }),
    );
  });
});
