// The MCP tools: read-only views over the OpenELIS add-on API. Each tool names
// the core scopes it needs; the patient tools also need the agent's
// `identifiers` switch. Inputs are checked here (zod) before anything reaches
// OpenELIS; OpenELIS checks the same caps again.
import * as z from "zod";

export const LAB_NUMBER = /^[A-Za-z0-9._-]{1,64}$/;
export const MAX_RANGE_DAYS = 31;

/** Calls the add-on API for one tool call (service key, actor and purpose already set). */
export type CoreGet = <T>(
  path: string,
  template: string,
  query?: Record<string, string | number | undefined>,
) => Promise<T>;

export interface ToolResult {
  data: Record<string, unknown>;
  rows: number;
  summary: string;
}

export interface ToolDef {
  name: string;
  title: string;
  description: string;
  /** Core scopes the add-on must hold for this tool to be offered. */
  scopes: string[];
  /** Offered only to agents whose token has `identifiers` on. */
  identifiers: boolean;
  input: z.ZodType<Record<string, unknown>>;
  output: z.ZodType<Record<string, unknown>>;
  run(core: CoreGet, args: Record<string, unknown>): Promise<ToolResult>;
}

const labNumber = z
  .string()
  .regex(LAB_NUMBER, "a lab number: letters, digits, '.', '_' or '-', at most 64");
const day = z.iso.date().describe("YYYY-MM-DD");
const section = z
  .string()
  .min(1)
  .max(100)
  .optional()
  .describe("Section (unit) name, as in OpenELIS");

function rangeOk(v: { from?: unknown; to?: unknown }) {
  if (typeof v.from !== "string" || typeof v.to !== "string") return true;
  const d = (Date.parse(`${v.to}T00:00:00Z`) - Date.parse(`${v.from}T00:00:00Z`)) / 86400000;
  // both days count: 2026-10-01..2026-10-31 is 31 days, as OpenELIS counts them
  return d >= 0 && d <= MAX_RANGE_DAYS - 1;
}
const rangeMessage = {
  message: `'to' must be on or after 'from'; at most ${MAX_RANGE_DAYS} days, both included`,
  path: ["to"],
};

const str = z.string().nullable().optional();
const num = z.number().nullable().optional();

const patient = z
  .looseObject({
    patientId: z.union([z.string(), z.number()]).optional(),
    firstName: str,
    lastName: str,
    fullName: str,
    sex: str,
    birthDate: str,
    identifiers: z.record(z.string(), z.unknown()).optional(),
  })
  .describe("Only for agents allowed to see patient names and identifiers");

const result = z.looseObject({
  analysisId: z.union([z.string(), z.number()]).optional(),
  testCode: str,
  loinc: str,
  testName: str,
  section: str,
  value: z.union([z.string(), z.number()]).nullable().optional(),
  unit: str,
  low: z.union([z.string(), z.number()]).nullable().optional(),
  high: z.union([z.string(), z.number()]).nullable().optional(),
  abnormal: z.union([z.boolean(), z.string()]).nullable().optional(),
  flag: z
    .enum(["L", "H"])
    .optional()
    .describe("Below (L) or above (H) the numeric reference range"),
  status: str,
});

const orderSummary = z.looseObject({
  accessionNumber: z.string(),
  receivedDate: str,
  testCount: num,
  pending: num,
  awaitingValidation: num,
  finalized: num,
  sections: z.array(z.string()).optional(),
  patient: patient.optional(),
});

/**
 * "L" or "H" when a numeric value is outside its numeric reference range, as on
 * the printed report; nothing otherwise (text results, no range). Computed
 * here, not by the model.
 */
export function withFlag(r: Record<string, unknown>): Record<string, unknown> {
  const num = (v: unknown) =>
    typeof v === "number"
      ? v
      : typeof v === "string" && /^-?\d+(\.\d+)?$/.test(v.trim())
        ? Number(v)
        : null;
  const value = num(r.value);
  const low = num(r.low);
  const high = num(r.high);
  if (value === null) return r;
  if (low !== null && value < low) return { ...r, flag: "L" };
  if (high !== null && value > high) return { ...r, flag: "H" };
  return r;
}

const enc = encodeURIComponent;
const n = (v: unknown) => (Array.isArray(v) ? v.length : 0);

export const TOOLS: ToolDef[] = [
  {
    name: "get_order",
    title: "Get an order and its results",
    description:
      "One laboratory order by lab number (accession number): received date, test count and every result " +
      "(test code, LOINC, name, value, unit, reference range, status, and L/H when a number is outside its range).",
    scopes: ["orders.search", "orders.read", "results.read"],
    identifiers: false,
    input: z.object({
      labNumber: labNumber.describe("Lab number (accession number) of the order"),
    }),
    output: z.looseObject({
      accessionNumber: z.string(),
      receivedDate: str,
      testCount: num,
      patient: patient.optional(),
      results: z.array(result),
    }),
    async run(core, a) {
      const lab = String(a.labNumber);
      const order = await core<Record<string, unknown>>(
        `orders/${enc(lab)}`,
        "/orders/{accession}",
      );
      const res = await core<{ results?: unknown[] }>(
        `orders/${enc(lab)}/results`,
        "/orders/{accession}/results",
      );
      const results = (Array.isArray(res.results) ? res.results : []).map((r) =>
        withFlag(r as Record<string, unknown>),
      );
      const abnormal = results.filter((r) => r.flag === "L" || r.flag === "H").length;
      return {
        data: { ...order, accessionNumber: String(order.accessionNumber ?? lab), results },
        rows: results.length,
        summary: `Order ${lab}: ${results.length} result(s), ${abnormal} flagged abnormal.`,
      };
    },
  },
  {
    name: "list_orders",
    title: "List orders by received date",
    description:
      `Orders received between two dates (at most ${MAX_RANGE_DAYS} days apart), optionally only those with a ` +
      "test in a given state or section. At most 100 per page; use offset for the next page.",
    scopes: ["orders.search", "orders.read"],
    identifiers: false,
    input: z
      .object({
        from: day,
        to: day,
        status: z.enum(["any", "pending", "awaiting-validation", "finalized"]).default("any"),
        section,
        limit: z.number().int().min(1).max(100).default(50),
        offset: z.number().int().min(0).max(100000).default(0),
      })
      .refine(rangeOk, rangeMessage),
    output: z.looseObject({
      from: str,
      to: str,
      total: num,
      offset: num,
      orders: z.array(orderSummary),
    }),
    async run(core, a) {
      const r = await core<Record<string, unknown>>("orders", "/orders", {
        from: a.from as string,
        to: a.to as string,
        status: a.status as string,
        section: a.section as string | undefined,
        limit: a.limit as number,
        offset: a.offset as number,
      });
      const orders = Array.isArray(r.orders) ? r.orders : [];
      return {
        data: { ...r, orders },
        rows: orders.length,
        summary: `${orders.length} of ${String(r.total ?? orders.length)} order(s) received ${String(a.from)} to ${String(a.to)}.`,
      };
    },
  },
  {
    name: "get_worklist",
    title: "Pending tests (worklist)",
    description:
      "Tests waiting for result entry or for validation, oldest first, with the minutes they have been waiting. " +
      "At most 100.",
    scopes: ["orders.search"],
    identifiers: false,
    input: z.object({
      stage: z.enum(["result-entry", "validation"]),
      section,
      limit: z.number().int().min(1).max(100).default(50),
    }),
    output: z.looseObject({
      stage: str,
      total: num,
      items: z.array(
        z.looseObject({
          accessionNumber: z.string(),
          analysisId: z.union([z.string(), z.number()]).optional(),
          testCode: str,
          testName: str,
          section: str,
          receivedDate: str,
          waitingMinutes: num,
        }),
      ),
    }),
    async run(core, a) {
      const r = await core<Record<string, unknown>>("worklist", "/worklist", {
        stage: a.stage as string,
        section: a.section as string | undefined,
        limit: a.limit as number,
      });
      const items = Array.isArray(r.items) ? r.items : [];
      return {
        data: { ...r, items },
        rows: items.length,
        summary: `${items.length} of ${String(r.total ?? items.length)} test(s) waiting for ${String(a.stage)}.`,
      };
    },
  },
  {
    name: "find_patients",
    title: "Find patients",
    description:
      "Patients whose name (accents and word order ignored) or an exact identifier matches. At most 20: " +
      "patient id, names, identifiers, sex, date of birth.",
    scopes: ["patient.identity"],
    identifiers: true,
    input: z.object({
      query: z.string().trim().min(2).max(100).describe("Name, or an exact identifier"),
    }),
    output: z.looseObject({ patients: z.array(patient) }),
    async run(core, a) {
      const r = await core<{ patients?: unknown[] }>("patients", "/patients", {
        q: a.query as string,
      });
      const patients = (Array.isArray(r.patients) ? r.patients : []).slice(0, 20);
      return {
        data: { patients },
        rows: patients.length,
        summary: `${patients.length} patient(s) found.`,
      };
    },
  },
  {
    name: "get_patient_results",
    title: "A patient's recent results",
    description:
      "The patient's most recent orders (newest first, at most 20) with their results. Find the patient id with find_patients.",
    // results of an order need orders.search for a service key (Appendix A, "same order rule")
    scopes: ["patient.identity", "results.read", "orders.search"],
    identifiers: true,
    input: z.object({
      patientId: z.string().regex(LAB_NUMBER, "a patient id as returned by find_patients"),
      limit: z.number().int().min(1).max(20).default(10),
    }),
    output: z.looseObject({
      patient: patient.optional(),
      orders: z.array(
        z.looseObject({
          accessionNumber: z.string(),
          receivedDate: str,
          testCount: num,
          results: z.array(result),
        }),
      ),
    }),
    async run(core, a) {
      const id = enc(String(a.patientId));
      const p = await core<Record<string, unknown>>(`patients/${id}`, "/patients/{patientId}");
      const list = await core<{ orders?: Record<string, unknown>[] }>(
        `patients/${id}/orders`,
        "/patients/{patientId}/orders",
        {
          limit: a.limit as number,
        },
      );
      const orders = (Array.isArray(list.orders) ? list.orders : []).slice(0, 20);
      const withResults = await mapLimit(orders, 4, async (o) => {
        const acc = String(o.accessionNumber ?? "");
        const r = await core<{ results?: unknown[] }>(
          `orders/${enc(acc)}/results`,
          "/orders/{accession}/results",
        );
        return {
          ...o,
          accessionNumber: acc,
          results: (Array.isArray(r.results) ? r.results : []).map((x) =>
            withFlag(x as Record<string, unknown>),
          ),
        };
      });
      const rows = withResults.reduce((s, o) => s + o.results.length, 0);
      return {
        data: { patient: p, orders: withResults },
        rows,
        summary: `${withResults.length} order(s), ${rows} result(s).`,
      };
    },
  },
  {
    name: "lab_statistics",
    title: "Laboratory statistics",
    description:
      "Aggregates only, never rows about a patient. report=orders: counts of orders, tests and finalized tests " +
      "grouped by day, section or status. report=tat: turnaround time (sample received to result validated) " +
      `count, median and 90th percentile in minutes, grouped by section or test. At most ${MAX_RANGE_DAYS} days.`,
    scopes: ["stats.read"],
    identifiers: false,
    input: z
      .object({
        report: z.enum(["orders", "tat"]),
        from: day,
        to: day,
        groupBy: z
          .enum(["day", "section", "status", "test"])
          .describe("orders: day|section|status; tat: section|test"),
      })
      .refine(rangeOk, rangeMessage)
      .refine(
        (v) =>
          v.report === "orders"
            ? v.groupBy !== "test"
            : v.groupBy === "section" || v.groupBy === "test",
        {
          message:
            "groupBy must be day, section or status for report=orders, section or test for report=tat",
          path: ["groupBy"],
        },
      ),
    output: z.looseObject({
      from: str,
      to: str,
      groupBy: str,
      definition: str,
      rows: z.array(z.looseObject({ key: z.union([z.string(), z.number()]).nullable() })),
    }),
    async run(core, a) {
      const path = a.report === "tat" ? "stats/tat" : "stats/orders";
      const r = await core<Record<string, unknown>>(path, `/${path}`, {
        from: a.from as string,
        to: a.to as string,
        groupBy: a.groupBy as string,
      });
      const rows = Array.isArray(r.rows) ? r.rows : [];
      return {
        data: { ...r, rows },
        rows: rows.length,
        summary: `${a.report === "tat" ? "Turnaround" : "Order counts"} by ${String(a.groupBy)}, ${String(a.from)} to ${String(a.to)}: ${rows.length} row(s).`,
      };
    },
  },
  {
    name: "list_tests",
    title: "Test catalog",
    description: "The laboratory's tests: code, name, LOINC, section, sample type, unit, active.",
    scopes: ["catalog.read"],
    identifiers: false,
    input: z.object({
      section,
      active: z.enum(["true", "false", "any"]).default("true"),
    }),
    output: z.looseObject({
      tests: z.array(
        z.looseObject({
          code: str,
          name: str,
          loinc: str,
          section: str,
          sampleType: str,
          unit: str,
          active: z.boolean().nullable().optional(),
        }),
      ),
    }),
    async run(core, a) {
      const r = await core<{ tests?: unknown[] }>("catalog/tests", "/catalog/tests", {
        section: a.section as string | undefined,
        active: a.active as string,
      });
      const tests = Array.isArray(r.tests) ? r.tests : [];
      return { data: { tests }, rows: tests.length, summary: `${tests.length} test(s).` };
    },
  },
];

export const TOOL_NAMES = TOOLS.map((t) => t.name);

/** Is this tool offered to an agent, given the add-on's granted scopes? */
export function toolVisible(
  t: Pick<ToolDef, "name" | "scopes" | "identifiers">,
  granted: ReadonlySet<string>,
  agent: { identifiers: boolean; tools?: string[] },
): boolean {
  if (!t.scopes.every((s) => granted.has(s))) return false;
  if (t.identifiers && !agent.identifiers) return false;
  if (agent.tools && !agent.tools.includes(t.name)) return false;
  return true;
}

/** Removes every `patient` / `patients` member, at any depth (agents without identifiers). */
export function stripPatients<T>(v: T): T {
  if (Array.isArray(v)) return v.map((x) => stripPatients(x)) as T;
  if (v && typeof v === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, x] of Object.entries(v as Record<string, unknown>)) {
      if (k === "patient" || k === "patients") continue;
      out[k] = stripPatients(x);
    }
    return out as T;
  }
  return v;
}

async function mapLimit<T, R>(items: T[], limit: number, f: (t: T) => Promise<R>): Promise<R[]> {
  const out = new Array<R>(items.length);
  let next = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const i = next++;
      out[i] = await f(items[i] as T);
    }
  });
  await Promise.all(workers);
  return out;
}
