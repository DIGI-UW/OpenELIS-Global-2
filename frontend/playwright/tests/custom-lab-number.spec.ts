import { test, expect } from "@playwright/test";

test.describe("Custom Lab Number Configuration", () => {
  test("configure custom regex and template, then verify generation", async ({
    page,
  }) => {
    // 1. Navigate to Lab Number Management
    await page.goto("MasterListsPage/labNumber", {
      waitUntil: "domcontentloaded",
    });

    // 2. Select CUSTOM type
    const typeDropdown = page.locator("#lab_number_type");
    await expect(typeDropdown).toBeVisible({ timeout: 10000 });
    await typeDropdown.selectOption("CUSTOM");

    // 3. Fill in custom regex and template
    const regexInput = page.locator("#customAccessionRegex");
    const templateInput = page.locator("#customAccessionTemplate");

    await expect(regexInput).toBeVisible();
    await expect(templateInput).toBeVisible();

    await regexInput.fill("^LAB-\\d{4}-\\d{2}-\\d{6}$");
    await templateInput.fill("LAB-{YYYY}-{MM}-{SEQ:6}");

    // 4. Verify live preview
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = ("" + (now.getMonth() + 1)).padStart(2, "0");
    const expectedPreview = `LAB-${yyyy}-${mm}-000001`;

    await expect(page.locator("li code").first()).toHaveText(expectedPreview);

    // 5. Test sandbox
    const testInput = page.locator("#testInput");
    await testInput.fill(expectedPreview);
    await expect(page.getByText("Matches regex")).toBeVisible();

    await testInput.fill("INVALID-NUMBER");
    await expect(page.getByText("Does not match regex")).toBeVisible();

    // 6. Submit
    const submitButton = page.locator('[data-testid="submit-button"]');
    await submitButton.click();

    // 7. Verify success notification (using Carbon notification selector)
    await expect(page.locator(".cds--inline-notification")).toBeVisible({
      timeout: 15000,
    });
  });
});
