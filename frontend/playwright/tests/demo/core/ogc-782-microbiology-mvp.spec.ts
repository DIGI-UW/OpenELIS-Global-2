import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  cleanupMicrobiologyMvpCase,
  seedMicrobiologyMvpCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

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
        "Case workbench · isolate · manual AST · release readiness",
      );

      await test.step("Open the microbiology case", async () => {
        await page.goto(`/MicrobiologyCaseView/${seeded.caseId}`, {
          waitUntil: "domcontentloaded",
        });
        await expect(
          page.getByRole("heading", { name: "Microbiology case" }),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        await expect(
          page.locator("header").getByText("RECEIVED"),
        ).toBeVisible();
        await demo.evidence("ogc-782-case-opened");
        await demo.pause(1500);
      });

      await test.step("Record setup activity", async () => {
        await demo.step(1, "Record setup activity");
        await page.getByLabel("Activity note").fill("setup complete");
        await page.getByRole("button", { name: "Record activity" }).click();
        await expect(
          page.locator("header").getByText("SETUP_RECORDED"),
        ).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        await expect(page.getByText(/setup complete/)).toBeVisible();
        await demo.evidence("ogc-782-setup-recorded");
        await demo.pause(1500);
      });

      await test.step("Create a clinically significant isolate", async () => {
        await demo.step(2, "Create a clinically significant isolate");
        await page.getByLabel("Preliminary organism").fill("Escherichia coli");
        await page.getByRole("button", { name: "Create isolate" }).click();
        await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
          timeout: LONG_TIMEOUT,
        });
        await expect(
          page.getByLabel("Manual AST").getByText("Final release blocked"),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        await demo.evidence("ogc-782-isolate-created");
        await demo.pause(1500);
      });

      await test.step("Start an AST run and record a MIC reading", async () => {
        await demo.step(3, "Record manual AST");
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
        ).toContainText("IN_PROGRESS", {
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
        await demo.evidence("ogc-782-ast-reading");
        await demo.pause(1500);
      });

      await test.step("Override and review the AST run", async () => {
        await demo.step(4, "Override and review AST");
        await page
          .getByLabel("Override reason")
          .fill("mixed growth confirmed on repeat");
        await page.getByRole("button", { name: "Apply override" }).click();
        await expect(
          page.getByTestId("microbiology-ast-interpretation"),
        ).toContainText("RESISTANT", {
          timeout: LONG_TIMEOUT,
        });
        await demo.evidence("ogc-782-ast-overridden");
        await page.getByRole("button", { name: "Review AST run" }).click();
        await expect(
          page.getByTestId("microbiology-ast-run-status"),
        ).toContainText("REVIEWED", {
          timeout: LONG_TIMEOUT,
        });
        await expect(
          page.getByLabel("Manual AST").getByText("Final release ready"),
        ).toBeVisible({ timeout: LONG_TIMEOUT });
        await demo.evidence("ogc-782-ast-reviewed-ready");
        await demo.pause(2000);
      });

      await test.step("Release the final report", async () => {
        await demo.step(5, "Release final report");
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
        ).toContainText("FINAL_RELEASED", { timeout: LONG_TIMEOUT });
        await demo.evidence("ogc-782-final-released");
        await demo.pause(2000);
      });
    } finally {
      cleanupMicrobiologyMvpCase(seeded);
    }
  });
});
