import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../addonApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../addonApi")>();
  return { ...actual, loadExtensions: vi.fn(), requestToken: vi.fn() };
});

import { loadExtensions, requestToken, type AddonExtension } from "../addonApi";
import HeaderActionsSlot from "../HeaderActionsSlot";
import { SLOT_TOP_NAV_ACTIONS } from "../slots";

const header = (p: Partial<AddonExtension>): AddonExtension => ({
  addonId: "assistant",
  addonName: "AI assistant",
  version: "1.0.0",
  name: "assistant",
  slot: SLOT_TOP_NAV_ACTIONS,
  page: "panel.html",
  order: 10,
  title: "AI assistant",
  height: 240,
  icon: "magic-wand",
  ...p,
});

const renderAt = (path: string) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[path]}>
        <HeaderActionsSlot />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("top-nav-actions-slot", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders nothing without header extensions", async () => {
    vi.mocked(loadExtensions).mockResolvedValue([
      header({ slot: "result-lookup-side-panel-slot" }),
    ]);
    const { container } = renderAt("/");
    await Promise.resolve();
    expect(container).toBeEmptyDOMElement();
  });

  it("opens a sandboxed panel told only the screen it is on, and closes it", async () => {
    vi.mocked(loadExtensions).mockResolvedValue([header({})]);
    renderAt("/ResultValidation?accessionNumber=DEV0001");
    fireEvent.click(
      await screen.findByRole("button", { name: "AI assistant" }),
    );
    const panel = await screen.findByTestId("addon-header-panel");
    const frame = panel.querySelector("iframe") as HTMLIFrameElement;
    expect(frame.getAttribute("sandbox")).toBe("allow-scripts allow-forms");
    expect(frame.getAttribute("src")).toContain(
      "/rest/addons/assistant/ui/panel.html",
    );
    vi.mocked(requestToken).mockResolvedValue({
      token: "oeat.x",
      expiresIn: 600,
      scopes: ["ai.context"],
      context: { page: "result-validation" },
    });
    act(() => {
      window.dispatchEvent(
        new MessageEvent("message", {
          data: { type: "ready" },
          source: frame.contentWindow,
        }),
      );
    });
    await waitFor(() =>
      expect(requestToken).toHaveBeenCalledWith(
        "assistant",
        SLOT_TOP_NAV_ACTIONS,
        { page: "result-validation" },
      ),
    );

    fireEvent.click(screen.getByRole("button", { name: "Close AI assistant" }));
    expect(screen.queryByTestId("addon-header-panel")).toBeNull();
  });
});
