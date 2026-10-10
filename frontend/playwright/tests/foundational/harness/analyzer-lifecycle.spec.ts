import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { withAuthedPage } from "../../../helpers/api-session";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test.describe("An analyzer's lifecycle", () => {
  test("deactivates and reactivates an analyzer through the UI", async ({
    browser,
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Deactivate and reactivate an analyzer",
      "Both from the analyzer's actions on the dashboard.",
    );
    const runId = Date.now().toString().slice(-8);
    const analyzerName = `Lifecycle GeneXpert ${runId}`;
    await demo.caption(
      "Off screen: a GeneXpert is set up and activated through the same guided screens.",
    );
    await withAuthedPage(browser, (setupPage) =>
      activateShippedGeneXpert(setupPage, analyzerName, `GX-LIFE-${runId}`),
    );

    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    const analyzerRow = page.getByRole("row", {
      name: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    await expect(analyzerRow).toContainText("Active");

    await demo.caption(
      "Deactivate stops new use. Its configuration and history are kept.",
    );
    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Deactivate" }).click();
    await expect(page).toHaveURL(/lifecycle=deactivate/);
    await expect(
      page.getByRole("heading", { name: "Deactivate analyzer" }),
    ).toBeVisible();
    await demo.pause(1500);
    await page.getByRole("button", { name: "Deactivate analyzer" }).click();
    await expect(analyzerRow).toContainText("Inactive");
    await demo.caption("The analyzer is Inactive.");
    await demo.highlight(analyzerRow);

    await demo.caption(
      "Reactivate checks its setup again before it can be used.",
    );
    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Reactivate" }).click();
    await expect(page).toHaveURL(/lifecycle=reactivate/);
    await expect(
      page.getByRole("heading", { name: "Reactivate analyzer" }),
    ).toBeVisible();
    await demo.pause(1500);
    await page.getByRole("button", { name: "Reactivate analyzer" }).click();
    await expect(analyzerRow).toContainText("Active");
    await expect(analyzerRow).not.toContainText("Inactive");
    await demo.highlight(analyzerRow);
    await demo.verified(
      `${analyzerName} is active again`,
      "It was deactivated, then reactivated, from the dashboard.",
    );
  });
});
