import { test, expect, Page } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1609 — saving a translated setting (the banner) posted back the record it
 * was served, and the derived label flags in it made the body unreadable: 400,
 * nothing stored. The banner now saves and the header shows it.
 */

const API = "/api/OpenELIS-Global";

interface BannerText {
  id: string;
  english: string;
  french: string;
}

async function readBanner(page: Page): Promise<BannerText> {
  return page.evaluate(async (api) => {
    const list = await (
      await fetch(`${api}/rest/SiteInformationMenu`, {
        credentials: "include",
      })
    ).json();
    const rows = list.menuList || list;
    const row = rows.find(
      (r: { elementName?: string; name?: string }) =>
        (r.elementName || r.name) === "bannerHeading",
    );
    const form = await (
      await fetch(`${api}/rest/SiteInformation?ID=${row.id}`, {
        credentials: "include",
      })
    ).json();
    return {
      id: row.id as string,
      english: form.localization.values.en.value as string,
      french: form.localization.values.fr.value as string,
    };
  }, API);
}

async function writeBanner(page: Page, banner: BannerText) {
  const status = await page.evaluate(
    async ({ api, banner }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const form = await (
        await fetch(`${api}/rest/SiteInformation?ID=${banner.id}`, {
          credentials: "include",
        })
      ).json();
      form.localization.english = banner.english;
      form.localization.french = banner.french;
      const res = await fetch(`${api}/rest/SiteInformation?ID=${banner.id}`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-CSRF-Token": csrf },
        body: JSON.stringify(form),
      });
      return res.status;
    },
    { api: API, banner },
  );
  expect(status, "the record the screen was served saves back").toBe(200);
}

test.describe("OGC-1609 translated setting save", () => {
  test("the banner saves from Site Information and the header shows it", async ({
    page,
  }) => {
    const siteInfo = new SiteInformationPage(page);
    await siteInfo.goto();
    const original = await readBanner(page);
    const stamp = Date.now().toString().slice(-6);
    const english = `Banner QA ${stamp}`;

    try {
      const row = page.locator("tr", { hasText: "bannerHeading" });
      await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
      await row.locator("label").first().click();
      await page.getByRole("button", { name: "Modify", exact: true }).click();
      const englishInput = page.getByRole("textbox", { name: "English" });
      await expect(englishInput).toHaveValue(original.english, {
        timeout: UI_TIMEOUT,
      });
      await englishInput.fill(english);
      await page.getByRole("button", { name: "Save", exact: true }).click();

      await expect(
        page.getByText("Configuration Updated Successfully"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        page.getByRole("button", { name: "Modify", exact: true }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });

      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.locator("header").getByText(english)).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      expect((await readBanner(page)).english).toBe(english);
    } finally {
      await writeBanner(page, original);
    }
  });
});
