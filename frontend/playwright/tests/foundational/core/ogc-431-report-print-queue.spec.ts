import {
  test,
  expect,
  type Locator,
  type Page,
} from "../../../helpers/test-base";
import {
  createSampleOrder,
  enterResults,
  validateResults,
} from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-431: Reports › Report Print Queue lists an order once a result is
 * released, prints it as version 1 through the queue pipeline, serves the
 * archived file back byte for byte, and records a reprint without a new
 * version. The order is created, resulted and released through the app's
 * API; the queue is driven as a user drives it.
 *
 * The page opens the printed PDF in a new tab through a blob URL, which a
 * headless browser would download; the spec keeps the URL the page opens
 * and checks the print response's status and headers. The bytes are
 * compared through the API (archived file against a reprint), because the
 * browser keeps no copy of a no-store response for Playwright to read.
 */

/**
 * The first referring site and provider the Add Order form offers, so the
 * seeded order carries parties that exist on this deployment rather than
 * fixture ids that only the CI database has.
 */
async function orderParties(
  page: Page,
): Promise<{ referringSiteId?: string; providerPersonId?: string }> {
  const response = await page.request.get(
    `${API_PREFIX}/rest/SamplePatientEntry`,
  );
  expect(response.status(), "load Add Order form").toBe(200);
  const form = (await response.json()) as {
    sampleOrderItems?: {
      referringSiteList?: { id: string }[];
      providersList?: { id: string }[];
    };
  };
  return {
    referringSiteId: form.sampleOrderItems?.referringSiteList?.[0]?.id,
    providerPersonId: form.sampleOrderItems?.providersList?.[0]?.id,
  };
}

async function queueRow(page: Page, accession: string): Promise<Locator> {
  await page.goto(`/ReportPrintQueue?labNo=${accession}`, {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "Report Print Queue" }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  const row = page.getByTestId(`queue-row-${accession}`);
  await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
  return row;
}

/**
 * Clicks Print on a row and returns the print response's status and headers.
 * The body is not read here: the app answers with no-store caching, so the
 * browser keeps nothing for Playwright to read back once the page has
 * turned it into a blob. Bytes are compared through the API instead.
 */
async function printFromRow(page: Page, row: Locator) {
  const printed = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/reportQueue/print") &&
      response.request().method() === "POST",
  );
  await row.getByRole("button", { name: "Print", exact: true }).click();
  const response = await printed;
  expect(response.status()).toBe(200);
  expect(response.headers()["content-type"]).toContain("application/pdf");
  return response;
}

/** The queue's sample id of an accession, as the list reports it. */
async function sampleIdOf(page: Page, accession: string): Promise<number> {
  const listed = await page.request.get(
    `${API_PREFIX}/rest/reportQueue?labNo=${encodeURIComponent(accession)}`,
  );
  expect(listed.status(), "queue lookup").toBe(200);
  const body = (await listed.json()) as { rows?: { sampleId: number }[] };
  expect(body.rows?.length, `queue row for ${accession}`).toBe(1);
  return body.rows![0].sampleId;
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    (window as unknown as { __openedUrls: string[] }).__openedUrls = [];
    window.open = (url?: string | URL) => {
      (window as unknown as { __openedUrls: string[] }).__openedUrls.push(
        String(url),
      );
      return null;
    };
  });
});

test("a released order is queued, printed as version 1, archived and reprinted", async ({
  page,
}) => {
  const accession =
    await test.step("create an order with a result", async () => {
      const labNo = await createSampleOrder(page, {
        labNo: "",
        receivedDate: "",
        receivedTime: "",
        ...(await orderParties(page)),
      });
      expect(labNo).not.toBe("");
      await enterResults(page, labNo);
      return labNo;
    });

  await test.step("before release the lookup finds it with nothing to print", async () => {
    const row = await queueRow(page, accession);
    // "Not issued" in en.json; a regional overlay keeps "Not printed" until Transifex syncs.
    await expect(row).toContainText(/Not (issued|printed)/);
    // The tag repeats its text in a hidden definition tooltip.
    await expect(
      row.getByTestId(`nothing-released-${accession}`),
    ).toContainText("Nothing released yet on this order");
    await expect(
      row.getByRole("button", { name: "Print", exact: true }),
    ).toBeDisabled();
  });

  await test.step("release it", () => validateResults(page, accession));

  const row = await test.step("it lists as Final and Unprinted", async () => {
    const released = await queueRow(page, accession);
    await expect(released).toContainText("Final");
    await expect(released).toContainText("Unprinted");
    await expect(released).toContainText(/Not (issued|printed)/);
    return released;
  });

  await test.step("print it as version 1", async () => {
    const print = await printFromRow(page, row);
    expect(print.headers()["x-report-version"]).toBe("1");
    expect(print.headers()["x-report-reprint"]).toBe("false");
    await expect(
      page.getByText(`Printed version 1 of ${accession}`),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    const opened = await page.evaluate(
      () => (window as unknown as { __openedUrls: string[] }).__openedUrls,
    );
    expect(opened[0]).toMatch(/^blob:/);
    await expect(row).toContainText("Printed");
    await expect(row).toContainText("v1");
  });

  const archivedBytes =
    await test.step("the version history serves the archived file back", async () => {
      await row.getByRole("button", { name: /expand/i }).click();
      const openPdf = page.getByTestId("open-pdf-1");
      await expect(openPdf).toBeVisible({ timeout: UI_TIMEOUT });
      const href = await openPdf.getAttribute("href");
      expect(href).toContain("/rest/reportQueue/versions/");
      const archived = await page.request.get(href as string);
      expect(archived.headers()["content-type"]).toContain("application/pdf");
      const bytes = await archived.body();
      expect(bytes.subarray(0, 5).toString()).toBe("%PDF-");
      return bytes;
    });

  await test.step("printing again is a reprint of the same version", async () => {
    const print = await printFromRow(page, row);
    expect(print.headers()["x-report-version"]).toBe("1");
    expect(print.headers()["x-report-reprint"]).toBe("true");
    await expect(
      page.getByText(`Reprinted version 1 of ${accession}`),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.getByText(/^Reprinted .* by /)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    const reprint = await page.request.post(
      `${API_PREFIX}/rest/reportQueue/print`,
      {
        headers: { "X-CSRF-Token": await csrfToken(page) },
        data: { sampleId: await sampleIdOf(page, accession) },
      },
    );
    expect(reprint.status()).toBe(200);
    expect(reprint.headers()["x-report-reprint"]).toBe("true");
    expect((await reprint.body()).equals(archivedBytes)).toBe(true);
  });
});

/** The queue's detail of an order: its versions, newest last. */
async function versionsOf(page: Page, accession: string) {
  const detail = await page.request.get(
    `${API_PREFIX}/rest/reportQueue/orders/${await sampleIdOf(page, accession)}`,
  );
  expect(detail.status(), "queue detail").toBe(200);
  return (
    (await detail.json()) as {
      versions: { version: number; changeSummary?: string }[];
    }
  ).versions;
}

/** The first two numeric tests the server offers on Serum, by id and name. */
async function twoNumericSerumTests(page: Page) {
  const response = await page.request.get(
    `${API_PREFIX}/rest/sample-type-tests?sampleType=2`,
  );
  expect(response.status(), "Serum test list").toBe(200);
  const tests = (
    (await response.json()) as {
      tests?: { id: string; name: string; resultType?: string }[];
    }
  ).tests?.filter((t) => t.resultType === "N");
  expect(tests?.length, "two numeric Serum tests").toBeGreaterThan(1);
  return tests!.slice(0, 2);
}

/** The same value with its last digit moved up by one, keeping its precision. */
function bumped(value: string): string {
  const last = Number(value.slice(-1));
  return last < 9
    ? `${value.slice(0, -1)}${last + 1}`
    : `${value.slice(0, -1)}${last - 1}`;
}

/**
 * OGC-1529: a printed result corrected on Results waits for validation. Until
 * then the order stays Printed and a print reprints version 1; once the
 * correction is validated the next print is one corrected version naming the
 * old and the new value, never one that removes the result and adds it back.
 */
test("a correction awaiting validation keeps the order printed until it is validated", async ({
  page,
}) => {
  test.setTimeout(180_000);
  const tests = await twoNumericSerumTests(page);
  const accession =
    await test.step("print version 1 of a released order", async () => {
      const labNo = await createSampleOrder(page, {
        labNo: "",
        receivedDate: "",
        receivedTime: "",
        sampleTypeId: "2",
        testIds: tests.map((t) => t.id).join(","),
        ...(await orderParties(page)),
      });
      expect(labNo).not.toBe("");
      await enterResults(page, labNo);
      await validateResults(page, labNo);
      const row = await queueRow(page, labNo);
      const print = await printFromRow(page, row);
      expect(print.headers()["x-report-version"]).toBe("1");
      return labNo;
    });

  const corrected =
    await test.step("correct one result on Results without validating it", async () => {
      await page.goto(`/Results?accessionNumber=${accession}`, {
        waitUntil: "domcontentloaded",
        timeout: NAV_TIMEOUT,
      });
      const row = page
        .getByRole("row")
        .filter({ hasText: `${tests[1].name}(Serum)` });
      const edit = row.getByRole("button", { name: "Edit", exact: true });
      await expect(edit).toBeVisible({ timeout: UI_TIMEOUT });
      await edit.click();
      const box = row.getByRole("textbox", { name: /^Result for / });
      await expect(box).toBeVisible({ timeout: UI_TIMEOUT });
      const before = await box.inputValue();
      const after = bumped(before);
      await box.fill(after);
      await box.press("Tab");
      const saved = page.waitForResponse(
        (response) =>
          /\/rest\/results-entry\/analysis\/\d+\/result/.test(response.url()) &&
          response.request().method() === "POST",
      );
      await row.getByRole("button", { name: "Save", exact: true }).click();
      expect((await saved).status()).toBe(200);
      return after;
    });

  await test.step("the order stays Printed and a print is a reprint", async () => {
    const row = await queueRow(page, accession);
    await expect(row).toContainText("Printed");
    await expect(row).not.toContainText("Unprinted");
    await expect(row).not.toContainText("Amended");
    await expect(
      row.getByTestId(`awaiting-validation-${accession}`),
    ).toContainText("Correction awaiting validation");
    const print = await printFromRow(page, row);
    expect(print.headers()["x-report-version"]).toBe("1");
    expect(print.headers()["x-report-reprint"]).toBe("true");
    expect(await versionsOf(page, accession)).toHaveLength(1);
  });

  await test.step("once validated it prints one corrected version", async () => {
    await validateResults(page, accession);
    const row = await queueRow(page, accession);
    await expect(row).toContainText("Amended");
    await expect(row).toContainText("Unprinted");
    const print = await printFromRow(page, row);
    expect(print.headers()["x-report-version"]).toBe("2");
    const versions = await versionsOf(page, accession);
    expect(versions).toHaveLength(2);
    const summary = versions.find((v) => v.version === 2)?.changeSummary ?? "";
    expect(summary).toContain(`→ ${corrected}`);
    expect(summary).not.toMatch(/Removed|Added/);
  });
});

test("the home dashboard's unprinted results tile opens the queue", async ({
  page,
}) => {
  await page.goto("/", { waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
  const tile = page.getByText("UnPrinted Results Today");
  await expect(tile).toBeVisible({ timeout: UI_TIMEOUT });
  // The queue lists as soon as it mounts, so listen before the click.
  const listed = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/reportQueue?") &&
      response.url().includes("printStatus=UNPRINTED"),
  );
  await tile.click();
  await expect(page).toHaveURL(
    /\/ReportPrintQueue\?type=patient&printStatus=unprinted/,
    { timeout: UI_TIMEOUT },
  );
  await expect(
    page.getByRole("heading", { name: "Report Print Queue" }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  await listed;
  // "Print status" is also a column header; the filter is the combobox,
  // whose title is the selected item (its text adds the icon's label).
  await expect(
    page.getByRole("combobox", { name: "Print status" }),
  ).toHaveAttribute("title", "Unprinted", { timeout: UI_TIMEOUT });
});

test("a lab number range whose end comes first is refused on the page", async ({
  page,
}) => {
  await page.goto("/ReportPrintQueue", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "Report Print Queue" }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  // The toggle's label sits over the switch and takes the click.
  await page.locator('label[for="queue-lab-range"]').click();
  await expect(page.getByRole("switch", { name: "Range" })).toBeChecked();
  const from = page.getByRole("textbox", { name: "Lab Number", exact: true });
  const to = page.getByRole("textbox", { name: "To lab number" });
  await from.fill("DEV01260000000000900");
  await to.fill("DEV01260000000000100");
  await to.press("Enter");

  await expect(
    page.getByText("The To lab number is before the From lab number"),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  await expect(page.getByTestId("active-search")).toHaveCount(0);

  const refused = await page.request.get(
    `${API_PREFIX}/rest/reportQueue?labNo=DEV01260000000000900&labNoTo=DEV01260000000000100`,
  );
  expect(refused.status()).toBe(400);
  expect((await refused.json()).code).toBe("labRangeReversed");
});

test.describe("as a user without the Reports role", () => {
  // The EQA participant is a bench user with Results and Reception only.
  test.use({ storageState: "playwright/.auth/participant.json" });

  test("the queue is refused on the API and the page", async ({ page }) => {
    const listed = await page.request.get(
      `${API_PREFIX}/rest/reportQueue?pageSize=10`,
    );
    expect(listed.status()).toBe(403);
    const preferences = await page.request.get(
      `${API_PREFIX}/rest/reportQueue/preferences`,
    );
    expect(preferences.status()).toBe(403);

    await page.goto("/ReportPrintQueue", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByText("Access Denied")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { name: "Report Print Queue" }),
    ).not.toBeVisible();
  });
});
