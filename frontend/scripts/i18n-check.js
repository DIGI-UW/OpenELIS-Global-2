#!/usr/bin/env node
/**
 * i18n-check: duplicate-value and missing-key checks for en.json.
 * Constitution Principle VII (Key Reuse & Hygiene).
 *
 * Modes:
 *   --duplicates <base-en.json>  Warn for keys added (vs base) whose value
 *                                already exists under another key. common.*
 *                                keys are exempt (they are the canonical
 *                                targets duplicates get consolidated onto).
 *   --missing                    Warn for ids referenced at react-intl call
 *                                sites in src/ that do not exist in en.json.
 *   --hook                       Claude Code PostToolUse hook: reads the hook
 *                                JSON from stdin; if the edited file is
 *                                en.json, runs the duplicate check against
 *                                the git develop version and reports via
 *                                exit code 2 (feedback to the agent).
 *
 * All modes are WARN-ONLY for CI right now (exit 0) except --hook, which
 * exits 2 on findings so the agent self-corrects. Flip WARN_ONLY to false
 * once the consolidation baseline lands.
 */
const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const WARN_ONLY = true;
const FRONTEND = path.join(__dirname, "..");
const EN_PATH = path.join(FRONTEND, "src", "languages", "en.json");
const EN_REL = "frontend/src/languages/en.json";

const norm = (s) =>
  s
    .toLowerCase()
    .replace(/[:*.…]+$/g, "")
    .replace(/\s+/g, " ")
    .trim();

function loadJson(p) {
  return JSON.parse(fs.readFileSync(p, "utf8"));
}

function duplicateFindings(baseEn, headEn) {
  const valueIndex = new Map(); // norm(value) -> [existing keys]
  for (const [k, v] of Object.entries(headEn)) {
    if (typeof v !== "string") continue;
    const n = norm(v);
    if (!valueIndex.has(n)) valueIndex.set(n, []);
    valueIndex.get(n).push(k);
  }
  const findings = [];
  for (const [k, v] of Object.entries(headEn)) {
    if (k in baseEn) continue; // only newly added keys
    if (k.startsWith("common.")) continue; // canonical targets are exempt
    if (typeof v !== "string" || !v.trim()) continue;
    const others = (valueIndex.get(norm(v)) || []).filter((o) => o !== k);
    if (others.length) {
      const canonical = others.find((o) => o.startsWith("common."));
      findings.push({
        key: k,
        value: v,
        reuse: canonical || others.sort((a, b) => a.length - b.length)[0],
        others: others.length,
      });
    }
  }
  return findings;
}

function missingFindings(en) {
  const ids = new Set();
  const walk = (dir) => {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, e.name);
      if (e.isDirectory()) walk(p);
      else if (/\.(jsx?|tsx?)$/.test(e.name)) {
        const txt = fs.readFileSync(p, "utf8");
        for (const m of txt.matchAll(
          /formatMessage\(\s*\{\s*id:\s*["']([^"']+)["']/g,
        ))
          ids.add(m[1]);
        for (const m of txt.matchAll(
          /<FormattedMessage[^>]{0,200}?id=["']([^"']+)["']/gs,
        ))
          ids.add(m[1]);
      }
    }
  };
  walk(path.join(FRONTEND, "src"));
  return [...ids]
    .filter(
      (i) =>
        !(i in en) && i.includes(".") && !/\s/.test(i) && !/^[\d.]+$/.test(i),
    )
    .sort();
}

const mode = process.argv[2];

if (mode === "--duplicates") {
  const base = loadJson(process.argv[3]);
  const head = loadJson(EN_PATH);
  const findings = duplicateFindings(base, head);
  for (const f of findings)
    console.log(
      `::warning file=${EN_REL}::New key "${f.key}" duplicates existing value "${f.value}" — reuse "${f.reuse}" instead (${f.others} existing key(s) share this value). Run: npm run i18n:find -- "${f.value}"`,
    );
  console.log(`i18n duplicate-value check: ${findings.length} finding(s).`);
  process.exit(findings.length && !WARN_ONLY ? 1 : 0);
} else if (mode === "--missing") {
  const en = loadJson(EN_PATH);
  const missing = missingFindings(en);
  for (const k of missing)
    console.log(
      `::warning::id "${k}" is referenced at a react-intl call site but missing from en.json (renders as raw id / untranslatable).`,
    );
  console.log(`i18n missing-key check: ${missing.length} finding(s).`);
  process.exit(missing.length && !WARN_ONLY ? 1 : 0);
} else if (mode === "--hook") {
  let input = "";
  try {
    input = fs.readFileSync(0, "utf8");
  } catch (e) {
    process.exit(0);
  }
  let payload;
  try {
    payload = JSON.parse(input);
  } catch (e) {
    process.exit(0);
  }
  const file = (payload.tool_input && payload.tool_input.file_path) || "";
  if (!file.endsWith("languages/en.json")) process.exit(0);
  let baseRaw;
  try {
    baseRaw = execSync(`git show develop:${EN_REL}`, {
      cwd: path.join(FRONTEND, ".."),
      maxBuffer: 64 * 1024 * 1024,
    }).toString();
  } catch (e) {
    process.exit(0); // no base available; stay silent
  }
  let head;
  try {
    head = loadJson(EN_PATH);
  } catch (e) {
    console.error(`en.json is not valid JSON after this edit: ${e.message}`);
    process.exit(2);
  }
  const findings = duplicateFindings(JSON.parse(baseRaw), head);
  if (findings.length) {
    console.error(
      `i18n Key Reuse violation (Constitution VII): ${findings.length} newly added key(s) duplicate existing English values. REUSE the existing keys instead of minting new ones:`,
    );
    for (const f of findings.slice(0, 20))
      console.error(`  - "${f.key}" = "${f.value}" -> reuse "${f.reuse}"`);
    console.error(
      `If a duplicate is intentional (different translation context), record it in frontend/src/languages/i18n-context-exceptions.json and say so in the PR.`,
    );
    process.exit(2);
  }
  process.exit(0);
} else {
  console.error(
    "Usage: i18n-check.js --duplicates <base-en.json> | --missing | --hook",
  );
  process.exit(1);
}
