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
import { createDemoPresentation } from "../../../helpers/demo-presentation";

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

test(
  "keeps two laboratory units through case, bench and AST navigation and reload",
  { tag: ["@evidence", "@microbiology", "@case"] },
  async ({ page }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Working across two laboratory units",
      "A technologist who covers two laboratory units keeps both in view through the case, culture and AST worklists and a reload.",
    );
    await demo.caption(
      "Off screen: two growing cultures are set up, each in its own laboratory unit.",
    );
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
    const query = new URLSearchParams({ grain: "cases", q: "UATMICRO" });
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
    await demo.caption(
      "The case worklist, filtered to both units, lists both cases.",
    );
    await demo.highlight(table);
    await expect(table.getByRole("row")).toHaveCount(3);
    await demo.caption(
      "The culture worklist keeps both units; a culture opens on its case's culture section.",
    );
    await page.getByRole("button", { name: "Culture", exact: true }).click();
    await expect(
      table.getByRole("button", { name: first.accessionNumber, exact: true }),
    ).toBeVisible();
    await table
      .getByRole("button", { name: first.accessionNumber, exact: true })
      .click();
    await expect(
      page.getByLabel("Testing section", { exact: true }),
    ).toHaveValue("CULTURE");
    await demo.caption(
      "Back on the worklist and after a reload, both units are still selected.",
    );
    await page
      .getByTestId("microbiology-case-view")
      .getByRole("link", { name: "Microbiology worklist", exact: true })
      .click();
    await page.reload();
    await expect(table.getByRole("row")).toHaveCount(3);
    await demo.caption(
      "The AST worklist and case search carry the same two units.",
    );
    await page.getByRole("button", { name: "AST runs", exact: true }).click();
    await expect(page.getByTestId("microbiology-worklist")).toBeVisible();
    expect(
      new URL(page.url()).searchParams.getAll("labUnitIds").sort(),
    ).toEqual(units.sort());
    await page
      .getByRole("button", {
        name: "Case search",
        exact: true,
      })
      .click();
    await expect(table.getByRole("row")).toHaveCount(3);
    expect(
      new URL(page.url()).searchParams.getAll("labUnitIds").sort(),
    ).toEqual(units.sort());
    await demo.verified(
      "Both laboratory units stay selected everywhere",
      "The case, culture and AST worklists and a reload all keep the two units.",
    );
  },
);

test(
  "retains two-pass identification and isolate-targeted critical communication in the shared workspace",
  { tag: ["@evidence", "@microbiology", "@case"] },
  async ({ page }, info) => {
    const demo = createDemoPresentation(page, info);
    await demo.intro(
      "Identification in two passes, and a critical call about the isolate",
      "AST waits until the isolate is identified, and a critical notification logged from the isolate stays tied to it.",
    );
    await demo.caption("Off screen: a culture case with growth is set up.");
    const seeded = await seedMicrobiologyMvpCase(page);
    await page.goto(`/Microbiology/cases/${seeded.caseId}?view=workbench`);
    await createMicrobiologyIsolate(page, seeded);
    const isolate = page.locator('[data-testid^="isolate-"]');
    await expect(
      isolate.getByText("Gram negative rods", { exact: true }),
    ).toBeVisible();
    await demo.caption(
      "The isolate has its Gram stain but no organism yet, so Manual AST offers no run.",
    );
    await openMicrobiologyCaseSection(page, "Manual AST");
    await expect(
      page.getByRole("button", { name: "Start AST run", exact: true }),
    ).toHaveCount(0);
    await openMicrobiologyCaseSection(page, "Isolates");
    await demo.caption("In the second pass, the isolate is identified.");
    await identifyMicrobiologyIsolate(page, seeded.organismId!);
    await openMicrobiologyCaseSection(page, "Manual AST");
    await expect(
      page.getByRole("button", { name: "Start AST run", exact: true }),
    ).toBeEnabled();
    await demo.highlight(
      page.getByRole("button", { name: "Start AST run", exact: true }),
    );
    await openMicrobiologyCaseSection(page, "Isolates");
    await demo.caption(
      "A critical notification logged from ISO-1 targets that isolate, and the target cannot be changed.",
    );
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
    await demo.highlight(page.getByLabel("Critical result target"));
    await demo.caption("The case timeline records who did each step.");
    await openMicrobiologyCaseSection(page, "Timeline");
    await expect(
      page.getByRole("table", { name: "Case timeline", exact: true }),
    ).toContainText("Open ELIS");
    const frame = page.viewportSize();
    for (const width of [1440, 390]) {
      await page.setViewportSize({ width, height: 1000 });
      await expect
        .poll(() =>
          page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        )
        .toBe(true);
      await page.evaluate(() => window.scrollTo(0, 0));
      await page.screenshot({
        path: info.outputPath(`m9-history-${width}.png`),
        fullPage: true,
      });
    }
    if (frame) await page.setViewportSize(frame);
    await demo.verified(
      "AST waits for the identification; the critical call stays with the isolate",
      "Start AST run appeared only after ISO-1 was identified, and the notification's target is fixed to ISO-1.",
    );
  },
);

test(
  "reports an NCE and records a separate lost specimen with keyboard access",
  { tag: ["@evidence", "@microbiology", "@case"] },
  async ({ page }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Reporting a nonconformance and a lost specimen",
      "Both are recorded from the case with the keyboard alone, and each lands on the case timeline.",
    );
    test.setTimeout(90_000);
    for (const lost of [false, true]) {
      await demo.caption(
        lost
          ? "Off screen: a second case is ordered. Its specimen cannot be found."
          : "Off screen: a case is ordered. Its container arrived cracked.",
      );
      const seeded = await seedMicrobiologyCase(page);
      await page.goto(`/Microbiology/cases/${seeded.caseId}`);
      const action = page.getByRole("button", {
        name: lost ? "Mark lost" : "Report NCE",
        exact: true,
      });
      await demo.caption(
        lost
          ? "Mark lost opens the same form with its category fixed; the rest is filled from the keyboard."
          : "Report NCE opens a form with focus on its heading; it is filled from the keyboard.",
      );
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
      await expect(timeline).toContainText(
        lost ? "Specimen Lost" : description,
        {
          timeout: LONG_TIMEOUT,
        },
      );
      await expect(
        page.getByRole("region", { name: "Case timeline", exact: true }),
      ).toBeFocused();
      await demo.highlight(timeline);
      await page.reload();
      await expect(timeline).toContainText(
        lost ? "Specimen Lost" : description,
      );
    }
    await demo.verified(
      "The nonconformance and the lost specimen are both on record",
      "Each was entered from the keyboard and appears on its case timeline after a reload.",
    );
  },
);
