import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import { chooseCarbonOption } from "../../../helpers/carbon-select";

/**
 * OGC-1611 P4 (FR-B24 to B26, G8): Prepare Samples proposes one sample per
 * container the ordered tests need. Tests sharing a container are drawn in one
 * with the largest count, a test preferring another container shares its
 * alternate, and a test needing its own container gets a sample of its own.
 * The container is required where the catalog names one, a changed row is
 * marked Edited, and the chosen container is saved with the sample.
 */

const API = "/api/OpenELIS-Global/rest";

type Row = {
  containerTypeId: number;
  role: string;
  count: number;
  minVolume: number | null;
  volumeUnit: string | null;
  usedAsSampleTypeId: string | null;
};
type TestContainers = { containers: Row[]; needsOwnContainer: boolean };

async function api<T>(
  page: Page,
  method: string,
  path: string,
  body?: unknown,
) {
  return page.evaluate(
    async ({ method, url, body }) => {
      const response = await fetch(url, {
        method,
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF") || "",
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      const text = await response.text();
      return { status: response.status, json: text ? JSON.parse(text) : null };
    },
    { method, url: `${API}${path}`, body },
  ) as Promise<{ status: number; json: T }>;
}

const setContainers = (page: Page, testId: string, form: TestContainers) =>
  api(page, "PUT", `/test-catalog/tests/${testId}/containers`, {
    needsOwnContainer: form.needsOwnContainer,
    containers: form.containers.map((row) => ({
      containerTypeId: row.containerTypeId,
      role: row.role,
      count: row.count,
      minVolume: row.minVolume,
      volumeUnit: row.volumeUnit,
      usedAsSampleTypeId: row.usedAsSampleTypeId,
    })),
  });

test.describe("OGC-1611 proposed samples", () => {
  test("Prepare Samples proposes the containers the ordered tests need and saves the one chosen", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const sampleTypes = (
      await api<Array<{ id: string; name: string }>>(
        page,
        "GET",
        "/test-catalog/sample-types",
      )
    ).json;
    const serum = sampleTypes.find((type) => type.name === "Serum");
    expect(serum, "Serum is seeded").toBeTruthy();
    const catalog = (
      await api<{ rows: Array<{ testId: string; name: string }> }>(
        page,
        "GET",
        `/test-catalog/tests?sampleType=${serum!.id}&domain=CLINICAL&status=active&pageSize=8`,
      )
    ).json.rows;
    expect(catalog.length, "six clinical Serum tests").toBeGreaterThanOrEqual(
      6,
    );
    const [ownTest, sharedTest, alternateTest, bareTest] = catalog
      .slice(2, 6)
      .map((row) => row.testId);
    const containers = (
      await api<Array<{ id: number; code: string; name: string }>>(
        page,
        "GET",
        "/container-types?domain=CLINICAL",
      )
    ).json;
    const byCode = (code: string) => {
      const found = containers.find((container) => container.code === code);
      expect(found, `${code} is seeded`).toBeTruthy();
      return found!;
    };
    const sst = byCode("SST");
    const plain = byCode("SERUMPLAIN");

    const before: Record<string, TestContainers> = {};
    for (const testId of [ownTest, sharedTest, alternateTest, bareTest]) {
      before[testId] = (
        await api<TestContainers>(
          page,
          "GET",
          `/test-catalog/tests/${testId}/containers`,
        )
      ).json;
    }
    const row = (id: number, role: string, count = 1): Row => ({
      containerTypeId: id,
      role,
      count,
      minVolume: null,
      volumeUnit: null,
      usedAsSampleTypeId: null,
    });

    try {
      for (const [testId, form] of [
        [
          ownTest,
          { containers: [row(sst.id, "PREFERRED")], needsOwnContainer: true },
        ],
        [
          sharedTest,
          {
            containers: [row(sst.id, "PREFERRED", 2)],
            needsOwnContainer: false,
          },
        ],
        [
          alternateTest,
          {
            containers: [row(plain.id, "PREFERRED"), row(sst.id, "ALTERNATE")],
            needsOwnContainer: false,
          },
        ],
        [bareTest, { containers: [], needsOwnContainer: false }],
      ] as Array<[string, TestContainers]>) {
        expect((await setContainers(page, testId, form)).status).toBe(200);
      }
      const serumDefault = (
        await api<{ defaultContainerTypeId: number | null }>(
          page,
          "GET",
          `/sample-type-containers/${serum!.id}`,
        )
      ).json.defaultContainerTypeId;
      expect(
        serumDefault ?? null,
        "Serum has no default container, so the bare test gets no proposal",
      ).toBeNull();

      let labNumber = "";
      await test.step("Enter Order saves the Serum sample with the four tests", async () => {
        await page.goto("/order/clinical/enter", {
          waitUntil: "domcontentloaded",
        });
        await expect(page.locator("#labNumber")).not.toHaveValue("", {
          timeout: NAV_TIMEOUT,
        });
        labNumber = await page.locator("#labNumber").inputValue();
        const section = page.getByTestId("patient-search-section");
        await section.getByRole("button", { name: "New Patient" }).click();
        await section.locator("#nationalId").fill(`QA1611P4${Date.now()}`);
        await section.locator("#lastName").fill("Proposal");
        await section.locator("#firstName").fill("Ada");
        await section
          .getByRole("textbox", { name: "Date of Birth" })
          .fill("05/03/1988");
        await section
          .locator("#create_patient_gender label")
          .filter({ hasText: "Female" })
          .click();
        const sampleSection = page.getByTestId("order-sample-test-section");
        await chooseCarbonOption(
          sampleSection.getByRole("combobox", { name: /^Sample Type/ }),
          "Serum",
        );
        for (const testId of [ownTest, sharedTest, alternateTest, bareTest]) {
          const label = sampleSection.locator(`label[for="test-0-${testId}"]`);
          await expect(label).toBeVisible({ timeout: LONG_TIMEOUT });
          await label.click();
        }
        const saved = page.waitForResponse(
          (response) =>
            response.url().includes("/rest/SamplePatientEntry") &&
            response.request().method() === "POST",
          { timeout: LONG_TIMEOUT },
        );
        await page
          .getByRole("button", { name: "Save and next", exact: true })
          .click();
        expect((await saved).status()).toBe(200);
        await expect(page).toHaveURL(/\/order\/clinical\/collect/, {
          timeout: NAV_TIMEOUT,
        });
      });

      await test.step("Prepare Samples proposes one shared SST drawn twice and one SST of its own", async () => {
        const shared = page.getByTestId("container-field-0");
        const own = page.getByTestId("container-field-1");
        await expect(own).toBeVisible({ timeout: NAV_TIMEOUT });
        await expect(page.locator("#containerType-0")).toHaveValue(
          String(sst.id),
        );
        await expect(page.locator("#containerType-1")).toHaveValue(
          String(sst.id),
        );
        await expect(page.getByLabel("Container *").first()).toBeVisible();
        await expect(shared.getByTestId("container-proposed-0")).toBeVisible();
        await expect(shared.getByTestId("container-draw-0")).toHaveText(
          "Draw 2",
        );
        const names = Object.fromEntries(
          catalog.map((r) => [r.testId, r.name.replace(/\s*\([^)]*\)$/, "")]),
        );
        await expect(
          shared.getByTestId("container-proposed-0"),
        ).toHaveAttribute(
          "aria-label",
          `Proposed for ${names[sharedTest]}, ${names[alternateTest]}`,
        );
        await expect(own.getByTestId("container-proposed-1")).toHaveAttribute(
          "aria-label",
          `Proposed for ${names[ownTest]}`,
        );
        const cards = page.locator('[data-testid^="sample-collection-card"]');
        await expect(cards).toHaveCount(2);
        await expect(cards.nth(1).locator(".assigned-tags")).not.toContainText(
          names[bareTest],
        );
      });

      await test.step("a cleared container holds Save and next, and a changed one is marked Edited", async () => {
        await page.locator("#containerType-0").selectOption("");
        await expect(
          page.getByRole("link", {
            name: `Container for ${labNumber}-1 Serum`,
          }),
        ).toBeVisible({ timeout: UI_TIMEOUT });
        await expect(
          page.getByRole("button", { name: "Save and next", exact: true }),
        ).toBeDisabled();
        await page.locator("#containerType-0").selectOption(String(sst.id));
        await page.locator("#containerType-1").selectOption(String(plain.id));
        await expect(page.getByTestId("container-edited-1")).toHaveText(
          "Edited",
        );
        await expect(page.getByTestId("container-proposed-1")).toHaveCount(0);
      });

      await test.step("Save and exit stores each sample's container and a reload reads it back", async () => {
        const saved = page.waitForResponse(
          (response) =>
            response.url().includes("/rest/SamplePatientEntry") &&
            response.request().method() === "POST",
          { timeout: LONG_TIMEOUT },
        );
        await page
          .getByRole("button", { name: "Save and exit", exact: true })
          .click();
        expect((await saved).status()).toBe(200);
        const order = (
          await api<{ samples: Array<{ containerTypeId: string }> }>(
            page,
            "GET",
            `/order/search?labNumber=${encodeURIComponent(labNumber)}`,
          )
        ).json;
        expect(
          order.samples.map((sample) => sample.containerTypeId).sort(),
        ).toEqual([String(sst.id), String(plain.id)].sort());

        await page.goto(
          `/order/clinical/collect?labNumber=${encodeURIComponent(labNumber)}`,
          { waitUntil: "domcontentloaded" },
        );
        await expect(page.getByTestId("container-field-1")).toBeVisible({
          timeout: NAV_TIMEOUT,
        });
        const values = await page
          .locator('select[id^="containerType-"]')
          .evaluateAll((selects) =>
            selects.map((select) => (select as HTMLSelectElement).value),
          );
        expect(values.sort()).toEqual(
          [String(sst.id), String(plain.id)].sort(),
        );
        await expect(
          page.locator('[data-testid^="container-proposed-"]'),
        ).toHaveCount(0);
        await expect(
          page.locator('[data-testid^="container-edited-"]'),
        ).toHaveCount(0);
      });
    } finally {
      for (const testId of Object.keys(before)) {
        await setContainers(page, testId, before[testId]);
      }
    }
  });
});
