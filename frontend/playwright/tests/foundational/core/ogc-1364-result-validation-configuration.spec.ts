import { test, expect } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1364 v3 — with the validation policy on, Result & Validation
 * Configuration shows the result settings as switches grouped by purpose, each
 * group with its own Save, instead of the select-a-row-then-Modify table.
 */

const FLAG = "Use the validation policy";
const MESSAGE = "Critical result message";

test("the result settings are grouped switches, saved per group", async ({
  page,
}) => {
  const menu = new SiteInformationPage(page, "ResultConfigurationMenu");
  await menu.goto();
  const flagBefore = /true/i.test(await menu.getSettingValue(FLAG));
  await menu.setBooleanSetting(FLAG, true);

  let messageBefore = "";
  try {
    await menu.goto();
    await expect(
      page.getByRole("heading", { name: "Result & Validation Configuration" }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(page.getByTestId("settings-group-resultEntry")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Modify", exact: true }),
    ).toHaveCount(0);

    const technician = page.getByRole("switch", {
      name: /^Require the technician name/,
    });
    const autofill = page.getByRole("switch", {
      name: /^Show an autofill box/,
    });
    if ((await technician.getAttribute("aria-checked")) !== "true") {
      await expect(autofill).toBeDisabled();
    }

    messageBefore = await menu.getSettingValue(MESSAGE);
    const stamp = `Call the ward ${Date.now().toString().slice(-6)}`;
    await menu.setTextSetting(MESSAGE, stamp);
    await menu.goto();
    expect(await menu.getSettingValue(MESSAGE)).toBe(stamp);
    await expect(page.getByTestId("save-group-resultEntry")).toBeDisabled({
      timeout: UI_TIMEOUT,
    });
  } finally {
    await menu.goto();
    if (messageBefore) {
      await menu.setTextSetting(MESSAGE, messageBefore);
    }
    await menu.setBooleanSetting(FLAG, flagBefore);
  }
});
