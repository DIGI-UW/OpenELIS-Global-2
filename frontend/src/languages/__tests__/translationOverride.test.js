import {
  NO_OVERRIDE,
  applyOverride,
  loadTranslationOverride,
  overrideFilesFor,
} from "../translationOverride";

/**
 * Translations a deployment supplies for itself.
 *
 * The bundled bundles are compiled into the JS chunks, so a deployment cannot
 * change a string by replacing a file in a built image — the overrides are
 * fetched at runtime instead. Every failure has to resolve to "no override":
 * a deployment that mounts nothing, or mounts something broken, must see the
 * shipped UI rather than a blank screen or raw message ids.
 */

const BUNDLED = {
  "a.greeting": "Hello",
  "b.patient": "Patient",
  "c.save": "Save",
};

/** A fetch that answers from a map of url to body, 404ing anything else. */
const headers = (contentType) => ({ get: () => contentType });

const fetchFrom = (bodies) =>
  vi.fn(async (url) => {
    if (!(url in bodies)) {
      return {
        ok: false,
        status: 404,
        headers: headers("application/json"),
        json: async () => ({}),
      };
    }
    const body = bodies[url];
    return {
      ok: true,
      status: 200,
      headers: headers("application/json"),
      json: async () => {
        if (typeof body === "string") {
          throw new SyntaxError("Unexpected token");
        }
        return body;
      },
    };
  });

/** What both servers actually do for a path they have no file for. */
const fetchSpaShell = () =>
  vi.fn(async () => ({
    ok: true,
    status: 200,
    headers: headers("text/html; charset=utf-8"),
    json: async () => {
      throw new SyntaxError("Unexpected token '<'");
    },
  }));

describe("which files apply to a locale", () => {
  it("reads the underscored file name, not the BCP-47 tag", () => {
    // Transifex writes en_GB.json; react-intl talks in en-GB.
    expect(overrideFilesFor("en-GB")).toContain("/translation/en_GB.json");
  });

  it("layers a base language under its regional variant", () => {
    expect(overrideFilesFor("fr-MG")).toEqual([
      "/translation/fr.json",
      "/translation/fr_MG.json",
    ]);
  });

  it("asks for one file when the locale names no region", () => {
    expect(overrideFilesFor("fr")).toEqual(["/translation/fr.json"]);
  });

  it("asks for nothing when there is no locale", () => {
    expect(overrideFilesFor("")).toEqual([]);
    expect(overrideFilesFor(undefined)).toEqual([]);
  });
});

describe("loading a deployment's overrides", () => {
  it("returns the messages a locale's file supplies", async () => {
    const override = await loadTranslationOverride(
      "fr",
      fetchFrom({ "/translation/fr.json": { "b.patient": "Usager" } }),
    );

    expect(override).toEqual({ "b.patient": "Usager" });
  });

  it("lets the regional file win over its base language", async () => {
    const override = await loadTranslationOverride(
      "fr-MG",
      fetchFrom({
        "/translation/fr.json": { "b.patient": "Usager", "c.save": "Garder" },
        "/translation/fr_MG.json": { "b.patient": "Marary" },
      }),
    );

    expect(override).toEqual({ "b.patient": "Marary", "c.save": "Garder" });
  });

  it("still reaches a regional user from the base language file alone", async () => {
    // The case a deployment hits when it rewords fr.json and its users run fr-MG.
    const override = await loadTranslationOverride(
      "fr-MG",
      fetchFrom({ "/translation/fr.json": { "b.patient": "Usager" } }),
    );

    expect(override).toEqual({ "b.patient": "Usager" });
  });

  it("treats a mounted directory with no file for this locale as no override", async () => {
    expect(await loadTranslationOverride("fr", fetchFrom({}))).toBe(
      NO_OVERRIDE,
    );
  });

  it("treats an empty file as no override", async () => {
    expect(
      await loadTranslationOverride(
        "fr",
        fetchFrom({ "/translation/fr.json": {} }),
      ),
    ).toBe(NO_OVERRIDE);
  });

  it("ignores a file that is not valid JSON", async () => {
    expect(
      await loadTranslationOverride(
        "fr",
        fetchFrom({ "/translation/fr.json": "{ not json" }),
      ),
    ).toBe(NO_OVERRIDE);
  });

  it("ignores a file that is not an object of message ids", async () => {
    expect(
      await loadTranslationOverride(
        "fr",
        fetchFrom({ "/translation/fr.json": ["a", "b"] }),
      ),
    ).toBe(NO_OVERRIDE);
  });

  it("drops a single non-string value rather than the whole file", async () => {
    // react-intl formats a message as a string, so a stray nested object would
    // throw where it is rendered — costing the screen, not just the key.
    const override = await loadTranslationOverride(
      "fr",
      fetchFrom({
        "/translation/fr.json": {
          "b.patient": "Usager",
          "c.save": { nested: "oops" },
        },
      }),
    );

    expect(override).toEqual({ "b.patient": "Usager" });
  });

  it("does not mistake the SPA shell for an override", async () => {
    // nginx try_files and the Vite dev server both answer an unknown path with
    // index.html and a 200, so "not found" has to be read from the content type
    // rather than from the status or a JSON parse error.
    expect(await loadTranslationOverride("fr", fetchSpaShell())).toBe(
      NO_OVERRIDE,
    );
  });

  it("survives a fetch that rejects outright", async () => {
    const failing = vi.fn(async () => {
      throw new TypeError("Failed to fetch");
    });

    expect(await loadTranslationOverride("fr", failing)).toBe(NO_OVERRIDE);
  });

  it("asks the server not to answer from cache", async () => {
    const fetchImpl = fetchFrom({ "/translation/fr.json": { a: "b" } });

    await loadTranslationOverride("fr", fetchImpl);

    // An edited file must not be shadowed by a cached 200.
    expect(fetchImpl).toHaveBeenCalledWith("/translation/fr.json", {
      cache: "no-cache",
    });
  });
});

describe("applying overrides to the bundled messages", () => {
  it("replaces only the keys the override names", () => {
    const merged = applyOverride(BUNDLED, { "b.patient": "Client" });

    expect(merged["b.patient"]).toBe("Client");
    expect(merged["a.greeting"]).toBe("Hello");
    expect(merged["c.save"]).toBe("Save");
  });

  it("keeps every bundled key a three-key override does not mention", () => {
    expect(
      Object.keys(applyOverride(BUNDLED, { "b.patient": "Client" })),
    ).toHaveLength(Object.keys(BUNDLED).length);
  });

  it("hands back the bundled messages untouched when there is no override", () => {
    // Same object, so React skips the re-render.
    expect(applyOverride(BUNDLED, NO_OVERRIDE)).toBe(BUNDLED);
    expect(applyOverride(BUNDLED, {})).toBe(BUNDLED);
    expect(applyOverride(BUNDLED, null)).toBe(BUNDLED);
  });

  it("does not mutate the bundled messages", () => {
    applyOverride(BUNDLED, { "b.patient": "Client" });

    expect(BUNDLED["b.patient"]).toBe("Patient");
  });
});
