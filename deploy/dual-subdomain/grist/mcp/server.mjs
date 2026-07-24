// Grist UAT bridge — one process, two surfaces:
//   POST /mcp            MCP (Streamable HTTP, stateless). Bearer-gated authoring.
//   GET  /uat/:file      Public read. Returns the reviewer-widget checklist shape
//                        for <file>=<instance>.json, computed live from Grist.
//   GET  /healthz        Liveness.
//
// The Grist API key is held here (server-side) and never exposed to callers:
// the widget reads through GET /uat, LLM clients write through the authed tools.
//
// Env: GRIST_URL, GRIST_KEY | GRIST_KEY_FILE, GRIST_ORG, GRIST_DOC_NAME,
//      TOKEN_FILE (JSON [{token,label,created}]), PORT.

import express from "express";
import { readFileSync } from "node:fs";
import { z } from "zod";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";

const GRIST_URL = process.env.GRIST_URL || "http://grist:8484";
const GRIST_ORG = process.env.GRIST_ORG || "openelis";
const DOC_NAME = process.env.GRIST_DOC_NAME || "UAT Checklists";
const GRIST_KEY_FILE = process.env.GRIST_KEY_FILE;
const TOKEN_FILE = process.env.TOKEN_FILE;
const PORT = Number(process.env.PORT || 8585);

function gristKey() {
  if (process.env.GRIST_KEY) return process.env.GRIST_KEY.trim();
  if (GRIST_KEY_FILE) return readFileSync(GRIST_KEY_FILE, "utf8").trim();
  throw new Error("no GRIST_KEY or GRIST_KEY_FILE configured");
}

async function grist(path, opts = {}) {
  const r = await fetch(GRIST_URL + path, {
    ...opts,
    headers: {
      Authorization: "Bearer " + gristKey(),
      "Content-Type": "application/json",
      ...(opts.headers || {}),
    },
  });
  const text = await r.text();
  if (!r.ok) throw new Error(`Grist ${opts.method || "GET"} ${path} -> ${r.status} ${text}`);
  return text ? JSON.parse(text) : null;
}

// Doc id is stable for the life of the doc; resolve once by name.
let _docId = null;
async function docId() {
  if (_docId) return _docId;
  const wss = await grist(`/api/orgs/${GRIST_ORG}/workspaces`);
  for (const ws of wss) {
    const hit = (ws.docs || []).find((d) => d.name === DOC_NAME);
    if (hit) return (_docId = hit.id);
  }
  throw new Error(`Grist doc "${DOC_NAME}" not found in org ${GRIST_ORG}`);
}

async function listRecords(table, filter) {
  const doc = await docId();
  let path = `/api/docs/${doc}/tables/${table}/records`;
  if (filter) path += `?filter=${encodeURIComponent(JSON.stringify(filter))}`;
  return (await grist(path)).records;
}
async function addRecord(table, fields) {
  const doc = await docId();
  const res = await grist(`/api/docs/${doc}/tables/${table}/records`, {
    method: "POST",
    body: JSON.stringify({ records: [{ fields }] }),
  });
  return res.records[0].id;
}
async function patchRecord(table, id, fields) {
  const doc = await docId();
  await grist(`/api/docs/${doc}/tables/${table}/records`, {
    method: "PATCH",
    body: JSON.stringify({ records: [{ id, fields }] }),
  });
  return id;
}
async function deleteRecord(table, id) {
  const doc = await docId();
  await grist(`/api/docs/${doc}/tables/${table}/data/delete`, {
    method: "POST",
    body: JSON.stringify([id]),
  });
}

// Grist rows -> the widget's checklist shape (title/intro/sections[].steps[]).
async function uatDocument(instance) {
  const metaRecs = await listRecords("UAT_Meta", { instance: [instance] });
  const stepRecs = await listRecords("UAT_Steps", { instance: [instance] });
  const m = (metaRecs[0] && metaRecs[0].fields) || {};
  const rows = stepRecs
    .map((r) => r.fields)
    .sort((a, b) => a.section_order - b.section_order || a.step_order - b.step_order);
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
  return {
    title: m.title || `${instance} review`,
    instance,
    jira: m.jira || "",
    intro: m.intro || "",
    sections,
  };
}

const ok = (text) => ({ content: [{ type: "text", text }] });
const fail = (text) => ({ content: [{ type: "text", text }], isError: true });

// Stateless: a fresh server per request. Tools are the authoring surface.
function buildMcpServer() {
  const server = new McpServer({ name: "grist-uat", version: "1.0.0" });

  server.registerTool(
    "uat_list_instances",
    {
      description:
        "List the UAT checklist instances (e.g. amr, analyzers) with their title and Jira key.",
      inputSchema: {},
    },
    async () => {
      const metas = await listRecords("UAT_Meta");
      const list = metas.map((r) => ({
        instance: r.fields.instance,
        title: r.fields.title,
        jira: r.fields.jira,
      }));
      return ok(JSON.stringify(list, null, 2));
    },
  );

  server.registerTool(
    "uat_get",
    {
      description:
        "Get one instance's full checklist: meta plus every step. Each step includes its Grist row `id`, which uat_upsert_step/uat_delete_step need to target it.",
      inputSchema: { instance: z.string().describe("instance slug, e.g. 'amr'") },
    },
    async ({ instance }) => {
      const metaRecs = await listRecords("UAT_Meta", { instance: [instance] });
      const stepRecs = await listRecords("UAT_Steps", { instance: [instance] });
      const out = {
        meta: metaRecs[0] ? { id: metaRecs[0].id, ...metaRecs[0].fields } : null,
        steps: stepRecs
          .map((r) => ({ id: r.id, ...r.fields }))
          .sort((a, b) => a.section_order - b.section_order || a.step_order - b.step_order),
      };
      return ok(JSON.stringify(out, null, 2));
    },
  );

  server.registerTool(
    "uat_upsert_step",
    {
      description:
        "Create or update one checklist step. Omit `id` to create; pass an existing step's `id` (from uat_get) to update it in place. Reviewers see steps ordered by section_order then step_order.",
      inputSchema: {
        instance: z.string(),
        section: z.string().describe("section heading this step belongs under"),
        section_order: z.number().int().describe("0-based order of the section"),
        step_order: z.number().int().describe("0-based order of the step within its section"),
        do: z.string().describe("the action the reviewer performs"),
        expect: z.string().optional().describe("the expected result"),
        route: z.string().optional().describe("app route for this step, e.g. /MicrobiologyWorklist"),
        id: z.number().int().optional().describe("existing Grist row id to update; omit to create"),
      },
    },
    async ({ id, instance, section, section_order, step_order, do: doText, expect, route }) => {
      const fields = {
        instance,
        section,
        section_order,
        step_order,
        do: doText,
        expect: expect || "",
        route: route || "",
      };
      const rowId = id ? await patchRecord("UAT_Steps", id, fields) : await addRecord("UAT_Steps", fields);
      return ok(`${id ? "updated" : "created"} step id=${rowId}`);
    },
  );

  server.registerTool(
    "uat_delete_step",
    {
      description: "Delete one checklist step by its Grist row id (from uat_get).",
      inputSchema: { id: z.number().int().describe("Grist row id of the step to delete") },
    },
    async ({ id }) => {
      await deleteRecord("UAT_Steps", id);
      return ok(`deleted step id=${id}`);
    },
  );

  server.registerTool(
    "uat_set_meta",
    {
      description:
        "Create or update an instance's meta (title, intro, jira). Upserts by instance slug; only the fields you pass are changed.",
      inputSchema: {
        instance: z.string(),
        title: z.string().optional(),
        intro: z.string().optional(),
        jira: z.string().optional(),
      },
    },
    async ({ instance, title, intro, jira }) => {
      const existing = await listRecords("UAT_Meta", { instance: [instance] });
      const fields = { instance };
      if (title !== undefined) fields.title = title;
      if (intro !== undefined) fields.intro = intro;
      if (jira !== undefined) fields.jira = jira;
      const rowId = existing[0]
        ? await patchRecord("UAT_Meta", existing[0].id, fields)
        : await addRecord("UAT_Meta", fields);
      return ok(`meta upserted id=${rowId}`);
    },
  );

  return server;
}

// Token file is read per request so generate/revoke take effect without a restart.
function validTokens() {
  if (!TOKEN_FILE) return new Set();
  try {
    return new Set(JSON.parse(readFileSync(TOKEN_FILE, "utf8")).map((e) => e.token));
  } catch {
    return new Set();
  }
}

const app = express();
app.use(express.json({ limit: "1mb" }));

app.get("/healthz", (_req, res) => res.json({ ok: true }));

app.get("/uat/:file", async (req, res) => {
  const instance = req.params.file.replace(/\.json$/, "");
  try {
    res.set("Cache-Control", "no-store").json(await uatDocument(instance));
  } catch (e) {
    res.status(502).json({ error: String(e.message || e) });
  }
});

function requireToken(req, res, next) {
  const m = (req.headers.authorization || "").match(/^Bearer\s+(.+)$/i);
  if (!m || !validTokens().has(m[1])) {
    return res
      .status(401)
      .json({ jsonrpc: "2.0", error: { code: -32001, message: "unauthorized" }, id: null });
  }
  next();
}

app.post("/mcp", requireToken, async (req, res) => {
  const server = buildMcpServer();
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: undefined, // stateless
    enableJsonResponse: true, // plain JSON, no SSE — nginx-friendly
  });
  res.on("close", () => {
    transport.close();
    server.close();
  });
  await server.connect(transport);
  await transport.handleRequest(req, res, req.body);
});

// Stateless: no server-initiated SSE stream, no session to delete.
const noStream = (_req, res) => res.status(405).json({ error: "method not allowed (stateless)" });
app.get("/mcp", noStream);
app.delete("/mcp", noStream);

app.listen(PORT, () =>
  console.error(`[grist-uat] :${PORT} — POST /mcp (authed), GET /uat/:file (public)`),
);
