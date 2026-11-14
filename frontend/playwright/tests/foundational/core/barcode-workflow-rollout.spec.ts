import { expect, test } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Barcode workflow rollout (OGC-284)", () => {
  test("Generic Sample Order shows the shared labels section", async ({
    page,
  }) => {
    await page.goto("/GenericSample/Order", { waitUntil: "domcontentloaded" });

    await expect(
      page.getByRole("heading", { name: /label quantities/i }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    const labels = page.getByTestId("labels-section-root");
    await expect(labels).toBeVisible();
    await expect(labels.getByLabel(/order labels/i)).toBeVisible();
    await expect(labels.getByText(/running total/i)).toBeVisible();
  });
});
