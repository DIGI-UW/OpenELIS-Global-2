#!/usr/bin/env node
// Helper for OGC help ticket: list candidate root pages for the user and
// administrator manuals in English and French, so you can fill the pipeline's
// per-{guide, locale} root-id config. Throwaway.
//
// Usage:
//   set -a; source .env; set +a
//   node frontend/scripts/find-french-pages.mjs
//
// For each candidate it also prints a descendant count — a real manual root
// has many; a sub-page has few. If a group comes up empty, that corpus is not
// in Confluence yet or uses a title we did not try.

const { CONFLUENCE_HOST, CONFLUENCE_EMAIL, CONFLUENCE_TOKEN } = process.env;

if (!CONFLUENCE_HOST || !CONFLUENCE_EMAIL || !CONFLUENCE_TOKEN) {
  console.error(
    "need CONFLUENCE_HOST, CONFLUENCE_EMAIL, CONFLUENCE_TOKEN in env",
  );
  process.exit(2);
}

const auth =
  "Basic " +
  Buffer.from(`${CONFLUENCE_EMAIL}:${CONFLUENCE_TOKEN}`).toString("base64");

const api = async (path) => {
  const res = await fetch(`https://${CONFLUENCE_HOST}${path}`, {
    headers: { Authorization: auth, Accept: "application/json" },
  });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText} ${path}`);
  return res.json();
};

const descendantCount = async (id) => {
  const data = await api(`/wiki/api/v2/pages/${id}/descendants?limit=250`);
  return data.results?.length ?? 0;
};

const groups = [
  [
    "User manual (en)",
    ['title = "User Manual"', 'title = "User Guide"', 'title ~ "📖 User"'],
  ],
  [
    "User manual (fr)",
    [
      'title = "Manuel de l\'utilisateur"',
      'title = "Guide de l\'utilisateur"',
      'title ~ "📖 Manuel"',
    ],
  ],
  [
    "Admin manual (en)",
    [
      'title = "Administrator Manual"',
      'title = "Administrator Guide"',
      'title ~ "📖 Admin"',
    ],
  ],
  [
    "Admin manual (fr)",
    [
      'title ~ "Manuel administrateur"',
      'title ~ "Guide de l\'administrateur"',
      'title ~ "Guide administrateur"',
      'title ~ "Manuel de l\'administrateur"',
    ],
  ],
];

for (const [label, clauses] of groups) {
  console.log(`\n== ${label} candidates ==`);
  const cql = encodeURIComponent(
    `space=OG AND type=page AND (${clauses.join(" OR ")})`,
  );
  const data = await api(`/wiki/rest/api/content/search?cql=${cql}&limit=20`);
  if (!data.results?.length) {
    console.log("(none found)");
    continue;
  }
  for (const r of data.results) {
    const n = await descendantCount(r.id).catch(() => "?");
    const flag = typeof n === "number" && n >= 20 ? "  ← root?" : "";
    console.log(
      `  ${String(r.id).padEnd(12)} ${String(n).padStart(3)} descendants  ${r.title}${flag}`,
    );
  }
}

console.log(
  "\nFor each group, the id whose row says '← root?' is the folder root.",
);
console.log("Record the four ids; they go in help-refresh.config.json.\n");
