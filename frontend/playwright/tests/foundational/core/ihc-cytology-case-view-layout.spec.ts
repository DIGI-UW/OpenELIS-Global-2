import type { Browser, Locator, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  type PathologyOrderTarget,
} from "../../../helpers/seed-pathology-data";
import {
  createProgramOrder,
  programCaseIdOf,
} from "../../../helpers/program-order";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The immunohistochemistry and cytology case views fit the page at desktop
 * widths: neither scrolls sideways, with the prior results panel shown or
 * taken out of the flow.
 *
 * What only a deployed stack proves here: the views' order band, the shared
 * styles it carries and the Carbon grid under it are laid out by a real
 * browser, at each width a lab screen is used at.
 *
 * Every run orders its own patient: one pathology case, then a cytology case
 * and an immunohistochemistry case for the same patient, so each view lists
 * an earlier case in its panel.
 */

const WIDTHS = [1480, 1366, 1280];

const VIEW_HEIGHT = 900;

interface SeededCase {
  id: string;
  labNo: string;
}

let cytology: SeededCase;
let immunohistochemistry: SeededCase;

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
  IMMUNOHISTOCHEMISTRY: "/ImmunohistochemistryCaseView/",
  CYTOLOGY: "/CytologyCaseView/",
} as const;

type Bench = keyof typeof VIEW_ROUTES;

const VIEW_NAMES: Record<Bench, string> = {
  IMMUNOHISTOCHEMISTRY: "the immunohistochemistry view",
  CYTOLOGY: "the cytology view",
};

function rowsOf(page: Page): Locator {
  return page
    .getByTestId("prior-results")
    .locator('tbody tr[data-testid^="prior-result-"]');
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

/**
 * Opens the view at each width and measures it once the case has loaded (its
 * lab number is in the header) and the panel lists the patient's earlier
 * case, so the reading with the panel is taken with the panel there.
 */
async function readingsAtEachWidth(
  page: Page,
  bench: Bench,
  seeded: SeededCase,
) {
  const readings = [];
  for (const width of WIDTHS) {
    await page.setViewportSize({ width, height: VIEW_HEIGHT });
    await page.goto(`${VIEW_ROUTES[bench]}${seeded.id}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.locator("strong", { hasText: seeded.labNo }).first(),
      `${VIEW_NAMES[bench]} at ${width} shows the case's lab number`,
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(
      rowsOf(page).first(),
      `${VIEW_NAMES[bench]} at ${width} lists the patient's earlier case`,
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    readings.push({ width, ...(await sidewaysScroll(page)) });
  }
  return readings;
}

test.describe.serial("The IHC and cytology case views fit the page", () => {
  // The case views are heavy screens, and a first visit on a development
  // server compiles them as it serves them.
  test.setTimeout(NAV_TIMEOUT * 3);

  test.beforeAll(async ({ browser }) => {
    // Three orders, all through the API.
    test.setTimeout(NAV_TIMEOUT * 6);
    await withOwnPage(browser, async (page) => {
      const target = await discoverPathologyOrderTarget(page);
      const first = await createPathologyCase(page, target, {});
      cytology = await benchCase(
        page,
        target,
        "cytology",
        first.pathologySampleId,
      );
      immunohistochemistry = await benchCase(
        page,
        target,
        "immunohistochemistry",
        first.pathologySampleId,
      );
    });
  });

  test("the cytology view does not scroll sideways at 1480, 1366 and 1280, with and without the prior results panel", async ({
    page,
  }) => {
    const readings = await readingsAtEachWidth(page, "CYTOLOGY", cytology);
    // Soft, so one run reports every width and both panel states.
    for (const { width, withPanel, withoutPanel } of readings) {
      expect
        .soft(
          withPanel,
          `the cytology view at ${width} with the panel scrolls sideways by ${withPanel}px`,
        )
        .toBe(0);
      expect
        .soft(
          withoutPanel,
          `the cytology view at ${width} without the panel scrolls sideways by ${withoutPanel}px`,
        )
        .toBe(0);
    }
  });

  test("the immunohistochemistry view does not scroll sideways at 1480, 1366 and 1280, with and without the prior results panel", async ({
    page,
  }) => {
    const readings = await readingsAtEachWidth(
      page,
      "IMMUNOHISTOCHEMISTRY",
      immunohistochemistry,
    );
    // Soft, so one run reports every width and both panel states.
    for (const { width, withPanel, withoutPanel } of readings) {
      expect
        .soft(
          withPanel,
          `the immunohistochemistry view at ${width} with the panel scrolls sideways by ${withPanel}px`,
        )
        .toBe(0);
      expect
        .soft(
          withoutPanel,
          `the immunohistochemistry view at ${width} without the panel scrolls sideways by ${withoutPanel}px`,
        )
        .toBe(0);
    }
  });
});
