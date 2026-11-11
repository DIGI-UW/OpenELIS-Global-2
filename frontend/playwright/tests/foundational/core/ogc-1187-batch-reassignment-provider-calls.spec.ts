import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { chooseCarbonOption } from "../../../helpers/carbon-select";

/**
 * OGC-1187 — opening Admin → Batch test reassignment and cancelation fired two
 * provider calls with the literal string "null" as the id, and both answered
 * 500 on every visit. The page skips those calls until an id is chosen, and
 * the providers answer a bad id as the caller's mistake.
 */

const API = "/api/OpenELIS-Global";
const PROVIDER_CALL =
  /AllTestsForSampleTypeProvider|getPendingAnalysisForTestProvider/;

test.describe("OGC-1187 batch test reassignment provider calls", () => {
  test("opening the page asks for nothing until a sample type is chosen, and nothing fails", async ({
    page,
  }) => {
    const providerCalls: string[] = [];
    const serverErrors: string[] = [];
    page.on("request", (request) => {
      if (PROVIDER_CALL.test(request.url())) {
        providerCalls.push(request.url());
      }
    });
    page.on("response", (response) => {
      if (response.url().includes(`${API}/`) && response.status() >= 500) {
        serverErrors.push(`${response.status()} ${response.url()}`);
      }
    });

    const pageData = page.waitForResponse(
      (response) =>
        response.request().method() === "GET" &&
        response.url().endsWith("/rest/BatchTestReassignment"),
      { timeout: NAV_TIMEOUT },
    );
    await page.goto("/MasterListsPage/batchTestReassignment", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    await expect(
      main.getByRole("heading", { name: /batch test reassignment/i }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    const { sampleList } = (await (await pageData).json()) as {
      sampleList: { id: string; value: string }[];
    };
    const sampleType = main.getByRole("combobox", { name: "Sample Type" });
    await expect(sampleType).toBeVisible({ timeout: UI_TIMEOUT });
    await page.waitForLoadState("networkidle");

    expect(providerCalls, "no provider call while the ids are unset").toEqual(
      [],
    );
    expect(serverErrors, "no server error while the page settles").toEqual([]);

    // Choosing a sample type is what asks for its tests, with that id.
    const chosen = sampleList[0];
    expect(chosen?.id).toMatch(/^\d+$/);
    const testsRequest = page.waitForRequest(
      (request) =>
        request.url().includes("AllTestsForSampleTypeProvider") &&
        new URL(request.url()).searchParams.get("sampleTypeId") === chosen.id,
    );
    await chooseCarbonOption(sampleType, chosen.value);
    await testsRequest;

    const currentTest = main.getByRole("combobox", { name: "Current test" });
    await currentTest.click();
    await expect(
      page
        .locator(`[id="${await currentTest.getAttribute("aria-controls")}"]`)
        .getByRole("option")
        .first(),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    expect(
      providerCalls.filter((url) => /=null(&|$)/.test(url)),
      "no call ever carried a null id",
    ).toEqual([]);
    expect(
      serverErrors,
      "no server error after choosing a sample type",
    ).toEqual([]);
  });

  test("the providers answer a bad id as the caller's mistake, and an unknown id as an empty success", async ({
    page,
  }) => {
    for (const url of [
      `${API}/rest/AllTestsForSampleTypeProvider?sampleTypeId=null`,
      `${API}/rest/AllTestsForSampleTypeProvider?sampleTypeId=abc`,
      `${API}/rest/AllTestsForSampleTypeProvider?sampleTypeId=`,
      `${API}/rest/getPendingAnalysisForTestProvider?testId=null`,
      `${API}/rest/getPendingAnalysisForTestProvider?testId=abc`,
      `${API}/rest/getPendingAnalysisForTestProvider?testId=`,
    ]) {
      const response = await page.request.get(url);
      expect(response.status(), url).toBe(400);
      expect(await response.text(), url).not.toContain("Internal error");
    }

    const unknownSampleType = await page.request.get(
      `${API}/rest/AllTestsForSampleTypeProvider?sampleTypeId=99999`,
    );
    expect(unknownSampleType.status()).toBe(200);
    expect(await unknownSampleType.json()).toEqual({ tests: [] });

    const unknownTest = await page.request.get(
      `${API}/rest/getPendingAnalysisForTestProvider?testId=99999`,
    );
    expect(unknownTest.status()).toBe(200);
    const buckets = await unknownTest.json();
    for (const bucket of [
      "notStarted",
      "technicianRejection",
      "biologistRejection",
      "notValidated",
    ]) {
      expect(buckets[bucket], bucket).toEqual([]);
    }
  });
});
