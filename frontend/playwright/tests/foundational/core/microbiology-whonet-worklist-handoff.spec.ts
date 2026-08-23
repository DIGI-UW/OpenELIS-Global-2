import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyWhonetWorklistHandoffCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import {
  expectWhonetExportReady,
  selectWhonetFilterOption,
  whonetFixtureLabels,
} from "../../../helpers/whonet-export";
import type { Page, TestInfo } from "@playwright/test";

const attachScreenshot = async (
  page: Page,
  testInfo: TestInfo,
  name: string,
) => {
  await testInfo.attach(name, {
    body: await page.screenshot({ animations: "disabled", fullPage: true }),
    contentType: "image/png",
  });
};

const localDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const fullMonth = (monthOffset: number) => {
  const now = new Date();
  return {
    from: localDate(
      new Date(now.getFullYear(), now.getMonth() + monthOffset, 1),
    ),
    to: localDate(
      new Date(now.getFullYear(), now.getMonth() + monthOffset + 1, 0),
    ),
  };
};

test.describe("OGC-782 R12 AST worklist to WHONET handoff", () => {
  test("transfers only the editable surveillance population and clears to Reports defaults", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const seeded = await seedMicrobiologyWhonetWorklistHandoffCase(page);
    const currentMonth = fullMonth(0);
    const lastMonth = fullMonth(-1);

    await test.step("Open the reviewed AST worklist with its calendar-month default", async () => {
      const operationalQuery = new URLSearchParams({
        grain: "ast",
        status: "reviewed",
        urgency: "ROUTINE",
        q: seeded.caseId,
        sort: "newest",
        page: "1",
        pageSize: "20",
      });
      await page.goto(`/Microbiology/worklist?${operationalQuery}`, {
        waitUntil: "domcontentloaded",
      });

      await expect(page.getByRole("heading", { name: "AST runs" })).toBeVisible(
        {
          timeout: LONG_TIMEOUT,
        },
      );
      await expect(page.getByLabel("Reporting period")).toHaveValue(
        "THIS_MONTH",
      );
      await expect(
        page.getByRole("textbox", { name: "From", exact: true }),
      ).toHaveValue(currentMonth.from);
      await expect(
        page.getByRole("textbox", { name: "To", exact: true }),
      ).toHaveValue(currentMonth.to);
      await expect(
        page
          .getByRole("row")
          .filter({ hasText: seeded.accessionNumber })
          .first(),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Choose a structured surveillance population", async () => {
      await selectWhonetFilterOption(
        page,
        /^Specimen types/,
        "UAT micro specimen",
      );
      await selectWhonetFilterOption(page, /^Organisms/, "Escherichia coli");
      await selectWhonetFilterOption(
        page,
        /^Patient origins/,
        whonetFixtureLabels.inpatient,
      );
      await selectWhonetFilterOption(
        page,
        /^Inclusion/,
        "Clinically significant",
      );

      const exportLink = page.getByRole("link", { name: "Export to WHONET" });
      const href = await exportLink.getAttribute("href");
      expect(href).not.toBeNull();
      const exportUrl = new URL(href!, "https://openelis.test");
      expect(exportUrl.searchParams.get("from")).toBe(currentMonth.from);
      expect(exportUrl.searchParams.get("to")).toBe(currentMonth.to);
      expect(exportUrl.searchParams.getAll("specimen")).toEqual([
        seeded.sampleTypeId,
      ]);
      expect(exportUrl.searchParams.getAll("organism")).toEqual([
        seeded.organismId,
      ]);
      expect(exportUrl.searchParams.getAll("origin")).toEqual(["INPATIENT"]);
      expect(exportUrl.searchParams.getAll("significance")).toEqual([
        "CLINICALLY_SIGNIFICANT",
      ]);
      expect(exportUrl.searchParams.get("source")).toBe("ast-worklist");
      for (const operationalKey of [
        "grain",
        "status",
        "urgency",
        "q",
        "sort",
      ]) {
        expect(exportUrl.searchParams.has(operationalKey)).toBe(false);
      }
      await testInfo.attach("ast-worklist-surveillance-scope", {
        body: await page.screenshot({ animations: "disabled" }),
        contentType: "image/png",
      });
      await exportLink.click();
    });

    await test.step("Edit the transferred scope in the generator", async () => {
      await expectWhonetExportReady(page);
      await expect(
        page.getByText("Scope provided by the AST worklist"),
      ).toBeVisible();
      await expect(page.getByLabel("Reporting period")).toHaveValue(
        "THIS_MONTH",
      );
      await expect(
        page.getByRole("combobox", { name: /^Specimen types/ }),
      ).toBeEnabled();
      await page.getByLabel("Reporting period").selectOption("THIS_QUARTER");
      await expect(page).toHaveURL(/source=ast-worklist/);
      await expect(page.getByLabel("Reporting period")).toHaveValue(
        "THIS_QUARTER",
      );
      await attachScreenshot(page, testInfo, "whonet-worklist-scope-desktop");

      await page.setViewportSize({ width: 390, height: 844 });
      await expect
        .poll(() =>
          page.evaluate(
            () =>
              document.documentElement.scrollWidth <=
              document.documentElement.clientWidth,
          ),
        )
        .toBe(true);
      await attachScreenshot(page, testInfo, "whonet-worklist-scope-mobile");
    });

    await test.step("Clear worklist scope and restore direct Reports defaults", async () => {
      await page.getByRole("button", { name: "Clear worklist scope" }).click();
      await expect(
        page.getByText("Scope provided by the AST worklist"),
      ).toHaveCount(0);
      await expect(page.getByLabel("Reporting period")).toHaveValue(
        "LAST_MONTH",
      );
      await expect(
        page.getByRole("textbox", { name: "From", exact: true }),
      ).toHaveValue(lastMonth.from);
      await expect(
        page.getByRole("textbox", { name: "To", exact: true }),
      ).toHaveValue(lastMonth.to);
      const clearedUrl = new URL(page.url());
      expect(clearedUrl.searchParams.has("source")).toBe(false);
      expect(clearedUrl.searchParams.has("specimen")).toBe(false);
      expect(clearedUrl.searchParams.has("organism")).toBe(false);
      expect(clearedUrl.searchParams.has("origin")).toBe(false);
      expect(clearedUrl.searchParams.getAll("significance")).toEqual([
        "CLINICALLY_SIGNIFICANT",
      ]);
    });
  });
});
