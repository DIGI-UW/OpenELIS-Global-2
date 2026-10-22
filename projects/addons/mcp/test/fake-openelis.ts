// A small stand-in for the OpenELIS add-on API (Appendix A of the design), for tests.
import http from "node:http";
import type { AddressInfo } from "node:net";

export const SERVICE_KEY = "oeak_test_service_key";
export const ADMIN_TOKEN = "oeat.admin.sig";
export const VALIDATION_TOKEN = "oeat.validation.sig";

const PATIENT = {
  patientId: "1001",
  firstName: "Lan",
  lastName: "Nguyễn Thị",
  fullName: "Nguyễn Thị Lan",
  sex: "F",
  birthDate: "1980-04-02",
  identifiers: { nationalId: "079180000001" },
};

export interface FakeRequest {
  path: string;
  query: Record<string, string>;
  headers: http.IncomingHttpHeaders;
}

export interface Fake {
  url: string;
  scopes: string[];
  config: Record<string, unknown>;
  requests: FakeRequest[];
  close(): Promise<void>;
}

export async function startFakeOpenElis(
  scopes: string[],
  config: Record<string, unknown> = {},
): Promise<Fake> {
  const fake: Fake = { url: "", scopes, config, requests: [], close: async () => undefined };
  const has = (s: string) => fake.scopes.includes(s);
  const server = http.createServer((req, res) => {
    const u = new URL(req.url ?? "/", "http://x");
    const path = u.pathname.replace(/^\/rest\/addon-api\/v1/, "");
    const query = Object.fromEntries(u.searchParams);
    fake.requests.push({ path, query, headers: req.headers });
    const send = (status: number, body: unknown) => {
      res.writeHead(status, { "Content-Type": "application/json" });
      res.end(JSON.stringify(body));
    };
    const auth = req.headers.authorization;
    if (auth === `Bearer ${ADMIN_TOKEN}` && path === "/whoami") {
      return send(200, {
        addon: "mcp",
        kind: "user",
        scopes: fake.scopes,
        userId: "42",
        slot: "addon-settings-page-slot",
      });
    }
    if (auth === `Bearer ${VALIDATION_TOKEN}` && path === "/whoami") {
      return send(200, {
        addon: "mcp",
        kind: "user",
        scopes: fake.scopes,
        userId: "43",
        slot: "result-validation-side-panel-slot",
      });
    }
    if (auth !== `Bearer ${SERVICE_KEY}`) return send(401, { code: "auth" });
    const need = (s: string) => {
      if (has(s)) return false;
      send(403, { code: "scope", scope: s });
      return true;
    };
    let m: RegExpExecArray | null;
    if (path === "/whoami")
      return send(200, { addon: "mcp", kind: "service", scopes: fake.scopes });
    if (path === "/config") return send(200, fake.config);
    if (path === "/orders") {
      if (need("orders.search") || need("orders.read")) return;
      const order = {
        accessionNumber: "0221026-0004",
        receivedDate: "2026-10-22T01:00:00Z",
        testCount: 3,
        pending: 1,
        awaitingValidation: 1,
        finalized: 1,
        sections: ["Hematology"],
      };
      return send(200, {
        from: query.from,
        to: query.to,
        total: 1,
        offset: Number(query.offset ?? 0),
        orders: [has("patient.identity") ? { ...order, patient: PATIENT } : order],
      });
    }
    if (path === "/worklist") {
      if (need("orders.search")) return;
      return send(200, {
        stage: query.stage,
        total: 1,
        items: [
          {
            accessionNumber: "0221026-0004",
            analysisId: "77",
            testCode: "GLU",
            testName: "Glucose",
            section: "Biochemistry",
            receivedDate: "2026-10-22T01:00:00Z",
            waitingMinutes: 42,
          },
        ],
      });
    }
    if ((m = /^\/orders\/([^/]+)$/.exec(path))) {
      if (need("orders.read")) return;
      if (m[1] !== "0221026-0004") return send(404, { code: "order" });
      const o = { accessionNumber: m[1], receivedDate: "2026-10-22T01:00:00Z", testCount: 2 };
      return send(200, has("patient.identity") ? { ...o, patient: PATIENT } : o);
    }
    if ((m = /^\/orders\/([^/]+)\/results$/.exec(path))) {
      if (need("results.read")) return;
      if (m[1] !== "0221026-0004") return send(404, { code: "order" });
      return send(200, {
        accessionNumber: m[1],
        results: [
          {
            analysisId: "77",
            testCode: "GLU",
            loinc: "2345-7",
            testName: "Glucose",
            section: "Biochemistry",
            value: "7.9",
            unit: "mmol/L",
            low: "3.9",
            high: "6.4",
            abnormal: true,
            status: "Finalized",
          },
          {
            analysisId: "78",
            testCode: "CRE",
            loinc: "2160-0",
            testName: "Creatinine",
            section: "Biochemistry",
            value: "80",
            unit: "umol/L",
            low: "53",
            high: "97",
            status: "Finalized",
          },
        ],
      });
    }
    if (path === "/patients") {
      if (need("patient.identity")) return;
      return send(200, { patients: [PATIENT] });
    }
    if ((m = /^\/patients\/([^/]+)(\/orders)?$/.exec(path))) {
      if (need("patient.identity")) return;
      if (m[1] !== "1001") return send(404, { code: "patient" });
      if (m[2])
        return send(200, {
          patientId: "1001",
          orders: [
            { accessionNumber: "0221026-0004", receivedDate: "2026-10-22T01:00:00Z", testCount: 2 },
          ],
        });
      return send(200, PATIENT);
    }
    if (path === "/stats/orders" || path === "/stats/tat") {
      if (need("stats.read")) return;
      if (path === "/stats/tat") {
        return send(200, {
          from: query.from,
          to: query.to,
          groupBy: query.groupBy,
          definition: "sample received → result released (validated), minutes",
          rows: [{ key: "Biochemistry", count: 12, medianMinutes: 55, p90Minutes: 120 }],
        });
      }
      return send(200, {
        from: query.from,
        to: query.to,
        groupBy: query.groupBy,
        rows: [{ key: "2026-10-21", orders: 30, tests: 90, finalized: 88 }],
      });
    }
    if (path === "/catalog/tests") {
      if (need("catalog.read")) return;
      return send(200, {
        tests: [
          {
            code: "GLU",
            name: "Glucose",
            loinc: "2345-7",
            section: "Biochemistry",
            sampleType: "Serum",
            unit: "mmol/L",
            active: true,
          },
        ],
      });
    }
    send(404, { code: "route" });
  });
  await new Promise<void>((r) => server.listen(0, "127.0.0.1", r));
  fake.url = `http://127.0.0.1:${(server.address() as AddressInfo).port}/rest/addon-api/v1`;
  fake.close = () =>
    new Promise((r) => {
      server.closeAllConnections();
      server.close(() => r());
    });
  return fake;
}
