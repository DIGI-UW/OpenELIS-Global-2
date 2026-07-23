// Grist <-> uat-*.json sync. Two modes:
//   seed      review/uat-*.json  ->  Grist (authoring source of truth)
//   generate  Grist              ->  review/uat-*.json (what the overlay serves)
//
// Env: GRIST_URL, GRIST_KEY, GRIST_ORG (default "openelis"),
//      GRIST_DOC_NAME (default "UAT Checklists"), REVIEW_DIR (default ../review).
// Schema: UAT_Meta(instance,title,intro,jira), UAT_Steps(instance,section,
// section_order,step_order,do,expect,route), UAT_Results(reviewer,instance,
// step_key,mark,note,page_url,at) — Results is created now, filled later.

import { readFileSync, writeFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

const URL = process.env.GRIST_URL || "http://grist:8484";
const KEY = process.env.GRIST_KEY;
const ORG = process.env.GRIST_ORG || "openelis";
const DOC_NAME = process.env.GRIST_DOC_NAME || "UAT Checklists";
const REVIEW_DIR = process.env.REVIEW_DIR || join(import.meta.dirname, "..", "review");
if (!KEY) throw new Error("GRIST_KEY is required");

const TABLES = {
  UAT_Meta: [
    ["instance", "Text"],
    ["title", "Text"],
    ["intro", "Text"],
    ["jira", "Text"],
  ],
  UAT_Steps: [
    ["instance", "Text"],
    ["section", "Text"],
    ["section_order", "Int"],
    ["step_order", "Int"],
    ["do", "Text"],
    ["expect", "Text"],
    ["route", "Text"],
  ],
  UAT_Results: [
    ["reviewer", "Text"],
    ["instance", "Text"],
    ["step_key", "Text"],
    ["mark", "Text"],
    ["note", "Text"],
    ["page_url", "Text"],
    ["at", "Text"],
  ],
};

async function api(path, opts = {}) {
  const r = await fetch(URL + path, {
    ...opts,
    headers: {
      Authorization: "Bearer " + KEY,
      "Content-Type": "application/json",
      ...(opts.headers || {}),
    },
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`${opts.method || "GET"} ${path} -> ${r.status} ${text}`);
  return text ? JSON.parse(text) : null;
}

async function resolveDoc() {
  const wss = await api(`/api/orgs/${ORG}/workspaces`);
  for (const ws of wss) {
    const hit = (ws.docs || []).find((d) => d.name === DOC_NAME);
    if (hit) return hit.id;
  }
  const ws = wss[0];
  return await api(`/api/workspaces/${ws.id}/docs`, {
    method: "POST",
    body: JSON.stringify({ name: DOC_NAME }),
  });
}

async function ensureTables(doc) {
  const existing = new Set((await api(`/api/docs/${doc}/tables`)).tables.map((t) => t.id));
  for (const [id, cols] of Object.entries(TABLES)) {
    if (existing.has(id)) continue;
    await api(`/api/docs/${doc}/tables`, {
      method: "POST",
      body: JSON.stringify({
        tables: [
          {
            id,
            columns: cols.map(([c, type]) => ({ id: c, fields: { label: c, type } })),
          },
        ],
      }),
    });
    console.log(`  created table ${id}`);
  }
}

async function clearTable(doc, table) {
  const recs = (await api(`/api/docs/${doc}/tables/${table}/records`)).records;
  if (recs.length)
    await api(`/api/docs/${doc}/tables/${table}/data/delete`, {
      method: "POST",
      body: JSON.stringify(recs.map((r) => r.id)),
    });
}

async function addRecords(doc, table, rows) {
  if (!rows.length) return;
  await api(`/api/docs/${doc}/tables/${table}/records`, {
    method: "POST",
    body: JSON.stringify({ records: rows.map((fields) => ({ fields })) }),
  });
}

function instancesFromReviewDir() {
  return readdirSync(REVIEW_DIR)
    .filter((f) => /^uat-.*\.json$/.test(f))
    .map((f) => f.replace(/^uat-|\.json$/g, ""));
}

async function seed() {
  const doc = await resolveDoc();
  await ensureTables(doc);
  const metaRows = [];
  const stepRows = [];
  for (const inst of instancesFromReviewDir()) {
    const j = JSON.parse(readFileSync(join(REVIEW_DIR, `uat-${inst}.json`), "utf8"));
    metaRows.push({
      instance: inst,
      title: j.title || "",
      intro: j.intro || "",
      jira: j.jira || "",
    });
    (j.sections || []).forEach((sec, si) => {
      (sec.steps || []).forEach((step, ti) => {
        stepRows.push({
          instance: inst,
          section: sec.title,
          section_order: si,
          step_order: ti,
          do: step.do || step.text || "",
          expect: step.expect || "",
          route: step.route || "",
        });
      });
    });
  }
  await clearTable(doc, "UAT_Meta");
  await clearTable(doc, "UAT_Steps");
  await addRecords(doc, "UAT_Meta", metaRows);
  await addRecords(doc, "UAT_Steps", stepRows);
  console.log(`seeded doc ${doc}: ${metaRows.length} meta, ${stepRows.length} steps`);
  console.log(doc);
}

async function generate() {
  const doc = await resolveDoc();
  const meta = {};
  for (const r of (await api(`/api/docs/${doc}/tables/UAT_Meta/records`)).records)
    meta[r.fields.instance] = r.fields;
  const steps = (await api(`/api/docs/${doc}/tables/UAT_Steps/records`)).records.map((r) => r.fields);
  const byInstance = {};
  for (const s of steps) (byInstance[s.instance] ||= []).push(s);
  for (const [inst, rows] of Object.entries(byInstance)) {
    rows.sort((a, b) => a.section_order - b.section_order || a.step_order - b.step_order);
    const sections = [];
    for (const s of rows) {
      let sec = sections.find((x) => x._order === s.section_order);
      if (!sec) {
        sec = { title: s.section, steps: [], _order: s.section_order };
        sections.push(sec);
      }
      const step = { do: s.do };
      if (s.expect) step.expect = s.expect;
      if (s.route) step.route = s.route;
      sec.steps.push(step);
    }
    sections.forEach((s) => delete s._order);
    const m = meta[inst] || {};
    const out = {
      title: m.title || `${inst} review`,
      instance: inst,
      jira: m.jira || "",
      intro: m.intro || "",
      sections,
    };
    const path = join(REVIEW_DIR, `uat-${inst}.json`);
    writeFileSync(path, JSON.stringify(out, null, 2) + "\n");
    console.log(`wrote ${path}: ${sections.length} sections, ${rows.length} steps`);
  }
}

const mode = process.argv[2];
if (mode === "seed") await seed();
else if (mode === "generate") await generate();
else {
  console.error("usage: grist-sync.mjs seed|generate");
  process.exit(1);
}
