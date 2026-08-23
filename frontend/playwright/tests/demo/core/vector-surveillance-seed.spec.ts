import { test, expect } from "../../../helpers/test-base";
import { seedVectorSurveillanceDataset } from "../../../helpers/seed-vector-data";

/**
 * Populate a running instance with a representative vector-surveillance dataset
 * entirely through the REST API (NO SQL): sampling sites, backdated collections
 * with trap-effort, species identifications, and pathogen results across several
 * ISO weeks, so the dashboard's density / species / MIR / positivity panels all
 * render with real data (e.g. before recording the full-story demo).
 *
 * This is a demo-server maintenance operation, NOT a CI check. The seed test is
 * only defined when VECTOR_SEED is set, so it contributes nothing to a normal
 * run. Point it at any instance via BASE_URL:
 *
 *   BASE_URL=https://vector-demo.openelis-global.org TEST_USER=admin \
 *   TEST_PASS='...' VECTOR_SEED=1 VECTOR_SEED_WEEKS=5 \
 *   npm run pw:test:core-demo -- vector-surveillance-seed
 */
if (process.env.VECTOR_SEED) {
  test("seed representative surveillance dataset", async ({ page }) => {
    test.setTimeout(45 * 60_000); // full dataset, one order at a time over HTTP
    const weeks = parseInt(process.env.VECTOR_SEED_WEEKS || "5", 10);
    const result = await seedVectorSurveillanceDataset(page, { weeks });
    console.log(
      `VECTOR_SEED created ${result.ordersCreated} orders, ` +
        `${result.densityRows} density rows, skipped: ${result.skippedLanes.join(", ") || "none"}`,
    );
    // The seed verifies the dashboard internally; assert it populated data.
    expect(result.ordersCreated).toBeGreaterThan(0);
    expect(result.densityRows).toBeGreaterThan(0);
  });
}
