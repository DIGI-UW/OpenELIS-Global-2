import { test, expect, type Locator } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

async function expand(group: Locator) {
  await expect(group).toBeVisible({ timeout: NAV_TIMEOUT });
  if ((await group.getAttribute("aria-expanded")) !== "true") {
    await group.click();
  }
  await expect(group).toHaveAttribute("aria-expanded", "true");
}

test.describe("Help menu", () => {
  test("the manual and the request forms open in a new tab", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const nav = page.getByRole("navigation", { name: "Side navigation" });

    await expand(nav.getByRole("button", { name: "Help", exact: true }));
    const manual = nav.getByRole("link", { name: "User Manual", exact: true });
    await expect(manual).toHaveAttribute("href", "/docs/UserManual");

    await expand(
      nav.getByRole("button", { name: "Process Documentation", exact: true }),
    );
    const vlForm = nav.getByRole("link", { name: "VL Form", exact: true });
    await expect(vlForm).toHaveAttribute(
      "href",
      /^\/documentation\/FICHE_DEMANDE_CHARGE_VIRALE_VF_\d+\.pdf$/,
    );
    const dbsForm = nav.getByRole("link", { name: "DBS Form", exact: true });
    await expect(dbsForm).toHaveAttribute(
      "href",
      /^\/documentation\/DBS_Identn_\d+[A-Za-z]+\d+\.pdf$/,
    );

    for (const link of [manual, vlForm, dbsForm]) {
      await expect(link).toHaveAttribute("target", "_blank");
      await expect(link).toHaveAttribute("rel", "noopener noreferrer");
    }
  });

  test("following a help link leaves the application tab where it was", async ({
    page,
    context,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const nav = page.getByRole("navigation", { name: "Side navigation" });
    await expand(nav.getByRole("button", { name: "Help", exact: true }));
    const home = page.url();

    const opened = context.waitForEvent("page");
    await nav.getByRole("link", { name: "User Manual", exact: true }).click();
    const manual = await opened;

    await expect.poll(() => manual.url()).toContain("/docs/UserManual");
    await expect(page).toHaveURL(home);
    await manual.close();
  });
});
