/**
 * Access to the Analyzer Bridge's delivery outbox, and the container controls a
 * delivery-durability test needs.
 *
 * The bridge holds every received result until OpenELIS accepts it. Proving that
 * means interrupting the path between them, which no other spec in this repo
 * does, so the container plumbing lives here rather than inline in a spec.
 *
 * Container and port defaults follow `.github/ci/ci.analyzer-harness.yml`. Every
 * one is overridable, because `docker-compose.worktree.yml` resets
 * `container_name` and randomizes host ports for worktree-scoped dev stacks.
 */

import { execFileSync } from "child_process";
import {
  APIRequestContext,
  request as playwrightRequest,
} from "@playwright/test";

const DEFAULT_BRIDGE_CONTAINER = "openelis-analyzer-bridge";
const DEFAULT_WEBAPP_CONTAINER = "openelisglobal-webapp";

/** Bridge admin API, container 8443 published to host 8442. */
const DEFAULT_BRIDGE_ADMIN_URL = "https://localhost:8442";

/** Analyzer mock HTTP API, container 8080 published to host 8085. */
const DEFAULT_MOCK_URL = "http://localhost:8085";

/**
 * Where the mock opens its ASTM session. The bridge's per-connection listener
 * runs on 9600 inside the container and is deliberately not published, so
 * traffic has to be driven through the mock rather than by opening a socket
 * from the test process.
 */
const DEFAULT_ASTM_DESTINATION = "tcp://openelis-analyzer-bridge:9600";

function assertValidContainerName(name: string): void {
  if (!/^[a-zA-Z0-9_.-]+$/.test(name)) {
    throw new Error(`Invalid container name: ${name}`);
  }
}

function firstNonEmptyEnv(keys: string[], fallback: string): string {
  for (const key of keys) {
    const value = process.env[key]?.trim();
    if (value) {
      return value;
    }
  }
  return fallback;
}

export function resolveBridgeContainer(): string {
  const name = firstNonEmptyEnv(
    ["HARNESS_BRIDGE_CONTAINER", "BRIDGE_CONTAINER"],
    DEFAULT_BRIDGE_CONTAINER,
  );
  assertValidContainerName(name);
  return name;
}

export function resolveWebappContainer(): string {
  const name = firstNonEmptyEnv(
    ["HARNESS_WEBAPP_CONTAINER", "WEBAPP_CONTAINER", "OE_CONTAINER"],
    DEFAULT_WEBAPP_CONTAINER,
  );
  assertValidContainerName(name);
  return name;
}

export function bridgeAdminUrl(): string {
  return firstNonEmptyEnv(["BRIDGE_ADMIN_URL"], DEFAULT_BRIDGE_ADMIN_URL);
}

export function mockUrl(): string {
  return firstNonEmptyEnv(["MOCK_URL", "SIMULATOR_URL"], DEFAULT_MOCK_URL);
}

export function astmDestination(): string {
  return firstNonEmptyEnv(["ASTM_DESTINATION"], DEFAULT_ASTM_DESTINATION);
}

export function docker(...args: string[]): string {
  return execFileSync("docker", args, {
    encoding: "utf-8",
    timeout: 180_000,
  }).trim();
}

/** Whether a container is running right now. */
export function isContainerRunning(name: string): boolean {
  try {
    return docker("inspect", "-f", "{{.State.Running}}", name) === "true";
  } catch {
    return false;
  }
}

/**
 * A client for the bridge admin API. Self-signed certificate, HTTP Basic with
 * the same credentials the bridge forwards to OpenELIS with.
 */
export async function bridgeApi(): Promise<APIRequestContext> {
  return playwrightRequest.newContext({
    baseURL: bridgeAdminUrl(),
    ignoreHTTPSErrors: true,
    httpCredentials: {
      username: process.env.ANALYZER_BRIDGE_USERNAME || "admin",
      password: process.env.ANALYZER_BRIDGE_PASSWORD || "adminADMIN!",
    },
  });
}

export interface OutboxEntry {
  id: string;
  state: "RECEIVED" | "PENDING" | "RETRYING" | "DELIVERED" | "DMQ";
  accession: string | null;
  attempts: number;
  failureReason: string | null;
  lastError: string | null;
  lastHttpStatus: number | null;
  oeReceipt: string | null;
  rawBytes: number;
  fhirBytes: number;
}

/** True when this bridge build exposes the delivery outbox at all. */
export async function hasOutbox(api: APIRequestContext): Promise<boolean> {
  const response = await api.get("/admin/outbox/stats");
  return response.status() !== 404;
}

export async function listOutbox(
  api: APIRequestContext,
  query = "?limit=500&includeDismissed=true",
): Promise<OutboxEntry[]> {
  const response = await api.get(`/admin/outbox${query}`);
  if (!response.ok()) {
    throw new Error(`Outbox listing failed with ${response.status()}`);
  }
  return (await response.json()).rows as OutboxEntry[];
}

export async function getOutboxEntry(
  api: APIRequestContext,
  id: string,
): Promise<OutboxEntry | null> {
  const response = await api.get(`/admin/outbox/${encodeURIComponent(id)}`);
  return response.ok() ? ((await response.json()) as OutboxEntry) : null;
}

export async function outboxPayload(
  api: APIRequestContext,
  id: string,
  part: "raw" | "fhir",
): Promise<string> {
  const response = await api.get(
    `/admin/outbox/${encodeURIComponent(id)}/payload?part=${part}`,
  );
  if (!response.ok()) {
    throw new Error(`Payload ${part} for ${id} failed with ${response.status()}`);
  }
  return response.text();
}

async function sleep(ms: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

/** Wait for an accession to appear in the outbox and return its entry. */
export async function waitForOutboxEntry(
  api: APIRequestContext,
  accession: string,
  timeoutMs: number,
): Promise<OutboxEntry> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const match = (await listOutbox(api)).find(
      (row) => row.accession === accession,
    );
    if (match) {
      return match;
    }
    await sleep(1_000);
  }
  throw new Error(`The bridge never recorded a result for accession ${accession}`);
}

/** Wait for an entry to reach a state, reporting what it was doing if it does not. */
export async function waitForOutboxState(
  api: APIRequestContext,
  id: string,
  state: OutboxEntry["state"],
  timeoutMs: number,
): Promise<OutboxEntry> {
  const deadline = Date.now() + timeoutMs;
  let last: OutboxEntry | null = null;
  while (Date.now() < deadline) {
    last = await getOutboxEntry(api, id);
    if (last?.state === state) {
      return last;
    }
    await sleep(2_000);
  }
  throw new Error(
    `Outbox entry ${id} never reached ${state}. Last seen ${last?.state}` +
      ` after ${last?.attempts} attempts: ${last?.failureReason ?? last?.lastError}`,
  );
}

/** Wait until the bridge admin API answers again, after a restart. */
export async function waitForBridge(
  api: APIRequestContext,
  timeoutMs: number,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      if ((await api.get("/admin/outbox/stats")).ok()) {
        return;
      }
    } catch {
      // The bridge is still coming back; keep waiting.
    }
    await sleep(2_000);
  }
  throw new Error("The analyzer bridge did not come back after a restart");
}

/**
 * Push one ASTM result through the analyzer mock and return its accession.
 *
 * Mirrors `projects/analyzer-harness/seed-mvp-traffic.sh` push_astm.
 */
export async function pushAstmResult(options: {
  accession: string;
  testCode?: string;
  value?: string;
  template?: string;
}): Promise<void> {
  const context = await playwrightRequest.newContext({
    ignoreHTTPSErrors: true,
  });
  try {
    const response = await context.post(
      `${mockUrl()}/simulate/astm/${options.template ?? "genexpert"}`,
      {
        data: {
          destination: astmDestination(),
          sample_id: options.accession,
          results: [
            {
              test_code: options.testCode ?? "MTB",
              value: options.value ?? "NOT DETECTED",
            },
          ],
        },
      },
    );
    if (!response.ok()) {
      throw new Error(
        `Analyzer mock refused the push with ${response.status()}: ${await response.text()}`,
      );
    }
    const body = await response.json();
    if (body.pushed !== 1) {
      throw new Error(
        `Analyzer mock did not deliver the message: ${JSON.stringify(body)}`,
      );
    }
  } finally {
    await context.dispose();
  }
}

/**
 * A lane-10 (GeneXpert ASTM) accession outside every range the fixtures own.
 *
 * Unique per call on purpose: the bridge derives a delivery's identity from the
 * content it received, so sending byte-identical messages twice is recognized as
 * the retransmission it is rather than treated as a new result.
 */
export function uniqueLane10Accession(): string {
  const sequence = String(900_000_000 + Math.floor(Math.random() * 99_999_999))
    .padStart(11, "0")
    .slice(-11);
  return `DEV012610${sequence}`;
}

/** How many acceptances OpenELIS recorded for one delivery identity. */
export function countOpenElisAcceptances(
  dbContainer: string,
  deliveryId: string,
): number {
  if (!/^[A-Za-z0-9:.-]+$/.test(deliveryId)) {
    throw new Error(`Refusing to query with an unexpected delivery id: ${deliveryId}`);
  }
  const output = docker(
    "exec",
    dbContainer,
    "psql",
    "-U",
    "clinlims",
    "-d",
    "clinlims",
    "-tAc",
    `SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE message_id = '${deliveryId}'`,
  );
  return Number.parseInt(output, 10);
}
