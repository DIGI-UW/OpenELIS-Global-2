import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  bundlePath,
  devServerBase,
  messageCodes,
  useDevServerMessages,
} from "./devModuleMessages";

const ASSET_BASE = "/api/OpenELIS-Global/modules/alpha/0123456789abcdef/";

const alpha = {
  id: "alpha",
  remoteName: "alpha",
  assetBase: ASSET_BASE,
  frontend: { entry: "remoteEntry.js", messages: "messages/{locale}.json" },
};

/**
 * A module dev server behind the dev stack's redirect: the shell asks the
 * asset base and lands on http://localhost:5174, which serves the files in
 * `files` as they are at the time of the request, and, as Vite does, its
 * index page for any other path.
 */
function devServer(files) {
  const requests = [];
  const fetchBundle = vi.fn(async (url) => {
    requests.push(url);
    const devUrl = url.startsWith(ASSET_BASE)
      ? `http://localhost:5174/${url.substring(ASSET_BASE.length)}`
      : url;
    const path = new URL(devUrl).pathname.substring(1);
    const body = files[path];
    return {
      ok: true,
      status: 200,
      redirected: url.startsWith(ASSET_BASE),
      url: devUrl,
      headers: {
        get: (name) =>
          name.toLowerCase() === "content-type"
            ? body === undefined
              ? "text/html"
              : "application/json"
            : null,
      },
      text: async () => (body === undefined ? "<!doctype html>" : body),
    };
  });
  return { fetchBundle, requests };
}

function Probe({ modules, locale, options }) {
  const bundles = useDevServerMessages(modules, locale, options);
  return <pre data-testid="bundles">{JSON.stringify(bundles)}</pre>;
}

const shown = () => JSON.parse(screen.getByTestId("bundles").textContent);

describe("dev module messages", () => {
  beforeEach(() => window.localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it("reads the codes the shell layers for a locale, English first", () => {
    expect(messageCodes("fr-MG")).toEqual(["en", "fr", "fr_MG", "fr-MG"]);
    expect(messageCodes("fr")).toEqual(["en", "fr"]);
    expect(messageCodes(null)).toEqual(["en"]);
  });

  it("finds a bundle where the manifest says the module ships it", () => {
    expect(bundlePath(alpha, "fr")).toBe("messages/fr.json");
    expect(
      bundlePath({ frontend: { messages: "i18n/{locale}.json" } }, "fr"),
    ).toBe("i18n/fr.json");
    expect(bundlePath({ frontend: {} }, "fr")).toBe("messages/fr.json");
  });

  it("knows a module's dev server from the redirect of its English bundle", async () => {
    const { fetchBundle } = devServer({ "messages/en.json": "{}" });

    expect(await devServerBase(alpha, fetchBundle)).toBe(
      "http://localhost:5174/",
    );
    expect(fetchBundle).toHaveBeenCalledWith(`${ASSET_BASE}messages/en.json`);
  });

  it("takes a module served from its JAR as no dev server", async () => {
    const fetchBundle = vi.fn(async (url) => ({
      ok: true,
      redirected: false,
      url,
      text: async () => "{}",
    }));

    expect(await devServerBase(alpha, fetchBundle)).toBeNull();
    expect(
      await devServerBase(alpha, vi.fn().mockRejectedValue(new Error("down"))),
    ).toBeNull();
  });

  it("takes the dev override's directory without asking the asset base", async () => {
    window.localStorage.setItem(
      "oe.module-override.alpha",
      "http://localhost:5180/remoteEntry.js",
    );
    const fetchBundle = vi.fn();

    expect(await devServerBase(alpha, fetchBundle)).toBe(
      "http://localhost:5180/",
    );
    expect(fetchBundle).not.toHaveBeenCalled();
  });

  it("shows a saved bundle on the next read and keeps the last good copy of a broken one", async () => {
    const files = {
      "messages/en.json": '{"mod.alpha.title":"Lab notes"}',
      "messages/fr.json": '{"mod.alpha.title":"Notes"}',
    };
    const { fetchBundle } = devServer(files);
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    render(
      <Probe
        modules={[alpha]}
        locale="fr"
        options={{ enabled: true, intervalMs: 20, fetchBundle }}
      />,
    );

    await waitFor(() =>
      expect(shown()).toEqual({
        alpha: {
          en: { "mod.alpha.title": "Lab notes" },
          fr: { "mod.alpha.title": "Notes" },
        },
      }),
    );

    files["messages/fr.json"] = '{"mod.alpha.title":"Notes de labo"}';
    await waitFor(() =>
      expect(shown().alpha.fr).toEqual({ "mod.alpha.title": "Notes de labo" }),
    );

    files["messages/fr.json"] = '{"mod.alpha.title": ';
    await waitFor(() => expect(warn).toHaveBeenCalled());
    expect(warn.mock.calls[0][0]).toContain(
      "messages/fr.json is not valid JSON",
    );
    expect(shown().alpha.fr).toEqual({ "mod.alpha.title": "Notes de labo" });
  });

  it("asks once, quietly, for a code the module does not ship", async () => {
    const files = { "messages/en.json": '{"mod.alpha.title":"Lab notes"}' };
    const { fetchBundle, requests } = devServer(files);
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    render(
      <Probe
        modules={[alpha]}
        locale="en-US"
        options={{ enabled: true, intervalMs: 10, fetchBundle }}
      />,
    );

    await waitFor(() =>
      expect(shown()).toEqual({
        alpha: { en: { "mod.alpha.title": "Lab notes" } },
      }),
    );
    await new Promise((resolve) => setTimeout(resolve, 80));
    const missing = (code) =>
      requests.filter(
        (url) => url === `http://localhost:5174/messages/${code}.json`,
      );
    expect(missing("en_US")).toHaveLength(1);
    expect(missing("en-US")).toHaveLength(1);
    expect(missing("en").length).toBeGreaterThan(2);
    expect(warn).not.toHaveBeenCalled();
  });

  it("keeps reading a file that disappears for a while and shows it when it is back", async () => {
    const files = {
      "messages/en.json": '{"mod.alpha.title":"Lab notes"}',
      "messages/fr.json": '{"mod.alpha.title":"Notes"}',
    };
    const { fetchBundle, requests } = devServer(files);
    render(
      <Probe
        modules={[alpha]}
        locale="fr"
        options={{ enabled: true, intervalMs: 20, fetchBundle }}
      />,
    );
    await waitFor(() =>
      expect(shown().alpha.fr).toEqual({ "mod.alpha.title": "Notes" }),
    );

    delete files["messages/fr.json"];
    const asked = requests.length;
    await waitFor(() => expect(requests.length).toBeGreaterThan(asked + 4));
    files["messages/fr.json"] = '{"mod.alpha.title":"Notes de labo"}';

    await waitFor(() =>
      expect(shown().alpha.fr).toEqual({ "mod.alpha.title": "Notes de labo" }),
    );
  });

  it("asks a JAR-served module once and then leaves it alone", async () => {
    const fetchBundle = vi.fn(async (url) => ({
      ok: true,
      redirected: false,
      url,
      text: async () => "{}",
    }));
    render(
      <Probe
        modules={[alpha]}
        locale="fr"
        options={{ enabled: true, intervalMs: 10, fetchBundle }}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 80));
    expect(fetchBundle).toHaveBeenCalledTimes(1);
    expect(shown()).toEqual({});
  });

  it("does nothing when switched off, as in a production build", async () => {
    const { fetchBundle } = devServer({ "messages/en.json": "{}" });
    render(
      <Probe
        modules={[alpha]}
        locale="fr"
        options={{ enabled: false, intervalMs: 10, fetchBundle }}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(fetchBundle).not.toHaveBeenCalled();
    expect(shown()).toEqual({});
  });
});
