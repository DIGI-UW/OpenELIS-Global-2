import { randomInt, randomUUID } from "node:crypto";
import { expect as baseExpect, test } from "../../../helpers/test-base";
import { worklistFor } from "../../../helpers/analyzer-api";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { activateShippedAnalyzer } from "../../../helpers/analyzer-setup-flow";
import {
  API,
  numeric,
  publish,
  send,
  type Draft,
} from "../../../helpers/analyzer-profile-api";
import {
  applyMappings,
  confirmMapping,
  saveMapping,
} from "../../../helpers/analyzer-mapping-page";
import { acceptAll, savedValue } from "../../../helpers/analyzer-review";
import { csrfToken } from "../../../helpers/api-session";

const expect = baseExpect.configure({ timeout: 15_000 });

// Synthetic chemistry traffic exercises a copied layout mismatch. This does
// not certify the mock template or this profile for any manufacturer's device.
for (const decimal of [
  { separator: ".", raw: "1.02", saved: "1.02" },
  { separator: ",", raw: "1,4", saved: "1.40" },
]) {
  test(
    `a copied layout is corrected and ${decimal.raw} is saved using its declared decimal format`,
    {
      tag: ["@astm", "@evidence", "@results"],
    },
    async ({ page }, testInfo) => {
      test.setTimeout(120_000);
      const run = randomUUID().slice(0, 8);
      const name = `Numeric interpretation ${run}`;
      const sender = `N1575-${run}`;
      const code = "N1575";
      const [target] = await seedNumericTests(
        page,
        run,
        [
          {
            name: `Numeric recovery ${run}`,
            loinc: `${randomInt(1_000_000, 9_000_000)}-1`,
          },
        ],
        { testSection: "Molecular Biology", sampleType: "Sputum" },
      );
      const tests = [numeric(code, target.loinc)];
      // Revision 1 intentionally retains GeneXpert's call in R.4.1 and number in
      // R.4.2. The generic mock's single numeric value is in R.4.1.
      const first: Draft = await send(
        page,
        "post",
        "/analyzer-types/cepheid-genexpert-astm/duplicate",
        { sourceRevision: 1, displayName: name },
      );
      (first.profile.configDefaults as Record<string, unknown>).numberFormat =
        decimal.separator;
      await publish(page, first, tests);
      const profileId = first.profile.profileMeta.id;
      const analyzer = await activateShippedAnalyzer(
        page,
        {
          profileId,
          revision: 1,
          displayName: name,
          labUnit: "Molecular Biology",
        },
        name,
        { senderId: sender },
      );
      const order = await createClinicalOrder(page, {
        testIds: [target.id],
        specimenName: "Sputum",
      });
      const transmit = async (
        completedAt: string,
        accession = order.accession,
        testCode = code,
      ) => {
        const probe = await page.request.post(
          `${API}/analyzer/analyzers/${analyzer.id}/test-connection`,
          {
            headers: { "X-CSRF-Token": await csrfToken(page) },
          },
        );
        const listener = (await probe.json()).checks.find(
          (check: { key: string }) => check.key === "listener",
        );
        const mock = process.env.MOCK_SIMULATOR_URL || "http://localhost:8085";
        await page.request.post(`${mock}/simulate/astm/mindray_ba88a`, {
          data: {
            destination: `tcp://bridge.openelis.org:${listener.details.port}`,
            sender_id: sender,
            sample_id: accession,
            completed_at: completedAt,
            results: [
              // TEXT asks the mock to preserve the literal decimal spelling.
              // The ASTM value field itself carries no data-type tag.
              {
                test_code: testCode,
                type: "TEXT",
                value: decimal.raw,
                unit: "mmol/L",
              },
            ],
          },
        });
      };
      const rows = () => worklistFor(page, analyzer.id, order.accession);
      await transmit("20261028120000");
      await expect.poll(async () => (await rows()).length).toBe(1);
      const original = (await rows())[0];
      expect(original.importIssueReason).toBeTruthy();
      await page.goto(`/analyzers/${analyzer.id}/mapping`);
      const expand = async () => {
        const button = page.getByRole("button", {
          name: new RegExp(`^${code}\\b`),
        });
        if ((await button.getAttribute("aria-expanded")) === "false")
          await button.click();
      };
      await expand();
      const received = page.getByRole("table", {
        name: `Received held results for ${code}`,
      });
      await expect(received).toContainText(decimal.raw);
      await expect(received).toContainText("Text");
      await expect(
        page.getByText("Text answers cannot map to a numeric result"),
      ).toBeVisible();
      await expect(
        page.getByRole("link", { name: "Add result options in Test Catalog" }),
      ).toHaveCount(0);
      await page.screenshot({
        path: testInfo.outputPath("numeric-read-as-text.png"),
        fullPage: true,
      });

      const corrected: Draft = await send(
        page,
        "post",
        `/analyzer-types/${encodeURIComponent(profileId)}/update`,
        { sourceRevision: 1 },
      );
      const defaults = corrected.profile.configDefaults as {
        extractionOverrides: { resultParts: Record<string, string> };
      };
      defaults.extractionOverrides.resultParts.number = "R.4.1";
      delete defaults.extractionOverrides.resultParts.call;
      await publish(page, corrected, tests);
      await page.goto(`/analyzers/${analyzer.id}/adoption?revision=2`);
      await page.getByRole("button", { name: "Save as revision 2" }).click();
      await expect(
        page.getByText("Revision 2 adopted.", { exact: false }),
      ).toBeVisible();
      await confirmMapping(page);
      await applyMappings(page);
      // Applying current mappings must not reinterpret the original retained text.
      expect(
        (await rows()).find((row) => row.id === original.id)?.importIssueReason,
      ).toBeTruthy();
      await expand();
      await expect(received).toContainText("Text");

      await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
      await page
        .getByRole("button", { name: "Remove from review", exact: true })
        .click();
      await expect.poll(async () => (await rows()).length).toBe(0);

      // Keep the value and its position unchanged. Only the corrected profile is
      // different; the completion time identifies a new instrument run.
      await transmit("20261028120100");
      await expect
        .poll(
          async () =>
            (await rows()).filter((row) => !row.importIssueReason).length,
        )
        .toBe(1);
      await acceptAll(page, analyzer, [order.accession], {
        note: "Synthetic mock supplies a generated patient name. Accession verified against the order created by this test.",
      });
      await expect
        .poll(() => savedValue(page, order, target.id))
        .toBe(decimal.saved);
      expect(await rows()).toHaveLength(0);
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(order.accession)}`,
      );
      await expect(
        page.getByRole("row").filter({ hasText: target.name }).first(),
      ).toContainText(decimal.saved);
      await page.screenshot({
        path: testInfo.outputPath("numeric-received-and-saved.png"),
        fullPage: true,
      });
      // A correctly interpreted number can also be held solely because its
      // code is unmapped. Applying the local mapping must recover that row.
      const extraCode = "N1575-EXTRA";
      const extraOrder = await createClinicalOrder(page, {
        testIds: [target.id],
        specimenName: "Sputum",
      });
      await transmit("20261028120200", extraOrder.accession, extraCode);
      const extraRows = () =>
        worklistFor(page, analyzer.id, extraOrder.accession);
      await expect.poll(async () => (await extraRows()).length).toBe(1);
      expect((await extraRows())[0].importIssueReason).toBeTruthy();
      await page.goto(`/analyzers/${analyzer.id}/mapping`);
      const extraButton = page.getByRole("button", {
        name: new RegExp(`^${extraCode}\\b`),
      });
      if ((await extraButton.getAttribute("aria-expanded")) === "false")
        await extraButton.click();
      const numericReceived = page.getByRole("table", {
        name: `Received held results for ${extraCode}`,
      });
      await expect(numericReceived).toContainText("Number");
      await expect(numericReceived).toContainText(decimal.raw);
      await expect(
        page.getByRole("combobox", {
          name: `OpenELIS result for ${decimal.raw}`,
          exact: true,
        }),
      ).toHaveCount(0);
      const picker = page.getByRole("combobox", {
        name: `OpenELIS test for ${extraCode}`,
        exact: true,
      });
      await picker.fill(target.name);
      await page
        .getByRole("option", { name: new RegExp(target.name) })
        .first()
        .click();
      await saveMapping(page);
      await confirmMapping(page);
      await applyMappings(page);
      await expect
        .poll(
          async () =>
            (await extraRows()).filter((row) => !row.importIssueReason).length,
        )
        .toBe(1);
      await acceptAll(page, analyzer, [extraOrder.accession], {
        note: "Synthetic mock supplies a generated patient name. Accession verified against the order created by this test.",
      });
      await expect
        .poll(() => savedValue(page, extraOrder, target.id))
        .toBe(decimal.saved);

      await testInfo.attach("numeric-recovery", {
        contentType: "application/json",
        body: JSON.stringify({
          source: "synthetic mock ASTM traffic",
          analyzerId: analyzer.id,
          profileId,
          revision: 2,
          accession: order.accession,
          savedValue: decimal.saved,
          decimalSeparator: decimal.separator,
          originalHeldId: original.id,
          recoveredNumericAccession: extraOrder.accession,
        }),
      });
    },
  );
}
