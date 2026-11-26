import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Browser, Locator, Page, TestInfo } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  apiGet,
  createPathologyCase,
  discoverPathologyOrderTarget,
  moveCaseToStage,
  readCase,
  saveCaseThroughApi,
  type PathologyOrderTarget,
} from "../../../helpers/seed-pathology-data";
import {
  createProgramOrder,
  programCaseIdOf,
} from "../../../helpers/program-order";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Under the patient band of the pathology, immunohistochemistry and cytology
 * case views, the patient's earlier anatomic-pathology cases are listed
 * newest first, across all three benches, with each one's conclusion once it
 * is signed out.
 *
 * What only a deployed stack proves here:
 *   - one patient's cases on the three benches are listed in order on each
 *     view, a signed-out case with its conclusion and the others as not
 *     reported yet,
 *   - the patient's first case, and a patient with one case, say there is
 *     nothing earlier,
 *   - a patient with more earlier cases than the list shows offers Show all,
 *     and the lab number opens that case in a new tab, and
 *   - the panel fits the three views at desktop widths and passes axe.
 *
 * Every run orders its own patients, so other cases on the stack do not
 * change what is listed. Patient X has pathology P1 (signed out with a free
 * text conclusion), then cytology C1, then immunohistochemistry I1, then
 * pathology P2; patient Y has pathology P3 only; patient Z has seven
 * pathology cases.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

const EMPTY_SENTENCE = "No prior anatomic-pathology results for this patient.";
const NOT_REPORTED = "Not reported yet";

interface SeededCase {
  id: string;
  labNo: string;
}

let p1: SeededCase;
let c1: SeededCase;
let i1: SeededCase;
let p2: SeededCase;
let p3: SeededCase;
let zCases: SeededCase[] = [];
let p1Conclusion = "";
/** Today as the order was dated, in the site's date format. */
let siteToday = "";

async function withOwnPage<T>(
  browser: Browser,
  work: (page: Page) => Promise<T>,
): Promise<T> {
  const context = await browser.newContext({
    storageState: "playwright/.auth/user.json",
  });
  try {
    const page = await context.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded" });
    return await work(page);
  } finally {
    await context.close();
  }
}

async function pathologyCase(
  page: Page,
  target: PathologyOrderTarget,
  existingPatientCaseId?: string,
): Promise<SeededCase> {
  const seeded = await createPathologyCase(page, target, {
    existingPatientCaseId,
  });
  return { id: seeded.pathologySampleId, labNo: seeded.accessionNumber };
}

async function benchCase(
  page: Page,
  target: PathologyOrderTarget,
  bench: "immunohistochemistry" | "cytology",
  existingPatientCaseId: string,
): Promise<SeededCase> {
  const { labNo } = await createProgramOrder(page, {
    ...(bench === "cytology"
      ? { program: "Cytology", sampleType: "Fluid" }
      : {
          program: "Immunohistochemistry",
          sampleType: "Immunohistochemistry specimen",
        }),
    existingPatientCaseId,
    providerId: target.providerId,
    providerPersonId: target.providerPersonId,
    referringSiteId: target.referringSiteId,
  });
  return { id: await programCaseIdOf(page, bench, labNo), labNo };
}

const VIEW_ROUTES = {
  PATHOLOGY: "/PathologyCaseView/",
  IMMUNOHISTOCHEMISTRY: "/ImmunohistochemistryCaseView/",
  CYTOLOGY: "/CytologyCaseView/",
} as const;

type Bench = keyof typeof VIEW_ROUTES;

function panelOf(page: Page): Locator {
  return page.getByTestId("prior-results");
}

function rowsOf(page: Page): Locator {
  return panelOf(page).locator('tbody tr[data-testid^="prior-result-"]');
}

function rowOf(page: Page, bench: Bench, seeded: SeededCase): Locator {
  return panelOf(page).getByTestId(`prior-result-${bench}-${seeded.id}`);
}

/** Open a case view and wait until the panel has answered. */
async function openView(page: Page, bench: Bench, seeded: SeededCase) {
  await page.goto(`${VIEW_ROUTES[bench]}${seeded.id}`, {
    waitUntil: "domcontentloaded",
  });
  await expect(panelOf(page)).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(panelOf(page).getByTestId("prior-results-loading")).toHaveCount(
    0,
    { timeout: NAV_TIMEOUT },
  );
}

/** The rows' benches and case ids, top to bottom. */
async function expectRowsInOrder(page: Page, expected: [Bench, SeededCase][]) {
  await expect(rowsOf(page)).toHaveCount(expected.length);
  for (const [index, [bench, seeded]] of expected.entries()) {
    await expect(rowsOf(page).nth(index)).toHaveAttribute(
      "data-testid",
      `prior-result-${bench}-${seeded.id}`,
    );
    await expect(rowsOf(page).nth(index).locator("td").first()).toContainText(
      seeded.labNo,
    );
  }
}

/**
 * How far the page scrolls sideways as it stands, and with the panel taken out
 * of the flow for the reading and put back straight after.
 */
async function sidewaysScroll(page: Page) {
  return page.evaluate(() => {
    const root = document.documentElement;
    const measure = () => root.scrollWidth - root.clientWidth;
    const withPanel = measure();
    const panel = document.querySelector<HTMLElement>(".prior-results");
    const display = panel?.style.display ?? "";
    if (panel) panel.style.display = "none";
    const withoutPanel = measure();
    if (panel) panel.style.display = display;
    return { withPanel, withoutPanel };
  });
}

async function expectNoSidewaysScroll(
  page: Page,
  bench: Bench,
  label: string,
  testInfo: TestInfo,
) {
  const { withPanel, withoutPanel } = await sidewaysScroll(page);
  if (bench === "PATHOLOGY") {
    expect(withPanel, `${label} scrolls sideways by ${withPanel}px`).toBe(0);
  } else {
    // The immunohistochemistry and cytology views already scroll sideways
    // without the panel: their order band's grid and the embedded results
    // table run past the right edge. That is recorded on the run; what is
    // asserted is that the panel adds nothing to it.
    if (withoutPanel > 0) {
      testInfo.annotations.push({
        type: "view overflow without the panel",
        description: `${label}: ${withoutPanel}px`,
      });
    }
    expect(
      withPanel,
      `${label} scrolls sideways by ${withPanel}px, ${withoutPanel}px without the panel`,
    ).toBeLessThanOrEqual(Math.max(0, withoutPanel));
  }

  const panel = await panelOf(page).evaluate((section) => {
    const box = section.getBoundingClientRect();
    return {
      left: box.left,
      right: box.right,
      overflow: section.scrollWidth - section.clientWidth,
      width: document.documentElement.clientWidth,
    };
  });
  expect(
    panel.left,
    `${label}: the panel starts on screen`,
  ).toBeGreaterThanOrEqual(0);
  expect(panel.right, `${label}: the panel ends on screen`).toBeLessThanOrEqual(
    panel.width,
  );
  expect(
    panel.overflow,
    `${label}: the panel scrolls inside`,
  ).toBeLessThanOrEqual(0);
}

/**
 * Every lab number in the panel sits on one line: the line boxes its text
 * spans are counted by their distinct tops, so a number broken in the middle
 * reads as two.
 */
async function expectLabNumbersOnOneLine(page: Page, label: string) {
  const broken = await rowsOf(page).evaluateAll((rows) =>
    rows
      .map((row) => {
        const cell = row.querySelector("td");
        if (!cell) return { text: "", lines: 0 };
        const walker = document.createTreeWalker(cell, NodeFilter.SHOW_TEXT);
        let node = walker.nextNode();
        while (node && !node.textContent?.trim()) node = walker.nextNode();
        if (!node) return { text: "", lines: 0 };
        const range = document.createRange();
        range.selectNodeContents(node);
        const tops = new Set(
          Array.from(range.getClientRects())
            .filter((rect) => rect.width > 0)
            .map((rect) => Math.round(rect.top)),
        );
        return { text: node.textContent ?? "", lines: tops.size };
      })
      .filter(({ lines }) => lines !== 1),
  );
  expect(broken, `${label}: lab numbers not on one line`).toEqual([]);
}

test.describe
  .serial("Prior anatomic-pathology results on the case views", () => {
  // Order entry and the case views are heavy screens, and a first visit on a
  // development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    // Twelve orders and one case taken to sign-out, all through the API.
    test.setTimeout(480_000);
    await withOwnPage(browser, async (page) => {
      const target = await discoverPathologyOrderTarget(page);
      const config = await apiGet<Record<string, string>>(
        page,
        `${API_PREFIX}/rest/open-configuration-properties`,
      );
      siteToday = config.currentDateAsText ?? "";

      // Patient X: P1 signed out with a free-text conclusion, then C1, I1, P2.
      p1 = await pathologyCase(page, target);
      p1Conclusion = `Adenocarcinoma, moderately differentiated (${p1.labNo})`;
      // The server offers no stage after Ready for Pathologist: the release
      // on the save is the sign-out, as Ready For release is on the screen.
      await moveCaseToStage(
        page,
        { pathologySampleId: p1.id },
        "READY_PATHOLOGIST",
      );
      await saveCaseThroughApi(page, p1.id, await readCase(page, p1.id), {
        conclusionText: p1Conclusion,
        release: true,
      });
      expect((await readCase(page, p1.id)).status).toBe("COMPLETED");

      c1 = await benchCase(page, target, "cytology", p1.id);
      i1 = await benchCase(page, target, "immunohistochemistry", p1.id);
      p2 = await pathologyCase(page, target, p1.id);

      // Patient Y: one case only.
      p3 = await pathologyCase(page, target);

      // Patient Z: seven pathology cases, the first one making the patient.
      zCases = [await pathologyCase(page, target)];
      for (let n = 1; n < 7; n += 1) {
        zCases.push(await pathologyCase(page, target, zCases[0].id));
      }
    });
  });

  test("the later pathology case lists the patient's IHC, cytology and signed-out pathology cases newest first", async ({
    page,
  }) => {
    await openView(page, "PATHOLOGY", p2);
    await expectRowsInOrder(page, [
      ["IMMUNOHISTOCHEMISTRY", i1],
      ["CYTOLOGY", c1],
      ["PATHOLOGY", p1],
    ]);

    await expect(rowOf(page, "CYTOLOGY", c1).locator("td").nth(3)).toHaveText(
      NOT_REPORTED,
    );
    await expect(
      rowOf(page, "IMMUNOHISTOCHEMISTRY", i1).locator("td").nth(3),
    ).toHaveText(NOT_REPORTED);
    await expect(rowOf(page, "PATHOLOGY", p1).locator("td").nth(3)).toHaveText(
      p1Conclusion,
    );

    // The orders were all dated today; the cell shows it as the site writes
    // dates, never as the ISO date the endpoint sends.
    expect(siteToday).toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
    await expect(rowOf(page, "PATHOLOGY", p1).locator("td").nth(1)).toHaveText(
      siteToday,
    );
  });

  test("the patient's first pathology case shows the no prior results sentence", async ({
    page,
  }) => {
    await openView(page, "PATHOLOGY", p1);
    await expect(panelOf(page)).toContainText(EMPTY_SENTENCE);
    await expect(rowsOf(page)).toHaveCount(0);
  });

  test("the cytology view lists the earlier pathology case and the IHC view lists cytology and pathology", async ({
    page,
  }) => {
    await openView(page, "CYTOLOGY", c1);
    await expectRowsInOrder(page, [["PATHOLOGY", p1]]);
    await expect(rowOf(page, "PATHOLOGY", p1).locator("td").nth(3)).toHaveText(
      p1Conclusion,
    );

    await openView(page, "IMMUNOHISTOCHEMISTRY", i1);
    await expectRowsInOrder(page, [
      ["CYTOLOGY", c1],
      ["PATHOLOGY", p1],
    ]);
  });

  test("a patient with one case sees the no prior results sentence and no table", async ({
    page,
  }) => {
    await openView(page, "PATHOLOGY", p3);
    await expect(panelOf(page)).toContainText(EMPTY_SENTENCE);
    await expect(panelOf(page).getByRole("table")).toHaveCount(0);
    await expect(panelOf(page).getByTestId("prior-results-toggle")).toHaveCount(
      0,
    );
  });

  test("six earlier cases show five rows, Show all 6 lists six and Show fewer lists five", async ({
    page,
  }) => {
    const newest = zCases[zCases.length - 1];
    await openView(page, "PATHOLOGY", newest);
    const earlier = zCases.slice(0, -1).reverse();
    await expectRowsInOrder(
      page,
      earlier.slice(0, 5).map((seeded) => ["PATHOLOGY", seeded]),
    );

    const toggle = panelOf(page).getByTestId("prior-results-toggle");
    await expect(toggle).toHaveText("Show all 6");
    await expect(toggle).toHaveAttribute("aria-expanded", "false");
    await toggle.click();
    await expectRowsInOrder(
      page,
      earlier.map((seeded) => ["PATHOLOGY", seeded]),
    );
    await expect(toggle).toHaveText("Show fewer");
    await expect(toggle).toHaveAttribute("aria-expanded", "true");

    await toggle.click();
    await expect(rowsOf(page)).toHaveCount(5);
    await expect(toggle).toHaveText("Show all 6");
  });

  test("the lab number of the signed-out case opens that case in a new tab", async ({
    page,
  }) => {
    await openView(page, "PATHOLOGY", p2);
    const link = rowOf(page, "PATHOLOGY", p1).getByRole("link", {
      name: `Open Pathology case ${p1.labNo} in a new tab`,
    });
    await expect(link).toHaveAttribute("target", "_blank");

    const [opened] = await Promise.all([
      page.context().waitForEvent("page"),
      link.click(),
    ]);
    await opened.waitForLoadState("domcontentloaded");
    await expect(opened).toHaveURL(new RegExp(`/PathologyCaseView/${p1.id}$`));
    await expect(
      opened.getByRole("heading", { name: /^Pathology Case\b/, level: 3 }),
    ).toContainText(p1.labNo, { timeout: NAV_TIMEOUT });
    // The case view that opened the link stays where it was.
    await expect(page).toHaveURL(new RegExp(`/PathologyCaseView/${p2.id}$`));
    await opened.close();
  });

  test("the panel fits the three views at 1480, 1366 and 1280 and passes axe", async ({
    page,
  }, testInfo) => {
    const views: [Bench, SeededCase, string][] = [
      ["PATHOLOGY", p2, "pathology"],
      ["IMMUNOHISTOCHEMISTRY", i1, "ihc"],
      ["CYTOLOGY", c1, "cytology"],
    ];
    for (const [bench, seeded, name] of views) {
      await page.setViewportSize({ width: 1480, height: 900 });
      await openView(page, bench, seeded);
      await expect(rowsOf(page).first()).toBeVisible();

      // The shared header's notification bell is outside the panel, so the
      // scan is scoped to the panel itself.
      const scan = await new AxeBuilder({ page })
        .withTags(WCAG_21_AA_TAGS)
        .include('[data-testid="prior-results"]')
        .analyze();
      const jsonPath = testInfo.outputPath(`axe-prior-results-${name}.json`);
      await writeFile(
        jsonPath,
        `${JSON.stringify(scan.violations, null, 2)}\n`,
      );
      await testInfo.attach(`axe-prior-results-${name}`, {
        path: jsonPath,
        contentType: "application/json",
      });
      const violations = scan.violations.map(({ id, impact, nodes }) => ({
        id,
        impact,
        targets: nodes.map((node) => node.target),
      }));
      expect(violations, JSON.stringify(violations, null, 2)).toEqual([]);

      // Opened once and resized in place: a reload while the first one's
      // lookups are in flight aborts them.
      for (const width of [1480, 1366, 1280]) {
        await page.setViewportSize({ width, height: 900 });
        await expect(rowsOf(page).first()).toBeVisible();
        await expectNoSidewaysScroll(
          page,
          bench,
          `The ${name} view at ${width}`,
          testInfo,
        );
        await expectLabNumbersOnOneLine(page, `The ${name} view at ${width}`);
        if (width !== 1366) {
          await panelOf(page).scrollIntoViewIfNeeded();
          const shotPath = testInfo.outputPath(
            `prior-results-${name}-${width}.png`,
          );
          await page.screenshot({ path: shotPath, fullPage: true });
          await testInfo.attach(`prior-results-${name}-${width}`, {
            path: shotPath,
            contentType: "image/png",
          });
        }
      }
    }
  });
});
