import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  cleanupMicrobiologyMvpCase,
  seedMicrobiologyMvpCase,
} from "../../../helpers/seed-microbiology-data";
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

test.describe("OGC-782 microbiology MVP", () => {
  test("case setup, isolate creation, manual AST, override, and review", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const seeded = seedMicrobiologyMvpCase();

    try {
      await demo.title(
        "OGC-782 Microbiology MVP",
        "Guided bacteriology path: setup, isolate, AST, review, release",
      );

      await test.step("Open the microbiology case", async () => {
        await demo.step(1, "Open the case workbench and confirm the next step");
        await page.goto(`/MicrobiologyCaseView/${seeded.caseId}`, {
          waitUntil: "domcontentloaded",
        });
        await expect(
          page.getByRole("heading", { name: "Microbiology case" }),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        await expect(
          page.locator("header").getByText("Received"),
        ).toBeVisible();
        await expect(
          page.getByTestId("microbiology-progress-rail"),
        ).toContainText("Inoculation");
        await captureViewport(page, demo, "ogc-782-01-case-workbench-overview");
      });

      await test.step("Record setup activity", async () => {
        await demo.step(2, "Start inoculation and write the activity timeline");
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
        await expect(
          page.getByRole("option", { name: /Ciprofloxacin/ }),
        ).toBeAttached();
        await page.getByRole("button", { name: "Record AST reading" }).click();
        await expect(
          page.getByTestId("microbiology-ast-interpretation"),
        ).toContainText("SUSCEPTIBLE", {
          timeout: LONG_TIMEOUT,
        });
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

      await test.step("Release the final report", async () => {
        await demo.step(6, "Review report readiness and release final report");
        await expect(
          page.getByRole("heading", { name: "Report readiness" }),
        ).toBeVisible();
        await expect(
          page.getByRole("button", { name: "Release final report" }),
        ).toBeEnabled({ timeout: LONG_TIMEOUT });
        await page
          .getByRole("button", { name: "Release final report" })
          .click();
        await expect(
          page.getByTestId("microbiology-release-state"),
        ).toContainText("Final Released", { timeout: LONG_TIMEOUT });
        await captureCard(
          page,
          demo,
          "microbiology-report-card",
          "ogc-782-08-final-released-readiness",
        );
        await demo.title(
          "MVP checkpoint complete",
          "Setup, isolate, manual AST, review, final release, and WHONET readiness were exercised.",
          3500,
        );
      });
    } finally {
      cleanupMicrobiologyMvpCase(seeded);
    }
  });
});
