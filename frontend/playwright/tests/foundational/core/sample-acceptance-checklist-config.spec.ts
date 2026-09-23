import { expect, test, Page } from "../../../helpers/test-base";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

// S-09 (OGC-580) — Admin → General Configuration → Order Entry Configuration →
// Sample Acceptance Checklist. Drives the real config endpoints and asserts on
// visible UI state (per .specify/guides/playwright-best-practices.md):
// waitForResponse is used only to sync a mutation, never as the pass/fail check.

const ITEMS_URL = "/rest/sample-acceptance-checklist/admin/items";
const ENFORCEMENT_URL = "/rest/sample-acceptance-checklist/admin/enforcement/";

type Domain = "all" | "clinical" | "environmental" | "vector";

const DOMAIN_LINK: Record<Domain, string> = {
  all: "sampleAcceptanceAll",
  clinical: "sampleAcceptanceClinical",
  environmental: "sampleAcceptanceEnvironmental",
  vector: "sampleAcceptanceVector",
};

async function openDomain(page: Page, domain: Domain, headingFragment: RegExp) {
  await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });

  const adminNav = page.locator(".adminSideNav");
  await expect(adminNav).toBeVisible({ timeout: LONG_TIMEOUT });

  const menu = adminNav.getByRole("button", {
    name: /Sample Acceptance Checklist/i,
  });
  await expect(menu).toBeVisible({ timeout: UI_TIMEOUT });
  if ((await menu.getAttribute("aria-expanded")) !== "true") {
    await menu.click();
  }

  await adminNav.locator(`[data-cy="${DOMAIN_LINK[domain]}"]`).click();
  await expect(page).toHaveURL(
    new RegExp(`SampleAcceptanceChecklist/${domain}`),
    { timeout: LONG_TIMEOUT },
  );
  await expect(
    page.getByRole("heading", { name: headingFragment }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

test.describe("Sample Acceptance Checklist configuration", () => {
  test.describe.configure({ mode: "serial" });
  test.setTimeout(90_000);

  test("admin adds, edits, then deactivates a lab-wide checklist item", async ({
    page,
  }) => {
    // Unique per run so repeated runs never collide on the unique-label rule.
    const label = `E2E acceptance ${Date.now()}`;
    const edited = `${label} edited`;

    await openDomain(page, "all", /All domains/i);

    // --- Add ---
    await page.getByRole("button", { name: /Add item/i }).click();
    await page.getByRole("textbox", { name: "Label" }).fill(label);
    const addSync = page.waitForResponse(
      (r) => r.url().includes(ITEMS_URL) && r.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Save" }).click();
    await addSync;
    await expect(
      page.getByRole("cell", { name: label, exact: true }),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    // --- Edit the label ---
    await page
      .getByRole("row", { name: label })
      .getByRole("button", { name: /Edit/i })
      .click();
    await page.getByRole("textbox", { name: "Label" }).fill(edited);
    const editSync = page.waitForResponse(
      (r) =>
        r.url().includes(`${ITEMS_URL}/`) && r.request().method() === "PUT",
    );
    await page.getByRole("button", { name: "Save" }).click();
    await editSync;
    await expect(
      page.getByRole("cell", { name: edited, exact: true }),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    // --- Deactivate (also leaves the item out of the active list = cleanup) ---
    await page
      .getByRole("row", { name: edited })
      .getByRole("button", { name: /Edit/i })
      .click();
    await page
      .getByRole("combobox", { name: "Active" })
      .selectOption("inactive");
    const deactivateSync = page.waitForResponse(
      (r) =>
        r.url().includes(`${ITEMS_URL}/`) && r.request().method() === "PUT",
    );
    await page.getByRole("button", { name: "Save" }).click();
    await deactivateSync;
    await expect(
      page.getByRole("row", { name: edited }).getByText(/Inactive/i),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("admin sets and restores per-domain enforcement", async ({ page }) => {
    await openDomain(page, "clinical", /Clinical/i);

    const select = page.getByRole("combobox", {
      name: /Checklist enforcement/i,
    });
    await expect(select).toBeVisible({ timeout: UI_TIMEOUT });
    const original = (await select.inputValue()) || "OPTIONAL";

    try {
      const setSync = page.waitForResponse(
        (r) =>
          r.url().includes(ENFORCEMENT_URL) && r.request().method() === "PUT",
      );
      await select.selectOption("MANDATORY");
      await setSync;
      await expect(
        page.getByRole("combobox", { name: /Checklist enforcement/i }),
      ).toHaveValue("MANDATORY", { timeout: UI_TIMEOUT });

      // Reload to prove it persisted server-side (read back via getEnforcement).
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("combobox", { name: /Checklist enforcement/i }),
      ).toHaveValue("MANDATORY", { timeout: LONG_TIMEOUT });
    } finally {
      const restoreSync = page.waitForResponse(
        (r) =>
          r.url().includes(ENFORCEMENT_URL) && r.request().method() === "PUT",
      );
      await page
        .getByRole("combobox", { name: /Checklist enforcement/i })
        .selectOption(original);
      await restoreSync;
    }
  });
});
