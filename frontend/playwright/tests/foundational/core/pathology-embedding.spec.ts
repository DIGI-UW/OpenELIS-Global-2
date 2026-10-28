import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureTissueType,
  SeededPathologyCase,
  TissueType,
} from "../../../helpers/seed-pathology-data";
import {
  cassetteRows,
  expandSection,
  openCase,
  railStep,
  rowNamed,
  saveDraft,
  sectionHeader,
  setStage,
  summaryRow,
} from "../../../helpers/pathology-case-view";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The embedding section of the pathology case view turns each cassette cut
 * at grossing into a block, one at a time, and names the cassettes still
 * waiting (FR-6.1 to FR-6.3). Nothing about the count is stored: the badge,
 * the banner, the rail step and the summary row are all read off the rows.
 *
 * What only a deployed stack proves here:
 *   - the embed reaches the server and the row comes back a block, on this
 *     list and on the grossing list, as the same row with the same barcode,
 *   - the outstanding cassettes are named in the banner, on the rail and in
 *     the summary, and they shrink as each one is embedded,
 *   - an unsaved edit holds Embed back until it is saved or discarded, so the
 *     read-back after an embed can never discard work,
 *   - a retired cassette is never offered for embedding.
 *
 * Serial, and on one seeded case, because the walk is the point: the
 * cassettes cut at grossing are what wait here, and each embed changes what
 * the next test reads.
 *
 * The Embedding stage must be tracked on the stack (its stage switch on, the
 * shipped default); with it off the section says so instead.
 */

let seeded: SeededPathologyCase;
let tissue: TissueType;

const GROSSING = "pathology-section-grossing";
const EMBEDDING = "pathology-section-embedding";

/** Every row on the embedding list, retired ones included when shown. */
function embeddingRows(page: Page) {
  return page.locator(`#${EMBEDDING} .pathology-case-view__row`);
}

/** The badge in the embedding section's header. */
function embeddingBadge(page: Page) {
  return sectionHeader(page, EMBEDDING).locator(".cds--tag");
}

/** The warning that counts the cassettes and names the ones outstanding. */
function outstandingBanner(page: Page) {
  return page.locator(`#${EMBEDDING} .cds--inline-notification--warning`);
}

function embedButton(page: Page, designation: string) {
  return page.getByRole("button", {
    name: `Embed cassette ${designation}`,
    exact: true,
  });
}

function storageLocation(page: Page, designation: string) {
  return page
    .locator(`#${EMBEDDING}`)
    .getByLabel(`Storage Location ${designation}`, { exact: true });
}

/** A row's state tag, matched whole. */
function stateTag(page: Page, sectionId: string, designation: string) {
  const rows = page.locator(`#${sectionId} .pathology-case-view__row`);
  return rowNamed(rows, designation).locator(".cds--tag", {
    hasText: /^(Cassette|Block)$/,
  });
}

/** Wait for the case to be read back, the way the screen re-reads it. */
function caseReadBack(page: Page) {
  return page.waitForResponse(
    (r) =>
      r.request().method() === "GET" &&
      new URL(r.url()).pathname.endsWith(
        `/rest/pathology/caseView/${seeded.pathologySampleId}`,
      ),
    { timeout: UI_TIMEOUT },
  );
}

/**
 * Embed one cassette and wait for the screen to finish with it: the post,
 * the toast that names it, and the case read back, after which the row no
 * longer offers Embed.
 */
async function embed(page: Page, designation: string) {
  const button = embedButton(page, designation);
  await expect(button).toBeEnabled();
  const posted = page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      /\/rest\/pathology\/block\/[^/]+\/embed$/.test(new URL(r.url()).pathname),
  );
  const readBack = caseReadBack(page);
  await button.click();
  await posted;
  await readBack;

  await expect(
    page
      .locator(".cds--toast-notification")
      .filter({ hasText: `Cassette ${designation} embedded` }),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await expect(
    page.locator(".cds--toast-notification--error"),
    "the embed raised no error toast",
  ).toHaveCount(0);
  await expect(button).toHaveCount(0);
}

/** The reconciliation every view of the section agrees on. */
async function expectOutstanding(
  page: Page,
  embedded: number,
  total: number,
  outstanding: string,
  railLabel: string = `${outstanding} outstanding`,
) {
  const count = `${embedded} of ${total} embedded`;
  await expect(embeddingBadge(page)).toHaveText(count);
  await expect(
    outstandingBanner(page).locator(".cds--inline-notification__title"),
  ).toHaveText(count);
  await expect(
    outstandingBanner(page).locator(".cds--inline-notification__subtitle"),
  ).toHaveText(`${outstanding} outstanding`);
  // The rail names at most two cassettes in its visible label and carries
  // the whole sentence as the step's hover title.
  const step = railStep(page, "Embedding");
  await expect(step).toContainText(railLabel);
  await expect(
    step.locator(`[title="${outstanding} outstanding"]`),
  ).not.toHaveCount(0);
  // The row names the outstanding cassettes on hover and, for a screen
  // reader, in hidden text after the count.
  await expect(summaryRow(page, "Embedding")).toHaveText(
    new RegExp(`^Embedding\\s*${count}\\s*${outstanding} outstanding$`),
  );
}

test.describe.serial("Pathology embedding section", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120_000);
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await context.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    tissue = await ensureTissueType(page);
    await context.close();
  });

  test("the cassettes cut at grossing wait for embedding", async ({ page }) => {
    await openCase(page, seeded);

    await test.step("three cassettes are cut at grossing, the first with a tissue", async () => {
      await setStage(page, "Grossing");
      await expandSection(page, GROSSING);
      await page
        .getByLabel("Macroscopic description", { exact: true })
        .fill("Three cores of breast tissue, each 1.5 cm, submitted whole.");
      const addCassette = page.getByRole("button", { name: "Add cassette" });
      await addCassette.click();
      await addCassette.click();
      await addCassette.click();
      await expect(cassetteRows(page)).toHaveCount(3);
      await chooseCarbonOption(
        page.getByRole("combobox", {
          name: "Tissue type New cassette 1",
          exact: true,
        }),
        tissue.name,
      );
      await saveDraft(page, seeded.pathologySampleId);
      await expect(rowNamed(cassetteRows(page), "A3")).toHaveCount(1);
    });

    await test.step("at the stage, the section opens", async () => {
      await setStage(page, "Embedding");
      await saveDraft(page, seeded.pathologySampleId);
      await expect(sectionHeader(page, EMBEDDING)).toBeEnabled();
      await expandSection(page, EMBEDDING);
    });

    await test.step("every cassette is named as outstanding", async () => {
      await expectOutstanding(
        page,
        0,
        3,
        "A1, A2, A3",
        "A1, A2 and 1 more outstanding",
      );
    });

    await test.step("each row shows its tissue and is still a cassette", async () => {
      await expect(embeddingRows(page)).toHaveCount(3);
      await expect(rowNamed(embeddingRows(page), "A1")).toContainText(
        tissue.name,
      );
      for (const designation of ["A2", "A3"]) {
        await expect(rowNamed(embeddingRows(page), designation)).toContainText(
          "not recorded",
        );
      }
      for (const designation of ["A1", "A2", "A3"]) {
        await expect(stateTag(page, EMBEDDING, designation)).toHaveText(
          "Cassette",
        );
        await expect(embedButton(page, designation)).toBeEnabled();
      }
    });

    await test.step("storage is typed for now, and says so", async () => {
      await expect(storageLocation(page, "A1")).toBeVisible();
      await expect(
        rowNamed(embeddingRows(page), "A1").getByText(
          /storage locations arrives/,
        ),
      ).toBeVisible();
    });
  });

  test("embedding one cassette names the two still waiting", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, EMBEDDING);

    await embed(page, "A1");

    await expectOutstanding(page, 1, 3, "A2, A3");
    await expect(stateTag(page, EMBEDDING, "A1")).toHaveText("Block");

    await test.step("the grossing list shows the same row, now a block", async () => {
      await expandSection(page, GROSSING);
      await expect(stateTag(page, GROSSING, "A1")).toHaveText("Block");
      // The same row on both lists: one barcode, read whole on each.
      const barcode = new RegExp(
        `${seeded.accessionNumber.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\.A1$`,
      );
      await expect(
        rowNamed(cassetteRows(page), "A1").locator(
          ".pathology-case-view__row-barcode",
        ),
      ).toHaveText(barcode);
      await expect(
        rowNamed(embeddingRows(page), "A1").locator(
          ".pathology-case-view__row-barcode",
        ),
      ).toHaveText(barcode);
    });
  });

  test("a typed storage location holds Embed until it is saved or discarded", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, EMBEDDING);
    const button = embedButton(page, "A2");
    await expect(button).toBeEnabled();

    await test.step("an unsaved edit holds Embed and says why", async () => {
      await storageLocation(page, "A2").fill("Tray 4");
      await expect(button).toBeDisabled();
      await expect(button).toHaveAttribute(
        "title",
        "Save or discard your changes before embedding",
      );
    });

    await test.step("discarding the edit releases it", async () => {
      const readBack = caseReadBack(page);
      await page.getByRole("button", { name: "Discard changes" }).click();
      await readBack;
      await expect(storageLocation(page, "A2")).toHaveValue("");
      await expect(button).toBeEnabled();
    });

    await test.step("saving the edit releases it, and the location is kept", async () => {
      await storageLocation(page, "A2").fill("Tray 4");
      await expect(button).toBeDisabled();
      await saveDraft(page, seeded.pathologySampleId);
      await expect(button).toBeEnabled();

      await openCase(page, seeded);
      await expandSection(page, EMBEDDING);
      await expect(storageLocation(page, "A2")).toHaveValue("Tray 4");
      await expect(embedButton(page, "A2")).toBeEnabled();
      // The grossing row and this one are the same cassette, so the
      // location typed here is the one grossing shows.
      await expandSection(page, GROSSING);
      await expect(
        rowNamed(cassetteRows(page), "A2").getByLabel("Storage Location A2", {
          exact: true,
        }),
      ).toHaveValue("Tray 4");
    });
  });

  test("the last cassette embedded completes the stage", async ({ page }) => {
    await openCase(page, seeded);
    await expandSection(page, EMBEDDING);

    await embed(page, "A2");
    await expectOutstanding(page, 2, 3, "A3");
    await embed(page, "A3");

    await test.step("nothing is outstanding", async () => {
      await expect(embeddingBadge(page)).toHaveText("Complete");
      await expect(outstandingBanner(page)).toHaveCount(0);
      await expect(railStep(page, "Embedding")).not.toContainText(
        "outstanding",
      );
      await expect(summaryRow(page, "Embedding")).toHaveText(
        /^Embedding\s*3 of 3 embedded$/,
      );
      for (const designation of ["A1", "A2", "A3"]) {
        await expect(stateTag(page, EMBEDDING, designation)).toHaveText(
          "Block",
        );
      }
    });

    await test.step("once the case moves on, the rail marks the stage done", async () => {
      await setStage(page, "Microtomy");
      await saveDraft(page, seeded.pathologySampleId);
      await expect(railStep(page, "Embedding")).toHaveClass(
        /cds--progress-step--complete/,
      );
    });
  });

  test("a retired cassette cannot be embedded", async ({ page }) => {
    await openCase(page, seeded);
    await expandSection(page, GROSSING);

    await test.step("a fourth cassette is cut and waits for embedding", async () => {
      await page.getByRole("button", { name: "Add cassette" }).click();
      await saveDraft(page, seeded.pathologySampleId);
      await expect(rowNamed(cassetteRows(page), "A4")).toHaveCount(1);
      await expandSection(page, EMBEDDING);
      await expect(embedButton(page, "A4")).toBeEnabled();
      await expectOutstanding(page, 3, 4, "A4");
    });

    await test.step("it is retired from the grossing list", async () => {
      await page
        .getByRole("button", { name: "Deactivate cassette A4", exact: true })
        .click();
      const dialog = page.getByRole("dialog");
      await expect(dialog).toBeVisible();
      await dialog.getByLabel("Reason", { exact: true }).fill("Cassette empty");
      const retired = page.waitForResponse(
        (r) =>
          r.request().method() === "POST" &&
          /\/rest\/pathology\/block\/[^/]+\/deactivate$/.test(
            new URL(r.url()).pathname,
          ),
      );
      const readBack = caseReadBack(page);
      // Carbon prefixes a danger button's name with a visually hidden word.
      await dialog.getByRole("button", { name: /Deactivate$/ }).click();
      await retired;
      await readBack;
      await expect(dialog).toHaveCount(0);
    });

    await test.step("the embedding list leaves it out, and out of the count", async () => {
      await expect(rowNamed(embeddingRows(page), "A4")).toHaveCount(0);
      await expect(embedButton(page, "A4")).toHaveCount(0);
      await expect(embeddingBadge(page)).toHaveText("Complete");
      await expect(outstandingBanner(page)).toHaveCount(0);
    });

    await test.step("shown, it is marked retired and offers no Embed", async () => {
      const toggle = page.locator("#showDeactivatedEmbedding");
      await expect(toggle).toBeVisible();
      await page.locator(`#${EMBEDDING} label.cds--toggle__label`).click();
      await expect(toggle).toHaveAttribute("aria-checked", "true");

      const row = rowNamed(embeddingRows(page), "A4");
      await expect(row).toBeVisible();
      await expect(
        row.locator(".cds--tag", { hasText: /^Deactivated$/ }),
      ).toBeVisible();
      await expect(embedButton(page, "A4")).toHaveCount(0);
    });
  });

  test("the case view at two desktop widths, for the pull request", async ({
    page,
  }) => {
    // Opened once and resized in place: a second load while the first one's
    // lookups are still in flight aborts them, and the browser logs each as a
    // failed fetch.
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expandSection(page, EMBEDDING);

    for (const width of [1480, 1280]) {
      await page.setViewportSize({ width, height: 900 });

      await expect(embeddingBadge(page)).toHaveText("Complete");
      await expect(rowNamed(embeddingRows(page), "A1")).toBeVisible();
      await expect(storageLocation(page, "A2")).toHaveValue("Tray 4");

      await page.screenshot({
        path: new URL(
          `../../../../e2e-evidence/ogc-264-s6-embedding-${width}.png`,
          import.meta.url,
        ).pathname,
        fullPage: true,
      });
    }
  });
});
