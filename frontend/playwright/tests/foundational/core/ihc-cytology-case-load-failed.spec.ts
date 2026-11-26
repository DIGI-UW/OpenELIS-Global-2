import type { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  discoverPathologyOrderTarget,
  type PathologyOrderTarget,
} from "../../../helpers/seed-pathology-data";
import {
  createProgramOrder,
  programCaseIdOf,
} from "../../../helpers/program-order";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The immunohistochemistry and cytology case views show one notice, and no
 * form, when the case cannot be read; a case that can be read opens as before.
 *
 * What only a deployed stack proves here: the server's answer for a case id
 * that does not exist reaches the view through the real request helper, and
 * the page throws nothing while it loads.
 */

// Repeated once from en.json (caseView.error.loadFailed).
const LOAD_FAILED_NOTICE =
  "This case could not be opened. Go back to the dashboard and open it from the list.";

const UNKNOWN_CASE_ID = "424242";

const VIEW_ROUTES = {
  cytology: "/CytologyCaseView/",
  immunohistochemistry: "/ImmunohistochemistryCaseView/",
} as const;

type Bench = keyof typeof VIEW_ROUTES;

const BENCHES: Bench[] = ["cytology", "immunohistochemistry"];

interface SeededCase {
  id: string;
  labNo: string;
}

const seeded: Partial<Record<Bench, SeededCase>> = {};

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

async function orderCase(
  page: Page,
  target: PathologyOrderTarget,
  bench: Bench,
): Promise<SeededCase> {
  const { labNo } = await createProgramOrder(page, {
    ...(bench === "cytology"
      ? { program: "Cytology", sampleType: "Fluid" }
      : {
          program: "Immunohistochemistry",
          sampleType: "Immunohistochemistry specimen",
        }),
    // A patient name takes letters only; the helper makes each patient's
    // national id unique, so the name need not be.
    patient: { firstName: "Loadfailed", lastName: bench },
    providerId: target.providerId,
    providerPersonId: target.providerPersonId,
    referringSiteId: target.referringSiteId,
  });
  return { id: await programCaseIdOf(page, bench, labNo), labNo };
}

function saveButtons(page: Page) {
  return page.getByRole("button", { name: "Save", exact: true });
}

/**
 * Opens the view on a case id that does not exist and hands back every error
 * the page threw while it loaded.
 */
async function openUnknownCase(page: Page, bench: Bench): Promise<string[]> {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  await page.goto(`${VIEW_ROUTES[bench]}${UNKNOWN_CASE_ID}`, {
    waitUntil: "domcontentloaded",
  });
  return pageErrors;
}

// No case is ordered here, so the claim does not wait on any ordering.
test.describe("The IHC and cytology case views when the case cannot be read", () => {
  // The case views are heavy screens, and a first visit on a development
  // server compiles them as it serves them.
  test.setTimeout(NAV_TIMEOUT * 3);

  for (const bench of BENCHES) {
    test(`${bench}_unknownCase_showsTheNoticeAndNoSave`, async ({ page }) => {
      const pageErrors = await openUnknownCase(page, bench);

      await expect(page.getByText(LOAD_FAILED_NOTICE)).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await expect(saveButtons(page)).toHaveCount(0);
      expect(
        pageErrors,
        `the ${bench} view threw nothing while it loaded`,
      ).toEqual([]);
    });
  }
});

test.describe
  .serial("The IHC and cytology case views when the case can be read", () => {
  test.setTimeout(NAV_TIMEOUT * 3);

  test.beforeAll(async ({ browser }) => {
    // Two orders, both through the API.
    test.setTimeout(NAV_TIMEOUT * 4);
    await withOwnPage(browser, async (page) => {
      const target = await discoverPathologyOrderTarget(page);
      for (const bench of BENCHES) {
        seeded[bench] = await orderCase(page, target, bench);
      }
    });
  });

  for (const bench of BENCHES) {
    test(`${bench}_realCase_showsTheFormAndNoNotice`, async ({ page }) => {
      const own = seeded[bench];
      expect(own, `a ${bench} case was ordered`).toBeDefined();
      await page.goto(`${VIEW_ROUTES[bench]}${own!.id}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.locator("strong", { hasText: own!.labNo }).first(),
        `the ${bench} view shows the case's lab number`,
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(saveButtons(page).first()).toBeVisible();
      await expect(page.getByText(LOAD_FAILED_NOTICE)).toHaveCount(0);
    });
  }
});
