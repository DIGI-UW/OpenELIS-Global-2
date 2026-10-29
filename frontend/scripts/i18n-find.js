#!/usr/bin/env node
/**
 * i18n-find: search en.json for existing keys matching an English string.
 * Usage: npm run i18n:find -- "collection date"
 * Per Constitution Principle VII (Key Reuse): run this BEFORE adding any
 * new key. If a canonical (common.*) key matches, reuse it. If the value
 * exists only under a feature-scoped key, promote it to common.* instead
 * of referencing another feature's key.
 */
const fs = require("fs");
const path = require("path");

const query = process.argv.slice(2).join(" ").trim();
if (!query) {
  console.error('Usage: npm run i18n:find -- "<english text>"');
  process.exit(1);
}

const enPath = path.join(__dirname, "..", "src", "languages", "en.json");
const en = JSON.parse(fs.readFileSync(enPath, "utf8"));

const norm = (s) =>
  s
    .toLowerCase()
    .replace(/[:*.…]+$/g, "")
    .replace(/\s+/g, " ")
    .trim();

const bigrams = (s) => {
  const set = new Set();
  const t = ` ${s} `;
  for (let i = 0; i < t.length - 1; i++) set.add(t.slice(i, i + 2));
  return set;
};

const dice = (a, b) => {
  const A = bigrams(a);
  const B = bigrams(b);
  let inter = 0;
  for (const g of A) if (B.has(g)) inter++;
  return (2 * inter) / (A.size + B.size);
};

const q = norm(query);
const results = [];
for (const [key, value] of Object.entries(en)) {
  if (typeof value !== "string") continue;
  const v = norm(value);
  let score;
  if (v === q) score = 1;
  else if (v.includes(q) || q.includes(v)) score = 0.9;
  else score = dice(q, v) * 0.89;
  if (score >= 0.6) results.push({ key, value, score });
}

results.sort(
  (a, b) =>
    b.score - a.score ||
    (b.key.startsWith("common.") ? 1 : 0) -
      (a.key.startsWith("common.") ? 1 : 0) ||
    a.key.length - b.key.length,
);

// canonical keys first within equal scores
const canonical = results.filter((r) => r.key.startsWith("common."));
const rest = results.filter((r) => !r.key.startsWith("common."));

if (!results.length) {
  console.log(`No match for "${query}" — a new key is justified.`);
  console.log("Namespace it by domain (e.g. qc.controlLot.field.expiry).");
  process.exit(0);
}

if (canonical.length) {
  console.log("CANONICAL (reuse these):");
  for (const r of canonical.slice(0, 5))
    console.log(
      `  ${(r.score * 100).toFixed(0).padStart(3)}%  ${r.key} = "${r.value}"`,
    );
}
if (rest.length) {
  console.log(
    "Feature-scoped matches (do NOT cross-reference — promote to common.* if you need one):",
  );
  for (const r of rest.slice(0, 10))
    console.log(
      `  ${(r.score * 100).toFixed(0).padStart(3)}%  ${r.key} = "${r.value}"`,
    );
}
