import { randomInt, randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { API, send } from "../../../helpers/analyzer-profile-api";
import { printedReport } from "../../../helpers/patient-report";

// The clinical value crosses the critical boundary although every configured
// display rounds it to 6. Each case owns its catalog rows and patient order.
for (const places of [null, 0, 1, 2]) {
  test(`critical boundary and printable result with ${places ?? "unset"} reporting decimal places`, async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const run = randomUUID().slice(0, 8);
    const [target] = await seedNumericTests(
      page,
      run,
      [
        {
          name: `Reporting boundary ${run}`,
          loinc: `${randomInt(1_000_000, 9_000_000)}-2`,
        },
      ],
      {
        testSection: "Molecular Biology",
        sampleType: "Plasma",
        reportingDecimalPlaces: 2,
      },
    );
    const path = `/test-catalog/tests/${target.id}/sample-results`;
    const configuration = async () =>
      (await page.request.get(`${API}${path}`)).json();
    const setPlaces = async (value: number | null) => {
      const before = await configuration();
      expect(before.components).toHaveLength(1);
      await send(page, "put", path, {
        testId: target.id,
        version: before.version,
        components: before.components.map(
          (component: Record<string, unknown>) => ({
            ...component,
            reportingDecimalPlaces: value,
          }),
        ),
      });
      const after = await configuration();
      // The application's NON_NULL JSON policy omits an unset property.
      expect(after.components[0].reportingDecimalPlaces ?? null).toBe(value);
      return after.components[0].id;
    };
    // Explicitly clearing an authored count must survive a separate read.
    await setPlaces(null);
    const componentId = await setPlaces(places);
    await send(page, "put", `/test-catalog/tests/${target.id}/ranges`, {
      testId: target.id,
      ranges: [
        {
          componentId,
          gender: null,
          minAge: 0,
          maxAge: null,
          lowNormal: 0,
          highNormal: 5,
          lowCritical: null,
          highCritical: 6,
          lowValid: 0,
          highValid: 100,
        },
      ],
    });
    const order = await createClinicalOrder(page, {
      testIds: [target.id],
      specimenName: "Plasma",
    });
    await page.goto(`/Results?accessionNumber=${order.accession}`);
    // The worklist adds the specimen to the catalog name. This order owns one
    // test and one editable numeric component; do not depend on that suffix.
    const row = page.getByRole("row").filter({ hasText: target.name });
    await expect(row).toHaveCount(1, { timeout: 15_000 });
    const input = row.getByRole("textbox");
    await expect(input).toHaveCount(1);
    await input.fill("6.004");
    await row.getByRole("button", { name: "Save", exact: true }).click();
    const modal = page.getByTestId("result-alert-modal");
    await expect(modal).toBeVisible();
    await expect(modal).toContainText("6.004");
    await modal.getByRole("button", { name: /Acknowledge and save/ }).click();
    const savedRow = page.getByRole("row").filter({ hasText: target.name });
    await expect(savedRow.getByTestId("flag-CRITICAL")).toBeVisible();
    await expect(
      savedRow.getByRole("button", { name: "Edit", exact: true }),
    ).toBeVisible();
    const read = async () => {
      const body = await (
        await page.request.get(
          `${API}/accession-results?accessionNumber=${order.accession}`,
        )
      ).json();
      return body.testResult.find(
        (item: { testId: string }) => item.testId === target.id,
      );
    };
    const saved = await read();
    expect(saved.rawResultValue).toBe("6.004");
    expect(saved.resultValue).toBe(
      places === null ? "6.004" : (6).toFixed(places),
    );
    await page.goto(`/validation?labNumber=${order.accession}`);
    const validationRow = page
      .getByRole("row")
      .filter({ hasText: target.name });
    await expect(validationRow).toHaveCount(1);
    await validationRow.getByRole("button", { name: /expand/i }).click();
    await expect(page.getByTestId("critical-banner")).toBeVisible();
    await page
      .getByRole("button", { name: "Validate & release", exact: true })
      .click();
    await expect(validationRow).toHaveCount(0);
    expect((await read()).rawResultValue).toBe("6.004");
    await page.goto(`/ReportPrintQueue?labNo=${order.accession}`);
    const queued = page.getByTestId(`queue-row-${order.accession}`);
    await expect(queued).toContainText("Critical");
    await queued.getByRole("button", { name: /expand/i }).click();
    await expect(page.getByText(target.name)).toBeVisible();
    if (places !== null) {
      await expect(
        page.getByText("Value used for clinical decisions: 6.004", {
          exact: true,
        }),
      ).toBeVisible();
    }
    await page.screenshot({
      path: testInfo.outputPath("critical-report-preview.png"),
      fullPage: true,
    });
    const pdf = await printedReport(page, order.accession);
    expect(pdf.subarray(0, 5).toString()).toBe("%PDF-");
    await testInfo.attach(`report-${places ?? "unset"}.pdf`, {
      body: pdf,
      contentType: "application/pdf",
    });
    await testInfo.attach("clinical-boundary", {
      body: JSON.stringify({
        accession: order.accession,
        testId: target.id,
        places,
        clinical: "6.004",
        criticalAbove: 6,
        saved: await read(),
      }),
      contentType: "application/json",
    });
  });
}

test("an explicitly configured report logarithm follows its component after renaming", async ({
  page,
}, testInfo) => {
  test.setTimeout(180_000);
  const run = randomUUID().slice(0, 8);
  const [target] = await seedNumericTests(
    page,
    run,
    [
      {
        name: `Log display ${run}`,
        loinc: `${randomInt(1_000_000, 9_000_000)}-3`,
      },
    ],
    {
      testSection: "Molecular Biology",
      sampleType: "Plasma",
      reportingDecimalPlaces: 2,
    },
  );
  const editor = `/MasterListsPage/TestCatalogEditor/${target.id}/sample-results`;
  await page.goto(editor);
  const checkbox = page.getByRole("checkbox", {
    name: "Show calculated log10 on patient reports",
    exact: true,
  });
  await expect(checkbox).not.toBeChecked();
  await page
    .getByText("Show calculated log10 on patient reports", { exact: true })
    .click();
  await page.locator("#comp-label-0").fill(`Renamed measurement ${run}`);
  const saved = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/tests/${target.id}/sample-results`) &&
      response.request().method() === "PUT",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await saved;
  await page.reload();
  await expect(checkbox).toBeChecked();
  await expect(page.locator("#comp-label-0")).toHaveValue(
    `Renamed measurement ${run}`,
  );
  await page.screenshot({
    path: testInfo.outputPath("explicit-logarithm-setting.png"),
    fullPage: true,
  });
  const order = await createClinicalOrder(page, {
    testIds: [target.id],
    specimenName: "Plasma",
  });
  await page.goto(`/Results?accessionNumber=${order.accession}`);
  const row = page.getByRole("row").filter({ hasText: target.name });
  await expect(row).toHaveCount(1);
  await row.getByRole("textbox").fill("0.0064");
  await row.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    row.getByRole("button", { name: "Edit", exact: true }),
  ).toBeVisible();
  await page.goto(`/validation?labNumber=${order.accession}`);
  const review = page.getByRole("row").filter({ hasText: target.name });
  await review.getByRole("button", { name: /expand/i }).click();
  await page
    .getByRole("button", { name: "Validate & release", exact: true })
    .click();
  await expect(review).toHaveCount(0);
  const pdf = await printedReport(page, order.accession);
  await testInfo.attach("explicit-logarithm.pdf", {
    body: pdf,
    contentType: "application/pdf",
  });
  const body = await (
    await page.request.get(
      `${API}/accession-results?accessionNumber=${order.accession}`,
    )
  ).json();
  const result = body.testResult.find(
    (item: { testId: string }) => item.testId === target.id,
  );
  expect(result.rawResultValue).toBe("0.0064");
  expect(result.resultValue).toBe("0.01");
  await testInfo.attach("explicit-logarithm-evidence", {
    body: JSON.stringify({
      testId: target.id,
      accession: order.accession,
      result,
    }),
    contentType: "application/json",
  });
  await page.goto(editor);
  await page
    .getByText("Show calculated log10 on patient reports", { exact: true })
    .click();
  const disabled = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/tests/${target.id}/sample-results`) &&
      response.request().method() === "PUT",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await disabled;
  await page.reload();
  await expect(checkbox).not.toBeChecked();
  await testInfo.attach("explicit-logarithm-disabled.pdf", {
    body: await printedReport(page, order.accession),
    contentType: "application/pdf",
  });
});
