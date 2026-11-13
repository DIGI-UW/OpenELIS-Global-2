import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1611 P2 (FR-G5 to G7): a test names the containers it expects, one
 * Preferred and the rest Alternate, and a container yielding another sample
 * type is used as the test's own; that sample type shows the test as secondary,
 * and it takes a default container of its own.
 */

const API = "/api/OpenELIS-Global/rest";

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
      return { status: response.status, json: await response.json() };
    },
    { method, url: `${API}${path}`, body },
  ) as Promise<{ status: number; json: T }>;
}

test.describe("OGC-1611 test containers", () => {
  test("a test expects its containers and a sample type shows the tests it serves", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const sampleTypes = (
      await api<Array<{ id: string; name: string }>>(
        page,
        "GET",
        "/test-catalog/sample-types",
      )
    ).json;
    const serum = sampleTypes.find((type) => type.name === "Serum");
    const plasma = sampleTypes.find((type) => type.name === "Plasma");
    expect(serum && plasma, "Serum and Plasma are seeded").toBeTruthy();
    const page1 = (
      await api<{ rows: Array<{ testId: string; name: string }> }>(
        page,
        "GET",
        `/test-catalog/tests?sampleType=${serum!.id}&domain=CLINICAL&pageSize=1`,
      )
    ).json;
    const target = page1.rows[0];
    expect(target, "a clinical Serum test exists").toBeTruthy();
    const containers = (
      await api<Array<{ id: number; code: string }>>(
        page,
        "GET",
        "/container-types?domain=CLINICAL",
      )
    ).json;
    const idOf = (code: string) =>
      String(containers.find((container) => container.code === code)!.id);
    const sstId = idOf("SST");
    const pstId = idOf("PST");
    const edtaId = idOf("K2EDTA4");
    const lihepId = idOf("LIHEP");
    const before = (
      await api<{ defaultContainerTypeId: number | null }>(
        page,
        "GET",
        `/sample-type-containers/${plasma!.id}`,
      )
    ).json.defaultContainerTypeId;

    const cleared = await api(
      page,
      "PUT",
      `/test-catalog/tests/${target.testId}/containers`,
      { containers: [], needsOwnContainer: false },
    );
    expect(cleared.status, "the test starts with no containers").toBe(200);

    try {
      await test.step("The test names a Preferred tube and a plasma tube used as Serum", async () => {
        await page.goto(
          `/MasterListsPage/TestCatalogEditor/${target.testId}/containers`,
          { waitUntil: "domcontentloaded" },
        );
        const section = page.getByTestId("containers-section");
        await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
        const add = page.getByLabel("Add a container");
        await add.selectOption({ label: "Serum separator tube (SST)" });
        await add.selectOption({
          label: "Plasma separator tube (PST) (Plasma)",
        });
        const sst = page.getByTestId(`container-row-${sstId}`);
        const pst = page.getByTestId(`container-row-${pstId}`);
        await expect(sst.locator("select[id^=container-role-]")).toHaveValue(
          "PREFERRED",
        );
        await expect(pst.locator("select[id^=container-role-]")).toHaveValue(
          "ALTERNATE",
        );
        await expect(pst.locator("select[id^=container-used-as-]")).toHaveValue(
          serum!.id,
        );
        await sst.locator("input[id^=container-count-]").fill("2");
        await page.getByTestId("containers-save").click();
        await expect(
          page.getByText("The test's containers are saved."),
        ).toBeVisible({
          timeout: UI_TIMEOUT,
        });

        await page.reload({ waitUntil: "domcontentloaded" });
        await expect(page.getByTestId(`container-row-${sstId}`)).toBeVisible({
          timeout: NAV_TIMEOUT,
        });
        await expect(
          page
            .getByTestId(`container-row-${sstId}`)
            .locator("input[id^=container-count-]"),
        ).toHaveValue("2");
        await expect(page.getByTestId("containers-secondary")).toContainText(
          "Plasma, via Plasma separator tube (PST), used as Serum",
        );
      });

      await test.step("Plasma names the test as secondary and takes a default container", async () => {
        await page.goto(
          `/MasterListsPage/SampleTypeEditor/${plasma!.id}/containers`,
          { waitUntil: "domcontentloaded" },
        );
        await expect(
          page.getByTestId("sample-type-secondary-tests"),
        ).toContainText("via Plasma separator tube (PST), used as Serum", {
          timeout: NAV_TIMEOUT,
        });
        const chosen = String(before) === edtaId ? lihepId : edtaId;
        await page.getByLabel("Default container type").selectOption(chosen);
        await page.getByTestId("sample-type-containers-save").click();
        await expect(
          page.getByText("The default container type is saved."),
        ).toBeVisible({ timeout: UI_TIMEOUT });
        await page.reload({ waitUntil: "domcontentloaded" });
        await expect(page.getByLabel("Default container type")).toHaveValue(
          chosen,
          { timeout: NAV_TIMEOUT },
        );
      });
    } finally {
      await api(
        page,
        "PUT",
        `/test-catalog/tests/${target.testId}/containers`,
        {
          containers: [],
          needsOwnContainer: false,
        },
      );
      await api(page, "PUT", `/sample-type-containers/${plasma!.id}`, {
        defaultContainerTypeId: before,
      });
    }
  });
});
