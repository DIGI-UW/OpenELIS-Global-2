import crypto from "node:crypto";
import { execSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { afterEach, describe, expect, test } from "vitest";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

import {
  adfToHtml,
  assetHash,
  buildSlugMap,
  compareVersions,
  injectOnlineLink,
  injectWipNotice,
  isAuthoringNoiseParagraph,
  isSupersededLegacy,
  isUnderLegacyFolder,
  matchesReleaseLabel,
  nodeText,
  resolveWikiUrl,
  rewriteAssetSrc,
  slugify,
  stableStringify,
  stripAuthoringNoise,
} from "./help-refresh.mjs";

describe("version helpers", () => {
  test("compareVersions handles major/minor/patch", () => {
    expect(compareVersions("3.0.0", "3.0.0")).toBe(0);
    expect(compareVersions("3.0.0", "3.4.0")).toBeLessThan(0);
    expect(compareVersions("3.4.1", "3.4.0")).toBeGreaterThan(0);
    expect(compareVersions("3", "2.9.9")).toBeGreaterThan(0);
  });

  test("isSupersededLegacy only drops titles below the release", () => {
    expect(isSupersededLegacy("Entering results (before 3.0.0)", "3.4.0")).toBe(
      true,
    );
    expect(isSupersededLegacy("Entering results (avant 3.0.0)", "3.4.0")).toBe(
      true,
    );
    expect(isSupersededLegacy("New flow (before 3.5.0)", "3.4.0")).toBe(false);
    expect(isSupersededLegacy("Legacy screen view", "3.4.0")).toBe(false);
    expect(isSupersededLegacy("(legacy screen) Order entry", "3.4.0")).toBe(
      false,
    );
  });

  test("isUnderLegacyFolder matches any ancestor title", () => {
    expect(isUnderLegacyFolder(["User Manual", "LEGACY Versions"])).toBe(true);
    expect(isUnderLegacyFolder(["User Manual", "Legacy Version"])).toBe(true);
    expect(isUnderLegacyFolder(["User Manual"])).toBe(false);
    expect(isUnderLegacyFolder([])).toBe(false);
  });

  test("matchesReleaseLabel honours applies-vX-Y", () => {
    expect(matchesReleaseLabel([], "3.4.0")).toBe(true);
    expect(matchesReleaseLabel(["applies-v3-4"], "3.4.0")).toBe(true);
    expect(matchesReleaseLabel(["applies-v3-2"], "3.4.0")).toBe(false);
    expect(matchesReleaseLabel(["applies-v3-2", "applies-v3-4"], "3.4.0")).toBe(
      true,
    );
  });
});

describe("slugify", () => {
  test("strips emoji, lowercases, replaces separators", () => {
    expect(slugify("📖 Lab Context")).toBe("lab-context");
    expect(slugify("Working with Patients")).toBe("working-with-patients");
    expect(slugify("  Trailing & special!! chars  ")).toBe(
      "trailing-special-chars",
    );
  });

  test("empty / nonsense input is handled", () => {
    expect(slugify("")).toBe("");
    expect(slugify(null)).toBe("");
    expect(slugify("💥💥💥")).toBe("");
  });

  test("deterministic for same input", () => {
    expect(slugify("Working with Patients")).toBe(
      slugify("Working with Patients"),
    );
  });
});

describe("authoring noise", () => {
  test("flags Provenance and comment paragraphs", () => {
    expect(
      isAuthoringNoiseParagraph({
        type: "paragraph",
        content: [{ type: "text", text: "Provenance: edited by X" }],
      }),
    ).toBe(true);
    expect(
      isAuthoringNoiseParagraph({
        type: "paragraph",
        content: [{ type: "text", text: "add a comment to this page" }],
      }),
    ).toBe(true);
    expect(
      isAuthoringNoiseParagraph({
        type: "paragraph",
        content: [{ type: "text", text: "normal content" }],
      }),
    ).toBe(false);
  });

  test("stripAuthoringNoise drops only noise paragraphs", () => {
    const adf = {
      type: "doc",
      content: [
        { type: "paragraph", content: [{ type: "text", text: "keep me" }] },
        {
          type: "paragraph",
          content: [{ type: "text", text: "Provenance: X" }],
        },
      ],
    };
    const out = stripAuthoringNoise(adf);
    expect(out.content).toHaveLength(1);
    expect(nodeText(out.content[0])).toBe("keep me");
  });
});

describe("resolveWikiUrl", () => {
  const slugMap = new Map([
    ["101", "working-with-patients"],
    ["200", "contexte-du-laboratoire"],
  ]);

  test("rewrites /pages/<id>/<title> URLs to /help/<slug>", () => {
    expect(
      resolveWikiUrl(
        "https://example.atlassian.net/wiki/spaces/OG/pages/101/Working+with+Patients",
        slugMap,
      ),
    ).toBe("/help/working-with-patients");
  });

  test("rewrites pageId query form", () => {
    expect(
      resolveWikiUrl(
        "https://example.atlassian.net/wiki/display?pageId=200",
        slugMap,
      ),
    ).toBe("/help/contexte-du-laboratoire");
  });

  test("returns null when the target is not in the corpus", () => {
    expect(
      resolveWikiUrl(
        "https://example.atlassian.net/wiki/spaces/OG/pages/999/External",
        slugMap,
      ),
    ).toBeNull();
  });

  test("returns null for non-wiki URLs", () => {
    expect(resolveWikiUrl("https://example.com", slugMap)).toBeNull();
    expect(resolveWikiUrl("", slugMap)).toBeNull();
  });
});

describe("adfToHtml", () => {
  const makeCtx = () => ({
    pageIdToSlug: new Map([["101", "working-with-patients"]]),
    assetRefs: [],
    unresolvedWikiLinks: [],
    unhandled: new Set(),
    allowWikiUrls: false,
  });

  test("renders a heading with a slug id", () => {
    const html = adfToHtml(
      {
        type: "doc",
        content: [
          {
            type: "heading",
            attrs: { level: 2 },
            content: [{ type: "text", text: "Lab Context" }],
          },
        ],
      },
      makeCtx(),
    );
    expect(html).toContain('<h2 id="lab-context">');
    expect(html).toContain("Lab Context");
  });

  test("renders inlineCard pointing at a corpus page as /help/<slug>", () => {
    const ctx = makeCtx();
    const html = adfToHtml(
      {
        type: "doc",
        content: [
          {
            type: "paragraph",
            content: [
              {
                type: "inlineCard",
                attrs: {
                  url: "https://example.atlassian.net/wiki/spaces/OG/pages/101/Working+with+Patients",
                },
              },
            ],
          },
        ],
      },
      ctx,
    );
    expect(html).toContain('href="/help/working-with-patients"');
    expect(html).not.toContain("/wiki/");
    expect(ctx.unresolvedWikiLinks).toHaveLength(0);
  });

  test("records unresolved /wiki/ links", () => {
    const ctx = makeCtx();
    adfToHtml(
      {
        type: "doc",
        content: [
          {
            type: "paragraph",
            content: [
              {
                type: "inlineCard",
                attrs: {
                  url: "https://example.atlassian.net/wiki/spaces/OG/pages/999/Unknown",
                },
              },
            ],
          },
        ],
      },
      ctx,
    );
    expect(ctx.unresolvedWikiLinks).toHaveLength(1);
  });

  test("renders panel, codeBlock and media with the expected classes", () => {
    const ctx = makeCtx();
    const html = adfToHtml(
      {
        type: "doc",
        content: [
          {
            type: "panel",
            attrs: { panelType: "info" },
            content: [
              {
                type: "paragraph",
                content: [{ type: "text", text: "Heads up" }],
              },
            ],
          },
          {
            type: "codeBlock",
            attrs: { language: "json" },
            content: [{ type: "text", text: "{}" }],
          },
          {
            type: "mediaSingle",
            content: [
              {
                type: "media",
                attrs: { id: "att-1", type: "file", alt: "diagram" },
              },
            ],
          },
        ],
      },
      ctx,
    );
    expect(html).toContain('class="help-panel help-panel--info"');
    expect(html).toContain('data-language="json"');
    expect(html).toContain('src="pending:att-1"');
    expect(ctx.assetRefs).toEqual(["att-1"]);
  });

  test("drops Provenance paragraphs before rendering", () => {
    const html = adfToHtml(
      {
        type: "doc",
        content: [
          {
            type: "paragraph",
            content: [{ type: "text", text: "Keep" }],
          },
          {
            type: "paragraph",
            content: [{ type: "text", text: "Provenance: edited" }],
          },
        ],
      },
      makeCtx(),
    );
    expect(html).toContain("Keep");
    expect(html).not.toContain("Provenance");
  });

  test("sanitize-html strips script tags from text content", () => {
    const html = adfToHtml(
      {
        type: "doc",
        content: [
          {
            type: "paragraph",
            content: [{ type: "text", text: "<script>alert(1)</script> hi" }],
          },
        ],
      },
      makeCtx(),
    );
    expect(html).not.toContain("<script>");
    expect(html).toContain("hi");
  });
});

describe("buildSlugMap", () => {
  test("maps page ids to slugs and tags pages with .slug", () => {
    const pages = {
      "user/en": [
        { id: "100", title: "Lab Context" },
        { id: "101", title: "Working with Patients" },
      ],
    };
    const map = buildSlugMap(pages);
    expect(map.get("100")).toBe("lab-context");
    expect(map.get("101")).toBe("working-with-patients");
    expect(pages["user/en"][0].slug).toBe("lab-context");
  });

  test("deduplicates colliding slugs across corpora", () => {
    const pages = {
      "user/en": [{ id: "100", title: "Overview" }],
      "admin/en": [{ id: "200", title: "Overview" }],
    };
    buildSlugMap(pages);
    expect(pages["user/en"][0].slug).toBe("overview");
    expect(pages["admin/en"][0].slug).toBe("overview-2");
  });
});

describe("assetHash and rewriteAssetSrc", () => {
  test("assetHash is deterministic", () => {
    const buf = Buffer.from("hello");
    expect(assetHash(buf)).toBe(assetHash(buf));
    expect(assetHash(buf)).toHaveLength(16);
  });

  test("rewriteAssetSrc replaces pending:<id> with the web path", () => {
    const html = '<img src="pending:att-1" /><img src="pending:att-2" />';
    const rewritten = rewriteAssetSrc(html, {
      "att-1": "/help-content/assets/a.webp",
      "att-2": "/help-content/assets/b.webp",
    });
    expect(rewritten).toContain('src="/help-content/assets/a.webp"');
    expect(rewritten).toContain('src="/help-content/assets/b.webp"');
    expect(rewritten).not.toContain("pending:");
  });
});

describe("stableStringify", () => {
  test("sorts object keys recursively", () => {
    expect(stableStringify({ b: 1, a: { d: 2, c: 3 } })).toBe(
      '{\n  "a": {\n    "c": 3,\n    "d": 2\n  },\n  "b": 1\n}',
    );
  });

  test("produces identical output for logically equal inputs", () => {
    const a = { x: 1, y: [1, 2, 3], z: { b: 2, a: 1 } };
    const b = { z: { a: 1, b: 2 }, y: [1, 2, 3], x: 1 };
    expect(stableStringify(a)).toBe(stableStringify(b));
  });
});

describe("notices and online link", () => {
  test("injectWipNotice only prepends when wip label is present", () => {
    expect(injectWipNotice("<p>x</p>", [], "en")).toBe("<p>x</p>");
    const out = injectWipNotice("<p>x</p>", ["wip"], "en");
    expect(out).toMatch(/help-notice--wip/);
    expect(out).toMatch(/work-in-progress/i);
  });

  test("injectWipNotice uses French copy for fr", () => {
    const out = injectWipNotice("<p>x</p>", ["wip"], "fr");
    expect(out).toMatch(/en cours de développement/);
  });

  test("injectOnlineLink appends an absolute Confluence URL", () => {
    const out = injectOnlineLink(
      "<p>x</p>",
      "/wiki/spaces/OG/pages/100/Lab",
      "example.atlassian.net",
      "en",
    );
    expect(out).toMatch(/View the latest version online/);
    expect(out).toContain(
      "https://example.atlassian.net/wiki/spaces/OG/pages/100/Lab",
    );
  });

  test("injectOnlineLink is a no-op without webuiPath", () => {
    expect(injectOnlineLink("<p>x</p>", null, "h", "en")).toBe("<p>x</p>");
  });
});

describe("end-to-end fixtures run", () => {
  const projectRoot = path.resolve(__dirname, "..", "..");
  const outDir = path.join(projectRoot, "frontend", "public", "help-content");

  afterEach(() => {
    // keep the output around so you can inspect it; only tests clean up on fail
  });

  test("produces deterministic, byte-identical output across repeat runs", () => {
    const scriptPath = path.join(__dirname, "help-refresh.mjs");
    const run = () =>
      execSync(`node ${scriptPath} --fixtures`, {
        cwd: path.resolve(__dirname, ".."),
        stdio: "pipe",
      });

    run();
    const snapshot = snapshotDir(outDir);
    run();
    const second = snapshotDir(outDir);
    expect(second).toEqual(snapshot);

    expect(snapshot.has("en/manifest.json")).toBe(true);
    expect(snapshot.has("fr/manifest.json")).toBe(true);
    expect(snapshot.has("en/user/lab-context.html")).toBe(true);
    expect(snapshot.has("en/user/working-with-patients.html")).toBe(true);
    expect(snapshot.has("fr/user/contexte-du-laboratoire.html")).toBe(true);

    const labContext = fs.readFileSync(
      path.join(outDir, "en/user/lab-context.html"),
      "utf8",
    );
    expect(labContext).toContain("/help/working-with-patients");
    expect(labContext).not.toContain("Provenance");
    expect(labContext).not.toContain("add a comment to this page");
    expect(labContext).toMatch(/View the latest version online/);

    const workingWith = fs.readFileSync(
      path.join(outDir, "en/user/working-with-patients.html"),
      "utf8",
    );
    expect(workingWith).toMatch(
      /src="\/help-content\/assets\/[a-f0-9]{16}\.webp"/,
    );

    // legacy page dropped
    expect(snapshot.has("en/user/entering-results-before-3-0-0.html")).toBe(
      false,
    );

    // WIP notice present
    const wip = fs.readFileSync(
      path.join(outDir, "en/user/new-feature-preview.html"),
      "utf8",
    );
    expect(wip).toMatch(/help-notice--wip/);
  }, 60_000);
});

function snapshotDir(root) {
  const out = new Map();
  if (!fs.existsSync(root)) return out;
  const walk = (dir, prefix) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const sub = path.join(dir, entry.name);
      const rel = prefix ? `${prefix}/${entry.name}` : entry.name;
      if (entry.isDirectory()) {
        walk(sub, rel);
      } else {
        const buf = fs.readFileSync(sub);
        out.set(rel, assetDigest(buf));
      }
    }
  };
  walk(root, "");
  return out;
}

function assetDigest(buf) {
  return crypto.createHash("sha256").update(buf).digest("hex");
}
