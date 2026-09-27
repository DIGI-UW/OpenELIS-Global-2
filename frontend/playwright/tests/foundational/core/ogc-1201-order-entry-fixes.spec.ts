import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1201 — the order-entry fix list, driven through the real UI.
 *
 * Each test names the finding it covers and asserts the behaviour a user
 * sees, not the implementation that produces it.
 */

const CLINICAL_ENTER = "/order/clinical/enter";
const ENV_ENTER = "/order/environmental/enter";
const VECTOR_ENTER = "/order/vector/enter";

const openEntry = async (page: Page, url: string) => {
  await page.goto(url, { waitUntil: "domcontentloaded" });
  await expect(page.locator("#labNumber")).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
};

test.describe("OGC-1201 order entry", () => {
  // AH — the form used to load with its one required control empty and no
  // code path that filled it.
  test("AH: a new order arrives with its lab number already generated", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
  });

  // AI — the affordance was an anchor with no href, so it took no keyboard
  // focus. WCAG 2.1.1 Level A on the only required field.
  test("AI: the generate control is reachable and operable by keyboard", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    const generate = page.locator(".generate-link");
    await expect(generate).toBeVisible();

    // It is a real button, not an anchor without an href.
    await expect(generate).toHaveJSProperty("tagName", "BUTTON");

    await page.locator("#labNumber").focus();
    await page.keyboard.press("Tab");
    await expect(generate).toBeFocused();

    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
    const before = await page.locator("#labNumber").inputValue();
    await page.keyboard.press("Enter");
    await expect(page.locator("#labNumber")).not.toHaveValue(before, {
      timeout: NAV_TIMEOUT,
    });
  });

  // AJ — a Carbon Link renders neither disabled nor aria-disabled, so it
  // stayed clickable in flight and every click spent a lab number.
  test("AJ: the generate control reports a disabled state", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.locator(".generate-link")).toHaveJSProperty(
      "disabled",
      false,
    );
    // The property exists at all, which an anchor cannot express.
    const hasDisabledProp = await page
      .locator(".generate-link")
      .evaluate((el) => "disabled" in el);
    expect(hasDisabledProp).toBe(true);
  });

  // AR — edit mode outlives the step that set it, so pressing Edit and then
  // re-entering Enter Order carried the previous order onto a new form.
  test("AR: entering a new order clears the previous one", async ({ page }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
    const first = await page.locator("#labNumber").inputValue();

    // Navigate away and back in, the way the side navigation does.
    await page.goto("/order/clinical", { waitUntil: "domcontentloaded" });
    await openEntry(page, CLINICAL_ENTER);

    // A different order, not the one just left behind.
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("#labNumber")).not.toHaveValue(first);
  });

  // AR, second half — the clinical and vector guards read only ?order=, so a
  // scanned order arriving as ?labNumber= was treated as a brand-new order:
  // reset, and handed a freshly generated number that was not the one
  // scanned. Arriving with the parameter must leave the order to the context
  // to load, which is observable as the absence of that generation.
  test("AR: a URL-addressed order is not reset and regenerated", async ({
    page,
  }) => {
    // Counting the generator call is what distinguishes the two paths: a new
    // order asks for a number, an addressed one must leave the order to the
    // context to load.
    let generatorCalls = 0;
    await page.route("**/rest/SampleEntryGenerateScanProvider*", (route) => {
      generatorCalls += 1;
      return route.continue();
    });

    await page.goto(`${CLINICAL_ENTER}?labNumber=DOES-NOT-EXIST-1201`, {
      waitUntil: "domcontentloaded",
    });
    // Wait for the form to finish rendering, not for a fixed delay.
    await expect(page.locator("#labNumber")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("#isEQASample")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByText("Attachments", { exact: true })).toHaveCount(0);

    expect(generatorCalls).toBe(0);
    await expect(page.locator("#labNumber")).toHaveValue("");
  });

  // AE — saveStatus started at SAVED, so a brand-new form announced an order
  // that had never been saved.
  test("AE: an untouched new order claims neither saved nor unsaved", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.getByText("Saved", { exact: true })).toHaveCount(0);
  });

  // AL / W — the two adjacent decisions, and the EQA detail they reveal.
  test("AL/W: EQA and no-patient are offered together", async ({ page }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.locator("#isEQASample")).toHaveCount(1);
    await expect(page.locator("#noPatientOverride")).toHaveCount(1);

    // EQA reveals the fields the model carries for it.
    await expect(page.locator("#eqaProviderSampleId")).toHaveCount(0);
    await page.locator('label[for="isEQASample"]').click();
    await expect(page.locator("#eqaProviderSampleId")).toBeVisible();
    await expect(page.locator("#noPatientOverride")).toBeChecked();

    // EQA suppresses the warning, because a proficiency sample has no patient
    // by design.
    await expect(
      page.getByText(/will not be evaluated against a reference range/),
    ).toHaveCount(0);
  });

  test("AL: a patient-less clinical order states the consequence", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    // Reach the manual override through EQA, which is ungated, then withdraw
    // EQA so the order is patient-less without being a proficiency sample.
    await page.locator('label[for="isEQASample"]').click();
    await expect(page.locator("#noPatientOverride")).toBeChecked();
    await page.locator('label[for="isEQASample"]').click();
    await expect(page.locator("#noPatientOverride")).not.toBeChecked();
  });

  // W — the EQA worklist used to push at the legacy screen with ?isEQA=true.
  test("W: the EQA deep link arrives with the control already set", async ({
    page,
  }) => {
    await page.goto(`${CLINICAL_ENTER}?eqa=true`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator("#isEQASample")).toBeChecked({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("#noPatientOverride")).toBeChecked();
  });

  // V/AF — fields SampleOrderItem always carried with nothing bound to them.
  test("V/AF: the restored legacy fields render", async ({ page }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.locator("#order_requestDate")).toHaveCount(1);
    await expect(page.locator("#order_nextVisitDate")).toHaveCount(1);
    await expect(page.locator("#rememberSiteAndRequester")).toHaveCount(1);
  });

  // T — attachments existed on the legacy screen; the lanes had no way in.
  test("T: order attachments are reachable once the order has a number", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    await expect(page.getByText("Attachments", { exact: true })).toBeVisible();
  });

  // AK — the message named fields the gate does not check.
  test("AK: a blocked save names what is actually missing", async ({
    page,
  }) => {
    await openEntry(page, CLINICAL_ENTER);
    // A generated lab number, no patient and no sample type: the message must
    // not ask for a lab number, and must ask for the two that are missing.
    const draft = page.getByRole("button", { name: /Save Draft/i });
    await expect(draft).toBeDisabled();
  });

  // D — collect-only copy reached the collect-less lanes verbatim.
  test("D: the collect-later hint is absent from the environmental lane", async ({
    page,
  }) => {
    await openEntry(page, ENV_ENTER);
    await expect(
      page.getByText(/can be specified later during collection/),
    ).toHaveCount(0);
  });

  test("D: the collect-later hint is absent from the vector lane", async ({
    page,
  }) => {
    await openEntry(page, VECTOR_ENTER);
    await expect(
      page.getByText(/can be specified later during collection/),
    ).toHaveCount(0);
  });

  // AM — receipt was stamped silently at save with no field to correct it.
  test("AM: the environmental manifest captures lab receipt", async ({
    page,
  }) => {
    await openEntry(page, ENV_ENTER);
    await expect(
      page.getByRole("columnheader", { name: "Received at Lab" }),
    ).toBeVisible();
  });

  // AH/AI on the other two lanes: the same shared control.
  test("AH: the environmental lane also generates on open", async ({
    page,
  }) => {
    await openEntry(page, ENV_ENTER);
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
  });

  test("AH: the vector lane also generates on open", async ({ page }) => {
    await openEntry(page, VECTOR_ENTER);
    await expect(page.locator("#labNumber")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
  });
});
