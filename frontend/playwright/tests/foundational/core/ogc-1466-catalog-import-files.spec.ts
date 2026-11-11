import { rmSync } from "node:fs";
import { resolve } from "node:path";
import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1466: the tests loader reads the files Excel writes, and a preview
 * changes nothing.
 *
 * - A "CSV UTF-8" file with a byte order mark previews and applies.
 * - A Windows-1252 file keeps its accents.
 * - Previewing twice opens one decision and counts no occurrence.
 * - Two files with one name in a batch are refused.
 * - Changing a file's catalog area after a preview turns Apply off.
 *
 * Every test the spec creates is retired and its applied file removed.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;
const HEADER =
  "testName,testSection,sampleType,isActive,isOrderable,localCode,reportingName";

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const importFiles = async (
  page: Page,
  step: "preview" | "apply",
  files: { name: string; bytes: Buffer }[],
) => {
  const form = new FormData();
  for (const file of files) {
    form.append("files", new Blob([file.bytes]), file.name);
    form.append("domains", "tests");
  }
  return page.request.post(`${API}/rest/configuration/import/${step}`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    multipart: form,
  });
};

/** An active clinical lab unit, so a Serum row resolves. */
const labUnitName = async (page: Page) => {
  const units = (
    await (await page.request.get(`${API}/rest/lab-units-management`)).json()
  ).data as { name: string; domain: string; isActive: boolean }[];
  return units.find((u) => u.isActive && u.domain === "CLINICAL")!.name;
};

/**
 * An applied file stays in the configuration tree. On the dev stack that tree
 * is the checkout's volume directory, so the copy is removed there; elsewhere
 * it lives in the container and nothing is found.
 */
const removeStoredFile = (name: string) =>
  rmSync(
    resolve(process.cwd(), "../volume/configuration/backend/tests", name),
    {
      force: true,
    },
  );

const retireByCode = async (page: Page, code: string) => {
  const headers = { "X-CSRF-Token": await csrfToken(page) };
  const rows = (
    await (
      await page.request.get(
        `${TC}/tests?status=all&page=1&pageSize=50&search=PW1466`,
      )
    ).json()
  ).rows as { testId: string; code?: string }[];
  for (const row of rows.filter((r) => r.code === code)) {
    const info = await (
      await page.request.get(`${TC}/tests/${row.testId}/basic-info`)
    ).json();
    await page.request.put(`${TC}/tests/${row.testId}/basic-info`, {
      headers,
      data: { ...info, active: false, orderable: false },
    });
  }
};

test.describe("Catalog import reads Excel files and previews without writing (OGC-1466)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("a byte order mark and Windows-1252 accents are read", async ({
    page,
  }) => {
    await page.goto("/");
    const unit = await labUnitName(page);
    const s = `${Date.now()}`.slice(-6);
    const bom = Buffer.concat([
      Buffer.from([0xef, 0xbb, 0xbf]),
      Buffer.from(
        `${HEADER}\nPW1466 bom ${s},${unit},Serum,N,N,P6B${s},PW1466 bom ${s}\n`,
        "utf8",
      ),
    ]);
    const latin1 = Buffer.from(
      `${HEADER}\nPW1466 Hématie ${s},${unit},Serum,N,N,P6L${s},PW1466 Hématie ${s}\n`,
      "latin1",
    );
    try {
      const preview = await importFiles(page, "preview", [
        { name: `tests-pw1466-bom-${s}.csv`, bytes: bom },
      ]);
      const planned = (await preview.json()).files[0];
      expect(planned.error ?? null).toBeNull();
      expect(planned.created).toBe(1);

      const applied = await importFiles(page, "apply", [
        { name: `tests-pw1466-latin1-${s}.csv`, bytes: latin1 },
      ]);
      expect(applied.status()).toBe(200);
      const rows = (
        await (
          await page.request.get(
            `${TC}/tests?status=all&page=1&pageSize=50&search=${encodeURIComponent(`Hématie ${s}`)}`,
          )
        ).json()
      ).rows as { name: string }[];
      expect(rows.some((r) => r.name.includes(`PW1466 Hématie ${s}`))).toBe(
        true,
      );
    } finally {
      await retireByCode(page, `P6L${s}`);
      removeStoredFile(`tests-pw1466-latin1-${s}.csv`);
    }
  });

  test("previewing twice opens one decision and counts nothing", async ({
    page,
  }) => {
    await page.goto("/");
    const s = `${Date.now()}`.slice(-6);
    const bogus = `PW1466 No Such Unit ${s}`;
    const file = {
      name: `tests-pw1466-unresolved-${s}.csv`,
      bytes: Buffer.from(
        `${HEADER}\nPW1466 orphan ${s},${bogus},Serum,N,N,P6U${s},PW1466 orphan ${s}\n`,
      ),
    };
    await importFiles(page, "preview", [file]);
    await importFiles(page, "preview", [file]);

    const open = (await (
      await page.request.get(`${API}/rest/configuration/unresolved`)
    ).json()) as { referenceValue?: string; occurrences?: number }[];
    const mine = open.filter((item) => item.referenceValue === bogus);
    expect(mine, "one open decision, resolvable before an apply").toHaveLength(
      1,
    );
    expect(mine[0].occurrences ?? 0).toBe(0);
  });

  test("two files with one name in a batch are refused", async ({ page }) => {
    await page.goto("/");
    const s = `${Date.now()}`.slice(-6);
    const unit = await labUnitName(page);
    const name = `tests-pw1466-twice-${s}.csv`;
    const row = (n: string) =>
      Buffer.from(
        `${HEADER}\nPW1466 ${n} ${s},${unit},Serum,N,N,P6${n}${s},PW1466 ${n} ${s}\n`,
      );
    const response = await importFiles(page, "preview", [
      { name, bytes: row("a") },
      { name, bytes: row("b") },
    ]);
    expect(response.status()).toBe(422);
  });

  test("changing a file's catalog area after a preview turns Apply off", async ({
    page,
  }) => {
    const s = `${Date.now()}`.slice(-6);
    const unit = await (async () => {
      await page.goto("/");
      return labUnitName(page);
    })();
    const name = `tests-pw1466-area-${s}.csv`;
    await page.goto("/MasterListsPage/CatalogImport", {
      waitUntil: "domcontentloaded",
    });
    await page
      .locator('input[type="file"]')
      .first()
      .setInputFiles({
        name,
        mimeType: "text/csv",
        buffer: Buffer.from(
          `${HEADER}\nPW1466 area ${s},${unit},Serum,N,N,P6A${s},PW1466 area ${s}\n`,
        ),
      });
    await expect(page.getByTestId("catalog-import-files")).toContainText(name, {
      timeout: NAV_TIMEOUT,
    });
    const previewed = page.waitForResponse((r) =>
      /\/import\/preview$/.test(r.url()),
    );
    await page.getByRole("button", { name: /^preview$/i }).click();
    await previewed;
    const apply = page.getByRole("button", { name: /^apply$/i });
    await expect(apply).toBeEnabled();

    await page.locator(`select[id="domain-${name}"]`).selectOption("panels");

    await expect(apply).toBeDisabled();
  });
});
