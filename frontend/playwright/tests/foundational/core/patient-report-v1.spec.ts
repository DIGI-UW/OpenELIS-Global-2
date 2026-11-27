import { test, expect } from "../../../helpers/test-base";
import {
  createSampleOrder,
  enterResults,
  validateResults,
} from "../../../helpers/seed-tat-data";
import { printedReport } from "../../../helpers/patient-report";

/**
 * OGC-932: Reports › Patient Status Report prints the redesigned patient
 * report for an order, both while a result awaits validation and once it is
 * released.
 *
 * The order is created and resulted through the app's API. The report is
 * requested through the form exactly as a user requests it, and the URL the
 * form opens is then fetched with the user's session so the PDF it serves can
 * be checked: a headless browser downloads a PDF rather than showing it. The
 * report's content is covered by the backend tests that render it.
 */

test("Patient Status Report prints an order awaiting validation and once released", async ({
  page,
}) => {
  const accession = await test.step("create an order", () =>
    createSampleOrder(page, { labNo: "", receivedDate: "", receivedTime: "" }));
  expect(accession).not.toBe("");

  await test.step("enter its result", () => enterResults(page, accession));
  const pending = await test.step("print it awaiting validation", () =>
    printedReport(page, accession));
  expect(pending.subarray(0, 5).toString()).toBe("%PDF-");

  await test.step("release it", () => validateResults(page, accession));
  const released = await test.step("print it released", () =>
    printedReport(page, accession));
  expect(released.subarray(0, 5).toString()).toBe("%PDF-");
  expect(released.toString("latin1")).toContain("DejaVuSans");
});
