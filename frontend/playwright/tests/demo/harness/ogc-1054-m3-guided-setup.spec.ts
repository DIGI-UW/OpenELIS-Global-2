import { expect, test } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { withAuthedPage } from "../../../helpers/api-session";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { expectNoPageHorizontalOverflow } from "../../../helpers/responsive-layout";

const SOURCE_PROFILE = "Cepheid GeneXpert (ASTM Mode)";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe("OGC-1054 M3 guided analyzer setup", () => {
  test("creates, verifies, connects, activates and links QC through the UI", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Set up a GeneXpert through the guided screens",
      "Choose its type, confirm the mappings, connect it, and activate it.",
    );
    const runId = Date.now().toString().slice(-8);
    const analyzerName = `M3 GeneXpert ${runId}`;
    const senderId = `GX-GUIDED-${runId}`;
    const profileName = `Guided GeneXpert ${runId}`;
    // A new profile exercises first-time confirmation on every run, through the
    // same duplication/publish workflow an operator uses. No mapping is seeded.
    await demo.caption(
      "A new analyzer type is published from the GeneXpert profile the Bridge ships, so its mappings start unconfirmed.",
    );
    await page.goto("/analyzers/types", { waitUntil: "domcontentloaded" });
    await page
      .getByRole("button", { name: "Duplicate Profile", exact: true })
      .click();
    const duplicate = page.getByRole("dialog", { name: "Duplicate Profile" });
    await duplicate
      .getByRole("combobox", { name: "Source analyzer type" })
      .selectOption({ label: `${SOURCE_PROFILE} · ASTM` });
    await duplicate
      .getByRole("textbox", { name: "New profile name" })
      .fill(profileName);
    await duplicate
      .getByRole("button", { name: "Duplicate Profile", exact: true })
      .click();
    await expect(duplicate.getByText("Ready to publish")).toBeVisible();
    await duplicate
      .getByRole("button", { name: "Publish Profile", exact: true })
      .click();
    await expect(page.getByText("Profile duplicated")).toBeVisible();
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);

    await demo.caption(
      "Add an analyzer: choose its type, name it, and pick its lab unit.",
    );
    await list.goto();
    await list.expectLoaded();
    const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    await expect(
      breadcrumb.getByRole("link", { name: "Home" }),
    ).toHaveAttribute("href", "/");
    await expect(
      page.getByRole("heading", { level: 1, name: "Analyzers" }),
    ).toBeVisible();

    await list.clickAdd();
    await setup.expectOpen();
    await expect(page).toHaveURL(/\/analyzers\?setup=instrument$/);
    await setup.selectProfile(profileName);
    await setup.fillName(analyzerName);
    await setup.selectLabUnit("Molecular Biology");
    await setup.continueToVerify();

    await expect(
      page.getByRole("button", { name: "Edit Instrument" }),
    ).toBeVisible();
    const notConfirmed = page.getByText("Not confirmed", { exact: true });
    await expect(notConfirmed).toBeVisible();
    await demo.caption(
      "Verify shows each assay's default mapping to a local test. Nothing is used until the operator confirms.",
    );
    await demo.highlight(notConfirmed);
    // The mapping is reviewed in Verify itself; nothing is confirmed until the operator says so.
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeVisible();
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
    await expect(page.getByText("Current", { exact: true })).toBeVisible();
    await expect(page.getByText("Current confirmation")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Continue to Connect" }),
    ).toBeEnabled();
    await demo.caption("Confirmed. Continue to Connect opens.");
    await demo.highlight(page.getByText("Current confirmation"));
    const verifyUrl = page.url();
    await capture(page, testInfo, "m3-verify");

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(verifyUrl);
    await expect(
      page.getByRole("button", { name: "Continue to Connect" }),
    ).toBeEnabled();
    await setup.continueToConnect();
    await expect(
      page.getByRole("button", { name: "Edit Verify" }),
    ).toBeVisible();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );

    await demo.caption(
      "Connect: the system name the instrument sends tells the Bridge which analyzer a message is from.",
    );
    await setup.fillSenderId(senderId);
    await demo.caption("Setup can be saved and finished later.");
    await page.getByRole("button", { name: "Save and finish later" }).click();
    await expect(setup.surface).not.toBeVisible();

    let analyzerRow = page.getByRole("row", {
      name: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    await expect(analyzerRow).toBeVisible();
    await list.search(analyzerName);
    await expect(
      page.getByTestId("stat-total").locator(".stat-value"),
    ).toHaveText("1");
    await expect(
      page.getByTestId("stat-setup").locator(".stat-value"),
    ).toHaveText("1");
    await expect(analyzerRow).toContainText("Setup");
    await demo.caption("The analyzer waits in Setup on the dashboard.");
    await demo.highlight(analyzerRow);
    await capture(page, testInfo, "m3-in-setup-dashboard");
    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Configure connection" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );
    await expect(
      setup.surface.getByRole("textbox", {
        name: "Instrument system name",
        exact: true,
      }),
    ).toHaveValue(senderId);
    await expect(page.getByText("Analyzer is ready to activate")).toBeVisible();
    await demo.caption(
      "Back in Connect, everything is ready. Finish and activate.",
    );
    await demo.highlight(page.getByText("Analyzer is ready to activate"));
    await capture(page, testInfo, "m3-ready-to-activate");

    await page.getByRole("button", { name: "Finish and activate" }).click();
    await expect(setup.surface).not.toBeVisible();

    analyzerRow = page.getByRole("row", {
      name: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    await expect(analyzerRow).toBeVisible();
    await expect(analyzerRow).toContainText("Active");
    await expect(analyzerRow).toContainText(profileName);
    await expect(analyzerRow).not.toContainText(/\b\d+ units?\b/);
    await list.search(analyzerName);
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("search") === analyzerName,
    );
    await expect(analyzerRow).toBeVisible();
    await demo.caption("The analyzer is Active.");
    await demo.highlight(analyzerRow);
    await capture(page, testInfo, "m3-active-dashboard");

    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Configure connection" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );
    await expect(
      page.getByRole("button", { name: "Save changes" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Finish and activate" }),
    ).not.toBeVisible();
    await expect(
      page.getByRole("button", { name: "Save and finish later" }),
    ).not.toBeVisible();
    await demo.caption(
      "Test connection shows what the Bridge reports for this analyzer.",
    );
    await setup.testConnection();
    const evidence = page.getByRole("heading", { name: "Connection evidence" });
    await evidence.scrollIntoViewIfNeeded();
    await demo.pause(2500);
    await capture(page, testInfo, "m3-connection-evidence");
    await setup.close();
    await expect(analyzerRow).toBeVisible();

    await demo.caption("Its Quality Control page opens from the analyzer.");
    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Quality Control" }).click();
    await expect(page).toHaveURL(
      /\/analyzers\/qc\/instruments\/\d+\?returnTo=/,
    );
    expect(await page.evaluate(() => window.scrollY)).toBe(0);
    await expect(
      page.getByRole("heading", { level: 1, name: analyzerName }),
    ).toBeVisible();
    const qcBreadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    const analyzerReturnLink = qcBreadcrumb.getByRole("link", {
      name: "Analyzers",
    });
    const analyzerReturnHref = await analyzerReturnLink.getAttribute("href");
    expect(analyzerReturnHref).not.toBeNull();
    const analyzerReturnUrl = new URL(analyzerReturnHref!, page.url());
    expect(analyzerReturnUrl.pathname).toBe("/analyzers");
    expect(analyzerReturnUrl.searchParams.get("search")).toBe(analyzerName);
    await demo.pause(2000);
    await capture(page, testInfo, "m3-linked-operational-qc");

    await analyzerReturnLink.click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("search") === analyzerName,
    );
    await list.expectLoaded();
    await expect(analyzerRow).toBeVisible();

    await demo.caption("The dashboard also fits a phone screen.");
    await page.setViewportSize({ width: 390, height: 844 });
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(analyzerRow).toBeVisible();
    await expect(analyzerRow).toContainText(profileName);
    await expect(page.getByTestId("content-wrapper")).toHaveCSS(
      "margin-left",
      "0px",
    );
    await expectNoPageHorizontalOverflow(
      page,
      "Analyzer dashboard should not overflow the mobile page horizontally",
    );
    await demo.pause(2000);
    await capture(page, testInfo, "m3-mobile-dashboard");
    await demo.verified(
      `${analyzerName} is active on ${profileName}`,
      "Its mappings were confirmed in Verify, then it was connected and activated.",
    );
  });

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
