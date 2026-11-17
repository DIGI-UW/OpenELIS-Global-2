import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  exposesFor,
  federationOptionsFor,
  openelisModule,
  outDirFor,
  readManifest,
  remoteNameFor,
} from "../../../packages/module-sdk/build.js";
import {
  coreRemote,
  sharedForModule,
  sharedSingletons,
} from "../../../packages/module-sdk/shared.js";

// A module checkout as the preset sees it: frontend/ is the root, the
// manifest sits in the Maven resources next to it.
const manifest = {
  id: "lab-notes",
  frontend: {
    entry: "remoteEntry.js",
    routes: [
      { path: "/m/lab-notes/notes", expose: "./NotesPage" },
      { path: "/m/lab-notes/notes/:id", expose: "./NoteDetailPage" },
    ],
    slots: [{ slot: "home.dashboard.cards", expose: "./NotesCard" }],
  },
};

let checkout;
let root;

function file(path, content = "export default () => null;") {
  mkdirSync(join(root, path, ".."), { recursive: true });
  writeFileSync(join(root, path), content);
}

beforeEach(() => {
  checkout = mkdtempSync(join(tmpdir(), "openelis-module-"));
  root = join(checkout, "frontend");
  mkdirSync(join(checkout, "src/main/resources/META-INF"), { recursive: true });
  writeFileSync(
    join(checkout, "src/main/resources/META-INF/openelis-module.json"),
    JSON.stringify(manifest),
  );
  mkdirSync(root);
});

afterEach(() => {
  rmSync(checkout, { recursive: true, force: true });
});

describe("the module build preset (OGC-1641)", () => {
  it("exposes each manifest route and slot from the component file of its name", () => {
    file("src/pages/NotesPage.jsx");
    file("src/pages/NoteDetailPage.tsx");
    file("src/slots/NotesCard.tsx");

    expect(exposesFor(manifest, { root })).toEqual({
      "./NotesPage": "./src/pages/NotesPage.jsx",
      "./NoteDetailPage": "./src/pages/NoteDetailPage.tsx",
      "./NotesCard": "./src/slots/NotesCard.tsx",
    });
  });

  it("names every expose without a file and the paths it tried, so a renamed page fails the build", () => {
    file("src/pages/NotesPage.jsx");
    file("src/slots/NotesCard.tsx");

    expect(() => exposesFor(manifest, { root })).toThrow(
      /expose "\.\/NoteDetailPage" \(route \/m\/lab-notes\/notes\/:id\) has no source file; tried \.\/src\/pages\/NoteDetailPage\.jsx, \.\/src\/pages\/NoteDetailPage\.tsx/,
    );
  });

  it("takes a file override for an expose that does not follow the convention", () => {
    file("src/pages/NotesPage.jsx");
    file("src/slots/NotesCard.tsx");
    file("src/detail/index.jsx");

    const exposes = exposesFor(manifest, {
      root,
      overrides: { "./NoteDetailPage": "./src/detail/index.jsx" },
    });
    expect(exposes["./NoteDetailPage"]).toBe("./src/detail/index.jsx");

    expect(() =>
      exposesFor(manifest, {
        root,
        overrides: { "./NoteDetailPage": "./src/detail/missing.jsx" },
      }),
    ).toThrow(
      /expose "\.\/NoteDetailPage" \(route \/m\/lab-notes\/notes\/:id\) is overridden with \.\/src\/detail\/missing\.jsx, which does not exist/,
    );
  });

  it("derives the remote name, the output directory and the shared list from the manifest and the SDK", async () => {
    file("src/pages/NotesPage.jsx");
    file("src/pages/NoteDetailPage.jsx");
    file("src/slots/NotesCard.jsx");

    const remote = federationOptionsFor(manifest, { root });
    expect(remote).toEqual({
      name: "lab_notes",
      filename: "remoteEntry.js",
      exposes: {
        "./NotesPage": "./src/pages/NotesPage.jsx",
        "./NoteDetailPage": "./src/pages/NoteDetailPage.jsx",
        "./NotesCard": "./src/slots/NotesCard.jsx",
      },
      remotes: { openelis_core: coreRemote() },
      shared: sharedForModule(),
      dts: false,
    });
    expect(Object.values(remote.shared).every((s) => s.import === false)).toBe(
      true,
    );

    // The federation plugin itself registers nothing under Vitest, so the
    // options it is given are what the test reads. The plugins are loaded
    // from the module's node_modules, here the core's, so the config function
    // is asynchronous.
    const config = await openelisModule({ root })({ mode: "production" });
    expect(config.plugins.flat(Infinity).length).toBeGreaterThan(0);
    expect(remoteNameFor("lab-notes")).toBe("lab_notes");
    expect(outDirFor("lab-notes")).toBe(
      "../target/classes/META-INF/resources/modules/lab-notes",
    );
    expect(config.build.outDir).toBe(outDirFor("lab-notes"));
    expect(config.build.sourcemap).toBe(false);
    expect(config.server.port).toBe(5174);
    expect(config.resolve.dedupe).toEqual(Object.keys(sharedSingletons));
    expect(config.test.environment).toBe("jsdom");
  });

  it("refuses a manifest that cannot be read", () => {
    expect(() => readManifest(join(checkout, "nowhere.json"))).toThrow(
      /the manifest .*nowhere\.json cannot be read/,
    );
    expect(() => openelisModule({ root, manifest: "../missing.json" })).toThrow(
      /cannot be read/,
    );
  });
});
