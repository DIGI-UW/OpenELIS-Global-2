import { execFileSync } from "node:child_process";
import { test, expect, Page } from "../../../helpers/test-base";
import { SHORT_TIMEOUT } from "../../../helpers/timeouts";
import { resolveDbContainer } from "../../../helpers/db-container";

/**
 * Imported box contents (T-42, OGC-613 cross-site gap fixes).
 *
 * A box imported from a partner site has no local box_sample_item rows — the
 * FKs name rows only the sender has. What the consignment says it holds
 * arrives as content-item extensions and is kept read-side on
 * shipping_box.imported_contents; BoxDetails must render that manifest as
 * labelled rows instead of an empty table.
 *
 * The box is created through the real REST write path; the manifest column is
 * set directly in the DB because only the FHIR import poll writes it, and a
 * remote FHIR store is not part of this stack's contract.
 */

const REST = "/api/OpenELIS-Global/rest";
const RUN = Date.now().toString(36);
const BOX_CODE = `E2E-T42-${RUN}`;

function psql(sql: string): string {
  return execFileSync(
    "docker",
    [
      "exec",
      "-i",
      resolveDbContainer(),
      "psql",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-tAc",
      sql,
    ],
    { encoding: "utf8" },
  ).trim();
}

async function csrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  throw new Error("No CSRF token in storage state — auth.setup did not run?");
}

test.describe("Imported box renders its manifest (T-42)", () => {
  let boxDbId: number | undefined;

  test.afterAll(() => {
    if (boxDbId !== undefined) {
      psql(
        `DELETE FROM clinlims.shipping_box WHERE id = ${Math.trunc(boxDbId)}`,
      );
    }
  });

  test("a box with no local contents rows lists the consignment's labelled items", async ({
    page,
  }) => {
    const csrf = await csrfToken(page);

    // Any destination organization will do — take the first the picker offers.
    const orgs = (await (
      await page.request.get(`${REST}/displayList/REFERRAL_ORGANIZATIONS`)
    ).json()) as Array<{ id: string }>;
    expect(
      orgs.length,
      "the stack needs at least one referral organization",
    ).toBeGreaterThan(0);

    const created = await page.request.post(`${REST}/shipping-box`, {
      headers: { "X-CSRF-Token": csrf, "Content-Type": "application/json" },
      data: {
        boxId: BOX_CODE,
        destinationFacilityId: Number(orgs[0].id),
        capacity: 10,
        state: "DRAFT",
      },
    });
    expect(created.ok()).toBeTruthy();
    boxDbId = ((await created.json()) as { id: number }).id;

    // What the import poll would have stored off the SupplyDelivery.
    psql(
      `UPDATE clinlims.shipping_box SET imported_contents =` +
        ` '[{"label":"E2E-PS-1","type":"E2E panel"},{"label":"E2E-PS-2","type":"E2E panel"}]'` +
        ` WHERE id = ${Math.trunc(boxDbId)}`,
    );

    await page.goto(`/SampleShipment/box/${boxDbId}`);
    await expect(
      page.getByRole("heading", { name: `Box ${BOX_CODE}` }),
    ).toBeVisible({
      timeout: SHORT_TIMEOUT,
    });

    // The manifest renders as rows — label and type — not an empty table.
    await expect(page.getByRole("cell", { name: "E2E-PS-1" })).toBeVisible();
    await expect(page.getByRole("cell", { name: "E2E-PS-2" })).toBeVisible();
    await expect(
      page.getByRole("cell", { name: "E2E panel" }).first(),
    ).toBeVisible();
    await expect(page.getByText("2 Samples")).toBeVisible();
  });
});
