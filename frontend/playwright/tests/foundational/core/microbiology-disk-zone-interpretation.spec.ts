import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { API_PREFIX, apiPost } from "../../../helpers/admin-ui";
import { csrfToken } from "../../../helpers/api-session";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  createMicrobiologyIsolate,
  identifyMicrobiologyIsolate,
  openMicrobiologyCaseSection,
} from "../../../helpers/microbiology-ui";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const REFERENCE = "/rest/microbiology/admin/reference";
const BREAKPOINTS = "/rest/microbiology/admin/breakpoints";

test(
  "a disk diffusion run uses the organism's default panel and reads each zone against its own disk content",
  { tag: ["@evidence", "@microbiology", "@ast"] },
  async ({ page }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Disk zones read against their own disk content",
      "An identified organism brings its default AST panel, and each zone diameter is interpreted only against breakpoints for the disk the panel uses.",
    );
    await demo.caption(
      "Off screen: a panel with a 30 mcg and a 5 mcg disk becomes an organism's default. Zone breakpoints are imported and activated: for the first drug on a 30 mcg disk, for the second on a 10 mcg disk.",
    );
    // The scenario activates its own CLSI standard, and one standard per authority is
    // active, so the story's standard is activated after it.
    const seeded = await seedMicrobiologyMvpCase(page);
    const run = randomUUID().replace(/-/g, "").slice(0, 6).toUpperCase();
    const organismName = `Zone organism ${run}`;
    const zoneDrug = (await apiPost(page, `${REFERENCE}/antibiotics`, {
      displayName: `Zone drug ${run}`,
      whonetCode: `ZA${run}`,
      antibioticClass: "Cephalosporin",
      route: "IV",
    })) as { id: string };
    const otherDiskDrug = (await apiPost(page, `${REFERENCE}/antibiotics`, {
      displayName: `Other disk drug ${run}`,
      whonetCode: `ZB${run}`,
      antibioticClass: "Cephalosporin",
      route: "IV",
    })) as { id: string };
    const panel = (await apiPost(page, `${REFERENCE}/ast-panels`, {
      name: `Disk panel ${run}`,
      antibiotics: [
        {
          antibioticId: zoneDrug.id,
          displayOrder: 1,
          tier: 1,
          reportBehavior: "ALWAYS",
          diskContent: "30 mcg",
        },
        {
          antibioticId: otherDiskDrug.id,
          displayOrder: 2,
          tier: 1,
          reportBehavior: "ALWAYS",
          diskContent: "5 mcg",
        },
      ],
    })) as { id: string; name: string };
    const organism = (await apiPost(page, `${REFERENCE}/organisms`, {
      displayName: organismName,
      whonetCode: `zo${run.toLowerCase()}`,
      organismGroup: `Zone group ${run}`,
      gramStain: "NEGATIVE",
      initialSignificance: "USUALLY",
      defaultAstPanelId: panel.id,
    })) as { id: string };

    // One rule per drug: the zone drug's is for the panel's 30 mcg disk, the other drug's for a 10 mcg disk the panel does not use.
    const version = `ZONE-${run}`;
    const csv = [
      "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,breakpoint_type,disk_content,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units",
      `CLSI,${version},${organismName},ZA${run},ZONE,,ZONE,30 mcg,23,20,22,19,mm`,
      `CLSI,${version},${organismName},ZB${run},ZONE,,ZONE,10 mcg,18,15,17,14,mm`,
    ].join("\n");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const preview = await page.request.post(
      `${API_PREFIX}${BREAKPOINTS}/imports/preview`,
      { data: csv, headers: { ...headers, "Content-Type": "text/csv" } },
    );
    expect(preview.ok(), await preview.text()).toBeTruthy();
    const { previewToken, validRows, errors } = await preview.json();
    expect(validRows, JSON.stringify(errors)).toBe(2);
    const applied = await page.request.post(
      `${API_PREFIX}${BREAKPOINTS}/imports/${previewToken}/apply`,
      { headers },
    );
    expect(applied.ok(), await applied.text()).toBeTruthy();
    const standards = (await (
      await page.request.get(`${API_PREFIX}${BREAKPOINTS}/standards`)
    ).json()) as { rows: Array<{ id: string; version: string }> };
    const standard = standards.rows.find((row) => row.version === version);
    expect(standard).toBeTruthy();
    const activated = await page.request.post(
      `${API_PREFIX}${BREAKPOINTS}/standards/${standard!.id}/activate?effectiveDate=${new Date().toISOString().slice(0, 10)}`,
      { headers },
    );
    expect(activated.ok(), await activated.text()).toBeTruthy();

    await page.goto(`/Microbiology/cases/${seeded.caseId}?view=workbench`);
    await demo.caption(
      "The culture has grown off screen. On the case, an isolate is picked and identified as the new organism.",
    );
    await createMicrobiologyIsolate(page, seeded);
    await identifyMicrobiologyIsolate(page, organism.id);

    await openMicrobiologyCaseSection(page, "Manual AST");
    const isolate = page.locator('[data-testid^="ast-isolate-"]');
    const assigned = isolate.getByText(`Assigned panels: ${panel.name}`);
    await expect(assigned).toBeVisible({ timeout: LONG_TIMEOUT });
    await demo.caption(
      "The organism's default panel is already assigned to the isolate.",
    );
    await demo.highlight(assigned);
    await isolate
      .getByRole("button", { name: "Start AST run", exact: true })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByLabel("Panel", { exact: true })).toHaveValue(
      panel.id,
    );
    await dialog
      .getByLabel("Technique", { exact: true })
      .selectOption("DISK_DIFFUSION");
    await dialog
      .getByLabel("Breakpoint standard", { exact: true })
      .selectOption(standard!.id);
    await expect(dialog.getByLabel(/Reason/)).toHaveCount(0);
    await demo.caption(
      "The run starts on that panel by disk diffusion. Using the default panel needs no reason.",
    );
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });

    const runRow = (drug: string) =>
      isolate.getByRole("row").filter({ hasText: drug });
    const enterZone = async (drug: string, millimetres: string) => {
      await runRow(drug)
        .getByRole("button", { name: "Enter reading", exact: true })
        .click();
      await dialog.getByLabel("MIC / zone", { exact: true }).fill(millimetres);
      await dialog.getByRole("button", { name: "Save", exact: true }).click();
      await expect(dialog).toHaveCount(0, { timeout: LONG_TIMEOUT });
    };

    await demo.caption(
      "The 30 mcg disk reads 21 mm: intermediate under its 30 mcg breakpoint.",
    );
    await enterZone(`Zone drug ${run}`, "21");
    const zoneReading = runRow(`Zone drug ${run}`);
    await expect(zoneReading).toContainText("Intermediate", {
      timeout: LONG_TIMEOUT,
    });
    await demo.highlight(zoneReading);

    await demo.caption(
      "The second drug's disk is 5 mcg, but its breakpoint is for a 10 mcg disk, so its zone is not interpreted with it.",
    );
    await enterZone(`Other disk drug ${run}`, "25");
    const otherReading = runRow(`Other disk drug ${run}`);
    await expect(otherReading).toContainText("No matching breakpoint", {
      timeout: LONG_TIMEOUT,
    });
    await demo.highlight(otherReading);

    await demo.verified(
      "Each zone is read against its own disk",
      "21 mm on the 30 mcg disk is intermediate, and the 5 mcg disk has no matching breakpoint.",
    );
  },
);
