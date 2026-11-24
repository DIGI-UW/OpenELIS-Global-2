import { randomInt, randomUUID } from "node:crypto";
import type { Page } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { worklistFor } from "../../../helpers/analyzer-api";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { geneXpertMockSource } from "../../../helpers/analyzer-native-traffic";
import { activateShippedAnalyzer } from "../../../helpers/analyzer-setup-flow";
import {
  numeric,
  publish,
  send,
  type Draft,
} from "../../../helpers/analyzer-profile-api";
import { signIfAsked } from "../../../helpers/results-nce-ui";

/**
 * OGC-1200 slice 7 (FR-E4): a run lists what it cannot accept until someone
 * resolves it, in its Exceptions block. An unordered test is ignored with a
 * reason, an unmapped code opens the analyzer's mapping and is held with a
 * reason, and a value whose lab number names no order is matched to the order
 * the technician finds, then accepted there like any other value.
 */

const UI_TIMEOUT = 20_000;

async function sendMessage(
  page: Page,
  destination: string,
  sender: string,
  sampleId: string,
  results: Array<{ test_code: string; value: string }>,
) {
  const received = await page.request.post(
    `${process.env.MOCK_SIMULATOR_URL || "http://localhost:8085"}/simulate/astm/mindray_ba88a`,
    {
      data: {
        destination,
        sender_id: sender,
        sample_id: sampleId,
        completed_at: "20261124120000",
        results: results.map((result) => ({
          ...result,
          type: "TEXT",
          unit: "mmol/L",
        })),
      },
    },
  );
  expect(received.ok(), await received.text()).toBeTruthy();
}

async function runIdOf(page: Page, analyzerName: string, except: string[]) {
  const runs = await page.evaluate(async () => {
    const response = await fetch("/api/OpenELIS-Global/rest/result-runs", {
      credentials: "include",
    });
    return (await response.json()).runs as Array<{
      id: number;
      analyzerName: string;
    }>;
  });
  const run = runs.find(
    (candidate) =>
      candidate.analyzerName === analyzerName &&
      !except.includes(String(candidate.id)),
  );
  expect(run, `a run of ${analyzerName}`).toBeTruthy();
  return String(run!.id);
}

test(
  "a run's exceptions are matched, mapped, held or ignored on its header",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(300_000);
    const run = randomUUID().slice(0, 8);
    const sender = `R1200E-${run}`;
    const unmappedCode = `RXMAP${run.slice(0, 4).toUpperCase()}`;
    const unknownLabNumber = `DEV0199${String(randomInt(0, 10_000_000)).padStart(7, "0")}${String(randomInt(0, 1_000_000)).padStart(6, "0")}`;
    const values = [
      { name: `Run exception ordered ${run}`, code: "RXORD" },
      { name: `Run exception unordered ${run}`, code: "RXUNORD" },
    ].map((entry) => ({
      ...entry,
      loinc: `${randomInt(1_000_000, 9_000_000)}-${randomInt(0, 9)}`,
    }));
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const [ordered, unordered] = await seedNumericTests(page, run, values, {
      testSection: "Molecular Biology",
      sampleType: "Plasma",
    });

    const name = `Run exceptions ${run}`;
    const draft: Draft = await send(
      page,
      "post",
      "/analyzer-types/cepheid-genexpert-astm/duplicate",
      { sourceRevision: 1, displayName: name },
    );
    const defaults = draft.profile.configDefaults as {
      extractionOverrides: { resultParts: Record<string, string> };
    };
    defaults.extractionOverrides.resultParts.number = "R.4.1";
    delete defaults.extractionOverrides.resultParts.call;
    await publish(
      page,
      draft,
      values.map(({ code, loinc }) => numeric(code, loinc)),
    );
    const analyzer = await activateShippedAnalyzer(
      page,
      {
        profileId: draft.profile.profileMeta.id,
        revision: 1,
        displayName: name,
        labUnit: "Molecular Biology",
      },
      name,
      { senderId: sender },
    );
    const source = await geneXpertMockSource(page.request);
    const onlyOrdered = await createClinicalOrder(page, {
      testIds: [ordered.id],
      specimenName: "Plasma",
    });
    const target = await createClinicalOrder(page, {
      testIds: [ordered.id],
      specimenName: "Plasma",
    });

    await sendMessage(page, source.destination, sender, onlyOrdered.accession, [
      { test_code: "RXORD", value: "50" },
      { test_code: "RXUNORD", value: "60" },
      { test_code: unmappedCode, value: "7" },
    ]);
    await expect
      .poll(
        async () =>
          (await worklistFor(page, analyzer.id, onlyOrdered.accession)).length,
      )
      .toBe(3);
    await sendMessage(page, source.destination, sender, unknownLabNumber, [
      { test_code: "RXORD", value: "55" },
    ]);
    await expect
      .poll(
        async () =>
          (await worklistFor(page, analyzer.id, unknownLabNumber)).length,
      )
      .toBe(1);

    let firstRun = "";
    await test.step("The run lists the unordered test and the unmapped code", async () => {
      await page.goto(`/Results?accessionNumber=${onlyOrdered.accession}`);
      await page
        .getByText(/^Waiting in RUN-\d+$/)
        .first()
        .click();
      await expect(page).toHaveURL(/\/Results\?run=\d+$/);
      firstRun = new URL(page.url()).searchParams.get("run") || "";
      const block = page.getByTestId("run-exceptions");
      await expect(block).toContainText("Exceptions (2)");
      await expect(block).toContainText(
        `${unordered.name} is not ordered on lab number ${onlyOrdered.accession} (1 value)`,
      );
      await expect(block).toContainText(
        `Analyzer code ${unmappedCode} has no mapped test (1 value)`,
      );
      await expect(
        page.getByTestId("run-rows").getByText("Unordered test"),
      ).toBeVisible();
    });

    await test.step("Map now opens the analyzer's mapping on the code", async () => {
      const mapNow = page
        .getByTestId("run-exceptions")
        .getByRole("link", { name: "Map now" });
      await expect(mapNow).toHaveAttribute(
        "href",
        new RegExp(
          `/analyzers/${analyzer.id}/mapping\\?returnTo=%2FResults%3Frun%3D${firstRun}&focusTest=${unmappedCode}`,
        ),
      );
      await mapNow.click();
      await expect(page).toHaveURL(
        new RegExp(`/analyzers/${analyzer.id}/mapping\\?`),
      );
      await expect(page.getByText(unmappedCode).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page.goto(`/Results?run=${firstRun}`);
    });

    await test.step("The unordered test is ignored with a reason, and only it leaves", async () => {
      const block = page.getByTestId("run-exceptions");
      const line = block
        .locator("li")
        .filter({ hasText: "is not ordered on lab number" });
      await line.getByRole("button", { name: "Ignore" }).click();
      await line.getByLabel(/Reason/).fill("Run by mistake on this tube");
      await line.getByRole("button", { name: "Ignore" }).click();
      await expect(
        page.getByText(
          "Ignored with your reason. Only these values are affected.",
        ),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(block).toContainText("Exceptions (1)");
      await expect(
        page
          .getByTestId("run-rows")
          .locator('> tbody > tr[data-testid^="run-row-"]'),
      ).toHaveCount(2);
    });

    await test.step("The unmapped code is held with a reason the run shows", async () => {
      const line = page.getByTestId("run-exceptions").locator("li").first();
      await line.getByRole("button", { name: "Hold" }).click();
      await line
        .getByLabel(/Reason/)
        .fill("Waiting for the vendor's code list");
      await line.getByRole("button", { name: "Hold" }).click();
      await expect(line).toContainText(
        /Held by .+, \d{2}\/\d{2}\/\d{4} \d{2}:\d{2}: Waiting for the vendor's code list/,
        { timeout: UI_TIMEOUT },
      );
    });

    await test.step("A lab number that names no order is matched to the order found, then accepted there", async () => {
      const secondRun = await runIdOf(page, name, [firstRun]);
      await page.goto(`/Results?run=${secondRun}`);
      const block = page.getByTestId("run-exceptions");
      await expect(block).toContainText(
        `Lab number ${unknownLabNumber} names no order (1 value)`,
      );
      await block.getByRole("button", { name: "Find lab number" }).click();
      await block.getByRole("combobox").fill(target.accession);
      await page
        .getByRole("option", {
          name: new RegExp(`^${target.accession} · .*test ordered$`),
        })
        .click();
      await block
        .getByLabel(/Reason/)
        .fill("Tube label misread by the scanner");
      await block.getByRole("button", { name: "Match" }).click();
      await expect(
        page.getByText(
          `Lab number ${target.accession} now holds 1 value from this run.`,
        ),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(page.getByTestId("run-exceptions")).toBeHidden();

      const valueRows = page
        .getByTestId("run-rows")
        .locator('> tbody > tr[data-testid^="run-row-"]');
      await expect(valueRows).toHaveCount(1);
      await expect(valueRows.first()).toContainText(target.accession);
      await expect(valueRows.first()).toContainText("Ready");
      await valueRows
        .first()
        .getByRole("button", { name: /expand row/i })
        .click();
      const entry = page.locator('[data-testid^="run-row-entry-"]');
      await expect(
        entry.locator('input[id^="unifiedResultValue-"]'),
      ).toHaveValue("55", { timeout: UI_TIMEOUT });
      await entry
        .getByRole("button", { name: /^accept$/i })
        .first()
        .click();
      await signIfAsked(page);
      await expect(valueRows).toHaveCount(0, { timeout: UI_TIMEOUT });
    });
  },
);
