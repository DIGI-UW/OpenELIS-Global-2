import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1060 — a vector order needs a test or panel on every sample, as an
 * environmental order does. The vector form only asked for a sample type, so
 * an order saved with a sample and no test, and the save notice never said a
 * test was missing.
 */

test("a vector sample without a test is named in the save notice and blocks Save", async ({
  page,
}) => {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const name = "OGC1060 Vector Pool";
  const created = await page.evaluate(async (sampleTypeName) => {
    const response = await fetch("/api/OpenELIS-Global/rest/SampleTypeCreate", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF") || "",
      },
      body: JSON.stringify({
        formName: "sampleTypeCreateForm",
        sampleTypeEnglishName: sampleTypeName,
        sampleTypeFrenchName: sampleTypeName,
        description: sampleTypeName.slice(0, 40),
        domain: "VECTOR",
        active: true,
        microbiologyIsolate: false,
      }),
    });
    return response.status;
  }, name);
  expect([200, 409], "the vector sample type must exist").toContain(created);

  await page.goto("/order/vector/enter", { waitUntil: "domcontentloaded" });
  const sampleType = page
    .getByRole("combobox", { name: "Sample Type" })
    .first();
  await expect(sampleType).toBeVisible({ timeout: NAV_TIMEOUT });
  await sampleType.click();
  await page.getByRole("option", { name }).click();

  const notice = page.getByRole("status").filter({
    hasText: "Required before saving",
  });
  await expect(notice).toContainText(
    "at least one test or panel on every sample",
    { timeout: UI_TIMEOUT },
  );
  await expect(notice).not.toContainText("at least one sample type");
  await expect(
    page.getByRole("button", { name: "Save", exact: true }),
  ).toBeDisabled();
});
