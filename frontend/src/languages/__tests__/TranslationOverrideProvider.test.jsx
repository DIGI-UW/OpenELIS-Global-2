import React from "react";
import { act, render, cleanup, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { FormattedMessage, IntlProvider } from "react-intl";

/**
 * The override as it is actually wired: the flag read from the configuration the
 * app already fetches, and the locale and bundled messages read from the
 * IntlProvider that already resolved them.
 *
 * What matters here is the gate. With the flag off nothing is requested at all,
 * so a deployment that has not opted in cannot be affected by a stray file; with
 * it on, only the ids a file names change.
 */

import TranslationOverrideProvider from "../TranslationOverrideProvider";
import { ConfigurationContext as Ctx } from "../../components/layout/contexts";

const BUNDLED = { "a.save": "Save", "b.submit": "Submit" };

const flush = () => new Promise((r) => setTimeout(r, 0));

const renderApp = ({ flag, body }) => {
  const fetchImpl = vi.fn(async () => ({
    ok: true,
    status: 200,
    headers: { get: () => "application/json" },
    json: async () => body,
  }));
  global.fetch = fetchImpl;

  render(
    <Ctx.Provider
      value={{
        configurationProperties: { OVERRIDE_DEFAULT_TRANSLATION: flag },
      }}
    >
      <IntlProvider locale="en" defaultLocale="en" messages={BUNDLED}>
        <TranslationOverrideProvider>
          <p data-testid="save">
            <FormattedMessage id="a.save" />
          </p>
          <p data-testid="submit">
            <FormattedMessage id="b.submit" />
          </p>
        </TranslationOverrideProvider>
      </IntlProvider>
    </Ctx.Provider>,
  );
  return fetchImpl;
};

describe("the override, as wired into the app", () => {
  beforeEach(() => {
    cleanup();
    delete global.fetch;
  });

  it("asks for nothing and changes nothing when the flag is off", async () => {
    const fetchImpl = renderApp({ flag: "false", body: { "a.save": "Keep" } });
    await flush();

    expect(
      fetchImpl,
      "a deployment that has not opted in must not even be asked",
    ).not.toHaveBeenCalled();
    expect(screen.getByTestId("save")).toHaveTextContent("Save");
  });

  it("treats a missing flag as off", async () => {
    const fetchImpl = renderApp({
      flag: undefined,
      body: { "a.save": "Keep" },
    });
    await flush();

    expect(fetchImpl).not.toHaveBeenCalled();
    expect(screen.getByTestId("save")).toHaveTextContent("Save");
  });

  it("renders the deployment's wording when the flag is on", async () => {
    renderApp({ flag: "true", body: { "a.save": "Keep" } });
    await flush();

    expect(screen.getByTestId("save")).toHaveTextContent("Keep");
  });

  it("leaves the ids the deployment did not name alone", async () => {
    renderApp({ flag: "true", body: { "a.save": "Keep" } });
    await flush();

    expect(screen.getByTestId("submit")).toHaveTextContent("Submit");
  });

  it("keeps the shipped wording when the flag is on but nothing is mounted", async () => {
    renderApp({ flag: "true", body: {} });
    await flush();

    expect(screen.getByTestId("save")).toHaveTextContent("Save");
    expect(screen.getByTestId("submit")).toHaveTextContent("Submit");
  });

  it("keeps the children mounted when the override arrives", async () => {
    const mounts = vi.fn();
    const Probe = () => {
      React.useEffect(() => {
        mounts();
      }, []);
      return <FormattedMessage id="a.save" />;
    };
    global.fetch = vi.fn(async () => ({
      ok: true,
      status: 200,
      headers: { get: () => "application/json" },
      json: async () => ({ "a.save": "Keep" }),
    }));
    render(
      <Ctx.Provider
        value={{
          configurationProperties: { OVERRIDE_DEFAULT_TRANSLATION: "true" },
        }}
      >
        <IntlProvider locale="en" defaultLocale="en" messages={BUNDLED}>
          <TranslationOverrideProvider>
            <p data-testid="probe">
              <Probe />
            </p>
          </TranslationOverrideProvider>
        </IntlProvider>
      </Ctx.Provider>,
    );
    // The override lands in a state update outside React's batching; act
    // flushes the passive effects of the render it causes, which is where a
    // remounted child would register its second mount.
    await act(async () => {
      await flush();
      await flush();
    });

    expect(screen.getByTestId("probe")).toHaveTextContent("Keep");
    expect(
      mounts,
      "the page under the provider mounted once",
    ).toHaveBeenCalledTimes(1);
  });
});
