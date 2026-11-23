import { test, expect } from "../../../helpers/test-base";
import {
  UI_TIMEOUT,
  LONG_TIMEOUT,
  NAV_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  seedProviderScheme,
  firstTestWithAnalyte,
  ProviderSchemeSeed,
  PROVIDER_PARTICIPANT_COUNT,
} from "../../../helpers/seed-eqa-data";
import { pickCalendarDay } from "../../../helpers/carbon-date-picker";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1665: the provider's follow-up register shows the target a qualitative
 * result was judged against.
 *
 * The register printed a dash in the Target column for every qualitative row,
 * while the participant's Performance Report printed the sealed target. Here a
 * qualitative panel goes through the whole provider lane in the UI: the wizard
 * seals two samples of one test (R and NR), the five panels ship and arrive,
 * each laboratory's answers are keyed in the intake dialog, and scoring puts
 * the one laboratory that answered NR to the R sample on the register. Its row
 * must show R as the target. Prep counts two samples for five participants
 * plus five in reserve. The register row nests its results table, so the
 * result row is found as the matching row that holds no table of its own.
 */

const RUN = Date.now().toString(36);
const N = PROVIDER_PARTICIPANT_COUNT;
const API = "/api/OpenELIS-Global/rest";
const REACTIVE_SAMPLE = `Q-${RUN}-1`;
const NON_REACTIVE_SAMPLE = `Q-${RUN}-2`;

let seed: ProviderSchemeSeed;

test.describe("EQA follow-up register qualitative target (OGC-1665)", () => {
  test.beforeAll(() => {
    seed = seedProviderScheme(`${RUN}q`);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("a qualitative failure shows the target it was judged against", async ({
    page,
  }) => {
    test.setTimeout(600_000);
    let cycleId = "";
    const banner = (state: string) =>
      expect(page.getByText(state, { exact: true }).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });

    await test.step("the scheme runs a testable test that reports an analyte", async () => {
      const headers = { "X-CSRF-Token": await csrfToken(page) };
      const testable = await page.request.get(`${API}/eqa/testable-tests`, {
        headers,
      });
      expect(testable.ok(), `testable-tests: ${testable.status()}`).toBe(true);
      const testId = firstTestWithAnalyte(
        ((await testable.json()) as unknown[]).map(String),
      );
      const assigned = await page.request.put(
        `${API}/eqa/programs/${seed.programId}/tests`,
        { headers, data: { testIds: [Number(testId)] } },
      );
      expect(assigned.ok(), `assign test: ${assigned.status()}`).toBe(true);
    });

    await test.step("the wizard seals two qualitative samples of the test", async () => {
      await page.goto(`/qa/eqa/provider/schemes/${seed.programId}/cycles/new`, {
        timeout: NAV_TIMEOUT,
      });
      await expect(page.locator("#cycle-name")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.locator("#cycle-name").fill(`E2E ${RUN} qualitative cycle`);
      await page.locator("#cycle-number").fill("1");
      await page.locator("#cycle-planned-start").click();
      await pickCalendarDay(page, 1, 1);
      await page.locator("#cycle-planned-end").click();
      await pickCalendarDay(page, 15, 1);
      await expect(page.locator("#cycle-planned-end")).not.toHaveValue("");
      await page.getByRole("button", { name: "Next", exact: true }).click();

      await page.locator("#panel-name").fill(`E2E ${RUN} qualitative panel`);
      await page.getByRole("button", { name: "Add sample" }).click();
      await page.locator("#sample-code-0").fill(REACTIVE_SAMPLE);
      await page.locator("select#sample-test-0").selectOption({ index: 1 });
      await page.locator("#sample-target-0").fill("R");
      await page.locator("#sample-code-1").fill(NON_REACTIVE_SAMPLE);
      await page.locator("select#sample-test-1").selectOption({ index: 1 });
      await page.locator("#sample-target-1").fill("NR");
      await page.getByRole("button", { name: "Next", exact: true }).click();

      await expect(
        page.getByText("Participating laboratories").first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await page.locator('label[for="method-CSV"]').click();
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await page
        .getByRole("button", { name: "Create cycle and begin prep" })
        .click();
      await expect(page).toHaveURL(
        /\/qa\/eqa\/provider\/cycles\/\d+\/workbench/,
        { timeout: LONG_TIMEOUT },
      );
      cycleId = page.url().match(/cycles\/(\d+)\/workbench/)?.[1] ?? "";
      expect(cycleId).not.toBe("");
    });

    await test.step("prep clears the gate and every panel ships", async () => {
      await banner("Prep in progress");
      await page.locator('input[id^="produced-"]').fill("15");
      await page.locator('input[id^="reserved-"]').fill("5");
      await page.locator('label[for^="qc-"]').first().click();
      await page.getByRole("button", { name: "Save prep record" }).click();
      await expect(page.getByText("All prep requirements met")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page
        .getByRole("button", { name: "Mark cycle ready to ship" })
        .click();
      await banner("Ready to ship");

      await page.getByRole("tab", { name: "Shipments" }).click();
      const shipmentRows = page.locator("tr", {
        has: page.locator('input[id^="courier-"]'),
      });
      await expect(shipmentRows).toHaveCount(N, { timeout: UI_TIMEOUT });
      for (let i = 0; i < N; i++) {
        const shipmentRow = shipmentRows.nth(i);
        await shipmentRow.locator('input[id^="courier-"]').fill("E2E courier");
        await shipmentRow
          .getByRole("button", { name: "Save", exact: true })
          .click();
        await expect(shipmentRow.getByText("READY TO SEND")).toBeVisible({
          timeout: UI_TIMEOUT,
        });
      }
      await page.locator('label[for="select-all-shipments"]').click();
      await page.getByRole("button", { name: `Mark ${N} shipped` }).click();
      await expect(
        page.getByText(`${N} participant shipments dispatched.`),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("every delivery is received and submissions open", async () => {
      await page.reload({ timeout: NAV_TIMEOUT });
      await expect(page.getByRole("tab", { name: "Prep" })).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("tab", { name: "Receipts & scoring" }).click();
      for (const name of seed.organizationNames) {
        const receiptRow = page.locator("tr", { hasText: name });
        await receiptRow.getByRole("button", { name: "Mark received" }).click();
        await expect(
          receiptRow.getByText("Delivered", { exact: true }),
        ).toBeVisible({ timeout: UI_TIMEOUT });
      }
      await banner("Submissions open");
    });

    await test.step("each laboratory's answers are keyed in the intake dialog", async () => {
      for (const [index, name] of seed.organizationNames.entries()) {
        const answers: [string, string][] = [
          [REACTIVE_SAMPLE, index === 0 ? "NR" : "R"],
          [NON_REACTIVE_SAMPLE, "NR"],
        ];
        await page
          .locator("tr", { hasText: name })
          .getByRole("button", { name: "Enter results" })
          .click();
        const dialog = page.getByRole("dialog", {
          name: `Results from ${name}`,
        });
        await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
        for (const [sample, answer] of answers) {
          await dialog
            .getByRole("textbox", { name: new RegExp(`${sample}$`) })
            .fill(answer);
        }
        await dialog.getByRole("button", { name: "Save results" }).click();
        await expect(
          page.getByText(`Results recorded for ${name}.`).first(),
        ).toBeVisible({ timeout: UI_TIMEOUT });
        await expect(dialog).toBeHidden({ timeout: UI_TIMEOUT });
      }
    });

    await test.step("scoring puts the failing laboratory on the register", async () => {
      await page.getByRole("button", { name: "Score cycle" }).click();
      await expect(
        page.getByText(
          "Cycle scored. Unacceptable participants are in the follow-up register.",
        ),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await banner("Scored");
      await expect(
        page
          .locator("tr", { hasText: seed.organizationNames[0] })
          .getByText("1 unacceptable of 2"),
      ).toBeVisible();
    });

    await test.step("the register row shows R as the target, as the report does", async () => {
      await page.getByRole("link", { name: "Follow-up register" }).click();
      await expect(
        page.getByRole("heading", { name: "Participant follow-up" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const registerRow = page.locator("tr", {
        hasText: seed.organizationNames[0],
      });
      await registerRow.getByRole("button", { name: "Triage" }).click();
      const resultRow = page
        .locator("tr", { hasText: `Sample ${REACTIVE_SAMPLE}` })
        .filter({ hasNot: page.locator("table") });
      await expect(resultRow).toBeVisible({ timeout: UI_TIMEOUT });
      const cells = resultRow.getByRole("cell");
      await expect(cells.nth(1)).toHaveText("NR");
      await expect(cells.nth(2)).toHaveText("R");
    });
  });
});
