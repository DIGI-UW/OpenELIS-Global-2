import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
  NAV_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  seedParticipantCycle,
  ParticipantCycleSeed,
} from "../../../helpers/seed-eqa-data";
import { enterResults, validateResults } from "../../../helpers/seed-tat-data";

/**
 * EQA participant lane smoke (OGC-613).
 *
 * The lab is enrolled in a scheme with a PLANNED cycle (seeded). The spec
 * drives the participant-facing surfaces end to end:
 *
 *   My Cycles (Planned) → Add Order with the EQA box (programme, cycle,
 *   panel receipt — flips the cycle to Panel received in the same
 *   transaction) → results entered + validated (REST, mirroring the UI save;
 *   the result-entry UI itself is covered by other foundational specs) →
 *   live progress 1 / 1 → walk to READY_TO_SUBMIT through the real
 *   transition endpoint → "Review & submit" in the UI → Submitted.
 *
 * Why the two REST transitions: the PANEL_RECEIVED → TESTING →
 * READY_TO_SUBMIT sweep runs on a 5-minute scheduler
 * (EQADeadlineAlertScheduler), which no spec can wait for. The sweep itself
 * is integration-tested; what only a browser can prove is that My Cycles
 * renders each state and that Review & submit works — so the spec drives
 * exactly those.
 */

const RUN = Date.now().toString(36);
const API = "/api/OpenELIS-Global";

let seed: ParticipantCycleSeed;

/** PARTICIPANT-machine transition through the real endpoint — the same call
 * cyclesApi.js makes, minus the button the scheduler normally stands in for. */
async function transition(
  page: Page,
  cycleId: string,
  newState: string,
  reason: string,
): Promise<void> {
  const res = await page.evaluate(
    async ({ url, body }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const r = await fetch(url, {
        method: "PATCH",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          "X-CSRF-Token": csrf,
        },
        body: JSON.stringify(body),
      });
      return { status: r.status, text: await r.text().catch(() => "") };
    },
    {
      url: `${API}/rest/eqa/cycles/${cycleId}/transition`,
      body: { newState, stateMachine: "PARTICIPANT", reason },
    },
  );
  if (res.status < 200 || res.status >= 300) {
    throw new Error(
      `transition(${newState}) failed: HTTP ${res.status}: ${res.text.substring(0, 300)}`,
    );
  }
}

test.describe("EQA participant lane", () => {
  test.beforeAll(() => {
    seed = seedParticipantCycle(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("a planned cycle is received via Add Order, tested, and submitted", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const row = () => page.getByTestId(`cycle-row-${seed.cycleId}`);
    let accession = "";

    await test.step("My Cycles shows the planned cycle", async () => {
      await page.goto("/qa/eqa/my-cycles", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "My EQA Cycles" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      for (const tile of [
        "kpi-active",
        "kpi-ready",
        "kpi-awaiting",
        "kpi-nce",
      ]) {
        await expect(page.getByTestId(tile)).toBeVisible();
      }
      await expect(row()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row().getByText("Planned")).toBeVisible();

      await row().click();
      const expanded = page.getByTestId(`cycle-expanded-${seed.cycleId}`);
      await expect(
        expanded.getByRole("button", {
          name: "Receive panel — open Add Order",
        }),
      ).toBeVisible();
      await expanded
        .getByRole("button", { name: "Receive panel — open Add Order" })
        .click();
    });

    await test.step("Add Order records the panel receipt", async () => {
      await expect(page).toHaveURL(/SamplePatientEntry\?isEQA=true/, {
        timeout: UI_TIMEOUT,
      });
      // The EQA box is pre-armed by ?isEQA=true and loads the EQA placeholder
      // patient, so the patient step needs no input.
      await expect(page.locator("#eqa-sample-checkbox")).toBeChecked({
        timeout: UI_TIMEOUT,
      });
      await page.getByRole("button", { name: "Next", exact: true }).click();

      await expect(
        page.getByRole("heading", { name: "EQA Sample Information" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page
        .locator("select#eqa-program")
        .selectOption({ label: seed.programName });
      await page
        .locator("select#eqa-cycle")
        .selectOption({ label: seed.cycleName });
      await page.locator("#eqa-provider-sample-id").fill(`PS-${RUN}`);
      // Choosing programme + cycle reveals the Panel Receipt block.
      await expect(
        page.getByRole("heading", { name: "Panel Receipt" }),
      ).toBeVisible();
      await page.locator("#eqa-received-temp").fill("22");
      await page.getByRole("button", { name: "Next", exact: true }).click();

      // Sample step: a sample type that offers tests, first test on offer —
      // which test it is does not matter, only that the order carries one
      // analysis. Not every fixture type has tests (Skin has none), so probe
      // the common ones until checkboxes render.
      const sampleType = page.locator("select#sampleId_0");
      await expect(sampleType).toBeVisible({ timeout: UI_TIMEOUT });
      const firstTest = page.locator('label[for^="test_0_"]').first();
      let testsOffered = false;
      for (const label of ["Serum", "Plasma", "Whole Blood", "Urine"]) {
        await sampleType.selectOption({ label });
        try {
          await expect(firstTest).toBeVisible({ timeout: SHORT_TIMEOUT });
          testsOffered = true;
          break;
        } catch {
          // this type offers no tests on this deployment — try the next
        }
      }
      if (!testsOffered) {
        throw new Error("No probed sample type offers an orderable test");
      }
      await firstTest.click();
      await page.getByRole("button", { name: "Next", exact: true }).click();

      const labNo = page.locator("input#labNo");
      await expect(labNo).toBeVisible({ timeout: UI_TIMEOUT });
      await page.locator("[data-cy='generate-labNumber']").click();
      await expect(labNo).not.toHaveValue("", { timeout: UI_TIMEOUT });
      accession = (await labNo.inputValue()).trim();

      await page.getByRole("button", { name: "Submit", exact: true }).click();
      await expect(page.locator(".orderEntrySuccessMsg")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("receipt flipped the cycle to Panel received", async () => {
      await page.goto("/qa/eqa/my-cycles", { timeout: NAV_TIMEOUT });
      await expect(row()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row().getByText("Panel received")).toBeVisible();
      await expect(row().getByText("0 / 1")).toBeVisible();
    });

    await test.step("validated result shows as live progress", async () => {
      await enterResults(page, accession);
      await validateResults(page, accession);
      await page.reload({ timeout: NAV_TIMEOUT });
      await expect(row()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row().getByText("1 / 1")).toBeVisible();
      await row().click();
      await expect(
        page
          .getByTestId(`cycle-expanded-${seed.cycleId}`)
          .getByText("Entered", { exact: true }),
      ).toBeVisible();
    });

    await test.step("Review & submit moves the cycle to Submitted", async () => {
      await transition(
        page,
        seed.cycleId,
        "TESTING",
        `E2E ${RUN}: testing started (stands in for the 5-minute sweep)`,
      );
      await transition(
        page,
        seed.cycleId,
        "READY_TO_SUBMIT",
        `E2E ${RUN}: all results validated (stands in for the 5-minute sweep)`,
      );
      await page.reload({ timeout: NAV_TIMEOUT });
      await expect(row()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row().getByText("Ready to submit")).toBeVisible();

      await row().click();
      const expanded = page.getByTestId(`cycle-expanded-${seed.cycleId}`);
      await expect(expanded.getByText("Pre-submission summary")).toBeVisible();
      await expanded.getByRole("button", { name: "Review & submit" }).click();
      await expect(
        page
          .getByText("Cycle submitted to provider — awaiting scores.")
          .first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      // A submitted cycle leaves the default Active bucket — flip the filter
      // to see it land in Awaiting scores.
      await page
        .locator("select#cycle-bucket-filter")
        .selectOption({ value: "awaiting" });
      await expect(row().getByText("Submitted", { exact: true })).toBeVisible({
        timeout: SHORT_TIMEOUT,
      });
    });

    await test.step("the EQA orders register lists the order by lab number", async () => {
      // The orders page is the participant's flat view of every EQA sample,
      // keyed on the accession this spec created rather than on the cycle.
      await page.goto("/qa/eqa/orders", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "EQA Orders" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      const orderRow = page.locator("tr", { hasText: accession });
      await expect(orderRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(orderRow.getByText(seed.programName)).toBeVisible();
      // Every analysis is finalized by now, so the derived status is complete.
      await expect(orderRow.getByText("Completed")).toBeVisible();
    });
  });
});
