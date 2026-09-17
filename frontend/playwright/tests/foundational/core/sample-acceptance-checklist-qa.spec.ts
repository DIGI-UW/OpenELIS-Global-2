import { expect, test, Page } from "../../../helpers/test-base";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

// S-09 (OGC-580) — QA-QC + Intake Acceptance, the operator surface embedded in
// the shared OrderQA step. This proves the SINGLE acceptance component mounts in
// every domain's order workflow (Clinical / Environmental / Vector) — the core
// "one shared surface" design claim — against the real bundle and routes.
//
// Scope note: the full Accept / Report-NCE / Resample happy-path runs against a
// live order at Step "QA". There is no order-workflow e2e driver in the repo to
// reach that state deterministically, so that flow is covered by the component
// unit suite (SampleAcceptanceChecklist.test.jsx, 9 cases) and the backend
// integration tests; building a reusable order-entry driver is a follow-up.
// Per playwright-best-practices.md: assert on visible UI, never on response.ok().

type Domain = "clinical" | "environmental" | "vector";
const DOMAINS: Domain[] = ["clinical", "environmental", "vector"];

async function openQaStep(page: Page, domain: Domain) {
  await page.goto(`/order/${domain}/qa`, { waitUntil: "domcontentloaded" });
}

test.describe("Intake acceptance — embedded in OrderQA", () => {
  test.setTimeout(90_000);

  for (const domain of DOMAINS) {
    test(`renders the Intake acceptance surface in the ${domain} QA step`, async ({
      page,
    }) => {
      await openQaStep(page, domain);

      // The acceptance tile heading is always present on the QA step — with
      // collected specimens it shows the resolved checklist; without any it shows
      // the "no collected specimens" placeholder. Either way it must mount without error.
      await expect(
        page.getByRole("heading", { name: /Intake acceptance/i }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      await expect(
        page.getByText(/No collected specimens to review yet\./i),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });
  }
});
