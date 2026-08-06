import { test, expect } from "../../../helpers/test-base";
import {
  seedMicrobiologyCase,
  seedMicrobiologyClassificationCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology case workbench", () => {
  test("records inoculation lineage and creates an isolate", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyCase(page);
    await page.goto(
      `/Microbiology/cases/${seeded.caseId}?workflow=BACTERIOLOGY&sort=newest`,
      {
        waitUntil: "commit",
      },
    );

    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const caseHeader = page.locator("header");
    const caseView = page.getByTestId("microbiology-case-view");
    await expect(caseHeader.getByTitle("Received")).toBeVisible();

    await caseView
      .getByRole("button", { name: "Inoculation", exact: true })
      .click();
    await expect(page).toHaveURL(/section=setup/);
    await page.getByRole("button", { name: "Start inoculation" }).click();
    await page.getByLabel("Bottle or plate ID").fill("BOTTLE-001");
    await page.getByLabel("Media or bottle").fill("Blood culture bottle");
    await page.getByLabel("Incubation").fill("35 C for 24 hours");
    await page.getByLabel("Atmosphere").fill("Ambient");
    await page.getByRole("button", { name: "Save media" }).click();
    await expect(caseHeader.getByTitle("Incubating")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByRole("cell", { name: "BOTTLE-001" })).toBeVisible();

    await page.getByRole("button", { name: "Add subculture" }).click();
    await page.getByLabel("Parent media").selectOption({ label: /BOTTLE-001/ });
    await page.getByLabel("Bottle or plate ID").fill("PLATE-002");
    await page.getByLabel("Media or bottle").fill("MacConkey agar");
    await page.getByRole("button", { name: "Save media" }).click();
    await expect(page.getByRole("cell", { name: "PLATE-002" })).toBeVisible();
    await expect(
      page.getByRole("cell", { name: /Subculture BOTTLE-001/ }),
    ).toBeVisible();

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline/);
    await expect(page.getByText("Inoculation Recorded")).toBeVisible();
    await expect(
      page.getByText(/BOTTLE-001 - Blood culture bottle/),
    ).toBeVisible();
    await expect(page.getByText("Subculture Recorded")).toBeVisible();
    await expect(page.getByText(/PLATE-002 - MacConkey agar/)).toBeVisible();

    await caseView
      .getByRole("button", { name: "Isolates", exact: true })
      .click();
    await expect(page).toHaveURL(/section=isolates/);
    await page.getByLabel("Preliminary organism").fill("Escherichia coli");
    await page.getByRole("button", { name: "Create isolate" }).click();
    await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline/);
    await expect(page.getByText(/Isolate Created/)).toBeVisible();
  });

  test("classifies unassigned work before profile-specific actions", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyClassificationCase(page);
    if (!seeded.siblingCaseId || !seeded.methodId) {
      throw new Error(
        "R1 classification fixture must provide a sibling case and compatible culture method",
      );
    }
    await page.goto(
      `/Microbiology/cases/${seeded.caseId}?workflow=UNASSIGNED&sort=newest&section=ast`,
      { waitUntil: "commit" },
    );

    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page).toHaveURL(
      new RegExp(
        `/Microbiology/cases/${seeded.caseId}\\?workflow=UNASSIGNED&sort=newest&section=case-info$`,
      ),
    );
    await expect(
      page.getByText("Workflow classification required"),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Inoculation", exact: true }),
    ).toBeDisabled();
    await expect(page.getByLabel("Bacteriology (Received)")).toHaveAttribute(
      "href",
      new RegExp(`/Microbiology/cases/${seeded.siblingCaseId}`),
    );

    await page.getByLabel("Workflow").selectOption("MYCOBACTERIOLOGY_TB");
    await expect(page.getByLabel("Culture Method")).toBeEnabled();
    await page.getByLabel("Culture Method").selectOption(seeded.methodId);
    await page
      .getByLabel("Reason for change")
      .fill("Corrected during accession review");
    await page.getByRole("button", { name: "Apply workflow" }).click();

    await expect(
      page.getByText("Workflow classification required"),
    ).toBeHidden();
    await expect(
      page.locator("header").getByText("Mycobacteriology TB"),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Inoculation", exact: true }),
    ).toBeEnabled();
    await page.getByRole("button", { name: "Timeline", exact: true }).click();
    await expect(page).toHaveURL(/section=timeline/);
    await expect(
      page.getByText("Corrected during accession review"),
    ).toBeVisible();
    await expect(page.getByText("Workflow Changed")).toBeVisible();
  });
});
