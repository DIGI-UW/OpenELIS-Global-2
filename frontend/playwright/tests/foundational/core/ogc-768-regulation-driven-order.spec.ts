import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-768: a regulation-driven environmental order names its compliance
 * standards, and their linked tests become the test plan each sample starts
 * from. Two standards are created for the run on an environmental sample type
 * that runs at least two tests; they share one test with different limits.
 */

type Seeded = {
  sampleType: { id: string; value: string };
  tests: { id: string; name: string }[];
  standards: { id: string; regulationNumber: string }[];
};

const suffix = () => Math.random().toString(36).slice(2, 7).toUpperCase();

async function csrf(page: Page) {
  return {
    "X-CSRF-Token": await page.evaluate(
      () => localStorage.getItem("CSRF") || "",
    ),
  };
}

async function json(request: APIRequestContext, url: string) {
  const response = await request.get(`${API_PREFIX}${url}`);
  expect(response.ok(), url).toBeTruthy();
  return response.json();
}

async function seed(page: Page): Promise<Seeded> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const types = await json(page.request, "/rest/environmental-sample-types");
  let chosen: Seeded["sampleType"] | undefined;
  let tests: Seeded["tests"] = [];
  for (const type of types) {
    const catalog = await json(
      page.request,
      `/rest/sample-type-tests?sampleType=${type.id}`,
    );
    if ((catalog.tests || []).length >= 2) {
      chosen = type;
      tests = catalog.tests
        .slice(0, 2)
        .map((t: { id: string; name: string }) => ({
          id: String(t.id),
          name: t.name,
        }));
      break;
    }
  }
  expect(chosen, "an environmental sample type with two tests").toBeTruthy();
  const headers = await csrf(page);
  const tag = suffix();
  const standards: Seeded["standards"] = [];
  const limits = [
    [
      { test: tests[0], type: "RANGE", min: 6, max: 9 },
      { test: tests[1], type: "MAXIMUM", min: null, max: 5 },
    ],
    [{ test: tests[0], type: "RANGE", min: 6.5, max: 8.5 }],
  ];
  for (let index = 0; index < 2; index++) {
    const regulationNumber = `PW768-${tag}-${index + 1}`;
    const created = await page.request.post(
      `${API_PREFIX}/rest/compliance/standards`,
      {
        headers,
        data: {
          name: `Playwright standard ${tag} ${index + 1}`,
          issuingBody: "Playwright Ministry",
          regulationNumber,
          version: "1",
          countryRegion: "Test",
          status: "ACTIVE",
          effectiveDate: "2026-01-01",
          sampleTypes: [chosen!.value],
        },
      },
    );
    expect(created.ok(), "standard created").toBeTruthy();
    const standard = await created.json();
    const group = await page.request.post(
      `${API_PREFIX}/rest/compliance/standards/${standard.id}/parameter-groups`,
      { headers, data: { name: "Physical", sortOrder: 1, isMandatory: true } },
    );
    expect(group.ok(), "parameter group created").toBeTruthy();
    const groupId = (await group.json()).id;
    for (const limit of limits[index]) {
      const threshold = await page.request.post(
        `${API_PREFIX}/rest/tests/${limit.test.id}/compliance-thresholds`,
        {
          headers,
          data: {
            group: { id: groupId },
            test: { id: limit.test.id },
            parameterCode: `PW${limit.test.id}`,
            displayName: limit.test.name,
            thresholdType: limit.type,
            minValue: limit.min,
            maxValue: limit.max,
            isMandatory: false,
          },
        },
      );
      expect(threshold.ok(), "threshold created").toBeTruthy();
    }
    standards.push({ id: String(standard.id), regulationNumber });
  }
  return { sampleType: chosen!, tests, standards };
}

async function retire(page: Page, seeded: Seeded | undefined) {
  if (!seeded) return;
  const headers = await csrf(page);
  for (const standard of seeded.standards) {
    await page.request.post(
      `${API_PREFIX}/rest/compliance/standards/${standard.id}/archive`,
      { headers },
    );
  }
}

test.describe("OGC-768 regulation-driven environmental order", () => {
  test.describe.configure({ timeout: 180_000 });
  let seeded: Seeded | undefined;

  test.afterEach(async ({ page }) => {
    await retire(page, seeded);
  });

  test("the standards' tests become the plan each sample starts from", async ({
    page,
  }) => {
    seeded = await seed(page);
    const [first, second] = seeded.standards;
    const [shared, onlyFirst] = seeded.tests;

    await page.goto("/order/environmental/enter", {
      waitUntil: "domcontentloaded",
    });
    const branch = page.getByTestId("env-order-branch");
    await expect(branch).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(branch.getByRole("radio", { name: /Ad-hoc/ })).toBeChecked();
    await expect(
      page.getByLabel("Regulatory Reference", { exact: true }),
    ).toBeVisible();

    await branch.locator("label", { hasText: "Regulation-driven" }).click();
    const section = page.getByTestId("compliance-standards");
    await expect(section).toBeVisible({ timeout: UI_TIMEOUT });

    const picker = section.getByRole("combobox", {
      name: /Compliance Standards/,
    });
    for (const standard of [first, second]) {
      await picker.click();
      await picker.fill(standard.regulationNumber);
      await page
        .getByRole("option", { name: new RegExp(standard.regulationNumber) })
        .click();
    }
    await page.keyboard.press("Escape");

    await expect(
      section.getByTestId(`compliance-standard-regulation-${first.id}`),
    ).toHaveText(first.regulationNumber);
    await expect(
      section.getByTestId("compliance-test-plan-summary"),
    ).toContainText("Tests in the plan: 2", { timeout: UI_TIMEOUT });
    await expect(
      section.getByTestId(`compliance-plan-test-${shared.id}`),
    ).toContainText("Limits differ");
    await expect(
      section.getByTestId(`compliance-plan-limits-${shared.id}`),
    ).toContainText("6.5 to 8.5");

    await section
      .getByTestId(`compliance-quick-add-${seeded.sampleType.id}`)
      .click();
    await expect(
      page.getByRole("button", { name: "Tests & Panels (2)" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      section.getByTestId(`compliance-plan-test-${onlyFirst.id}`),
    ).toContainText(first.regulationNumber);
    await expect(section.getByTestId("compliance-plan-blocked")).toHaveCount(0);

    await branch.locator("label", { hasText: "Ad-hoc" }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toContainText(
      "Switching to Ad-hoc clears the selected compliance standards",
    );
    await dialog.getByRole("button", { name: "Switch" }).click();
    await expect(page.getByTestId("compliance-standards")).toHaveCount(0);
    await expect(
      page.getByLabel("Regulatory Reference", { exact: true }),
    ).toBeVisible();
  });
});
