import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyCultureWorkspace } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const firstOption = async (field: import("@playwright/test").Locator) => {
  const value = await field
    .locator("option")
    .evaluateAll((options) =>
      options.map((o) => (o as HTMLOptionElement).value).find(Boolean),
    );
  expect(value).toBeTruthy();
  await field.selectOption(value!);
};

test("inoculates cultures, appends readings, extends, subcultures and attaches Gram stain", async ({
  page,
}, testInfo) => {
  const seeded = await seedMicrobiologyCultureWorkspace(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}`, {
    waitUntil: "domcontentloaded",
  });
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("CULTURE");
  await page
    .getByRole("button", { name: "Inoculate culture", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Bottle or plate identifier").fill("M4-PARENT");
  await firstOption(dialog.getByLabel("Medium lot", { exact: true }));
  await firstOption(dialog.getByLabel("Atmosphere", { exact: true }));
  await expect(
    dialog.getByLabel("Incubation duration", { exact: true }),
  ).toHaveValue("48");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const parent = page
    .locator('[data-testid^="culture-row-"]')
    .filter({ has: page.getByRole("heading", { name: /M4-PARENT/ }) })
    .first();
  await expect(parent).toBeVisible({ timeout: LONG_TIMEOUT });
  const parentId = (await parent.getAttribute("data-testid"))!.replace(
    "culture-row-",
    "",
  );
  for (const note of [
    "First observation preserved",
    "Second observation appended",
  ]) {
    await parent
      .getByRole("button", { name: "Add reading", exact: true })
      .first()
      .click();
    await firstOption(dialog.getByLabel("Reading", { exact: true }));
    await dialog.getByLabel("Note", { exact: true }).fill(note);
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(parent.getByText(note, { exact: true })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  }
  await expect(
    parent.getByRole("button", { name: "Edit inoculated time" }),
  ).toHaveCount(0);
  await parent
    .getByRole("button", { name: "Extend incubation", exact: true })
    .first()
    .click();
  await dialog.getByLabel("Extend by", { exact: true }).fill("12");
  await dialog
    .getByLabel("Extension unit", { exact: true })
    .selectOption("HOURS");
  await firstOption(dialog.getByLabel("Extension reason", { exact: true }));
  await dialog
    .getByLabel("Note", { exact: true })
    .fill("Keep the original clock");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    parent.getByText("Keep the original clock", { exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await parent
    .getByRole("button", { name: "Gram stain", exact: true })
    .first()
    .click();
  await expect(
    page.locator("#culture-test-chooser").getByRole("button", { name: /— 1$/ }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Add selected tests", exact: true })
    .click();
  const tests = parent
    .getByRole("table", { name: "Testing and results", exact: true })
    .first();
  await expect(
    tests.getByRole("cell", { name: /^UAT Gram stain [A-F0-9]+$/ }),
  ).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await tests
    .getByRole("button", { name: "Enter or edit", exact: true })
    .click();
  await tests
    .getByRole("textbox", { name: /^Result for/ })
    .fill("Gram-positive cocci");
  await tests
    .getByRole("button", { name: "Save results", exact: true })
    .click();
  await expect(
    tests.getByText("Awaiting validation", { exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await parent
    .getByRole("button", { name: "Subculture", exact: true })
    .first()
    .click();
  await dialog.getByLabel("Bottle or plate identifier").fill("M4-CHILD");
  await firstOption(dialog.getByLabel("Medium lot", { exact: true }));
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  const child = page
    .locator('[data-testid^="culture-row-"]')
    .filter({ has: page.getByRole("heading", { name: /M4-CHILD/ }) })
    .last();
  await expect(child).toBeVisible({ timeout: LONG_TIMEOUT });
  await child
    .getByRole("button", { name: "Record outcome", exact: true })
    .click();
  await dialog.getByLabel("Outcome", { exact: true }).selectOption("NO_GROWTH");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(child.getByText("No growth", { exact: true })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await page.reload();
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption("CULTURE");
  await expect(
    page
      .getByTestId(`culture-row-${parentId}`)
      .getByText("First observation preserved", { exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(
    page.getByText("Second observation appended", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Keep the original clock", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Gram-positive cocci", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText("No growth", { exact: true })).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("culture-workspace-desktop.png"),
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(
    page.getByRole("button", { name: "Inoculate culture", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("culture-workspace-mobile.png"),
    fullPage: true,
  });
});
