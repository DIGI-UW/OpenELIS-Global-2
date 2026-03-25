/**
 * Shared constants for analyzer E2E tests.
 *
 * Stable harness-backed tests use the minimal GeneXpert fixture from
 * src/test/resources/analyzer-minimal.sql (analyzer 2013 at 172.20.1.100:9600),
 * which matches projects/analyzer-harness/docker-compose.analyzer-test.yml.
 * Newly created analyzers are used only in the promotion-gate spec after
 * the fixture-backed lane is green.
 */
export const GENEXPERT_FIXTURE_ID = "2013";

/** Host/port for harness ASTM mock (same as fixture 2013). Used by promotion-gate. */
export const HARNESS_MOCK_HOST = "172.20.1.100";
export const HARNESS_MOCK_PORT = "9600";

/**
 * ASTM fixture IDs for multi-port mock (load with --analyzers=astm-full).
 * Port map: fixture ID -> mock port (one template per port).
 */
export const ASTM_FIXTURE_IDS = [
  "2013",
  "2014",
  "2015",
  "2016",
  "2017",
] as const;
export const ASTM_PORT_MAP: Record<string, string> = {
  "2013": "9600",
  "2014": "9601",
  "2015": "9602",
  "2016": "9603",
  "2017": "9604",
};
