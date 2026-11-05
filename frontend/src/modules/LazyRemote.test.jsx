import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../languages/en.json";

const runtime = vi.hoisted(() => ({
  loadRemote: vi.fn(),
  registerRemotes: vi.fn(),
}));

vi.mock("@module-federation/runtime", () => ({
  loadRemote: runtime.loadRemote,
  registerRemotes: runtime.registerRemotes,
}));

import LazyRemote, {
  registerModuleRemotes,
  resetRemoteComponents,
} from "./LazyRemote";

const Page = ({ who }) => <p>remote page for {who}</p>;

function mount() {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <LazyRemote
        moduleId="alpha"
        remoteName="alpha"
        expose="./Page"
        who="83"
      />
    </IntlProvider>,
  );
}

describe("LazyRemote", () => {
  let consoleError;

  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    runtime.loadRemote.mockReset();
    runtime.registerRemotes.mockReset();
    resetRemoteComponents();
    consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    registerModuleRemotes([
      {
        name: "alpha",
        entry: "/api/modules/alpha/remoteEntry.js",
        type: "module",
      },
    ]);
  });

  afterEach(() => {
    consoleError.mockRestore();
    vi.useRealTimers();
  });

  it("renders the remote's component with the props it was given", async () => {
    runtime.loadRemote.mockResolvedValue({ default: Page });
    mount();

    expect(await screen.findByText("remote page for 83")).toBeInTheDocument();
    expect(runtime.loadRemote).toHaveBeenCalledWith("alpha/Page");
  });

  it("retries a failed import without reloading, then recovers on Retry with a fresh entry", async () => {
    runtime.loadRemote.mockRejectedValue(
      new Error(
        "[ Federation Runtime ]: Failed to load script resources. #RUNTIME-008",
      ),
    );
    mount();

    expect(await screen.findByText("Module unavailable")).toBeInTheDocument();
    expect(
      screen.getByText(
        "The module alpha could not be shown: Failed to load script resources. #RUNTIME-008",
      ),
    ).toBeInTheDocument();
    expect(runtime.loadRemote).toHaveBeenCalledTimes(3);
    expect(runtime.registerRemotes).toHaveBeenCalledTimes(2);
    const [refreshed, options] = runtime.registerRemotes.mock.calls[1];
    expect(options).toEqual({ force: true });
    expect(refreshed[0].entry).toMatch(/remoteEntry\.js\?retry=\d+$/);

    runtime.loadRemote.mockResolvedValue({ default: Page });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    });

    expect(await screen.findByText("remote page for 83")).toBeInTheDocument();
    expect(runtime.loadRemote).toHaveBeenCalledTimes(4);
  });
});
