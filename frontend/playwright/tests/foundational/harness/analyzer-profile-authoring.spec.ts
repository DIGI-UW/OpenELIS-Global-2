import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import {
  analyzerTypeRow,
  openAnalyzerTypes,
  openRowAction,
} from "../../../helpers/analyzer-types-page";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { expectNoPageHorizontalOverflow } from "../../../helpers/responsive-layout";

const LANE = { FILE: "@file", ASTM: "@astm", HL7: "@hl7" } as const;

// Synthetic interface settings; these are not claims about any manufacturer's device.
for (const protocol of ["FILE", "ASTM", "HL7"] as const) {
  test.describe.serial(`A site profile for a ${protocol} instrument`, () => {
    const name = `Synthetic ${protocol} authoring ${Date.now()}`;

    test(
      `creates, reopens and publishes a ${protocol} profile with explicit control recognition`,
      { tag: ["@evidence", LANE[protocol], "@admin"] },
      async ({ page }, testInfo) => {
        const demo = createDemoPresentation(page, testInfo);
        await demo.intro(
          `Author a ${protocol} profile`,
          `A Bridge administrator describes a ${protocol} instrument as a site profile, saves the draft, and publishes it as revision 1.`,
        );
        await openAnalyzerTypes(page);
        await demo.caption(
          "Analyzer Types lists what the Bridge ships and what this site created. Create type starts a draft.",
        );
        await page
          .getByRole("button", { name: "Create type", exact: true })
          .click();
        const dialog = page.getByRole("dialog", {
          name: "Create type",
          exact: true,
        });
        await dialog
          .getByRole("textbox", { name: "Type name", exact: true })
          .fill(name);
        await dialog
          .getByRole("button", { name: "Create type", exact: true })
          .click();
        await expect(page).toHaveURL(/\/analyzers\/types\/drafts\//);
        const draft = page.getByTestId("analyzer-type-draft-page");
        await expect(
          draft.getByRole("heading", { level: 1, name: `New type: ${name}` }),
        ).toBeVisible();
        const draftUrl = page.url();
        await demo.caption(
          "The draft opens on its own page and lives in the Bridge. Its settings describe the interface: the protocol, what it receives, and how it connects.",
        );
        const text = async (label: string, value: string) =>
          draft.getByRole("textbox", { name: label, exact: true }).fill(value);
        const choose = async (label: string, value: string) =>
          draft
            .getByRole("combobox", { name: label, exact: true })
            .selectOption(value);
        await text("Type version", "1.0");
        await choose("Evidence confidence", "LOW");
        await choose("Laboratory discipline", "MOLECULAR");
        await choose("Protocol", protocol);
        await choose("Receives analyzer results", "true");
        await choose("Sends orders to the analyzer", "false");
        await choose("Supports a connection test", "true");

        if (protocol === "FILE") {
          await demo.caption(
            "A file profile says what the instrument writes: the format, the filename pattern, and what each column means.",
          );
          await choose("File format", "CSV");
          await choose("Default file format", "CSV");
          await text("Filename pattern", "*.csv");
          await choose("First row contains column names", "true");
          await text("Column delimiter", ",");
          const extensions = draft.getByRole("group", {
            name: "Supported file extensions",
            exact: true,
          });
          await extensions
            .getByRole("button", { name: "Add value", exact: true })
            .click();
          await extensions.getByRole("textbox").fill(".csv");
          const columns = [
            ["Sample", "sampleId"],
            ["Code", "testCode"],
            ["Value", "result"],
          ];
          for (const [index, [source, meaning]] of columns.entries()) {
            await draft
              .getByRole("button", { name: "Add column", exact: true })
              .click();
            const row = draft.getByRole("group", {
              name: `File column ${index + 1}`,
              exact: true,
            });
            await row
              .getByRole("textbox", {
                name: "Column name in the file",
                exact: true,
              })
              .fill(source);
            await row
              .getByRole("combobox", {
                name: "Meaning of the column",
                exact: true,
              })
              .selectOption(meaning);
          }
        } else {
          await demo.caption(
            `A ${protocol} profile says how the instrument names itself in messages and how the connection is made.`,
          );
          await text("Manufacturer", "Synthetic manufacturer");
          await text("Instrument name in messages", "SYNTHETIC");
          await text("Analyzer identifier pattern", "^SYNTHETIC$");
          await text(
            "Protocol version",
            protocol === "ASTM" ? "LIS02-A2" : "2.5.1",
          );
          if (protocol === "ASTM") {
            await choose("ASTM framing version", "LIS01_A");
            await choose("Result records to parse", "ALL");
          }
          await draft.getByText("Support TCP/IP", { exact: true }).click();
          await choose("Who starts communication", "ANALYZER_INITIATED");
          await choose(
            "Allows the laboratory system to start communication",
            "false",
          );
          await choose("Default connection role", "SERVER");
          await choose("Default transport", "TCP/IP");
          await choose("Result grouping", "PER_MESSAGE");
        }
        await demo.caption(
          "A connection field is what setup will ask for on each analyzer of this type.",
        );
        await draft
          .getByRole("button", { name: "Add connection field", exact: true })
          .click();
        const connection = draft.getByRole("group", {
          name: "Connection field 1",
          exact: true,
        });
        await connection
          .getByRole("textbox", { name: "Setting name", exact: true })
          .fill(protocol === "FILE" ? "directory" : "host");
        await connection
          .getByRole("textbox", { name: "Label translation key", exact: true })
          .fill(
            protocol === "FILE"
              ? "analyzer.connection.field.directory"
              : "analyzer.connection.field.host",
          );
        await connection
          .getByRole("combobox", { name: "Input type", exact: true })
          .selectOption(protocol === "FILE" ? "FILE_PATH" : "TEXT");
        await connection
          .getByRole("combobox", {
            name: "Required during connection setup",
            exact: true,
          })
          .selectOption(protocol === "FILE" ? "true" : "false");

        await draft
          .getByRole("button", {
            name: "Save and validate settings",
            exact: true,
          })
          .click();
        await expect(
          draft.getByText("Settings saved", { exact: true }),
        ).toBeVisible();
        await expect(
          draft.getByRole("button", { name: "Publish type", exact: true }),
        ).toBeDisabled();
        await demo.caption(
          "Settings saved. Publish stays shut until control recognition is stated. Reloading the draft's address brings it back as saved.",
        );
        await page.reload({ waitUntil: "domcontentloaded" });
        await expect(page).toHaveURL(draftUrl);
        await expect(
          draft.getByRole("textbox", { name: "Type name", exact: true }),
        ).toHaveValue(name);
        await expect(
          draft.getByRole("combobox", { name: "Protocol", exact: true }),
        ).toHaveValue(protocol);
        if (protocol === "FILE") {
          await expect(
            draft.getByRole("textbox", {
              name: "Filename pattern",
              exact: true,
            }),
          ).toHaveValue("*.csv");
          await expect(
            draft.getByRole("group", { name: /^File column \d+$/ }),
          ).toHaveCount(3);
        } else {
          await expect(
            draft.getByRole("combobox", {
              name: "Default connection role",
              exact: true,
            }),
          ).toHaveValue("SERVER");
        }
        await demo.caption(
          "Controls are recognized by a rule the author states, here specimen IDs starting with QC-. Nothing is assumed about them.",
        );
        // Use the existing UI's explicit rule authoring; never affirm absent controls by assumption.
        await draft
          .getByText("Recognize controls using these conditions", {
            exact: true,
          })
          .click();
        await draft
          .getByRole("combobox", { name: "New condition", exact: true })
          .selectOption("SPECIMEN_ID_STARTS_WITH|");
        await draft
          .getByRole("button", { name: "Add condition", exact: true })
          .click();
        await draft
          .getByRole("textbox", { name: "Specimen ID prefix", exact: true })
          .fill("QC-");
        await draft
          .getByRole("button", {
            name: "Save control recognition",
            exact: true,
          })
          .click();
        const publish = draft.getByRole("button", {
          name: "Publish type",
          exact: true,
        });
        await expect(publish).toBeEnabled();
        await demo.highlight(publish);
        if (protocol === "FILE") {
          const frame = page.viewportSize();
          await page.setViewportSize({ width: 390, height: 844 });
          await demo.caption("On a phone, Publish stays within reach.");
          await expect(publish).toBeInViewport();
          await expectNoPageHorizontalOverflow(
            page,
            "Profile authoring remains usable on a narrow viewport",
          );
          if (frame) {
            await page.setViewportSize(frame);
          }
        }
        await testInfo.attach(`saved-${protocol.toLowerCase()}-draft`, {
          body: await page.screenshot(),
          contentType: "image/png",
        });
        await demo.caption(
          "Publish type: the type is revision 1, site-created, and offered in analyzer setup.",
        );
        await publish.click();
        await expect(page).toHaveURL(/\/analyzers\/types$/);
        const row = analyzerTypeRow(page, name);
        await expect(row).toBeVisible();
        await expect(row).toContainText("Site");
        await expect(row).toContainText("revision 1");
        await expect(row).toContainText(protocol);
        await demo.highlight(row);
        await testInfo.attach(`published-${protocol.toLowerCase()}-profile`, {
          body: await page.screenshot(),
          contentType: "image/png",
        });
        await demo.verified(
          `${name} is published as revision 1`,
          "Authored from its settings and an explicit control rule; nothing copied.",
        );
      },
    );

    if (protocol !== "ASTM") {
      return;
    }

    test(
      "publishes a new revision through New revision; analyzers keep the revision they chose",
      { tag: ["@evidence", LANE[protocol], "@admin"] },
      async ({ page }, testInfo) => {
        const demo = createDemoPresentation(page, testInfo);
        await demo.intro(
          "Update a shared profile",
          "A change to a site profile becomes a new revision. Analyzers already set up stay on theirs.",
        );
        await openAnalyzerTypes(page);
        const row = analyzerTypeRow(page, name);
        await expect(row).toContainText("revision 1");
        await demo.caption("New revision starts a draft from revision 1.");
        await openRowAction(page, name, "New revision");
        const dialog = page.getByRole("dialog", {
          name: `New revision of ${name}`,
        });
        await expect(dialog).toContainText(
          "This starts a new immutable revision from revision 1.",
        );
        await dialog
          .getByRole("button", { name: "Start the revision", exact: true })
          .click();
        await expect(page).toHaveURL(/\/analyzers\/types\/drafts\//);
        const draft = page.getByTestId("analyzer-type-draft-page");
        await expect(
          draft.getByRole("heading", {
            level: 1,
            name: `New revision of ${name}`,
          }),
        ).toBeVisible();
        await demo.caption(
          "The draft opens on its own page. The version is raised and the settings saved again.",
        );
        await draft
          .getByRole("textbox", { name: "Type version", exact: true })
          .fill("1.1");
        await draft
          .getByRole("button", {
            name: "Save and validate settings",
            exact: true,
          })
          .click();
        await expect(
          draft.getByText("Settings saved", { exact: true }),
        ).toBeVisible();
        const publish = draft.getByRole("button", {
          name: "Publish type",
          exact: true,
        });
        await expect(publish).toBeEnabled();
        await demo.caption("Publish: revision 2 of the shared type.");
        await publish.click();
        await expect(page.getByText("Type updated")).toBeVisible();
        await expect(row).toContainText("revision 2");
        await demo.highlight(row);
        await demo.caption(
          "Its history keeps revision 1. An analyzer set up on it stays there until it adopts revision 2.",
        );
        await openRowAction(page, name, "History");
        const history = page.getByRole("dialog", { name: `${name} history` });
        await expect(history).toContainText("Revision 2");
        await expect(history).toContainText("Updated");
        await expect(history).toContainText("Revision 1");
        await demo.highlight(history);
        await page.keyboard.press("Escape");
        await expect(history).not.toBeVisible();
        await demo.verified(
          `${name} is at revision 2`,
          "Revision 1 stays in its history; nothing changed for analyzers on it.",
        );
      },
    );

    test(
      "deactivates the type, which leaves analyzer setup, and reactivates it",
      { tag: ["@evidence", LANE[protocol], "@admin"] },
      async ({ page }, testInfo) => {
        const demo = createDemoPresentation(page, testInfo);
        await demo.intro(
          "Deactivate and reactivate a type",
          "A deactivated type is not offered to new analyzers. Its history and the analyzers on it stay as they are.",
        );
        const list = new AnalyzerListPage(page);
        const setup = new AnalyzerSetupPage(page);
        const offered = () =>
          page.getByRole("option", { name: new RegExp(name, "i") });
        const openTypePicker = async () => {
          await list.goto();
          await list.expectLoaded();
          await list.clickAdd();
          await setup.expectOpen();
          await setup.typePicker.click();
          await setup.typePicker.fill(name);
        };

        await openAnalyzerTypes(page);
        await demo.caption(
          "Deactivate asks for confirmation and says what stays.",
        );
        await openRowAction(page, name, "Deactivate");
        const deactivate = page.getByRole("dialog", {
          name: `Deactivate ${name}`,
        });
        await expect(deactivate).toContainText(
          "This type will no longer be available for new analyzer setup.",
        );
        // Carbon names a danger button "danger <label>" for screen readers.
        await deactivate.getByRole("button", { name: /Deactivate$/ }).click();
        await expect(page.getByText("Type deactivated")).toBeVisible();
        await demo.caption(
          "Deactivated types leave the active list for their own view.",
        );
        await expect(analyzerTypeRow(page, name)).toHaveCount(0);
        await page.getByTestId("analyzer-type-view-deactivated").click();
        const row = analyzerTypeRow(page, name);
        await expect(row).toContainText("Deactivated");
        await demo.highlight(row);

        await demo.caption("In analyzer setup, the type is no longer offered.");
        await openTypePicker();
        await expect(offered()).toHaveCount(0);
        await demo.highlight(setup.typePicker);
        await setup.close();

        await demo.caption("Reactivate: the type is offered again.");
        await openAnalyzerTypes(page, "?view=deactivated");
        await openRowAction(page, name, "Reactivate");
        const reactivate = page.getByRole("dialog", {
          name: `Reactivate ${name}`,
        });
        await expect(reactivate).toContainText(
          "This type will be available for new analyzer setup again.",
        );
        await reactivate
          .getByRole("button", { name: "Reactivate", exact: true })
          .click();
        await expect(page.getByText("Type reactivated")).toBeVisible();
        await page.getByTestId("analyzer-type-view-all").click();
        await expect(analyzerTypeRow(page, name)).toContainText("Active");
        await openTypePicker();
        await expect(offered()).toHaveCount(1);
        await demo.highlight(offered());
        await setup.close();
        await demo.verified(
          `${name} is active again`,
          "While deactivated it was not offered in setup; its revisions and history were untouched.",
        );
      },
    );
  });
}
