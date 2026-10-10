import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { letters, seedPatient } from "../../../helpers/seed-patient-order";
import { csrfToken } from "../../../helpers/api-session";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1424 — clinical order entry v4 clean-up (FRS v0.20, slice M14): the
 * possible-match check before creating a patient, facility or provider; fax
 * fields only when switched on; Received by (you); the Handling group with
 * its mismatch tag; Mark tested elsewhere in the test row's menu; and the
 * retired fields gone from the clinical sample row.
 */

const API = "/api/OpenELIS-Global";

async function headers(page: Page) {
  return {
    "X-CSRF-Token": await csrfToken(page),
    "Content-Type": "application/json",
  };
}

test.describe("Order entry clean-up (OGC-1424)", () => {
  test("Create patient lists the existing patient for swapped names; Use this one and Create new anyway", async ({
    page,
  }) => {
    const existing = await seedPatient(page, {
      firstName: letters(9),
      lastName: letters(11),
    });

    await page.goto("/order/clinical/enter", { timeout: NAV_TIMEOUT });
    await expect(page.locator("#labNumber")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    await test.step("the Order section has no print shortcut", async () => {
      await expect(
        page.locator("main").getByText("Print Labels", { exact: true }),
      ).toHaveCount(0);
    });

    const enterSwappedNames = async () => {
      await page.getByRole("button", { name: "New Patient" }).click();
      const form = page.getByTestId("patient-search-section");
      await form.locator("#lastName").fill(existing.firstName);
      await form.locator("#firstName").fill(existing.lastName);
    };

    await test.step("Create patient shows the existing patient as a possible match", async () => {
      await enterSwappedNames();
      const check = page.waitForResponse((r) =>
        r.url().includes("/rest/possible-matches/patient"),
      );
      await page.getByTestId("order-create-patient").click();
      expect((await check).status()).toBe(200);
      const row = page.getByTestId(`possible-match-${existing.patientPK}`);
      await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row).toContainText("Matched on: name");
    });

    await test.step("Use this one puts the existing patient on the order", async () => {
      await page
        .getByTestId(`possible-match-use-${existing.patientPK}`)
        .click();
      await expect(
        page.getByTestId("patient-search-section").getByText("Selected"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(page.getByTestId("patient-search-section")).toContainText(
        existing.lastName,
      );
    });

    await test.step("Create new anyway is confirmed and recorded", async () => {
      await page
        .getByTestId("patient-search-section")
        .locator(".selected-card-header")
        .getByText("Clear", { exact: true })
        .click();
      await enterSwappedNames();
      await page.getByTestId("order-create-patient").click();
      await expect(
        page.getByTestId(`possible-match-${existing.patientPK}`),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Create new anyway" }).click();
      await expect(
        page.getByText(/Create a new record even though \d+ similar/),
      ).toBeVisible();
      const recorded = page.waitForResponse((r) =>
        r.url().includes("/rest/possible-matches/patient/override"),
      );
      await page.getByRole("button", { name: "Create new record" }).click();
      expect((await recorded).status()).toBe(201);
      await expect(page.getByTestId("order-new-patient-confirmed")).toBeVisible(
        { timeout: UI_TIMEOUT },
      );
    });

    await test.step("a second check shows the matches again, not the confirmation", async () => {
      await page.getByTestId("order-create-patient").click();
      await expect(
        page.getByTestId(`possible-match-${existing.patientPK}`),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(page.getByTestId("possible-matches-confirm")).toHaveCount(0);
      await page.getByRole("button", { name: "Cancel" }).click();
    });
  });

  test("Add new provider runs the check, fax stays hidden by default, and a new provider takes a title", async ({
    page,
  }) => {
    const search = await (
      await page.request.get(`${API}/rest/provider/search?search=a`)
    ).json();
    const provider = (search.providers || []).find(
      (p) =>
        (p.firstName || "").length >= 3 &&
        (p.lastName || "").length >= 3 &&
        /^[A-Za-z]+$/.test(p.firstName) &&
        /^[A-Za-z]+$/.test(p.lastName),
    );
    test.skip(!provider, "no provider with plain names to misspell");

    await page.goto("/order/clinical/enter", { timeout: NAV_TIMEOUT });
    await expect(page.locator("#providerName")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    await test.step("no fax field while showFaxFields is off", async () => {
      await expect(page.locator("#providerFax")).toHaveCount(0);
      await expect(page.locator("#siteContactFax")).toHaveCount(0);
    });

    const typed = `${provider.firstName}q ${provider.lastName}`;
    await page.locator("#providerName").fill(typed);
    const addNew = page.getByRole("button", {
      name: `+ Add new provider "${typed}"`,
    });
    await expect(addNew).toBeVisible({ timeout: UI_TIMEOUT });

    await test.step("the check lists the existing provider", async () => {
      await addNew.click();
      const dialog = page.getByTestId("possible-matches-dialog");
      await expect(
        dialog.getByText(`${provider.firstName} ${provider.lastName}`).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(dialog.getByText("Matched on: name").first()).toBeVisible();
    });

    await test.step("Create new anyway adds a new provider with a title picker", async () => {
      await page.getByRole("button", { name: "Create new anyway" }).click();
      const recorded = page.waitForResponse((r) =>
        r.url().includes("/rest/possible-matches/provider/override"),
      );
      await page.getByRole("button", { name: "Create new record" }).click();
      expect((await recorded).status()).toBe(201);
      await expect(page.locator("#providerTitle")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(page.locator("#providerLastName")).toHaveValue(
        provider.lastName,
      );
    });
  });

  test("Add new organization lists a facility named the same apart from common words; Use this one selects it", async ({
    page,
  }) => {
    const search = await (
      await page.request.get(`${API}/rest/organization/search?search=a`)
    ).json();
    const facility = (search.organizations || []).find((org) =>
      /^[A-Za-z][A-Za-z ]{5,40}$/.test(org.organizationName || ""),
    );
    test.skip(!facility, "no organization with a plain name");

    await page.goto("/order/clinical/enter", { timeout: NAV_TIMEOUT });
    const typed = `${facility.organizationName} Health Centre`;
    await page.locator("#siteName").fill(typed);
    const addNew = page.getByRole("button", {
      name: `+ Add new organization "${typed}"`,
    });
    await expect(addNew).toBeVisible({ timeout: UI_TIMEOUT });

    const check = page.waitForResponse((r) =>
      r.url().includes("/rest/possible-matches/facility"),
    );
    await addNew.click();
    expect((await check).status()).toBe(200);
    const row = page.getByTestId(`possible-match-${facility.id}`);
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(row).toContainText("Matched on: name");

    await page.getByTestId(`possible-match-use-${facility.id}`).click();
    await expect(page.locator("#siteName")).toHaveValue(
      facility.organizationName,
      { timeout: UI_TIMEOUT },
    );
  });

  test("Prepare Samples: Received by (you), the Handling group and its mismatch, and Mark tested elsewhere", async ({
    page,
  }) => {
    const testId = "13";
    const storageUrl = `${API}/rest/test-catalog/tests/${testId}/storage`;
    const storageBefore = await (await page.request.get(storageUrl)).json();
    await page.request.put(storageUrl, {
      headers: await headers(page),
      data: { ...storageBefore, storageCondition: "REFRIGERATED" },
    });

    try {
      const accessionNumber = await createSampleOrder(page, {
        testIds: testId,
      });
      expect(accessionNumber).not.toBe("");
      await page.goto(
        `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
        { timeout: NAV_TIMEOUT },
      );
      const card = page.getByTestId("sample-collection-card-0");
      await expect(card).toBeVisible({ timeout: NAV_TIMEOUT });

      await test.step("the clinical row has no retired fields", async () => {
        for (const label of [
          "Specimen Origin",
          "Collection Conditions",
          "Sample Temperature",
          "Lab performed sampling",
        ]) {
          await expect(card.getByLabel(label, { exact: true })).toHaveCount(0);
        }
      });

      await test.step("Received by reads as the signed-in user with Change", async () => {
        await expect(page.getByTestId("received-by-text")).toContainText(
          "(you)",
        );
        await expect(page.getByTestId("received-by-change")).toBeVisible();
      });

      await test.step("the Handling group shows the catalog requirement and flags a room-temperature arrival", async () => {
        await expect(page.getByTestId("handling-required-0")).toContainText(
          "Refrigerated",
          { timeout: UI_TIMEOUT },
        );
        await card
          .locator("#arrivalCondition-0")
          .selectOption("ROOM_TEMPERATURE");
        const flag = page.getByTestId("handling-mismatch-0");
        await expect(flag).toContainText("Handling mismatch");
        await expect(page.getByTestId("handling-report-nce-0")).toHaveAttribute(
          "href",
          new RegExp(`labNumber=${accessionNumber}&description=Needs`),
        );
      });

      await test.step("the arrival and the receiver are saved with the step and read back", async () => {
        const now = new Date();
        const dd = String(now.getUTCDate()).padStart(2, "0");
        const mm = String(now.getUTCMonth() + 1).padStart(2, "0");
        await card
          .locator("#receivedDate-0")
          .fill(`${dd}/${mm}/${now.getUTCFullYear()}`);
        await page.keyboard.press("Escape");
        await card.locator("#receivedTime-0").fill("09:30");
        await page.keyboard.press("Tab");
        await card.locator("#arrivalTemperature-0").fill("22");
        const saved = page.waitForResponse(
          (r) =>
            /rest\/SamplePatientEntry$/.test(r.url()) &&
            r.request().method() === "POST",
        );
        await page.getByRole("button", { name: "Save and exit" }).click();
        expect((await saved).status()).toBe(200);
        const order = await (
          await page.request.get(
            `${API}/rest/order/search?labNumber=${encodeURIComponent(accessionNumber)}`,
          )
        ).json();
        const stored = order.samples[0];
        expect(stored.arrivalCondition).toBe("ROOM_TEMPERATURE");
        expect(stored.arrivalTemperature).toBe("22");
        expect(stored.receivedById).not.toBe("");
      });

      await test.step("a temperature that cannot be stored is flagged and never clears the saved one", async () => {
        await page.goto(
          `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
          { timeout: NAV_TIMEOUT },
        );
        const temperature = page.locator("#arrivalTemperature-0");
        await expect(temperature).toHaveValue("22", { timeout: NAV_TIMEOUT });
        await temperature.fill("999");
        await expect(
          page.getByText("Enter a temperature from -100 to 60 °C."),
        ).toBeVisible();
        const saved = page.waitForResponse(
          (r) =>
            /rest\/SamplePatientEntry$/.test(r.url()) &&
            r.request().method() === "POST",
        );
        await page.getByRole("button", { name: "Save and exit" }).click();
        expect((await saved).status()).toBe(200);
        const order = await (
          await page.request.get(
            `${API}/rest/order/search?labNumber=${encodeURIComponent(accessionNumber)}`,
          )
        ).json();
        expect(order.samples[0].arrivalTemperature).toBe("22");
      });

      await test.step("Mark tested elsewhere from the test row's menu", async () => {
        await page.goto(
          `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
          { timeout: NAV_TIMEOUT },
        );
        const menu = page.getByTestId(`test-row-actions-${testId}`);
        await expect(menu).toBeVisible({ timeout: NAV_TIMEOUT });
        await menu.click();
        const marked = page.waitForResponse(
          (r) =>
            r.url().includes("/rest/order-tests/tested-elsewhere") &&
            r.request().method() === "PUT",
        );
        await page
          .getByRole("menuitem", { name: "Mark tested elsewhere" })
          .click();
        expect((await marked).status()).toBe(200);
        await expect(
          page.getByTestId(`tested-elsewhere-tag-${testId}`),
        ).toBeVisible();

        const value = page.locator(`#tested-elsewhere-value-${testId}`);
        await value.fill("Positive");
        const valueSaved = page.waitForResponse(
          (r) =>
            r.url().includes("/rest/order-tests/tested-elsewhere") &&
            r.request().method() === "PUT",
        );
        await value.blur();
        expect((await valueSaved).status()).toBe(200);
        const marks = await (
          await page.request.get(
            `${API}/rest/order-tests/tested-elsewhere?labNumber=${encodeURIComponent(accessionNumber)}`,
          )
        ).json();
        expect(marks).toEqual([
          expect.objectContaining({ testId, reportedValue: "Positive" }),
        ]);

        await menu.click();
        await page
          .getByRole("menuitem", { name: "Not tested elsewhere" })
          .click();
        await expect(
          page.getByTestId(`tested-elsewhere-tag-${testId}`),
        ).toHaveCount(0, { timeout: UI_TIMEOUT });
      });
    } finally {
      await page.request.put(storageUrl, {
        headers: await headers(page),
        data: storageBefore,
      });
    }
  });
});
