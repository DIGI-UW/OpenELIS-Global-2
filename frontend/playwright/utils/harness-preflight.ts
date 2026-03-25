/**
 * Harness preflight checks for analyzer E2E tests.
 *
 * Use before running fixture-backed bidirectional specs to fail fast when
 * the analyzer harness (mock server, bridge) is not available.
 */

const MOCK_SIMULATE_URL =
  process.env.MOCK_SIMULATE_URL || "http://localhost:8085";

/**
 * Check that the mock simulator HTTP API is reachable.
 * Returns true if GET /health returns 200.
 */
export async function isMockSimulateAvailable(): Promise<boolean> {
  try {
    const res = await fetch(`${MOCK_SIMULATE_URL}/health`, {
      method: "GET",
      signal: AbortSignal.timeout(5_000),
    });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Trigger a single GeneXpert ASTM results-push via the mock simulate API.
 * Use when running fixture-backed results-push-receive E2E.
 * destination: OpenELIS backend URL to receive the push (e.g. https://localhost).
 */
export async function triggerMockResultsPush(
  count = 1,
  destination?: string,
): Promise<{ ok: boolean; status: number; body: unknown }> {
  const url = `${MOCK_SIMULATE_URL}/simulate/astm/genexpert_astm`;
  const dest = destination || process.env.BASE_URL || "https://localhost";
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ destination: dest, count }),
      signal: AbortSignal.timeout(15_000),
    });
    const body = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, body };
  } catch (e) {
    return { ok: false, status: 0, body: { error: String(e) } };
  }
}
