import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

/**
 * OGC-1201 AL/W end to end: an EQA order saves without a patient, and the
 * decision is recorded against the order rather than merely permitted.
 *
 * This is the behaviour that replaces the sentinel "NULL NULL" patient every
 * EQA order used to share, so it is worth proving against a running server
 * rather than a mock: the order must save, and the override must come back.
 */
test("AL/W: an EQA order saves with no patient and records why", async ({
  page,
  request,
}) => {
  test.setTimeout(180_000);

  // OGC-1483: an EQA order names the lab's enrollment it belongs to.
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const scheme = `PW1201 scheme ${Date.now()}`.slice(0, 40);
  const enrolled = await page.request.post(
    "/api/OpenELIS-Global/rest/eqa/my-programs",
    {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: { programName: scheme, provider: "PW" },
    },
  );
  expect(enrolled.status()).toBe(201);
  const enrollmentId = String((await enrolled.json()).id);

  await page.goto("/order/clinical/enter?eqa=true", {
    waitUntil: "domcontentloaded",
  });
  await expect(page.locator("#labNumber")).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: NAV_TIMEOUT,
  });
  const labNumber = await page.locator("#labNumber").inputValue();

  // Arriving through the EQA link declares both decisions up front.
  await expect(page.locator("#isEQASample")).toBeChecked();
  await expect(page.locator("#noPatientOverride")).toBeChecked();
  await expect(
    page.locator(`#eqaProgramId option[value="${enrollmentId}"]`),
  ).toHaveCount(1, { timeout: LONG_TIMEOUT });
  await page.locator("#eqaProgramId").selectOption(enrollmentId);

  // A sample type is the only other thing the save gate wants; no patient is
  // chosen anywhere in this flow.
  const sampleType = page
    .getByTestId("order-sample-test-section")
    .getByLabel("Sample Type");
  await expect(sampleType).toBeVisible({ timeout: LONG_TIMEOUT });
  await sampleType.selectOption({ label: "Serum" });

  const saveAndExit = page.getByRole("button", { name: "Save and exit" });
  await expect(saveAndExit).toBeEnabled({ timeout: LONG_TIMEOUT });
  await saveAndExit.click();

  // The order exists on the server.
  await expect
    .poll(
      async () => {
        const found = await request.get(
          `/api/OpenELIS-Global/rest/order/search?labNumber=${encodeURIComponent(labNumber)}`,
        );
        return found.status();
      },
      { timeout: LONG_TIMEOUT },
    )
    .toBe(200);

  // And the decision to go without a patient is recorded against it, with EQA
  // as the reason, which is what lets a downstream consumer tell a
  // proficiency sample from a clinical order that lost its patient.
  await expect
    .poll(
      async () => {
        const overrides = await request.get(
          `/api/OpenELIS-Global/rest/order-override/${encodeURIComponent(labNumber)}`,
        );
        if (overrides.status() !== 200) {
          return [];
        }
        return (await overrides.json()) as Array<Record<string, unknown>>;
      },
      { timeout: LONG_TIMEOUT },
    )
    .toContainEqual(
      expect.objectContaining({
        overrideType: "NO_PATIENT",
        reasonCode: "EQA",
      }),
    );

  await page.request.delete(
    `/api/OpenELIS-Global/rest/eqa/my-programs/${enrollmentId}`,
    { headers: { "X-CSRF-Token": await csrfToken(page) } },
  );
});
