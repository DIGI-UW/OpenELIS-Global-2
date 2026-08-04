import AxeBuilder from "@axe-core/playwright";
import { expect } from "@playwright/test";
import { writeFile } from "fs/promises";
import type { Page, TestInfo } from "@playwright/test";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

export async function expectNoWcag21AaViolations(
  page: Page,
  testInfo: TestInfo,
  surfaceName: string,
) {
  await page.evaluate(async () => {
    await document.fonts?.ready;
    for (let pass = 0; pass < 4; pass += 1) {
      const finiteAnimations = document.getAnimations().filter((animation) => {
        const endTime = animation.effect?.getComputedTiming().endTime;
        return (
          (animation.playState === "pending" ||
            animation.playState === "running") &&
          typeof endTime === "number" &&
          Number.isFinite(endTime)
        );
      });
      if (finiteAnimations.length === 0) {
        break;
      }
      await Promise.all(
        finiteAnimations.map((animation) =>
          animation.finished.catch(() => undefined),
        ),
      );
    }
    await new Promise<void>((resolve) => {
      requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
    });
  });
  const results = await new AxeBuilder({ page })
    .withTags(WCAG_21_AA_TAGS)
    .analyze();
  const evidence = {
    surface: surfaceName,
    url: page.url(),
    testCommit: process.env.GITHUB_SHA || process.env.APP_SHA || "local",
    testEnvironment: testInfo.project.name,
    timestamp: new Date().toISOString(),
    violations: results.violations,
    passes: results.passes.map(({ id, impact, tags }) => ({
      id,
      impact,
      tags,
    })),
    incomplete: results.incomplete,
  };
  const safeSurfaceName = surfaceName.replace(/[^a-z0-9-]+/gi, "-");
  const jsonPath = testInfo.outputPath(`axe-${safeSurfaceName}.json`);
  const screenshotPath = testInfo.outputPath(
    `screenshot-${safeSurfaceName}.png`,
  );
  await Promise.all([
    writeFile(jsonPath, `${JSON.stringify(evidence, null, 2)}\n`),
    page.screenshot({ path: screenshotPath, fullPage: true }),
  ]);

  await testInfo.attach(`axe-${surfaceName}`, {
    path: jsonPath,
    contentType: "application/json",
  });
  await testInfo.attach(`screenshot-${surfaceName}`, {
    path: screenshotPath,
    contentType: "image/png",
  });

  const summary = results.violations.map(({ id, impact, help, nodes }) => ({
    id,
    impact,
    help,
    targets: nodes.map((node) => node.target),
  }));
  expect(summary, JSON.stringify(summary, null, 2)).toEqual([]);
}
