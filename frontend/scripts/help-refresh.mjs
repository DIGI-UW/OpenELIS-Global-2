#!/usr/bin/env node
// Help Refresh Pipeline (OGC help ticket, FRS §13).
//
// Walks the Confluence folders named in help-refresh.config.json, converts
// each page's ADF to sanitized HTML, downloads attachments, resolves cross-page
// links, applies version/legacy filters, and emits a per-locale manifest and
// FlexSearch index into the configured outputDir. Run by the release manager
// at release cut; the output is reviewed as a normal PR.
//
// Usage:
//   set -a; source .env; set +a
//   npm run help:refresh                 # live run against Confluence
//   npm run help:refresh -- --fixtures   # deterministic run against the
//                                        # synthetic fixtures bundled in
//                                        # help-refresh-fixtures/ (FRS AC 1)

import crypto from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

import FlexSearchDefault from "flexsearch";
import sanitizeHtml from "sanitize-html";
import sharp from "sharp";

const FlexSearch = FlexSearchDefault.default ?? FlexSearchDefault;

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const configPath = path.join(__dirname, "help-refresh.config.json");
const fixturesPath = path.join(
  __dirname,
  "help-refresh-fixtures",
  "fixtures.json",
);

const args = new Set(process.argv.slice(2));
const useFixtures = args.has("--fixtures");

const loadConfig = async () => {
  const raw = await fs.readFile(configPath, "utf8");
  const config = JSON.parse(raw);
  if (!config.corpora?.length) {
    throw new Error("config.corpora is empty — nothing to walk");
  }
  return config;
};

const requireEnv = (name) => {
  const v = process.env[name];
  if (!v) throw new Error(`missing env var ${name}`);
  return v;
};

const confluenceAuth = () => {
  const email = requireEnv("CONFLUENCE_EMAIL");
  const token = requireEnv("CONFLUENCE_TOKEN");
  const host = requireEnv("CONFLUENCE_HOST");
  const auth = "Basic " + Buffer.from(`${email}:${token}`).toString("base64");
  return { host, headers: { Authorization: auth, Accept: "application/json" } };
};

// Confluence v2 GET helper. Serial by design — a release-time pipeline running
// against ~100 pages does not need parallelism, and serial keeps us well under
// Cloud's per-user rate limit.
const confluenceGet = async (host, headers, pathOrUrl) => {
  const url = pathOrUrl.startsWith("http")
    ? pathOrUrl
    : `https://${host}${pathOrUrl}`;
  const res = await fetch(url, { headers });
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText} ${pathOrUrl}`);
  }
  return res.json();
};

// Every page descended from a root, via CQL. Returns id, title, labels, and
// ancestor titles in one call per batch — no N+1 label lookups, and the
// ancestor chain carries LEGACY Versions folders transparently so folder vs
// page content type does not need special-casing.
const listCorpusPages = async (host, headers, spaceKey, rootPageId) => {
  const cql = encodeURIComponent(
    `space=${spaceKey} AND type=page AND ancestor=${rootPageId}`,
  );
  const expand = encodeURIComponent("metadata.labels,ancestors");
  const limit = 50;
  const results = [];
  let start = 0;
  while (true) {
    const page = await confluenceGet(
      host,
      headers,
      `/wiki/rest/api/content/search?cql=${cql}&expand=${expand}&limit=${limit}&start=${start}`,
    );
    for (const r of page.results || []) {
      results.push({
        id: r.id,
        title: r.title,
        labels: (r.metadata?.labels?.results || []).map((l) => l.name),
        ancestorTitles: (r.ancestors || []).map((a) => a.title),
      });
    }
    if ((page.results?.length ?? 0) < limit) break;
    start += limit;
  }
  return results;
};

export const parseVersion = (s) => s.split(".").map(Number);
export const compareVersions = (a, b) => {
  const av = parseVersion(a);
  const bv = parseVersion(b);
  for (let i = 0; i < 3; i += 1) {
    if ((av[i] || 0) !== (bv[i] || 0)) return (av[i] || 0) - (bv[i] || 0);
  }
  return 0;
};

// "(before X.Y.Z)" and "(avant X.Y.Z)" marker pages document pre-X.Y.Z
// behavior; they are kept only while the build still predates X.Y.Z (FRS AC 3).
// "(legacy screen)" pages document unchanged legacy UI and stay in every build.
const legacyBeforeMarker = /\((?:before|avant) (\d+\.\d+\.\d+)\)/i;

export const isSupersededLegacy = (title, releaseVersion) => {
  const m = title.match(legacyBeforeMarker);
  if (!m) return false;
  return compareVersions(m[1], releaseVersion) <= 0;
};

// Pages tagged applies-vX-Y are version-scoped; a page tagged only
// applies-v3-2 is excluded from a 3.3 build (FRS AC 14). A page with no
// applies-* labels is always included.
const releaseAppliesLabel = (releaseVersion) => {
  const [maj, min] = parseVersion(releaseVersion);
  return `applies-v${maj}-${min}`;
};

export const matchesReleaseLabel = (labels, releaseVersion) => {
  const applies = labels.filter((l) => l.startsWith("applies-v"));
  if (applies.length === 0) return true;
  return applies.includes(releaseAppliesLabel(releaseVersion));
};

// A page is "under a LEGACY Versions folder" when any ancestor's title matches,
// regardless of folder vs page content type (FRS AC 3).
export const isUnderLegacyFolder = (ancestorTitles) =>
  ancestorTitles.some((t) => /^legacy versions?$/i.test(t));

export const filterCorpus = (pages, releaseVersion) => {
  const kept = [];
  let droppedLegacyFolder = 0;
  let droppedLegacyTitle = 0;
  let droppedByLabel = 0;
  for (const p of pages) {
    if (isUnderLegacyFolder(p.ancestorTitles || [])) {
      droppedLegacyFolder += 1;
      continue;
    }
    if (isSupersededLegacy(p.title, releaseVersion)) {
      droppedLegacyTitle += 1;
      continue;
    }
    if (!matchesReleaseLabel(p.labels || [], releaseVersion)) {
      droppedByLabel += 1;
      continue;
    }
    kept.push({ id: p.id, title: p.title, labels: p.labels || [] });
  }
  return {
    pages: kept,
    counts: {
      total: pages.length,
      kept: kept.length,
      droppedLegacyFolder,
      droppedLegacyTitle,
      droppedByLabel,
    },
  };
};

const walkCorpusLive = async (corpus, source, releaseVersion, spaceKey) => {
  const { host, headers } = source;
  const all = await listCorpusPages(host, headers, spaceKey, corpus.rootPageId);
  return filterCorpus(all, releaseVersion);
};

// ---- Fixtures mode ----

let fixturesCache = null;
const loadFixtures = async () => {
  if (!fixturesCache) {
    const raw = await fs.readFile(fixturesPath, "utf8");
    fixturesCache = JSON.parse(raw);
  }
  return fixturesCache;
};

const walkCorpusFixtures = async (corpus, releaseVersion) => {
  const fixtures = await loadFixtures();
  const key = `${corpus.guide}/${corpus.locale}`;
  const all = Object.entries(fixtures.pages)
    .filter(([, p]) => p.corpusKey === key)
    .map(([id, p]) => ({
      id,
      title: p.title,
      labels: p.labels || [],
      ancestorTitles: p.ancestorTitles || [],
    }));
  return filterCorpus(all, releaseVersion);
};

// Phase 1: walk — list every page under each corpus root, dropping legacy
// pages (title contains "(before X.Y.Z)" below the release version) and
// filtering by applies-vX-Y labels (FRS AC 3, AC 14).
const walk = async (config, source) => {
  const result = {};
  for (const corpus of config.corpora) {
    const key = `${corpus.guide}/${corpus.locale}`;
    console.log(`help-refresh: walking ${key} root=${corpus.rootPageId}`);
    const { pages, counts } =
      source.kind === "fixtures"
        ? await walkCorpusFixtures(corpus, config.releaseVersion)
        : await walkCorpusLive(
            corpus,
            source,
            config.releaseVersion,
            config.spaceKey,
          );
    console.log(
      `help-refresh:   ${counts.kept}/${counts.total} kept ` +
        `(legacy folder:${counts.droppedLegacyFolder}, ` +
        `legacy title:${counts.droppedLegacyTitle}, ` +
        `label:${counts.droppedByLabel})`,
    );
    result[key] = pages;
  }
  return result;
};

// Phase 2: fetch — download each page's ADF body, current version, and
// web-UI path. The web-UI path becomes the "View the latest version online"
// link (FRS AC 17); the version supports the deferred stale-translation Tag.
// Mutates each page in place to keep the pipeline a flat sequence.
const fetchPageBody = async (host, headers, pageId) => {
  const data = await confluenceGet(
    host,
    headers,
    `/wiki/api/v2/pages/${pageId}?body-format=atlas_doc_format`,
  );
  const adfJson = data.body?.atlas_doc_format?.value || "{}";
  return {
    adf: JSON.parse(adfJson),
    version: data.version?.number ?? null,
    webuiPath: data._links?.webui || null,
  };
};

const fetchPagesLive = async (pagesByCorpus, source) => {
  const { host, headers } = source;
  for (const [key, pages] of Object.entries(pagesByCorpus)) {
    console.log(
      `help-refresh: fetching ADF for ${key} (${pages.length} pages)`,
    );
    let i = 0;
    for (const p of pages) {
      i += 1;
      const body = await fetchPageBody(host, headers, p.id);
      Object.assign(p, body);
      if (i % 10 === 0 || i === pages.length) {
        console.log(`help-refresh:   ${i}/${pages.length}`);
      }
    }
  }
};

const fetchPagesFixtures = async (pagesByCorpus) => {
  const fixtures = await loadFixtures();
  for (const pages of Object.values(pagesByCorpus)) {
    for (const p of pages) {
      const src = fixtures.pages[p.id];
      if (!src) throw new Error(`fixture page ${p.id} not found`);
      p.adf = src.adf;
      p.version = src.version ?? null;
      p.webuiPath = src.webuiPath ?? null;
      p.attachments = src.attachments || [];
    }
  }
};

const fetchPages = async (pagesByCorpus, source) => {
  if (source.kind === "fixtures") {
    await fetchPagesFixtures(pagesByCorpus);
  } else {
    await fetchPagesLive(pagesByCorpus, source);
  }
  return pagesByCorpus;
};

// Phase 3: locale match — for each non-English page, read the configured
// content property (openelis-translation.sourceId per the Lango spike) and
// record its English source page id. sourceVersion is kept so a future stale-
// translation Tag can compare it against the English page's current version.
const fetchContentProperty = async (host, headers, pageId, key) => {
  const data = await confluenceGet(
    host,
    headers,
    `/wiki/api/v2/pages/${pageId}/properties`,
  );
  const prop = (data.results || []).find((r) => r.key === key);
  return prop?.value ?? null;
};

const matchLocalesLive = async (pagesByCorpus, source, config) => {
  const { contentPropertyKey, sourceIdField, sourceVersionField } =
    config.localeMatcher || {};
  if (!contentPropertyKey) {
    console.log(
      "help-refresh: no localeMatcher.contentPropertyKey; skipping locale join",
    );
    return;
  }
  const { host, headers } = source;
  for (const [key, pages] of Object.entries(pagesByCorpus)) {
    const [, locale] = key.split("/");
    if (locale === "en") continue;
    let joined = 0;
    for (const p of pages) {
      const value = await fetchContentProperty(
        host,
        headers,
        p.id,
        contentPropertyKey,
      );
      if (value && typeof value === "object") {
        p.sourceId = value[sourceIdField] ?? null;
        p.sourceVersion = value[sourceVersionField] ?? null;
        if (p.sourceId) joined += 1;
      }
    }
    console.log(
      `help-refresh: locale join ${key}: ${joined}/${pages.length} matched to en`,
    );
  }
};

const matchLocalesFixtures = async (pagesByCorpus) => {
  const fixtures = await loadFixtures();
  for (const [key, pages] of Object.entries(pagesByCorpus)) {
    const [, locale] = key.split("/");
    if (locale === "en") continue;
    let joined = 0;
    for (const p of pages) {
      const src = fixtures.pages[p.id];
      const prop = src?.translationProperty;
      if (prop) {
        p.sourceId = prop.sourceId ?? null;
        p.sourceVersion = prop.sourceVersion ?? null;
        if (p.sourceId) joined += 1;
      }
    }
    console.log(
      `help-refresh: locale join ${key}: ${joined}/${pages.length} matched to en`,
    );
  }
};

const matchLocales = async (pagesByCorpus, source, config) => {
  if (source.kind === "fixtures") {
    await matchLocalesFixtures(pagesByCorpus);
  } else {
    await matchLocalesLive(pagesByCorpus, source, config);
  }
};

// ---- Phase 4: convert ----

// Slug generation. Strips emoji, lowercases, replaces non-alphanumeric runs
// with a single hyphen, trims leading/trailing hyphens. Deterministic input
// → deterministic output, which keeps /help/<slug> URLs stable across runs
// (FRS AC 1).
export const slugify = (s) =>
  (s || "")
    .normalize("NFKD")
    .replace(/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/gu, "")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

// Flat text content of an ADF subtree. Used for heading ids and search text.
export const nodeText = (node) => {
  if (!node) return "";
  if (node.type === "text") return node.text || "";
  if (!Array.isArray(node.content)) return "";
  return node.content.map(nodeText).join("");
};

const headingId = (node) => slugify(nodeText(node));

const AUTHORING_NOISE_PATTERNS = [
  /^provenance\b/i,
  /^add a comment to this page$/i,
  /^click here to add a comment$/i,
];

export const isAuthoringNoiseParagraph = (node) => {
  if (node?.type !== "paragraph") return false;
  const text = nodeText(node).trim();
  return AUTHORING_NOISE_PATTERNS.some((re) => re.test(text));
};

export const stripAuthoringNoise = (adf) => {
  if (!adf || !Array.isArray(adf.content)) return adf;
  return {
    ...adf,
    content: adf.content.filter((n) => !isAuthoringNoiseParagraph(n)),
  };
};

const escapeHtml = (s) =>
  String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const escapeAttr = (s) =>
  String(s).replace(/&/g, "&amp;").replace(/"/g, "&quot;");

// Resolve a Confluence wiki URL (web-UI or page-id form) to a /help/<slug>
// link when the target is in the corpus. Returns null when the URL is not a
// known corpus page; callers then leave the URL alone (external) or raise if
// it is a /wiki/ URL (FRS AC 4).
export const resolveWikiUrl = (url, pageIdToSlug) => {
  if (!url) return null;
  const idMatch =
    url.match(/\/pages\/(\d+)(?:[\/?#]|$)/) || url.match(/[?&]pageId=(\d+)/);
  if (!idMatch) return null;
  const slug = pageIdToSlug.get(idMatch[1]);
  return slug ? `/help/${slug}` : null;
};

const applyMarks = (text, marks, ctx) => {
  let out = text;
  for (const m of marks || []) {
    switch (m.type) {
      case "strong":
        out = `<strong>${out}</strong>`;
        break;
      case "em":
        out = `<em>${out}</em>`;
        break;
      case "code":
        out = `<code>${out}</code>`;
        break;
      case "strike":
        out = `<s>${out}</s>`;
        break;
      case "underline":
        out = `<u>${out}</u>`;
        break;
      case "link": {
        const href = m.attrs?.href || "";
        const resolved = resolveWikiUrl(href, ctx.pageIdToSlug);
        if (resolved) {
          out = `<a href="${escapeAttr(resolved)}">${out}</a>`;
        } else if (/\/wiki\//.test(href) && !ctx.allowWikiUrls) {
          ctx.unresolvedWikiLinks.push(href);
          out = `<a href="${escapeAttr(href)}">${out}</a>`;
        } else {
          out = `<a href="${escapeAttr(href)}" rel="noreferrer" target="_blank">${out}</a>`;
        }
        break;
      }
      default:
        // Unknown mark — pass text through unmodified.
        break;
    }
  }
  return out;
};

const children = (node, ctx) =>
  (node.content || []).map((n) => adfNodeToHtml(n, ctx)).join("");

const adfNodeToHtml = (node, ctx) => {
  switch (node.type) {
    case "doc":
      return children(node, ctx);
    case "paragraph":
      return `<p>${children(node, ctx)}</p>`;
    case "heading": {
      const level = Math.min(6, Math.max(1, node.attrs?.level || 1));
      const id = headingId(node);
      return `<h${level} id="${escapeAttr(id)}">${children(node, ctx)}</h${level}>`;
    }
    case "text":
      return applyMarks(escapeHtml(node.text || ""), node.marks, ctx);
    case "bulletList":
      return `<ul>${children(node, ctx)}</ul>`;
    case "orderedList":
      return `<ol>${children(node, ctx)}</ol>`;
    case "listItem":
      return `<li>${children(node, ctx)}</li>`;
    case "codeBlock": {
      const lang = node.attrs?.language || "";
      const inner = (node.content || [])
        .map((c) => escapeHtml(c.text || ""))
        .join("");
      const attr = lang ? ` data-language="${escapeAttr(lang)}"` : "";
      return `<pre${attr}><code>${inner}</code></pre>`;
    }
    case "panel": {
      const panelType = node.attrs?.panelType || "info";
      return `<div class="help-panel help-panel--${escapeAttr(panelType)}" data-panel-type="${escapeAttr(panelType)}">${children(node, ctx)}</div>`;
    }
    case "blockquote":
      return `<blockquote>${children(node, ctx)}</blockquote>`;
    case "rule":
      return "<hr />";
    case "hardBreak":
      return "<br />";
    case "mediaSingle":
    case "mediaGroup":
      return `<figure class="help-media">${children(node, ctx)}</figure>`;
    case "media": {
      const id = node.attrs?.id || "";
      const alt = node.attrs?.alt || "";
      if (id) ctx.assetRefs.push(id);
      return `<img src="pending:${escapeAttr(id)}" alt="${escapeAttr(alt)}" loading="lazy" />`;
    }
    case "inlineCard": {
      const url = node.attrs?.url || "";
      const resolved = resolveWikiUrl(url, ctx.pageIdToSlug);
      if (resolved) {
        return `<a href="${escapeAttr(resolved)}">${escapeHtml(resolved)}</a>`;
      }
      if (/\/wiki\//.test(url) && !ctx.allowWikiUrls) {
        ctx.unresolvedWikiLinks.push(url);
      }
      return `<a href="${escapeAttr(url)}" rel="noreferrer" target="_blank">${escapeHtml(url)}</a>`;
    }
    case "table":
      return `<table>${children(node, ctx)}</table>`;
    case "tableRow":
      return `<tr>${children(node, ctx)}</tr>`;
    case "tableHeader":
      return `<th>${children(node, ctx)}</th>`;
    case "tableCell":
      return `<td>${children(node, ctx)}</td>`;
    default:
      ctx.unhandled.add(node.type);
      return `<div data-adf-unhandled="${escapeAttr(node.type)}"></div>`;
  }
};

const SANITIZE_OPTIONS = {
  allowedTags: [
    "p",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "ul",
    "ol",
    "li",
    "strong",
    "em",
    "code",
    "pre",
    "s",
    "u",
    "br",
    "hr",
    "blockquote",
    "a",
    "img",
    "figure",
    "div",
    "span",
    "table",
    "thead",
    "tbody",
    "tr",
    "th",
    "td",
    "section",
  ],
  allowedAttributes: {
    a: ["href", "rel", "target"],
    img: ["src", "alt", "loading", "width", "height"],
    div: ["class", "data-panel-type", "role"],
    figure: ["class"],
    section: ["class"],
    pre: ["data-language"],
    code: ["data-language"],
    "*": ["id"],
  },
  allowedSchemes: ["http", "https"],
  allowedSchemesAppliedToAttributes: ["href"],
  allowProtocolRelative: false,
};

export const adfToHtml = (adf, ctx) => {
  const stripped = stripAuthoringNoise(adf);
  const raw = adfNodeToHtml(stripped, ctx);
  return sanitizeHtml(raw, SANITIZE_OPTIONS);
};

export const buildSlugMap = (pagesByCorpus) => {
  const idToSlug = new Map();
  const used = new Map(); // slug -> count, for collision handling
  for (const pages of Object.values(pagesByCorpus)) {
    for (const p of pages) {
      let base = slugify(p.title) || `page-${p.id}`;
      let slug = base;
      const n = used.get(base) || 0;
      if (n > 0) slug = `${base}-${n + 1}`;
      used.set(base, n + 1);
      idToSlug.set(p.id, slug);
      p.slug = slug;
    }
  }
  return idToSlug;
};

// Phase 4: convert — ADF → sanitized HTML. Each page gains .html, .headings,
// .outgoingLinks and .assetRefs. Unresolved internal /wiki/ links cause the
// pipeline to fail per FRS AC 4.
const convert = async (pagesByCorpus) => {
  const pageIdToSlug = buildSlugMap(pagesByCorpus);
  const allUnhandled = new Set();
  const allUnresolved = [];
  for (const [key, pages] of Object.entries(pagesByCorpus)) {
    for (const p of pages) {
      const ctx = {
        pageIdToSlug,
        assetRefs: [],
        unresolvedWikiLinks: [],
        unhandled: new Set(),
        allowWikiUrls: false,
      };
      p.html = adfToHtml(p.adf, ctx);
      p.assetRefs = ctx.assetRefs;
      p.headings = extractHeadings(p.adf);
      for (const t of ctx.unhandled) allUnhandled.add(t);
      for (const u of ctx.unresolvedWikiLinks) {
        allUnresolved.push({ page: `${key}/${p.id}`, url: u });
      }
    }
  }
  if (allUnhandled.size > 0) {
    throw new Error(
      `convert: unhandled ADF node types: ${[...allUnhandled].sort().join(", ")} (FRS AC 2)`,
    );
  }
  if (allUnresolved.length > 0) {
    const detail = allUnresolved
      .slice(0, 5)
      .map((u) => `${u.page} -> ${u.url}`)
      .join("; ");
    throw new Error(
      `convert: ${allUnresolved.length} unresolved /wiki/ link(s): ${detail} (FRS AC 4)`,
    );
  }
};

const extractHeadings = (adf) => {
  const out = [];
  const visit = (n) => {
    if (!n || typeof n !== "object") return;
    if (n.type === "heading") {
      out.push({
        level: Math.min(6, Math.max(1, n.attrs?.level || 1)),
        text: nodeText(n),
        id: slugify(nodeText(n)),
      });
    }
    if (Array.isArray(n.content)) n.content.forEach(visit);
  };
  visit(adf);
  return out;
};

// ---- Phase 5: assets ----

export const assetHash = (buf) =>
  crypto.createHash("sha256").update(buf).digest("hex").slice(0, 16);

const downloadAttachmentLive = async (host, headers, downloadUrl) => {
  const url = downloadUrl.startsWith("http")
    ? downloadUrl
    : `https://${host}${downloadUrl}`;
  const res = await fetch(url, { headers });
  if (!res.ok)
    throw new Error(`${res.status} ${res.statusText} ${downloadUrl}`);
  return Buffer.from(await res.arrayBuffer());
};

const resolveAttachmentLive = async (host, headers, pageId, attachmentId) => {
  const data = await confluenceGet(
    host,
    headers,
    `/wiki/rest/api/content/${pageId}/child/attachment?limit=200`,
  );
  const hit = (data.results || []).find((r) => r.id === attachmentId);
  if (!hit) return null;
  const downloadUrl = hit._links?.download;
  if (!downloadUrl) return null;
  return downloadAttachmentLive(host, headers, downloadUrl);
};

const resolveAttachmentFixtures = async (attachmentId) => {
  const fixtures = await loadFixtures();
  const att = fixtures.attachments?.[attachmentId];
  if (!att) return null;
  return Buffer.from(att.base64, "base64");
};

const toWebp = async (buf) =>
  sharp(buf).webp({ quality: 80, effort: 4 }).toBuffer();

export const rewriteAssetSrc = (html, assetRewrites) => {
  let out = html;
  for (const [refId, webPath] of Object.entries(assetRewrites)) {
    const needle = `pending:${refId}`;
    out = out.split(needle).join(webPath);
  }
  return out;
};

const processAssets = async (pagesByCorpus, source) => {
  const assets = new Map(); // refId -> { webPath, bytes, fileName }
  let processed = 0;
  let failed = 0;

  for (const pages of Object.values(pagesByCorpus)) {
    for (const p of pages) {
      for (const refId of p.assetRefs || []) {
        if (assets.has(refId)) continue;
        let bytes;
        try {
          bytes =
            source.kind === "fixtures"
              ? await resolveAttachmentFixtures(refId)
              : await resolveAttachmentLive(
                  source.host,
                  source.headers,
                  p.id,
                  refId,
                );
        } catch (err) {
          console.warn(`help-refresh:   asset ${refId} failed: ${err.message}`);
          failed += 1;
          continue;
        }
        if (!bytes) {
          failed += 1;
          continue;
        }
        const webp = await toWebp(bytes);
        const hash = assetHash(webp);
        const fileName = `${hash}.webp`;
        assets.set(refId, {
          bytes: webp,
          fileName,
          webPath: `/help-content/assets/${fileName}`,
        });
        processed += 1;
      }
    }
  }

  const totalAttempts = processed + failed;
  if (totalAttempts > 0 && failed / totalAttempts > 0.01) {
    throw new Error(
      `processAssets: ${failed}/${totalAttempts} failed (>1%, FRS AC 2)`,
    );
  }

  const rewrites = {};
  for (const [refId, a] of assets.entries()) rewrites[refId] = a.webPath;
  for (const pages of Object.values(pagesByCorpus)) {
    for (const p of pages) {
      p.html = rewriteAssetSrc(p.html, rewrites);
    }
  }
  console.log(
    `help-refresh: assets processed=${processed} failed=${failed} unique=${assets.size}`,
  );
  return assets;
};

// ---- Phase 6: emit ----

// Deterministic JSON stringify: keys in a stable sort order. Guarantees that
// repeat runs with the same input produce byte-identical output (FRS AC 1).
export const stableStringify = (value) => {
  const seen = new WeakSet();
  const walk = (v) => {
    if (v === null || typeof v !== "object") return v;
    if (seen.has(v)) throw new Error("stableStringify: cycle");
    seen.add(v);
    if (Array.isArray(v)) return v.map(walk);
    const sorted = {};
    for (const k of Object.keys(v).sort()) sorted[k] = walk(v[k]);
    return sorted;
  };
  return JSON.stringify(walk(value), null, 2);
};

export const injectWipNotice = (html, labels, locale) => {
  if (!labels?.includes("wip")) return html;
  const notice =
    locale === "fr"
      ? "Cette page documente une fonctionnalité en cours de développement."
      : "This page documents a work-in-progress feature.";
  return `<div class="help-notice help-notice--wip" role="note">${escapeHtml(notice)}</div>${html}`;
};

export const injectOnlineLink = (html, webuiPath, host, locale) => {
  if (!webuiPath) return html;
  const url = webuiPath.startsWith("http")
    ? webuiPath
    : `https://${host}${webuiPath}`;
  const label =
    locale === "fr"
      ? "Voir la dernière version en ligne"
      : "View the latest version online";
  const link = `<p class="help-online-link"><a href="${escapeAttr(url)}" rel="noreferrer" target="_blank">${escapeHtml(label)}</a></p>`;
  return `${html}${link}`;
};

const buildManifest = (pagesByCorpus, locale, releaseVersion) => {
  const guides = {};
  for (const [key, pages] of Object.entries(pagesByCorpus)) {
    const [guide, pageLocale] = key.split("/");
    if (pageLocale !== locale) continue;
    guides[guide] = pages
      .slice()
      .sort((a, b) => a.slug.localeCompare(b.slug))
      .map((p) => ({
        id: p.id,
        slug: p.slug,
        title: p.title,
        headings: p.headings || [],
        sourceId: p.sourceId || null,
        webuiPath: p.webuiPath || null,
        labels: p.labels || [],
      }));
  }
  return { releaseVersion, locale, guides };
};

const buildSearchIndex = (pagesByCorpus, locale) => {
  const Document = FlexSearch.Document;
  const index = new Document({
    tokenize: "forward",
    document: {
      id: "slug",
      index: ["title", "headings", "text"],
      store: ["title", "guide"],
    },
  });

  for (const [key, pages] of Object.entries(pagesByCorpus)) {
    const [guide, pageLocale] = key.split("/");
    if (pageLocale !== locale) continue;
    for (const p of pages) {
      index.add({
        slug: p.slug,
        title: p.title,
        headings: (p.headings || []).map((h) => h.text).join(" "),
        text: nodeText(p.adf),
        guide,
      });
    }
  }

  const dump = {};
  return new Promise((resolve) => {
    let pending = 0;
    index.export((key, data) => {
      pending += 1;
      dump[key] = data;
      queueMicrotask(() => {
        pending -= 1;
        if (pending === 0) resolve(dump);
      });
    });
    queueMicrotask(() => {
      if (pending === 0) resolve(dump);
    });
  });
};

const computeTotalBytes = (filesToWrite) =>
  filesToWrite.reduce((sum, f) => sum + f.bytes.byteLength, 0);

// Phase 6: emit — manifest.json, index.json (FlexSearch), pages/<slug>.html,
// assets/*.webp. Enforces the sizeBudgetBytes ceiling before writing anything
// (FRS AC 19).
const emit = async (pagesByCorpus, config, assets, source) => {
  const outputDir = path.resolve(
    path.join(__dirname, "..", ".."),
    config.outputDir,
  );
  const host =
    source.kind === "fixtures"
      ? (await loadFixtures()).confluenceHost || "example.atlassian.net"
      : source.host;

  const locales = new Set(config.corpora.map((c) => c.locale));
  const filesToWrite = [];

  // Per-locale manifest + index + pages
  for (const locale of locales) {
    const manifest = buildManifest(
      pagesByCorpus,
      locale,
      config.releaseVersion,
    );
    const manifestJson = stableStringify(manifest);
    filesToWrite.push({
      relPath: path.join(locale, "manifest.json"),
      bytes: Buffer.from(manifestJson, "utf8"),
    });

    const indexDump = await buildSearchIndex(pagesByCorpus, locale);
    const indexJson = stableStringify(indexDump);
    filesToWrite.push({
      relPath: path.join(locale, "index.json"),
      bytes: Buffer.from(indexJson, "utf8"),
    });

    for (const [key, pages] of Object.entries(pagesByCorpus)) {
      const [guide, pageLocale] = key.split("/");
      if (pageLocale !== locale) continue;
      for (const p of pages) {
        let html = p.html;
        html = injectWipNotice(html, p.labels, locale);
        html = injectOnlineLink(html, p.webuiPath, host, locale);
        filesToWrite.push({
          relPath: path.join(locale, guide, `${p.slug}.html`),
          bytes: Buffer.from(html, "utf8"),
        });
      }
    }
  }

  for (const [, asset] of assets.entries()) {
    filesToWrite.push({
      relPath: path.join("assets", asset.fileName),
      bytes: asset.bytes,
    });
  }

  const total = computeTotalBytes(filesToWrite);
  if (total > config.sizeBudgetBytes) {
    throw new Error(
      `emit: output ${total} bytes exceeds budget ${config.sizeBudgetBytes} (FRS AC 19)`,
    );
  }
  console.log(
    `help-refresh: emit size=${total} bytes (budget=${config.sizeBudgetBytes})`,
  );

  await fs.rm(outputDir, { recursive: true, force: true });
  await fs.mkdir(outputDir, { recursive: true });
  for (const f of filesToWrite) {
    const dest = path.join(outputDir, f.relPath);
    await fs.mkdir(path.dirname(dest), { recursive: true });
    await fs.writeFile(dest, f.bytes);
  }
  console.log(
    `help-refresh: wrote ${filesToWrite.length} files to ${outputDir}`,
  );
};

const main = async () => {
  const config = await loadConfig();
  const source = useFixtures
    ? { kind: "fixtures" }
    : { kind: "live", ...confluenceAuth() };

  console.log(
    `help-refresh: release ${config.releaseVersion}, source=${source.kind}`,
  );
  console.log(`help-refresh: ${config.corpora.length} corpora to walk`);
  for (const c of config.corpora) {
    console.log(`  - ${c.guide}/${c.locale} rootPageId=${c.rootPageId}`);
  }

  const pages = await walk(config, source);
  await fetchPages(pages, source);
  await matchLocales(pages, source, config);
  await convert(pages);
  const assets = await processAssets(pages, source);
  await emit(pages, config, assets, source);
};

const isMain =
  import.meta.url === `file://${process.argv[1]}` ||
  process.argv[1]?.endsWith("help-refresh.mjs");

if (isMain) {
  main().catch((err) => {
    console.error(err.message);
    process.exit(1);
  });
}
