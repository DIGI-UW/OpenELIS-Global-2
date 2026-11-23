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

/**
 * OGC-1200 slice 5b: a test held by a failed control is sent for retest; the
 * next run of the same sample suggests "retest of RUN-x", the user confirms
 * it, and the earlier run reads "Superseded by RUN-y" (FR-C5, FR-A3). A re-run
 * that arrived before the Retest click is no longer stuck as a duplicate.
 */

interface Setup {
  run: string;
  code: string;
  testId: string;
  analyzerId: string;
  destination: string;
  sender: string;
  accession: string;
}

async function setUp(page: Page, prefix: string): Promise<Setup> {
  const run = randomUUID().slice(0, 8);
  const sender = `${prefix}-${run}`;
  const code = "RTRETEST";
  const loinc = `${randomInt(1_000_000, 9_000_000)}-${randomInt(0, 9)}`;
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const [target] = await seedNumericTests(
    page,
    run,
    [{ name: `Run retest ${run}`, code, loinc }],
    { testSection: "Molecular Biology", sampleType: "Plasma" },
  );
  const name = `Run retest ${run}`;
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
  await publish(page, draft, [numeric(code, loinc)]);
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
  const order = await createClinicalOrder(page, {
    testIds: [target.id],
    specimenName: "Plasma",
  });
  return {
    run,
    code,
    testId: target.id,
    analyzerId: analyzer.id,
    destination: source.destination,
    sender,
    accession: order.accession,
  };
}

async function sendResult(
  page: Page,
  setup: Setup,
  value: string,
  completedAt: string,
  staged: number,
) {
  const received = await page.request.post(
    `${process.env.MOCK_SIMULATOR_URL || "http://localhost:8085"}/simulate/astm/mindray_ba88a`,
    {
      data: {
        destination: setup.destination,
        sender_id: setup.sender,
        sample_id: setup.accession,
        completed_at: completedAt,
        results: [
          { test_code: setup.code, type: "TEXT", value, unit: "mmol/L" },
        ],
      },
    },
  );
  expect(received.ok(), await received.text()).toBeTruthy();
  await expect
    .poll(
      async () =>
        (await worklistFor(page, setup.analyzerId, setup.accession)).length,
    )
    .toBe(staged);
}

async function openWaitingRun(page: Page, accession: string, label?: string) {
  await page.goto(`/Results?accessionNumber=${accession}`);
  const waiting = label
    ? page.getByText(`Waiting in ${label}`, { exact: true }).first()
    : page.getByText(/^Waiting in RUN-\d+$/).first();
  await expect(waiting).toBeVisible();
  const name = (await waiting.textContent())?.replace("Waiting in ", "");
  await waiting.click();
  await expect(page).toHaveURL(/\/Results\?run=\d+$/);
  return { label: name as string, url: page.url() };
}

async function failControl(page: Page, setup: Setup) {
  const qc = page.getByTestId("run-qc");
  await qc.getByTestId("run-qc-record").click();
  const form = qc.getByTestId("run-qc-form");
  await form.locator("label", { hasText: /^Control line$/ }).click();
  await form.getByLabel("Kit lot").fill(`KIT-${setup.run}`);
  await form.locator("label", { hasText: /^Invalid$/ }).click();
  await form.getByTestId("run-qc-save").click();
  await expect(qc.getByTestId(`run-qc-decide-${setup.testId}`)).toBeVisible();
}

async function retest(page: Page, setup: Setup) {
  const line = page
    .getByTestId("run-qc")
    .getByTestId(`run-qc-test-${setup.testId}`);
  await line.getByRole("button", { name: "Retest", exact: true }).click();
  await line
    .getByTestId(`run-qc-decide-${setup.testId}`)
    .getByRole("button", { name: "Retest", exact: true })
    .last()
    .click();
  await expect(
    line.getByTestId(`run-qc-decision-${setup.testId}`),
  ).toContainText("Retest");
}

async function confirmAndSupersede(
  page: Page,
  setup: Setup,
  first: { label: string; url: string },
  second: { label: string; url: string },
) {
  const line = page
    .getByTestId("run-qc")
    .getByTestId(`run-qc-test-${setup.testId}`);
  await expect(line).toContainText(
    `Suggested: retest of ${first.label} (1 of 1 samples)`,
  );
  await line
    .getByRole("button", { name: `Confirm retest of ${first.label}` })
    .click();
  await expect(line).toContainText(`Retest of ${first.label}`);
  await expect(
    line.getByRole("button", { name: /Confirm retest/ }),
  ).toHaveCount(0);

  await page.goto(first.url);
  const earlier = page
    .getByTestId("run-qc")
    .getByTestId(`run-qc-test-${setup.testId}`);
  await expect(earlier).toContainText(`Superseded by ${second.label}`);
  await expect(page.getByTestId("run-settings")).toContainText("Superseded");
}

test(
  "a retest run is confirmed as the retest and supersedes the held run",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(300_000);
    const setup = await setUp(page, "R1200R");
    await sendResult(page, setup, "50", "20261124090000", 1);
    const first = await openWaitingRun(page, setup.accession);

    await failControl(page, setup);
    await retest(page, setup);

    await sendResult(page, setup, "52", "20261124100000", 1);
    const second = await openWaitingRun(page, setup.accession);
    expect(second.label).not.toBe(first.label);
    await confirmAndSupersede(page, setup, first, second);
  },
);

test(
  "a re-run that arrived before the Retest click is not left a duplicate",
  { tag: ["@astm", "@results"] },
  async ({ page }) => {
    test.setTimeout(300_000);
    const setup = await setUp(page, "R1200D");
    await sendResult(page, setup, "50", "20261124090000", 1);
    const first = await openWaitingRun(page, setup.accession);
    await failControl(page, setup);

    await sendResult(page, setup, "52", "20261124100000", 2);
    await page.goto(first.url);
    await retest(page, setup);

    const second = await openWaitingRun(page, setup.accession);
    expect(second.label).not.toBe(first.label);
    const row = page
      .getByTestId("run-rows")
      .locator('> tbody > tr[data-testid^="run-row-"]');
    await expect(row).toHaveCount(1);
    await expect(row).toContainText("52");
    await expect(row).not.toContainText("Duplicate");
    await confirmAndSupersede(page, setup, first, second);
  },
);
