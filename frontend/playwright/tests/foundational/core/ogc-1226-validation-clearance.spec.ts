import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { createEsigSampleOrder } from "../../../helpers/seed-esig-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1226: the Validation queue's Clear lane holds ordinary in-range results
 * (a patient result carries no QC evaluation, and that is not a risk), the
 * validator releases them in one action, and when the bulk action cannot be
 * used the queue says why next to the button.
 *
 * Each case orders the default seeded test (WBC, reference range 4 - 10 for the
 * seeded adult), enters a result at the bench and reads the queue for that
 * accession only, so runs never see each other's rows.
 */

const password = process.env.TEST_PASS || "adminADMIN!";

const enterResult = async (
  page: Page,
  accessionNumber: string,
  value: string,
) => {
  await page.goto(
    `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
    { waitUntil: "domcontentloaded" },
  );
  const main = page.getByRole("main");
  await expect(
    main.locator("tr", { hasText: accessionNumber }).first(),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  const input = main.locator('input[id^="unifiedResultValue-"]').first();
  await expect(input).toBeVisible({ timeout: UI_TIMEOUT });
  await input.fill(value);
  await main
    .getByRole("button", { name: /^save$/i })
    .first()
    .click();
  await signIfAsked(page);
  await expect(
    main.getByRole("button", { name: /^edit$/i }).first(),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
};

/** Whether the site currently requires an electronic signature. */
const esigEnabled = async (page: Page) => {
  const response = await page.request.get(
    "/api/OpenELIS-Global/rest/esig/enabled",
  );
  if (!response.ok()) {
    return true;
  }
  const body = (await response.json()) as { enabled?: boolean };
  return body.enabled === true;
};

/** Completes the e-signature ceremony when the site has it switched on. */
const signIfAsked = async (page: Page) => {
  if (!(await esigEnabled(page))) {
    return;
  }
  const dialog = page.getByRole("dialog");
  const passwordInput = dialog.locator('input[type="password"]');
  await expect(passwordInput).toBeVisible({ timeout: UI_TIMEOUT });
  const acknowledgement = dialog.locator(
    'label[for="certification-acknowledgement"]',
  );
  if (await acknowledgement.isVisible()) {
    await acknowledgement.click();
    await passwordInput.fill(password);
    await dialog.getByRole("button", { name: /certify|continue/i }).click();
    await expect(passwordInput).toBeVisible({ timeout: UI_TIMEOUT });
  }
  await passwordInput.fill(password);
  await dialog.getByRole("button", { name: /^sign/i }).click();
  await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
};

/**
 * Success toasts dismiss themselves after two seconds, so a visibility poll
 * that starts after a slower step can miss them. This records every
 * notification the page renders from now on; the test asserts on the record.
 */
const recordNotifications = (page: Page) =>
  page.evaluate(() => {
    const store = window as unknown as { __notifications?: string[] };
    store.__notifications = [];
    const seen = new Set<string>();
    const collect = () => {
      document
        .querySelectorAll<HTMLElement>(
          ".cds--toast-notification, .cds--inline-notification, [role='status']",
        )
        .forEach((element) => {
          const text = (element.innerText || "").trim();
          if (text && !seen.has(text)) {
            seen.add(text);
            store.__notifications!.push(text);
          }
        });
    };
    new MutationObserver(collect).observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true,
    });
    collect();
  });

const recordedNotifications = (page: Page) =>
  page.evaluate(
    () =>
      (window as unknown as { __notifications?: string[] }).__notifications ||
      [],
  );

const openQueueFor = async (page: Page, accessionNumber: string) => {
  await page.goto("/validation?type=order", { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  const search = main.getByPlaceholder(/accession|lab no/i);
  await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });
  await search.fill(accessionNumber);
  const loaded = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/AccessionValidation") &&
      response.request().method() === "GET",
    { timeout: LONG_TIMEOUT },
  );
  await main.getByRole("button", { name: /search/i }).click();
  await loaded;
  await expect(page.getByTestId("release-all-clear")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
};

test.describe("Validation clearance rule (OGC-1226)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("an ordinary in-range result sits in the Clear lane and releases in one action", async ({
    page,
  }) => {
    const order = await createEsigSampleOrder(page);
    expect(order, "a sample order is needed").toBeTruthy();
    const accession = order!.accessionNumber;

    await enterResult(page, accession, "6");
    await openQueueFor(page, accession);

    const summary = page.getByTestId("validation-lane-summary");
    await expect(summary).toContainText("Clear: 1");
    await expect(summary).toContainText("Needs review: 0");
    const release = page.getByTestId("release-all-clear");
    await expect(release).toHaveText("Release all clear (1)");
    await expect(release).toBeEnabled();
    await expect(page.getByTestId("release-all-clear-why")).toHaveCount(0);

    // The review panel shows no QC line for a result that has no QC verdict.
    await page
      .getByRole("main")
      .getByRole("button", { name: /expand row/i })
      .first()
      .click();
    const panel = page.locator('[data-testid^="validation-review-panel-"]');
    await expect(panel).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(panel.getByTestId("review-qc")).toHaveCount(0);

    await release.click();
    const modal = page.getByTestId("release-all-clear-modal");
    await expect(modal).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(modal.getByTestId("release-all-clear-scope")).toContainText(
      "does not confirm that quality control was performed",
    );
    await expect(modal).toContainText("no recorded QC failure");

    const reread = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/AccessionValidation") &&
        response.request().method() === "GET",
      { timeout: LONG_TIMEOUT },
    );
    await recordNotifications(page);
    await modal
      .getByTestId("release-all-clear-sign")
      .getByRole("button")
      .click();
    await signIfAsked(page);
    await reread;

    await expect(
      page.getByTestId("release-all-clear-why-queueEmpty"),
    ).toContainText("Nothing is waiting for validation.", {
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByTestId("release-all-clear")).toBeDisabled();
    expect((await recordedNotifications(page)).join("\n")).toContain(
      "1 clear result(s) released.",
    );
  });

  test("an abnormal result is held, and the queue says why the bulk release is unavailable", async ({
    page,
  }) => {
    const order = await createEsigSampleOrder(page);
    expect(order, "a sample order is needed").toBeTruthy();
    const accession = order!.accessionNumber;

    await enterResult(page, accession, "25");
    await openQueueFor(page, accession);

    await expect(page.getByTestId("validation-lane-summary")).toContainText(
      "Needs review: 1",
    );
    const release = page.getByTestId("release-all-clear");
    await expect(release).toHaveText("Release all clear (0)");
    await expect(release).toBeDisabled();
    const why = page.getByTestId("release-all-clear-why-signals");
    await expect(why).toContainText(
      "1 results in this queue carry something to check before release.",
    );
    await expect(why).toContainText("Most common: Abnormal.");

    // The held row is still releasable one at a time, with a confirmation.
    await page
      .getByTestId(/^review-row-/)
      .first()
      .click();
    const panel = page.locator('[data-testid^="validation-review-panel-"]');
    await expect(panel).toBeVisible({ timeout: UI_TIMEOUT });
    const reread = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/AccessionValidation") &&
        response.request().method() === "GET",
      { timeout: LONG_TIMEOUT },
    );
    await recordNotifications(page);
    await panel.getByRole("button", { name: /validate & release/i }).click();
    await signIfAsked(page);
    await reread;
    await expect(
      page.getByTestId("release-all-clear-why-queueEmpty"),
    ).toContainText("Nothing is waiting for validation.", {
      timeout: LONG_TIMEOUT,
    });
    expect((await recordedNotifications(page)).join("\n")).toContain(
      "Result validated and released.",
    );
  });
});
