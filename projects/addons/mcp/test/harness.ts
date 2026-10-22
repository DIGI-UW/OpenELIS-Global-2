import { mkdtempSync, rmSync } from "node:fs";
import type http from "node:http";
import type { AddressInfo } from "node:net";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { App } from "../src/app.js";
import { type Fake, SERVICE_KEY, startFakeOpenElis } from "./fake-openelis.js";

export const ALL_SCOPES = [
  "orders.read",
  "results.read",
  "orders.search",
  "stats.read",
  "catalog.read",
  "patient.identity",
];
export const PUBLIC_ORIGIN = "https://lims.example.org";

export interface Harness {
  app: App;
  fake: Fake;
  base: string;
  dataDir: string;
  close(): Promise<void>;
}

export async function startHarness(
  scopes: string[] = ALL_SCOPES,
  config: Record<string, unknown> = {},
  now?: () => Date,
): Promise<Harness> {
  const fake = await startFakeOpenElis(scopes, config);
  const dataDir = mkdtempSync(join(tmpdir(), "mcp-addon-"));
  const app = new App({
    env: {
      port: 0,
      dataDir,
      publicOrigin: PUBLIC_ORIGIN,
      mcpPath: "/addons/mcp/mcp",
      metadataPath: "/.well-known/oauth-protected-resource/addons/mcp/mcp",
      openelisApiUrl: fake.url,
      openelisApiKey: SERVICE_KEY,
      openelisTlsInsecure: false,
    },
    now,
  });
  const server: http.Server = await app.listen(0, "127.0.0.1");
  const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  return {
    app,
    fake,
    base,
    dataDir,
    async close() {
      server.closeAllConnections();
      await new Promise<void>((r) => server.close(() => r()));
      await app.close();
      await fake.close();
      rmSync(dataDir, { recursive: true, force: true });
    },
  };
}
