import React from "react";
import { act, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../addonApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../addonApi")>();
  return {
    ...actual,
    loadExtensions: vi.fn(),
    requestToken: vi.fn(),
  };
});

import { loadExtensions, requestToken, type AddonExtension } from "../addonApi";
import ExtensionSlot from "../ExtensionSlot";
import AddonFrame from "../AddonFrame";

const extension: AddonExtension = {
  addonId: "hello",
  addonName: "Hello",
  version: "1.0.0",
  name: "hello-panel",
  slot: "result-validation-side-panel-slot",
  page: "panel.html",
  order: 10,
  title: "Hello panel",
  height: 200,
};

const renderIntl = (node: React.ReactNode) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

const fromFrame = (source: MessageEventSource | null, data: unknown) =>
  act(() => {
    window.dispatchEvent(new MessageEvent("message", { data, source }));
  });

describe("AddonFrame", () => {
  beforeEach(() => vi.clearAllMocks());

  it("is sandboxed without same-origin and loads the add-on's page", () => {
    renderIntl(<AddonFrame extension={extension} />);
    const frame = screen.getByTitle("Hello panel");
    expect(frame.getAttribute("sandbox")).toBe("allow-scripts allow-forms");
    expect(frame.getAttribute("sandbox")).not.toContain("allow-same-origin");
    expect(frame.getAttribute("src")).toMatch(
      /\/rest\/addons\/hello\/ui\/panel\.html$/,
    );
  });

  it("answers the frame's ready with a scoped token and the filtered context", async () => {
    vi.mocked(requestToken).mockResolvedValue({
      token: "oeat.t.s",
      expiresIn: 600,
      scopes: ["orders.read"],
      context: { page: "result-validation", accessionNumbers: ["A1"] },
    });
    renderIntl(
      <AddonFrame
        extension={extension}
        state={{ page: "result-validation", accessionNumbers: ["A1"] }}
      />,
    );
    const frame = screen.getByTitle("Hello panel") as HTMLIFrameElement;
    const post = vi.spyOn(frame.contentWindow!, "postMessage");

    await fromFrame(frame.contentWindow, { type: "ready" });

    await waitFor(() => expect(post).toHaveBeenCalled());
    expect(requestToken).toHaveBeenCalledWith(
      "hello",
      "result-validation-side-panel-slot",
      { page: "result-validation", accessionNumbers: ["A1"] },
    );
    const [message, target] = post.mock.calls[0];
    expect(target).toBe("*");
    expect(message).toMatchObject({
      type: "context",
      slot: "result-validation-side-panel-slot",
      extension: "hello-panel",
      token: "oeat.t.s",
      context: { page: "result-validation", accessionNumbers: ["A1"] },
      apiBase: "/addons/hello/api/",
      locale: "en",
    });
  });

  it("ignores messages that do not come from its own frame", async () => {
    renderIntl(<AddonFrame extension={extension} />);
    await fromFrame(window, { type: "ready" });
    await fromFrame(null, { type: "ready" });
    expect(requestToken).not.toHaveBeenCalled();
  });

  it("says so when the token is refused", async () => {
    vi.mocked(requestToken).mockRejectedValue(new Error("403"));
    renderIntl(<AddonFrame extension={extension} />);
    const frame = screen.getByTitle("Hello panel") as HTMLIFrameElement;
    await fromFrame(frame.contentWindow, { type: "ready" });
    expect(
      await screen.findByText("The extension could not be loaded"),
    ).toBeInTheDocument();
  });
});

describe("ExtensionSlot", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders nothing at all without extensions", async () => {
    vi.mocked(loadExtensions).mockResolvedValue([]);
    const { container } = renderIntl(
      <ExtensionSlot name="result-validation-side-panel-slot" />,
    );
    await waitFor(() => expect(loadExtensions).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the frames of its own slot only", async () => {
    vi.mocked(loadExtensions).mockResolvedValue([
      extension,
      { ...extension, name: "menu", slot: "app-menu-slot", title: "Menu page" },
    ]);
    const { container } = renderIntl(
      <ExtensionSlot name="result-validation-side-panel-slot" />,
    );
    expect(await screen.findByTitle("Hello panel")).toBeInTheDocument();
    expect(screen.queryByTitle("Menu page")).not.toBeInTheDocument();
    expect(
      container.querySelector(
        '[data-extension-slot-name="result-validation-side-panel-slot"]',
      ),
    ).not.toBeNull();
  });
});
