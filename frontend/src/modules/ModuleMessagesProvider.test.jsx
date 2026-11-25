import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider, FormattedMessage } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ModuleHostContext } from "./ModuleHostProvider";
import ModuleMessagesProvider from "./ModuleMessagesProvider";

const devBundles = vi.hoisted(() => ({ current: {} }));

vi.mock("./devModuleMessages", () => ({
  useDevModuleMessages: () => devBundles.current,
}));

const host = {
  modules: [
    {
      id: "alpha",
      permissionPrefix: "mod.alpha.",
      messages: {
        en: { "mod.alpha.title": "Lab notes", "mod.alpha.save": "Save" },
        fr: { "mod.alpha.title": "Notes", "mod.alpha.save": "Enregistrer" },
      },
    },
  ],
  skipped: [],
  remotes: [],
  routes: [],
  slots: {},
  status: "ready",
};

const renderIn = (locale) =>
  render(
    <IntlProvider locale={locale} messages={{ "core.key": "Core" }}>
      <ModuleHostContext.Provider value={host}>
        <ModuleMessagesProvider>
          <p data-testid="title">
            <FormattedMessage id="mod.alpha.title" />
          </p>
          <p data-testid="save">
            <FormattedMessage id="mod.alpha.save" />
          </p>
          <p data-testid="core">
            <FormattedMessage id="core.key" />
          </p>
        </ModuleMessagesProvider>
      </ModuleHostContext.Provider>
    </IntlProvider>,
  );

describe("ModuleMessagesProvider", () => {
  beforeEach(() => {
    devBundles.current = {};
  });

  it("renders the registry's bundles when no dev server serves the module", () => {
    renderIn("fr");

    expect(screen.getByTestId("title")).toHaveTextContent("Notes");
    expect(screen.getByTestId("save")).toHaveTextContent("Enregistrer");
    expect(screen.getByTestId("core")).toHaveTextContent("Core");
  });

  it("takes a dev server's bundle for its language over the registry's", () => {
    devBundles.current = {
      alpha: {
        fr: {
          "mod.alpha.title": "Notes de labo",
          "mod.alpha.save": "Enregistrer",
        },
      },
    };
    renderIn("fr");

    expect(screen.getByTestId("title")).toHaveTextContent("Notes de labo");
    expect(screen.getByTestId("save")).toHaveTextContent("Enregistrer");
  });

  it("falls back to English for a key the saved file no longer has, as the rebuilt JAR would", () => {
    devBundles.current = {
      alpha: { fr: { "mod.alpha.title": "Notes de labo" } },
    };
    renderIn("fr");

    expect(screen.getByTestId("title")).toHaveTextContent("Notes de labo");
    expect(screen.getByTestId("save")).toHaveTextContent("Save");
  });

  it("never lets a dev bundle override a core message", () => {
    devBundles.current = {
      alpha: { en: { "core.key": "Hijacked", "mod.alpha.title": "Edited" } },
    };
    renderIn("en");

    expect(screen.getByTestId("core")).toHaveTextContent("Core");
    expect(screen.getByTestId("title")).toHaveTextContent("Edited");
  });
});
