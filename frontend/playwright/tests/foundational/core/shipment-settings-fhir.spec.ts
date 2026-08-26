import { test, expect, Page } from "../../../helpers/test-base";
import { SHORT_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Shipment Settings — cross-site identity and FHIR mapping (T-45 + T-44,
 * OGC-613 cross-site gap fixes).
 *
 * T-45: siteOrganizationFhirUuid is what makes cross-site shipping
 * addressable. Unset, the import filter is off and exports carry no supplier —
 * the Settings page must say so with a warning banner, and the banner must
 * clear once an organization is chosen.
 *
 * T-44: the non-conformity SNOMED codes are edited here and consumed by the
 * export transform through a real JSON parse; this asserts the UI write path
 * round-trips into the stored config.
 */

const REST = "/api/OpenELIS-Global/rest";
const SETTINGS_URL = "/SampleShipment/settings";
const RUN = Date.now().toString(36);

async function csrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  throw new Error("No CSRF token in storage state — auth.setup did not run?");
}

test.describe("Shipment Settings site organization warning (T-45)", () => {
  test("the banner shows while the site organization is unset and clears when it is chosen", async ({
    page,
  }) => {
    const csrf = await csrfToken(page);
    const headers = {
      "X-CSRF-Token": csrf,
      "Content-Type": "application/json",
    };

    // Remember what the stack had, restore no matter how the test ends.
    const before = await page.request.get(
      `${REST}/shipping-box/site-organization-uuid`,
    );
    expect(before.ok()).toBeTruthy();
    const original = (await before.json()) as { orgId?: string };

    try {
      // Unset ⇒ the warning is on the page.
      const cleared = await page.request.put(
        `${REST}/shipping-box/site-organization-uuid`,
        {
          headers,
          data: JSON.stringify(""),
        },
      );
      expect(cleared.ok()).toBeTruthy();

      await page.goto(SETTINGS_URL);
      await expect(page.getByText("Site organization not set")).toBeVisible({
        timeout: SHORT_TIMEOUT,
      });

      // Choose an organization through the picker ⇒ the warning clears and the
      // resolved FHIR UUID is shown.
      const picker = page.getByRole("combobox", { name: "This Laboratory" });
      await picker.click();
      const listbox = page.getByRole("listbox", { name: "This Laboratory" });
      const realOptions = listbox
        .getByRole("option")
        .filter({ hasNotText: "None (no filter)" });
      // Retrying visibility check doubles as "the stack has at least one
      // referral organization to pick" — count() would be a one-shot snapshot.
      await expect(realOptions.first()).toBeVisible();
      await realOptions.first().click();
      // Scoped to the tile so new Save buttons elsewhere cannot shift the index.
      await page
        .locator(".cds--tile", {
          has: page.getByRole("heading", { name: "Site Organization (FHIR)" }),
        })
        .getByRole("button", { name: "Save" })
        .click();

      await expect(
        page.getByText("Site organization not set"),
      ).not.toBeVisible();
      // Scoped to the paragraph: the save toast can quote the UUID too.
      await expect(page.locator("p", { hasText: /^FHIR UUID:/ })).toBeVisible();
    } finally {
      await page.request.put(`${REST}/shipping-box/site-organization-uuid`, {
        headers,
        data: JSON.stringify(original.orgId ?? ""),
      });
    }
  });
});

test.describe("Shipment Settings non-conformity codes (T-44)", () => {
  test("a code edited in the UI round-trips into the stored FHIR mapping config", async ({
    page,
  }) => {
    const csrf = await csrfToken(page);
    const headers = {
      "X-CSRF-Token": csrf,
      "Content-Type": "application/json",
    };

    const before = await page.request.get(
      `${REST}/shipping-box/fhir-mapping-config`,
    );
    expect(before.ok()).toBeTruthy();
    const original = await before.json();

    const customCode = `999${RUN.replace(/\D/g, "")}1`.slice(0, 9);
    try {
      await page.goto(SETTINGS_URL);
      const damagedCode = page.locator("#nc-RECEIVED_DAMAGED");
      await expect(damagedCode).toBeVisible({ timeout: SHORT_TIMEOUT });
      await damagedCode.fill(customCode);
      await page
        .locator(".cds--tile", {
          has: page.getByRole("heading", { name: "FHIR Mapping Options" }),
        })
        .getByRole("button", { name: "Save" })
        .click();

      // The stored config — what the export transform parses — carries the
      // code. Polled: the button disables while the save is still in flight,
      // so there is no visible "saved" moment to key off deterministically.
      await expect
        .poll(
          async () => {
            const after = await page.request.get(
              `${REST}/shipping-box/fhir-mapping-config`,
            );
            if (!after.ok()) return "";
            return ((await after.json()) as { nonConformityCodes: string })
              .nonConformityCodes;
          },
          { timeout: SHORT_TIMEOUT },
        )
        .toContain(customCode);
    } finally {
      await page.request.put(`${REST}/shipping-box/fhir-mapping-config`, {
        headers,
        data: JSON.stringify({
          containerTypeCode: original.containerTypeCode,
          containerTypeDisplay: original.containerTypeDisplay,
          nonConformityCodes: original.nonConformityCodes,
        }),
      });
    }
  });
});
