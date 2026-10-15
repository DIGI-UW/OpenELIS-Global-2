import { test, expect } from "../../../helpers/test-base";
import {
  ensureWorkspaceGrowthCulture,
  seedMicrobiologyCultureWorkspace,
  seedMicrobiologyMvpCase,
  seedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import {
  createMicrobiologyIsolate,
  identifyMicrobiologyIsolate,
  openMicrobiologyCaseSection,
} from "../../../helpers/microbiology-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const selectConfiguredOption = async (
  field: import("@playwright/test").Locator,
) => {
  await expect(field).toBeEnabled();
  const options = field.locator("option");
  await expect
    .poll(() =>
      options.evaluateAll(
        (rows) =>
          rows.map((r) => (r as HTMLOptionElement).value).filter(Boolean)
            .length,
      ),
    )
    .toBeGreaterThan(0);
  const value = await options.evaluateAll((rows) =>
    rows.map((r) => (r as HTMLOptionElement).value).find(Boolean),
  );
  await field.selectOption(value!);
};

test("keeps two laboratory units through case, bench and AST navigation and reload", async ({
  page,
}) => {
  const first = await seedMicrobiologyCultureWorkspace(page);
  const second = await seedMicrobiologyCultureWorkspace(page);
  await ensureWorkspaceGrowthCulture(page, first);
  await ensureWorkspaceGrowthCulture(page, second);
  const units = await Promise.all(
    [first, second].map(
      async (c) =>
        (
          await (
            await page.request.get(
              `/api/OpenELIS-Global/rest/microbiology/cases/${c.caseId}/shell`,
            )
          ).json()
        ).labUnitId,
    ),
  );
  expect(new Set(units).size).toBe(2);
  const query = new URLSearchParams({ q: "UATMICRO" });
  units.forEach((id) => query.append("labUnitIds", id));
  await page.goto(`/Microbiology/worklist?${query}`);
  const table = page.getByRole("table", {
    name: "Microbiology worklist",
    exact: true,
  });
  await expect(
    table.getByRole("link", { name: first.accessionNumber, exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(
    table.getByRole("link", { name: second.accessionNumber, exact: true }),
  ).toBeVisible();
  await expect(table.getByRole("row")).toHaveCount(3);
  await page.getByRole("button", { name: "Culture", exact: true }).click();
  await expect(
    table.getByRole("button", { name: first.accessionNumber, exact: true }),
  ).toBeVisible();
  await table
    .getByRole("button", { name: first.accessionNumber, exact: true })
    .click();
  await expect(page.getByLabel("Testing section", { exact: true })).toHaveValue(
    "CULTURE",
  );
  await page
    .getByTestId("microbiology-case-view")
    .getByRole("link", { name: "Microbiology worklist", exact: true })
    .click();
  await page.reload();
  await expect(table.getByRole("row")).toHaveCount(3);
  await page.getByRole("button", { name: "AST runs", exact: true }).click();
  await expect(page.getByTestId("microbiology-worklist")).toBeVisible();
  expect(new URL(page.url()).searchParams.getAll("labUnitIds").sort()).toEqual(
    units.sort(),
  );
  await page
    .getByRole("button", {
      name: "Search by accession number or patient name",
      exact: true,
    })
    .click();
  await expect(table.getByRole("row")).toHaveCount(3);
  expect(new URL(page.url()).searchParams.getAll("labUnitIds").sort()).toEqual(
    units.sort(),
  );
});

test("retains two-pass identification and isolate-targeted critical communication in the shared workspace", async ({
  page,
}, info) => {
  const seeded = await seedMicrobiologyMvpCase(page);
  await page.goto(`/Microbiology/cases/${seeded.caseId}?view=workbench`);
  await createMicrobiologyIsolate(page, seeded);
  const isolate = page.locator('[data-testid^="isolate-"]');
  await expect(
    isolate.getByText("Gram negative rods", { exact: true }),
  ).toBeVisible();
  await openMicrobiologyCaseSection(page, "Manual AST");
  await expect(
    page.getByRole("button", { name: "Start AST run", exact: true }),
  ).toHaveCount(0);
  await openMicrobiologyCaseSection(page, "Isolates");
  await identifyMicrobiologyIsolate(page, seeded.organismId!);
  await openMicrobiologyCaseSection(page, "Manual AST");
  await expect(
    page.getByRole("button", { name: "Start AST run", exact: true }),
  ).toBeEnabled();
  await openMicrobiologyCaseSection(page, "Isolates");
  await isolate
    .getByRole("button", {
      name: "Log critical notification for ISO-1",
      exact: true,
    })
    .click();
  await expect(page.getByLabel("Critical result target")).toHaveValue(
    "ISOLATE",
  );
  await expect(page.getByLabel("Critical result target")).toBeDisabled();
  await expect(page.getByLabel("Target record")).toBeDisabled();
  await openMicrobiologyCaseSection(page, "Timeline");
  await expect(
    page.getByRole("table", { name: "Case timeline", exact: true }),
  ).toContainText("Open ELIS");
  for (const width of [1440, 390]) {
    await page.setViewportSize({ width, height: 1000 });
    await expect
      .poll(() =>
        page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
      )
      .toBe(true);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: info.outputPath(`m9-history-${width}.png`),
      fullPage: true,
    });
  }
});

test("reports an NCE and records a separate lost specimen with keyboard access", async ({
  page,
}) => {
  test.setTimeout(90_000);
  for (const lost of [false, true]) {
    const seeded = await seedMicrobiologyCase(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}`);
    const action = page.getByRole("button", {
      name: lost ? "Mark lost" : "Nonconformance",
      exact: true,
    });
    await action.focus();
    await page.keyboard.press("Enter");
    const panel = page.getByTestId("microbiology-nce-panel");
    await expect(
      panel.getByRole("heading", {
        name: lost ? "Mark specimen lost" : "Report nonconformance",
        exact: true,
      }),
    ).toBeFocused();
    if (lost) {
      await expect(panel.getByLabel("Category")).toBeDisabled();
      await expect(panel.getByLabel("Type")).toHaveValue(/.+/);
    } else await selectConfiguredOption(panel.getByLabel("Category"));
    await selectConfiguredOption(panel.getByLabel("Reporting unit"));
    await panel
      .getByRole("radio", { name: "Major", exact: true })
      .press("Space");
    const description = lost
      ? "M9 specimen cannot be located"
      : "M9 cracked container at receipt";
    await panel.getByLabel("Description").fill(description);
    await panel
      .getByRole("button", {
        name: lost ? "Mark lost" : "Report NCE",
        exact: true,
      })
      .click();
    const timeline = page.getByRole("table", {
      name: "Case timeline",
      exact: true,
    });
    await expect(timeline).toContainText(lost ? "Specimen Lost" : description, {
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page.getByRole("region", { name: "Case timeline", exact: true }),
    ).toBeFocused();
    await page.reload();
    await expect(timeline).toContainText(lost ? "Specimen Lost" : description);
  }
});
