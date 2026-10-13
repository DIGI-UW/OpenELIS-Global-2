import { expect, test } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import {
  BC5380,
  GENEXPERT,
  QUANTSTUDIO,
  type ShippedProfile,
} from "../../../helpers/shipped-profiles";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { expectNoPageHorizontalOverflow } from "../../../helpers/responsive-layout";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe.serial("OGC-1054 M3 guided analyzer setup", () => {
  const runId = Date.now().toString().slice(-8);
  /**
   * One instrument per path a shipped type speaks. Setup is the same flow for
   * each; Connect asks for what the Bridge needs to tell this instrument's
   * results apart: the system name a message carries, or the folder a file
   * lands in.
   */
  const INSTRUMENTS: Array<{
    profile: ShippedProfile;
    model: string;
    protocol: string;
    lane: string;
    sends: string;
    name: string;
    connection: { senderId: string } | { directory: string };
    connectCaption: string;
    checkAnswer: string;
    shot: string;
  }> = [
    {
      profile: GENEXPERT,
      model: "GeneXpert",
      protocol: "ASTM",
      lane: "@astm",
      sends: "It sends its results over ASTM.",
      name: `M3 GeneXpert ${runId}`,
      connection: { senderId: `GX-GUIDED-${runId}` },
      connectCaption:
        "Connect: the system name the instrument sends tells the Bridge which analyzer a message is from.",
      checkAnswer: "Bridge listener is ready.",
      shot: "m3",
    },
    {
      profile: BC5380,
      model: "Mindray BC-5380",
      protocol: "HL7",
      lane: "@hl7",
      sends: "It sends its results over HL7.",
      name: `M3 BC-5380 ${runId}`,
      connection: { senderId: `BC-5380-GUIDED-${runId}` },
      connectCaption:
        "Connect: the system name the instrument sends tells the Bridge which analyzer a message is from.",
      checkAnswer: "Bridge listener is ready.",
      shot: "m3-bc5380",
    },
    {
      profile: QUANTSTUDIO,
      model: "QuantStudio",
      protocol: "file",
      lane: "@file",
      sends:
        "It writes a results workbook to a folder; the Bridge watches that folder.",
      name: `M3 QuantStudio ${runId}`,
      connection: {
        directory: `/data/analyzer-imports/quantstudio/incoming/${runId}`,
      },
      connectCaption:
        "Connect: the folder the instrument writes to is the one the Bridge watches for this analyzer.",
      checkAnswer: "Directory is ready.",
      shot: "m3-quantstudio",
    },
  ];

  for (const {
    profile,
    model,
    protocol,
    lane,
    sends,
    name,
    connection,
    connectCaption,
    checkAnswer,
    shot,
  } of INSTRUMENTS) {
    test(
      `sets a ${model} up on its shipped ${protocol} type: verifies, connects, activates and tests the connection`,
      { tag: ["@evidence", lane, "@setup"] },
      async ({ page }, testInfo) => {
        const demo = createDemoPresentation(page, testInfo);
        await demo.intro(
          `Set up a ${model} on its shipped type`,
          `${sends} Choose the type, confirm its mappings, connect it, and activate it.`,
        );
        const list = new AnalyzerListPage(page);
        const setup = new AnalyzerSetupPage(page);
        await list.goto();
        await list.expectLoaded();
        const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
        await expect(
          breadcrumb.getByRole("link", { name: "Home" }),
        ).toHaveAttribute("href", "/");
        await expect(
          page.getByRole("heading", { level: 1, name: "Analyzers" }),
        ).toBeVisible();
        await demo.caption(
          "Add an analyzer: choose its type, name it, and pick its lab unit.",
        );

        await list.clickAdd();
        await setup.expectOpen();
        await expect(page).toHaveURL(/\/analyzers\?setup=instrument$/);
        await setup.selectProfile(profile.displayName, profile);
        await setup.fillName(name);
        await setup.selectLabUnit(profile.labUnit);
        await setup.continueToVerify();

        await expect(
          page.getByRole("button", { name: "Edit Instrument" }),
        ).toBeVisible();
        const notConfirmed = page.getByText("Not confirmed", { exact: true });
        const current = page.getByText("Current confirmation");
        await expect(notConfirmed.or(current)).toBeVisible();
        if (await notConfirmed.isVisible()) {
          await demo.caption(
            "Verify shows each code's default mapping to a local test. Nothing is used until the operator confirms.",
          );
          await demo.highlight(notConfirmed);
          const confirm = page.getByRole("button", {
            name: "Confirm mappings and control recognition",
          });
          await expect(confirm).toBeEnabled();
          await confirm.click();
          await expect(
            page.getByText("Mappings and control recognition confirmed"),
          ).toBeVisible();
        }
        await expect(page.getByText("Current", { exact: true })).toBeVisible();
        await expect(current).toBeVisible();
        await expect(
          page.getByRole("button", { name: "Continue to Connect" }),
        ).toBeEnabled();
        await demo.caption(
          "The mappings are confirmed. Continue to Connect opens.",
        );
        await demo.highlight(current);
        const verifyUrl = page.url();
        await capture(page, testInfo, `${shot}-verify`);

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

        await demo.caption(connectCaption);
        if ("senderId" in connection) {
          await setup.fillSenderId(connection.senderId);
        } else {
          await setup.fillImportDirectory(connection.directory);
        }
        await demo.caption("Setup can be saved and finished later.");
        await page
          .getByRole("button", { name: "Save and finish later" })
          .click();
        await expect(setup.surface).not.toBeVisible();

        let analyzerRow = page.getByRole("row", {
          name: new RegExp(escapeRegExp(name), "i"),
        });
        await expect(analyzerRow).toBeVisible();
        await list.search(name);
        await expect(
          page.getByTestId("stat-total").locator(".stat-value"),
        ).toHaveText("1");
        await expect(
          page.getByTestId("stat-setup").locator(".stat-value"),
        ).toHaveText("1");
        await expect(analyzerRow).toContainText("Setup");
        await demo.caption("The analyzer waits in Setup on the dashboard.");
        await demo.highlight(analyzerRow);
        await capture(page, testInfo, `${shot}-in-setup-dashboard`);
        await analyzerRow.getByRole("button", { name: "Actions" }).click();
        await page
          .getByRole("menuitem", { name: "Configure connection" })
          .click();
        await expect(page).toHaveURL(
          (url) => url.searchParams.get("setup") === "connect",
        );
        if ("senderId" in connection) {
          await expect(
            setup.surface.getByRole("textbox", {
              name: "Instrument system name",
              exact: true,
            }),
          ).toHaveValue(connection.senderId);
        } else {
          await expect(
            setup.surface.getByRole("textbox", {
              name: "Directory",
              exact: true,
            }),
          ).toHaveValue(connection.directory);
        }
        await expect(
          page.getByText("Analyzer is ready to activate"),
        ).toBeVisible();
        await demo.caption(
          "Back in Connect, everything is ready. Finish and activate.",
        );
        await demo.highlight(page.getByText("Analyzer is ready to activate"));
        await capture(page, testInfo, `${shot}-ready-to-activate`);

        await page.getByRole("button", { name: "Finish and activate" }).click();
        await expect(setup.surface).not.toBeVisible();

        analyzerRow = page.getByRole("row", {
          name: new RegExp(escapeRegExp(name), "i"),
        });
        await expect(analyzerRow).toBeVisible();
        await expect(analyzerRow).toContainText("Active");
        await expect(analyzerRow).toContainText(profile.displayName);
        await expect(analyzerRow).not.toContainText(/\b\d+ units?\b/);
        await list.search(name);
        await expect(page).toHaveURL(
          (url) => url.searchParams.get("search") === name,
        );
        await expect(analyzerRow).toBeVisible();
        await demo.caption("The analyzer is Active.");
        await demo.highlight(analyzerRow);
        await capture(page, testInfo, `${shot}-active-dashboard`);

        await analyzerRow.getByRole("button", { name: "Actions" }).click();
        await page
          .getByRole("menuitem", { name: "Configure connection" })
          .click();
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
        await setup.testConnection();
        const evidence = page.getByRole("heading", {
          name: "Connection evidence",
        });
        await evidence.scrollIntoViewIfNeeded();
        // The caption quotes the answer, so the evidence line is found on the setup surface.
        const answer = setup.surface.getByText(checkAnswer);
        await expect(answer).toBeVisible();
        await demo.caption(
          `Test connection shows what the Bridge checked for this analyzer: "${checkAnswer}"`,
        );
        await demo.highlight(answer);
        await demo.pause(2500);
        await capture(page, testInfo, `${shot}-connection-evidence`);
        await setup.close();
        await expect(analyzerRow).toBeVisible();
        await demo.verified(
          `${name} is active on ${profile.displayName}`,
          "Its mappings were confirmed in Verify, then it was connected and activated.",
        );
      },
    );
  }

  test("the active analyzer opens its Quality Control page, and the dashboard fits a phone screen", async ({
    page,
  }, testInfo) => {
    const analyzerName = INSTRUMENTS[0].name;
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    const analyzerRow = page.getByRole("row", {
      name: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    await expect(analyzerRow).toContainText("Active");

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
    await capture(page, testInfo, "m3-linked-operational-qc");

    await analyzerReturnLink.click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("search") === analyzerName,
    );
    await list.expectLoaded();
    await expect(analyzerRow).toBeVisible();

    await page.setViewportSize({ width: 390, height: 844 });
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(analyzerRow).toBeVisible();
    await expect(analyzerRow).toContainText(GENEXPERT.displayName);
    await expect(page.getByTestId("content-wrapper")).toHaveCSS(
      "margin-left",
      "0px",
    );
    await expectNoPageHorizontalOverflow(
      page,
      "Analyzer dashboard should not overflow the mobile page horizontally",
    );
    await capture(page, testInfo, "m3-mobile-dashboard");
  });
});
