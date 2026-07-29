import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const settleForVideo = async (
  demo: ReturnType<typeof createDemoPresentation>,
  ms = 900,
) => {
  await demo.pause(ms);
};

const captureViewport = async (
  page: Page,
  demo: ReturnType<typeof createDemoPresentation>,
  name: string,
) => {
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }));
  await settleForVideo(demo);
  await demo.evidence(name, { fullPage: false });
  await demo.pause(3000);
};

const captureCard = async (
  page: Page,
  demo: ReturnType<typeof createDemoPresentation>,
  testId: string,
  name: string,
) => {
  const card = page.getByTestId(testId);
  await card.scrollIntoViewIfNeeded();
  const handle = await card.elementHandle();
  if (handle) {
    await page.evaluate((element) => {
      const rect = element.getBoundingClientRect();
      window.scrollBy({
        top: rect.top - 88,
        behavior: "instant",
      });
    }, handle);
  }
  await settleForVideo(demo);
  await demo.evidence(name, { locator: card });
  await demo.pause(3000);
};

const getCsrfToken = async (page: Page) => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

test.describe("OGC-782 microbiology MVP", () => {
  test("case setup, isolate creation, manual AST, override, and review", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyMvpCase(page);

    await demo.title(
      "OGC-782 Microbiology MVP",
      "Guided bacteriology path: setup, isolate, AST, review, release",
    );

    await test.step("Open the microbiology case", async () => {
      await demo.step(1, "Open the case workbench and confirm the next step");
      await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.locator("header").getByText("Received")).toBeVisible();
      await expect(
        page.getByTestId("microbiology-progress-rail"),
      ).toContainText("Inoculation");
      await captureViewport(page, demo, "ogc-782-01-case-workbench-overview");
    });

    await test.step("Record setup activity", async () => {
      await demo.step(2, "Start inoculation and write the activity timeline");
      await page.getByLabel("Media or bottle").fill("Blood culture bottle");
      await page.getByLabel("Incubation").fill("35 C for 24 hours");
      await page.getByLabel("Atmosphere").fill("Ambient");
      await page.getByLabel("Activity note").fill("setup complete");
      await captureCard(
        page,
        demo,
        "microbiology-setup-card",
        "ogc-782-02-inoculation-ready",
      );
      await page.getByRole("button", { name: "Start inoculation" }).click();
      await expect(
        page.locator("header").getByText("Setup Recorded"),
      ).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByText(/setup complete/)).toBeVisible();
      await expect(
        page.getByText(/Media or bottle: Blood culture bottle/),
      ).toBeVisible();
      await expect(
        page.getByText(/Incubation: 35 C for 24 hours/),
      ).toBeVisible();
      await expect(page.getByText(/Atmosphere: Ambient/)).toBeVisible();
      await captureCard(
        page,
        demo,
        "microbiology-timeline-card",
        "ogc-782-03-setup-recorded-timeline",
      );
    });

    await test.step("Create a clinically significant isolate", async () => {
      await demo.step(3, "Add a clinically significant isolate");
      await page.getByLabel("Preliminary organism").fill("Escherichia coli");
      await page.getByRole("button", { name: "Create isolate" }).click();
      await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page.getByLabel("Manual AST").getByText("Final release blocked"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await captureCard(
        page,
        demo,
        "microbiology-isolates-card",
        "ogc-782-04-isolate-created",
      );
      await page.getByRole("button", { name: "Update identification" }).click();
      await page
        .getByLabel("Preliminary organism")
        .fill("Escherichia coli confirmed");
      await page.getByLabel("Identification status").selectOption("CONFIRMED");
      await page.getByRole("button", { name: "Save identification" }).click();
      await expect(
        page.getByText(/ISO-1: Escherichia coli confirmed/),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByText(/Clinically Significant · Confirmed/),
      ).toBeVisible();
    });

    await test.step("Start an AST run and record a MIC reading", async () => {
      await demo.step(4, "Record manual AST and show the interpreted result");
      await expect(
        page.getByRole("heading", { name: "Manual AST" }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Start AST run" }),
      ).toBeEnabled({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("button", { name: "Start AST run" }).click();
      await expect(
        page.getByTestId("microbiology-ast-run-status"),
      ).toContainText("In Progress", {
        timeout: LONG_TIMEOUT,
      });
      const uatAntibiotic = page
        .getByTestId("microbiology-ast-card")
        .getByRole("option", { name: "Ciprofloxacin (UAT)", exact: true });
      await expect(uatAntibiotic).toBeAttached();
      await expect(uatAntibiotic).toHaveCount(1);
      await page.getByRole("button", { name: "Record AST reading" }).click();
      await expect(
        page.getByTestId("microbiology-ast-interpretation"),
      ).toContainText("SUSCEPTIBLE", {
        timeout: LONG_TIMEOUT,
      });
      await page
        .getByLabel("Antibiotic", { exact: true })
        .selectOption({ label: "Gentamicin (UAT)" });
      await page.getByRole("button", { name: "Record AST reading" }).click();
      await expect(
        page.getByTestId("microbiology-ast-reading-row"),
      ).toHaveCount(2, { timeout: LONG_TIMEOUT });
      await expect(page.getByTestId("microbiology-ast-card")).toContainText(
        "Ciprofloxacin (UAT)",
      );
      await expect(page.getByTestId("microbiology-ast-card")).toContainText(
        "Gentamicin (UAT)",
      );
      await captureCard(
        page,
        demo,
        "microbiology-ast-card",
        "ogc-782-05-ast-reading-interpreted",
      );
    });

    await test.step("Override and review the AST run", async () => {
      await demo.step(5, "Override the AST result and mark the run reviewed");
      await page
        .getByLabel("Override reason")
        .fill("mixed growth confirmed on repeat");
      await page.getByRole("button", { name: "Apply override" }).click();
      await expect(
        page.getByTestId("microbiology-ast-interpretation"),
      ).toContainText("RESISTANT", {
        timeout: LONG_TIMEOUT,
      });
      await captureCard(
        page,
        demo,
        "microbiology-ast-card",
        "ogc-782-06-ast-overridden",
      );
      await page.getByRole("button", { name: "Review AST run" }).click();
      await expect(
        page.getByTestId("microbiology-ast-run-status"),
      ).toContainText("Reviewed", {
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page.getByLabel("Manual AST").getByText("Final release ready"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await captureCard(
        page,
        demo,
        "microbiology-ast-card",
        "ogc-782-07-ast-reviewed-ready",
      );
    });

    await test.step("Complete a Result-target critical communication", async () => {
      await demo.step(
        6,
        "Log, acknowledge, and close a critical communication against the projected result",
      );
      await page
        .getByRole("button", { name: "Critical communication" })
        .click();
      await page.getByLabel("Critical result target").selectOption("RESULT");
      await expect(page.getByLabel("Target record")).not.toHaveValue("");
      await page
        .getByLabel("Recipient", { exact: true })
        .fill("Provider on call");
      await page
        .getByLabel("Message")
        .fill("Resistant isolate result called to provider");
      await page.getByRole("button", { name: "Log communication" }).click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("Open", { timeout: LONG_TIMEOUT });
      await page.getByRole("button", { name: "Acknowledge" }).click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("Acknowledged", { timeout: LONG_TIMEOUT });
      await page.getByRole("button", { name: "Close communication" }).click();
      await page
        .getByLabel("Resolution note")
        .fill("Provider read back and accepted the result");
      await page
        .getByRole("button", { name: "Close communication" })
        .last()
        .click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("Closed", { timeout: LONG_TIMEOUT });
      await expect(
        page.getByText("Provider read back and accepted the result"),
      ).toBeVisible();
      await captureCard(
        page,
        demo,
        "microbiology-critical-card",
        "ogc-782-08-critical-communication-closed",
      );
    });

    await test.step("Release the final report", async () => {
      await demo.step(7, "Review report readiness and release final report");
      await page.getByRole("button", { name: "Reports" }).click();
      await expect(
        page.getByRole("heading", { name: "Report readiness" }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Release final report" }),
      ).toBeEnabled({ timeout: LONG_TIMEOUT });
      await page.getByRole("button", { name: "Release final report" }).click();
      await expect(
        page.getByTestId("microbiology-release-state"),
      ).toContainText("Final Released", { timeout: LONG_TIMEOUT });
      await captureCard(
        page,
        demo,
        "microbiology-report-card",
        "ogc-782-09-final-released-readiness",
      );
      await expect(page.getByText("Final case is read-only")).toBeVisible();
      await page.getByRole("button", { name: "Isolates", exact: true }).click();
      await expect(
        page.getByRole("button", { name: "Update identification" }),
      ).toBeDisabled();
      await expect(
        page.getByRole("button", { name: "Create isolate" }),
      ).toBeDisabled();

      const lockedMutation = await page.request.post(
        "/api/OpenELIS-Global/rest/microbiology/isolates",
        {
          data: {
            caseId: seeded.caseId,
            isolateLabel: "ISO-LOCKED",
            preliminaryOrganismText: "Must not persist",
            significance: "UNKNOWN",
          },
          headers: { "X-CSRF-Token": await getCsrfToken(page) },
        },
      );
      expect(lockedMutation.status()).toBe(409);
      await expect(lockedMutation.json()).resolves.toMatchObject({
        error: "MICROBIOLOGY_CASE_LOCKED",
      });

      await demo.step(
        8,
        "Open the patient results screen and verify the released microbiology result",
      );
      await page.goto(`/PatientResults/${seeded.patientId}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Patient History" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByText("ISO-1: Escherichia coli")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByText("Ciprofloxacin (UAT) R")).toBeVisible();
      await expect(page.getByText("Gentamicin (UAT) S")).toBeVisible();
      await captureViewport(page, demo, "ogc-782-10-patient-results-released");
      await demo.title(
        "MVP checkpoint complete",
        "Setup, isolate, manual AST, review, final release, and visible patient results were exercised.",
        3500,
      );
    });
  });
});
