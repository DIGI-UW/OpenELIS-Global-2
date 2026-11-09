import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureStain,
  moveCaseToStage,
  SeededPathologyCase,
  Stain,
} from "../../../helpers/seed-pathology-data";
import {
  addSlideButton,
  addSlideTo,
  advanceCase,
  blockGroup,
  cassetteRows,
  expandSection,
  expectStageSwitchesOn,
  openCase,
  overlaps,
  rowNamed,
  saveDraft,
  sectionHeader,
  summaryRow,
} from "../../../helpers/pathology-case-view";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import { expectPdf, printLabels } from "../../../helpers/pathology-labels";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Cassettes and slides on the pathology case view are identified rows that
 * are deactivated, never deleted.
 *
 * The screen used to number them by hand: a count input added that many rows
 * at once, each carrying an integer the technician typed and nothing stopped
 * from repeating; a Remove button on every row deleted it, label and all, on
 * the next save; and every row printed its own label by passing its typed
 * number to a label servlet that could not resolve a case from it, so the
 * print failed on every click. Now the server names a row when it is first
 * saved (A1, A2 for cassettes, 1, 2 under a block for slides) and gives it a
 * barcode built from the lab number; a saved row is retired through its own
 * endpoint and kept; a slide is added under the block it was cut from, with
 * its level and stain; and each list prints the case's labels in one go from
 * the lab number.
 *
 * What only a deployed stack proves here:
 *   - the server, not the browser, names a new row, and the name and barcode
 *     arrive with the read-back that follows the save,
 *   - a second save keeps the row's identity and the location it was given,
 *   - the retire endpoint and the read-back that follows it, with the row
 *     still there behind the switch,
 *   - a slide added under a block is saved as cut from that block, and comes
 *     back under it with the level and the stain it was given, and
 *   - the label servlet answers a PDF for the case's lab number.
 *
 * Serial, and on one seeded case, because the walk is the point: the second
 * cassette's name depends on the first, the slide on a saved block, and the
 * printed labels on both.
 *
 * The slide is cut at Microtomy, so that stage must be tracked on the stack
 * (its stage switch on, the shipped default); the spec checks that first.
 */

let seeded: SeededPathologyCase;
let stain: Stain;

/** A barcode as the row renders it, behind its visually hidden label. */
function barcodeText(barcode: string) {
  return new RegExp(
    `^Barcode:\\s*${barcode.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`,
  );
}

/** The switch that brings a list's retired rows into view. */
function showDeactivated(page: Page, sectionId: string) {
  return page
    .locator(`#${sectionId}`)
    .getByRole("switch", { name: "Show deactivated" });
}

/**
 * Turn a list's switch on or off through its label, as a person clicks it:
 * Carbon draws the switch's track over its own button, so the button itself
 * never takes the pointer.
 */
async function setShowDeactivated(page: Page, sectionId: string, on: boolean) {
  const toggle = showDeactivated(page, sectionId);
  await expect(toggle).toBeVisible();
  if ((await toggle.getAttribute("aria-checked")) !== String(on)) {
    await page.locator(`#${sectionId} label.cds--toggle__label`).click();
  }
  await expect(toggle).toHaveAttribute("aria-checked", String(on));
}

async function turnOnShowDeactivated(page: Page, sectionId: string) {
  await setShowDeactivated(page, sectionId, true);
}

/** The slide rows listed under one block. */
function slidesUnder(page: Page, designation: string) {
  return blockGroup(page, designation).locator(".pathology-case-view__row");
}

/** The count a block's group header carries, "1 slide", "2 slides". */
function groupCount(page: Page, designation: string) {
  return blockGroup(page, designation)
    .locator(".pathology-case-view__group-header .cds--tag")
    .filter({ hasText: /^\d+ slides?$/ });
}

test.describe
  .serial("Pathology cassettes and slides as identified rows", () => {
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
    await expectStageSwitchesOn(page, ["MICROTOMY"]);
    const target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    stain = await ensureStain(page);
    await context.close();
  });

  test("a cassette added at grossing is named by the server on save", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await advanceCase(page, { label: "Begin grossing" });
    await expandSection(page, "pathology-section-grossing");

    await test.step("no count input is offered anywhere", async () => {
      await expect(page.locator('input[type="number"]')).toHaveCount(0);
    });

    await test.step("a new row says its name comes with the save", async () => {
      await page.getByRole("button", { name: "Add cassette" }).click();
      const row = cassetteRows(page);
      await expect(row).toHaveCount(1);
      await expect(
        row.locator(".pathology-case-view__row-designation"),
      ).toHaveText("Assigned on save");
      await expect(
        row.locator(".pathology-case-view__row-barcode"),
      ).toHaveCount(0);
    });

    await saveDraft(page, seeded.pathologySampleId);

    await test.step("the saved row carries the server's name and barcode", async () => {
      const row = rowNamed(cassetteRows(page), "A1");
      await expect(row).toHaveCount(1);
      await expect(row.locator(".pathology-case-view__row-barcode")).toHaveText(
        barcodeText(`${seeded.accessionNumber}.A1`),
      );
      await expect(row.locator(".cds--tag")).toHaveText(["Cassette"]);
      await expect(summaryRow(page, "Cassettes")).toHaveText(/^Cassettes\s*1$/);
      await expect(
        sectionHeader(page, "pathology-section-grossing").locator(".cds--tag"),
      ).toHaveText("1 cassette");
    });

    await page.screenshot({
      path: testInfo.outputPath("cassette-named.png"),
      fullPage: true,
    });
  });

  test("saving again keeps the cassette's identity", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-grossing");

    const location = rowNamed(cassetteRows(page), "A1").getByLabel(
      "Storage Location A1",
      {
        exact: true,
      },
    );
    await location.fill("Drawer 5");
    await saveDraft(page, seeded.pathologySampleId);

    await expect(cassetteRows(page)).toHaveCount(1);
    const row = rowNamed(cassetteRows(page), "A1");
    await expect(row).toHaveCount(1);
    await expect(row.locator(".pathology-case-view__row-barcode")).toHaveText(
      barcodeText(`${seeded.accessionNumber}.A1`),
    );

    // Read from a fresh load, so the location is what the server stored on
    // the same row rather than what the form still held.
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-grossing");
    await expect(
      rowNamed(cassetteRows(page), "A1").getByLabel("Storage Location A1", {
        exact: true,
      }),
    ).toHaveValue("Drawer 5");

    await page.screenshot({
      path: testInfo.outputPath("cassette-kept.png"),
      fullPage: true,
    });
  });

  test("a second cassette takes the next designation and a retired one is hidden until shown", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-grossing");

    await page.getByRole("button", { name: "Add cassette" }).click();
    await saveDraft(page, seeded.pathologySampleId);

    const second = rowNamed(cassetteRows(page), "A2");
    await expect(second).toHaveCount(1);
    await expect(
      second.locator(".pathology-case-view__row-barcode"),
    ).toHaveText(barcodeText(`${seeded.accessionNumber}.A2`));

    await test.step("deactivating asks first, then retires the row", async () => {
      await page
        .getByRole("button", { name: "Deactivate cassette A2", exact: true })
        .click();
      const dialog = page.getByRole("dialog");
      await expect(dialog).toBeVisible();
      await expect(
        dialog.getByRole("heading", { name: "Deactivate cassette A2?" }),
      ).toBeVisible();
      // The audit trail keeps the reason, so the dialog will not go on
      // without one: Deactivate with the field empty says what is missing
      // and posts nothing.
      const confirm = dialog.getByRole("button", { name: /Deactivate$/ });
      const reason = dialog.getByLabel("Reason", { exact: true });
      await expect(reason).toBeFocused();
      await expect(reason).toHaveAttribute("required", "");
      let blankPosts = 0;
      const countBlankPost = (request: import("@playwright/test").Request) => {
        if (
          request.method() === "POST" &&
          /\/deactivate$/.test(new URL(request.url()).pathname)
        ) {
          blankPosts += 1;
        }
      };
      page.on("request", countBlankPost);
      await confirm.click();
      await expect(
        dialog.getByText("Give a reason before deactivating", { exact: true }),
      ).toBeVisible();
      await expect(dialog).toBeVisible();
      page.off("request", countBlankPost);
      expect(blankPosts, "a blank reason posts nothing").toBe(0);
      await reason.fill("Section folded");
      await expect(confirm).toBeEnabled();

      const retired = page.waitForResponse(
        (r) =>
          r.request().method() === "POST" &&
          /\/rest\/pathology\/block\/[^/]+\/deactivate$/.test(
            new URL(r.url()).pathname,
          ),
      );
      const readBack = page.waitForResponse(
        (r) =>
          r.request().method() === "GET" &&
          new URL(r.url()).pathname.endsWith(
            `/rest/pathology/caseView/${seeded.pathologySampleId}`,
          ),
      );
      // Carbon prefixes a danger button's name with a visually hidden word,
      // hence the pattern on confirm above.
      await confirm.click();
      await retired;
      await readBack;

      await expect(dialog).toHaveCount(0);
      await expect(
        page
          .locator(".cds--toast-notification")
          .filter({ hasText: "Cassette A2 deactivated" }),
      ).not.toHaveCount(0);
    });

    await test.step("the retired row is out of the list and out of every count", async () => {
      await expect(rowNamed(cassetteRows(page), "A2")).toHaveCount(0);
      await expect(cassetteRows(page)).toHaveCount(1);
      await expect(summaryRow(page, "Cassettes")).toHaveText(/^Cassettes\s*1$/);
      await expect(
        sectionHeader(page, "pathology-section-grossing").locator(".cds--tag"),
      ).toHaveText("1 cassette");
    });

    await test.step("the switch brings it back, kept and no longer editable", async () => {
      await turnOnShowDeactivated(page, "pathology-section-grossing");

      const retiredRow = rowNamed(cassetteRows(page), "A2");
      await expect(retiredRow).toBeVisible();
      await expect(retiredRow).toHaveClass(
        /pathology-case-view__row--deactivated/,
      );
      await expect(
        retiredRow.locator(".cds--tag", { hasText: /^Deactivated$/ }),
      ).toBeVisible();
      await expect(
        retiredRow.getByLabel("Storage Location A2", { exact: true }),
      ).toBeDisabled();
      await expect(
        retiredRow.getByRole("button", {
          name: "Deactivate cassette A2",
          exact: true,
        }),
      ).toHaveCount(0);

      await expect(
        page.getByRole("button", {
          name: "Deactivate cassette A1",
          exact: true,
        }),
      ).toBeEnabled();
    });

    await page.screenshot({
      path: testInfo.outputPath("cassette-retired.png"),
      fullPage: true,
    });
  });

  test("a slide is added under the block it is cut from, with its level and stain", async ({
    page,
  }, testInfo) => {
    // The stages between are not what this spec is about, so the case is
    // taken there through the move endpoints, which describe the specimen and
    // embed its cassette on the way as the bench would, and opened again.
    await openCase(page, seeded);
    await moveCaseToStage(page, seeded, "MICROTOMY");
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-microtomy");

    await test.step("the block is listed with nothing cut from it, and the retired one offers nothing", async () => {
      await expect(blockGroup(page, "A1")).toHaveCount(1);
      await expect(
        blockGroup(page, "A1").locator(".pathology-case-view__group-header"),
      ).toContainText("No slides in use under this block");
      await expect(slidesUnder(page, "A1")).toHaveCount(0);
      // A2 was retired at grossing with no slide, so it is listed only
      // behind the switch, and even there takes no new slide.
      await expect(blockGroup(page, "A2")).toHaveCount(0);
      await setShowDeactivated(page, "pathology-section-microtomy", true);
      await expect(blockGroup(page, "A2")).toHaveCount(1);
      await expect(
        blockGroup(page, "A2").locator(".cds--tag", {
          hasText: /^Deactivated$/,
        }),
      ).toBeVisible();
      await expect(addSlideButton(page, "A2")).toHaveCount(0);
      await setShowDeactivated(page, "pathology-section-microtomy", false);
    });

    await test.step("Add slide under A1 adds an unsaved slide there, with no block to pick", async () => {
      await addSlideTo(page, "A1");
      const newRow = slidesUnder(page, "A1");
      await expect(newRow).toHaveCount(1);
      await expect(
        newRow.locator(".pathology-case-view__row-designation"),
      ).toHaveText("Assigned on save");
      await expect(
        newRow.getByLabel("Level New slide 1", { exact: true }),
      ).toBeVisible();
      await expect(
        newRow.getByRole("combobox", {
          name: "Stain New slide 1",
          exact: true,
        }),
      ).toBeVisible();
      await expect(
        newRow.getByLabel("Location New slide 1", { exact: true }),
      ).toBeVisible();
      await expect(
        newRow.getByRole("button", { name: "Remove New slide 1", exact: true }),
      ).toBeVisible();
      await expect(page.getByText(/Cut from block/)).toHaveCount(0);
    });

    await slidesUnder(page, "A1")
      .getByLabel("Level New slide 1", { exact: true })
      .fill("1");
    await chooseCarbonOption(
      slidesUnder(page, "A1").getByRole("combobox", {
        name: "Stain New slide 1",
        exact: true,
      }),
      stain.name,
    );
    await saveDraft(page, seeded.pathologySampleId);

    const barcode = `${seeded.accessionNumber}.A1.1`;
    const row = rowNamed(slidesUnder(page, "A1"), "1");
    await expect(row).toHaveCount(1);
    await expect(row.locator(".pathology-case-view__row-barcode")).toHaveText(
      barcodeText(barcode),
    );
    await expect(
      row.getByLabel(`Level ${barcode}`, { exact: true }),
    ).toHaveValue("1");
    await expect(
      row.getByRole("combobox", { name: `Stain ${barcode}`, exact: true }),
    ).toHaveValue(stain.name);
    await expect(summaryRow(page, "Slides")).toHaveText(/^Slides\s*1$/);
    await expect(
      sectionHeader(page, "pathology-section-microtomy").locator(".cds--tag"),
    ).toHaveText("1 slide");
    await expect(groupCount(page, "A1")).toHaveText("1 slide");
    await expect(
      row.getByRole("button", { name: "Upload file" }).first(),
    ).toBeVisible();
    await expect(
      row.getByRole("button", {
        name: `Deactivate slide ${barcode}`,
        exact: true,
      }),
    ).toBeVisible();
    await expect(row.getByRole("button", { name: /^Remove/ })).toHaveCount(0);

    await page.screenshot({
      path: testInfo.outputPath("slide-cut.png"),
      fullPage: true,
    });
  });

  test("the case's cassette labels print as one PDF from the lab number", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-grossing");

    const { popup, response } = await printLabels(
      page,
      "Print all cassette labels",
    );
    const wanted = `/LabelMakerServlet?labelType=block&code=${encodeURIComponent(seeded.accessionNumber)}`;
    expect(response.url()).toContain(wanted);
    await expectPdf(response);
    await popup.close();
  });

  test("the case's slide labels print as one PDF from the lab number", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-microtomy");

    const { popup, response } = await printLabels(
      page,
      "Print all slide labels",
    );
    const wanted = `/LabelMakerServlet?labelType=slide&code=${encodeURIComponent(seeded.accessionNumber)}`;
    expect(response.url()).toContain(wanted);
    await expectPdf(response);
    await popup.close();
  });

  test("deactivation is refused while the form holds unsaved changes", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-grossing");

    // A1 was embedded on the way to Microtomy, so it is a block now.
    const deactivate = page.getByRole("button", {
      name: "Deactivate block A1",
      exact: true,
    });
    await expect(deactivate).toBeEnabled();

    const location = rowNamed(cassetteRows(page), "A1").getByLabel(
      "Storage Location A1",
      {
        exact: true,
      },
    );
    await location.fill("Drawer 6");

    await expect(deactivate).toBeDisabled();
    await expect(deactivate).toHaveAttribute(
      "title",
      "Save or discard your changes before deactivating",
    );

    await page.screenshot({
      path: testInfo.outputPath("deactivate-refused-while-dirty.png"),
      fullPage: true,
    });

    await page.getByRole("button", { name: "Discard changes" }).click();
    await expect(location).toHaveValue("Drawer 5", { timeout: UI_TIMEOUT });
    await expect(deactivate).toBeEnabled();
  });

  test("the case view at two desktop widths, for the pull request", async ({
    page,
  }) => {
    for (const width of [1480, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      await openCase(page, seeded);
      await expandSection(page, "pathology-section-grossing");
      await expandSection(page, "pathology-section-microtomy");

      await turnOnShowDeactivated(page, "pathology-section-grossing");

      await expect(rowNamed(cassetteRows(page), "A1")).toBeVisible();
      await expect(rowNamed(cassetteRows(page), "A2")).toBeVisible();
      const barcode = `${seeded.accessionNumber}.A1.1`;
      const slide = rowNamed(slidesUnder(page, "A1"), "1");
      await expect(slide).toBeVisible();

      // Upload file and the slide's Deactivate are the actions a person can
      // see; Carbon's second, hidden uploader trigger is not one they can hit.
      await expect(
        slide.locator(".pathology-case-view__row-actions button:visible"),
      ).toHaveCount(2);

      // Every control on the slide row sits clear of the others at this
      // width.
      const controls = [
        slide.locator(".pathology-case-view__row-identity"),
        slide.getByLabel(`Level ${barcode}`, { exact: true }),
        slide.getByRole("combobox", { name: `Stain ${barcode}`, exact: true }),
        slide.getByRole("button", { name: "Upload file" }).first(),
        slide.getByRole("button", {
          name: `Deactivate slide ${barcode}`,
          exact: true,
        }),
      ];
      const boxes = [];
      for (const control of controls) {
        await expect(control).toBeVisible();
        const box = await control.boundingBox();
        expect(box).not.toBeNull();
        boxes.push(box!);
      }
      for (let first = 0; first < boxes.length; first += 1) {
        for (let second = first + 1; second < boxes.length; second += 1) {
          expect(
            overlaps(boxes[first], boxes[second]),
            `slide row controls ${first} and ${second} overlap at ${width}px`,
          ).toBe(false);
        }
      }

      await page.screenshot({
        path: new URL(
          `../../../../e2e-evidence/ogc-264-s4-cp3-case-view-${width}.png`,
          import.meta.url,
        ).pathname,
        fullPage: true,
      });
    }
  });
});
