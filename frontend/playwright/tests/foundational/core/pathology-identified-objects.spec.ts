import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  SeededPathologyCase,
} from "../../../helpers/seed-pathology-data";
import {
  expandSection,
  openCase,
  overlaps,
  saveDraft,
  sectionHeader,
  setStage,
  summaryRow,
} from "../../../helpers/pathology-case-view";
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
 * endpoint and kept; a slide names the saved block it was cut from; and each
 * list prints the case's labels in one go from the lab number.
 *
 * What only a deployed stack proves here:
 *   - the server, not the browser, names a new row, and the name and barcode
 *     arrive with the read-back that follows the save,
 *   - a second save keeps the row's identity and the location it was given,
 *   - the retire endpoint and the read-back that follows it, with the row
 *     still there behind the switch,
 *   - the slide picker offers only blocks the server already holds, and
 *   - the label servlet answers a PDF for the case's lab number.
 *
 * Serial, and on one seeded case, because the walk is the point: the second
 * cassette's name depends on the first, the slide on a saved block, and the
 * printed labels on both.
 */

let seeded: SeededPathologyCase;

/** Every cassette row on the grossing list, retired ones included when shown. */
function cassetteRows(page: Page) {
  return page.locator("#pathology-section-grossing .pathology-case-view__row");
}

/** Every slide row on the microtomy list. */
function slideRows(page: Page) {
  return page.locator("#pathology-section-microtomy .pathology-case-view__row");
}

/** The one row on a list carrying a designation, matched whole. */
function rowNamed(rows: ReturnType<typeof cassetteRows>, designation: string) {
  return rows.filter({
    has: rows.page().locator(".pathology-case-view__row-designation", {
      hasText: new RegExp(`^${designation}$`),
    }),
  });
}

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
 * Turn a list's switch on through its label, as a person clicks it: Carbon
 * draws the switch's track over its own button, so the button itself never
 * takes the pointer.
 */
async function turnOnShowDeactivated(page: Page, sectionId: string) {
  const toggle = showDeactivated(page, sectionId);
  await expect(toggle).toBeVisible();
  await page.locator(`#${sectionId} label.cds--toggle__label`).click();
  await expect(toggle).toHaveAttribute("aria-checked", "true");
}

/**
 * Click a print button and return the label servlet's answer. The labels open
 * in a new window, but a headless browser hands a PDF off as a download and
 * the window never commits a URL, so the request and its answer are read from
 * the browser context, which sees the window's traffic either way.
 */
async function printLabels(page: Page, buttonName: string) {
  const answer = page.context().waitForEvent("response", {
    predicate: (r) => r.url().includes("/LabelMakerServlet"),
    timeout: UI_TIMEOUT,
  });
  const popupOpened = page.waitForEvent("popup");
  await page.getByRole("button", { name: buttonName, exact: true }).click();
  const popup = await popupOpened;
  const response = await answer;
  return { popup, response };
}

/**
 * Fail with the servlet's own words when it answers anything but a PDF: an
 * HTML page from it means no label was produced, which is a defect.
 */
async function expectPdf(
  response: Awaited<ReturnType<typeof printLabels>>["response"],
) {
  const contentType = response.headers()["content-type"] ?? "";
  const preview = contentType.startsWith("application/pdf")
    ? ""
    : (await response.text()).slice(0, 200);
  expect(response.status(), `label servlet answered: ${preview}`).toBe(200);
  expect(
    contentType,
    `label servlet answered ${contentType} instead of a PDF: ${preview}`,
  ).toMatch(/^application\/pdf/);
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
    const target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    await context.close();
  });

  test("a cassette added at grossing is named by the server on save", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await setStage(page, "Grossing");
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
      "Location A1",
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
      rowNamed(cassetteRows(page), "A1").getByLabel("Location A1", {
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
        retiredRow.getByLabel("Location A2", { exact: true }),
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

  test("a slide is cut from a named block, and only an active saved block is offered", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await setStage(page, "Microtomy");
    await expandSection(page, "pathology-section-microtomy");

    const addSlide = page.getByRole("button", { name: "Add slide" });
    await expect(addSlide).toBeEnabled();
    await addSlide.click();

    const newRow = slideRows(page).first();
    // The picker's name carries the row's name after its visible title.
    const parent = newRow.getByRole("combobox", { name: /^Cut from block/ });

    await test.step("a slide naming no block is refused before anything is posted", async () => {
      const casePosts: string[] = [];
      const recordPost = (request: { method(): string; url(): string }) => {
        if (
          request.method() === "POST" &&
          request.url().includes("/rest/pathology/caseView/")
        ) {
          casePosts.push(request.url());
        }
      };
      page.on("request", recordPost);
      await page.getByRole("button", { name: "Save draft" }).click();

      const refusal = "Pick the block this slide was cut from";
      await expect(
        page
          .locator(".cds--toast-notification--error")
          .filter({ hasText: refusal }),
      ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
      // Carbon's ComboBox sets no aria-invalid; it ties the invalid text to
      // the input as its description, which is what a screen reader reads.
      await expect(parent).toHaveAccessibleDescription(refusal);
      await expect(newRow.getByText(refusal, { exact: true })).toBeVisible();
      await expect(page.getByText("Unsaved changes")).toBeVisible();
      page.off("request", recordPost);
      expect(casePosts, "the refused save posted nothing").toEqual([]);
    });

    await parent.click();
    // A2 is retired, so the one block a slide may still be cut from is A1.
    await expect(newRow.getByRole("option")).toHaveText(["A1"]);
    await newRow.getByRole("option", { name: "A1", exact: true }).click();
    await expect(parent).toHaveValue("A1");

    await saveDraft(page, seeded.pathologySampleId);

    const row = rowNamed(slideRows(page), "1");
    await expect(row).toHaveCount(1);
    await expect(row.locator(".pathology-case-view__row-barcode")).toHaveText(
      barcodeText(`${seeded.accessionNumber}.A1.1`),
    );
    await expect(row.locator(".pathology-case-view__row-meta")).toHaveText(
      /^Cut from block\s*A1$/,
    );
    await expect(summaryRow(page, "Slides")).toHaveText(/^Slides\s*1$/);
    await expect(
      sectionHeader(page, "pathology-section-microtomy").locator(".cds--tag"),
    ).toHaveText("1 slide");
    await expect(
      row.getByRole("button", { name: "Upload file" }).first(),
    ).toBeVisible();
    await expect(
      row.getByRole("button", {
        name: `Deactivate slide ${seeded.accessionNumber}.A1.1`,
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

    const deactivate = page.getByRole("button", {
      name: "Deactivate cassette A1",
      exact: true,
    });
    await expect(deactivate).toBeEnabled();

    const location = rowNamed(cassetteRows(page), "A1").getByLabel(
      "Location A1",
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
      const slide = rowNamed(slideRows(page), "1");
      await expect(slide).toBeVisible();

      // Every action on the slide row that a person can see sits clear of
      // the others at this width.
      const actions = slide.locator(
        ".pathology-case-view__row-actions button:visible",
      );
      // Upload file and the slide's Deactivate; Carbon's second, hidden uploader
      // trigger is not something a person can see or hit.
      await expect(actions).toHaveCount(2);
      const boxes = [];
      for (const action of await actions.all()) {
        const box = await action.boundingBox();
        expect(box).not.toBeNull();
        boxes.push(box!);
      }
      for (let first = 0; first < boxes.length; first += 1) {
        for (let second = first + 1; second < boxes.length; second += 1) {
          expect(
            overlaps(boxes[first], boxes[second]),
            `slide row actions ${first} and ${second} overlap at ${width}px`,
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
