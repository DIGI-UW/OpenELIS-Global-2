import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

// RPT-S06: approved navigation follow-up to the openelis-work reporting mock.
// No new upstream story ID is invented. Persistence/override regression tests
// remain in core-app; this focused proof connects navigation, drafts and editor.
test("Reporting navigation retains the draft and exposes configurable menus", async ({
  page,
}, testInfo) => {
  test.skip(
    process.env.REPORTING_INSTANCE_NAV !== "true",
    "Requires the Reporting UAT instance menu profile.",
  );
  testInfo.setTimeout(120_000);
  const demo = createDemoPresentation(page, testInfo);
  await page.setViewportSize({ width: 1280, height: 720 });
  await page.goto("/CustomDataExport?uat=navigation-proof");
  await expect(page).toHaveURL(
    /\/reports\/custom-data-export\?uat=navigation-proof$/,
  );
  const nav = page.getByRole("navigation", { name: "Side navigation" });
  await expect(nav.getByRole("heading")).toHaveText([
    "Main Menu",
    "Patient & Orders",
    "Reports",
    "Administration",
  ]);
  await demo.chapter({
    eyebrow: "Reporting milestone · Navigation",
    title: "A clear path through reports",
    subtitle: "Move between reports and the queue while keeping the draft.",
    durationMs: 4000,
  });
  await demo.evidence("navigation-01-overview");
  await page
    .getByRole("button", { name: "Start a new export", exact: true })
    .click();
  await page.getByRole("radio", { name: /^Sample & Testing/ }).click();
  await page
    .getByRole("searchbox", { name: "Find a field", exact: true })
    .fill("Accession Number");
  await page
    .getByRole("button", { name: "Add Accession Number", exact: true })
    .click();
  await page.getByRole("button", { name: "Clear search", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Your CSV columns (1)", exact: true }),
  ).toBeVisible();
  await demo.evidence("navigation-02-draft");
  const queue = nav.getByRole("link", { name: "My Report Queue", exact: true });
  await queue.click();
  await expect(page).toHaveURL(/view=queue/);
  await expect(queue).toHaveAttribute("aria-current", "page");
  await expect(
    page.getByRole("heading", { name: "My Report Queue", exact: true }),
  ).toBeVisible();
  await demo.scene("The selected view is part of the URL");
  await demo.evidence("navigation-03-queue");
  await page.reload();
  await expect(queue).toHaveAttribute("aria-current", "page");
  await nav
    .getByRole("link", { name: "Custom Data Export", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Continue current export", exact: true })
    .click();
  await expect(
    page
      .getByRole("region", { name: "CSV header preview" })
      .getByRole("columnheader"),
  ).toHaveText(["Accession Number"]);
  await expect(page).toHaveURL(/uat=navigation-proof/);
  await demo.scene("The existing column selection is retained");
  await demo.evidence("navigation-04-restored");

  await demo.chapter({
    eyebrow: "RPT-S06 · Menu configuration",
    title: "Database defaults and instance choices",
    subtitle: "The editor identifies which settings this instance controls.",
  });
  await page.goto("/MasterListsPage/globalMenuManagement");
  const form = page.getByRole("form", {
    name: "Global Menu Management",
    exact: true,
  });
  await form
    .getByRole("button", { name: "Administration", exact: true })
    .click();
  await form.getByRole("button", { name: "More tools", exact: true }).click();
  await form.getByRole("button", { name: "Alerts", exact: true }).click();
  const editable = page
    .getByTestId("menu-fields-menu_alerts_standalone")
    .getByRole("combobox", { name: "Icon", exact: true });
  await expect(editable).toBeEnabled();
  await editable.scrollIntoViewIfNeeded();
  await demo.evidence("navigation-05-database-setting");
  await page
    .getByTestId("menu-settings-menu_section_reports")
    .getByRole("button", { name: "Reports", exact: true })
    .click();
  await page
    .getByTestId("menu-settings-menu_reports")
    .getByRole("button", { name: "Reports", exact: true })
    .click();
  const managed = page.getByTestId("menu-fields-menu_reports");
  await expect(
    managed.getByRole("combobox", { name: "Icon", exact: true }),
  ).toBeDisabled();
  await expect(
    managed.getByText("Managed by instance configuration"),
  ).toHaveCount(2);
  await managed.scrollIntoViewIfNeeded();
  await demo.evidence("navigation-06-instance-setting");
  await demo.chapter({
    eyebrow: "Automated implementation evidence",
    title: "Draft retained; configuration remains available",
    subtitle: "Human UAT acceptance is recorded separately in Grist.",
    durationMs: 4000,
  });
});
