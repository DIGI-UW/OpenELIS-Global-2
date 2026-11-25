import React from "react";
import { act, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import UserSessionDetailsContext from "../UserSessionDetailsContext";

const api = vi.hoisted(() => ({ calls: [] }));

vi.mock("../components/utils/Utils", () => ({
  getFromOpenElisServer: (endpoint, callback) => {
    api.calls.push({ endpoint, callback });
  },
}));

vi.mock("./LazyRemote", () => ({
  registerModuleRemotes: vi.fn(),
}));

import ModuleHostProvider, { useModuleHost } from "./ModuleHostProvider";

const registry = {
  frontendContractVersion: "1.0.0",
  modules: [],
};

function Probe() {
  const host = useModuleHost();
  return (
    <p data-testid="host">
      {host.status}:{host.registry ? "registry" : "none"}
    </p>
  );
}

function mount(userSessionDetails) {
  const view = render(
    <UserSessionDetailsContext.Provider value={{ userSessionDetails }}>
      <ModuleHostProvider>
        <Probe />
      </ModuleHostProvider>
    </UserSessionDetailsContext.Provider>,
  );
  const session = (details) =>
    view.rerender(
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: details }}
      >
        <ModuleHostProvider>
          <Probe />
        </ModuleHostProvider>
      </UserSessionDetailsContext.Provider>,
    );
  return { session };
}

const answer = (call, data) => act(async () => call.callback(data));

describe("ModuleHostProvider", () => {
  beforeEach(() => {
    api.calls.length = 0;
  });

  it("requests the registry at mount, before the session answers, and keeps that answer once signed in", async () => {
    const { session } = mount({});

    expect(api.calls).toHaveLength(1);
    expect(api.calls[0].endpoint).toBe("/rest/module-registry");
    expect(screen.getByTestId("host")).toHaveTextContent("loading:none");

    await answer(api.calls[0], registry);
    session({ authenticated: true });

    expect(screen.getByTestId("host")).toHaveTextContent("ready:registry");
    expect(api.calls).toHaveLength(1);
  });

  it("keeps a read in flight across the session answer instead of starting a second one", async () => {
    const { session } = mount({});
    // The read left at mount, before the session answered ...
    expect(api.calls).toHaveLength(1);
    session({ authenticated: true });

    // ... and the confirmed sign-in rides on it instead of starting another.
    expect(api.calls).toHaveLength(1);
    await answer(api.calls[0], registry);
    expect(screen.getByTestId("host")).toHaveTextContent("ready:registry");
  });

  it("reads again after a refused speculative read once the session reports a sign-in", async () => {
    const { session } = mount({});

    await answer(api.calls[0], undefined);
    expect(screen.getByTestId("host")).toHaveTextContent("idle:none");

    session({ authenticated: true });
    expect(api.calls).toHaveLength(2);
    await answer(api.calls[1], registry);
    expect(screen.getByTestId("host")).toHaveTextContent("ready:registry");
  });

  it("reports an error only for a signed-in user whose read fails", async () => {
    const { session } = mount({});
    session({ authenticated: true });

    await answer(api.calls[0], undefined);
    expect(screen.getByTestId("host")).toHaveTextContent("error:none");
  });

  it("asks the registry for the active locale and reads again when it changes", async () => {
    const withLocale = (locale) => (
      <IntlProvider locale={locale} messages={{}}>
        <UserSessionDetailsContext.Provider
          value={{ userSessionDetails: { authenticated: true } }}
        >
          <ModuleHostProvider>
            <Probe />
          </ModuleHostProvider>
        </UserSessionDetailsContext.Provider>
      </IntlProvider>
    );
    const view = render(withLocale("fr-MG"));

    expect(api.calls.map((call) => call.endpoint)).toEqual([
      "/rest/module-registry?locale=fr-MG",
    ]);
    await answer(api.calls[0], registry);
    expect(screen.getByTestId("host")).toHaveTextContent("ready:registry");

    // A header locale switch: the registry carries that locale's bundles,
    // so the shell reads it again with the new locale.
    view.rerender(withLocale("en"));
    expect(api.calls.map((call) => call.endpoint)).toEqual([
      "/rest/module-registry?locale=fr-MG",
      "/rest/module-registry?locale=en",
    ]);
    await answer(api.calls[1], registry);
    expect(screen.getByTestId("host")).toHaveTextContent("ready:registry");
  });

  it("drops the registry and stops reading when the session is signed out", async () => {
    const { session } = mount({});
    await answer(api.calls[0], registry);

    session({ authenticated: false });
    expect(screen.getByTestId("host")).toHaveTextContent("idle:none");
    expect(api.calls).toHaveLength(1);
  });
});
