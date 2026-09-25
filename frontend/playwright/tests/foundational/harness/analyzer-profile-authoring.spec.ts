import { expect, test } from "../../../helpers/test-base";
import { expectNoPageHorizontalOverflow } from "../../../helpers/responsive-layout";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  TIMEOUT_SCALE,
} from "../../../helpers/timeouts";

// Synthetic interface settings; these are not claims about any manufacturer's device.
for (const protocol of ["FILE", "ASTM", "HL7"] as const) {
  test(`creates, reopens and publishes a ${protocol} profile with explicit control recognition`, async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const name = `Synthetic ${protocol} authoring ${Date.now()}`;
    await page.goto("/analyzers/types", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { level: 1, name: "Analyzer Types" }),
    ).toBeVisible();
    await page
      .getByRole("button", { name: "Create Profile", exact: true })
      .click();
    const dialog = page.getByRole("dialog", {
      name: "Create Profile",
      exact: true,
    });
    await dialog
      .getByRole("textbox", { name: "Profile name", exact: true })
      .fill(name);
    await dialog
      .getByRole("button", { name: "Create Profile", exact: true })
      .click();
    await expect(
      dialog.getByText("Profile draft created", { exact: true }),
    ).toBeVisible();
    await expect(page).toHaveURL(/action=create&draft=/);
    const draftUrl = page.url();
    const text = async (label: string, value: string) =>
      dialog.getByRole("textbox", { name: label, exact: true }).fill(value);
    const choose = async (label: string, value: string) =>
      dialog
        .getByRole("combobox", { name: label, exact: true })
        .selectOption(value);
    await text("Profile version", "1.0");
    await choose("Evidence confidence", "LOW");
    await choose("Laboratory discipline", "MOLECULAR");
    await choose("Protocol", protocol);
    await choose("Receives analyzer results", "true");
    await choose("Sends orders to the analyzer", "false");
    await choose("Supports a connection test", "true");

    if (protocol === "FILE") {
      await choose("Profile file format", "CSV");
      await choose("Default file format", "CSV");
      await text("Filename pattern", "*.csv");
      await choose("First row contains column names", "true");
      await text("Column delimiter", ",");
      const extensions = dialog.getByRole("group", {
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
        await dialog
          .getByRole("button", { name: "Add column", exact: true })
          .click();
        const row = dialog.getByRole("group", {
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
          .getByRole("combobox", { name: "Meaning of the column", exact: true })
          .selectOption(meaning);
      }
    } else {
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
      await dialog.getByText("Support TCP/IP", { exact: true }).click();
      await choose("Who starts communication", "ANALYZER_INITIATED");
      await choose(
        "Allows the laboratory system to start communication",
        "false",
      );
      await choose("Default connection role", "SERVER");
      await choose("Default transport", "TCP/IP");
      await choose("Result grouping", "PER_MESSAGE");
    }
    await dialog
      .getByRole("button", { name: "Add connection field", exact: true })
      .click();
    const connection = dialog.getByRole("group", {
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

    await dialog
      .getByRole("button", {
        name: "Save and validate profile settings",
        exact: true,
      })
      .click();
    await expect(
      dialog.getByText("Profile settings saved", { exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      dialog.getByRole("button", { name: "Publish Profile", exact: true }),
    ).toBeDisabled();
    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(page).toHaveURL(draftUrl);
    await expect(
      dialog.getByRole("textbox", { name: "Profile name", exact: true }),
    ).toHaveValue(name);
    await expect(
      dialog.getByRole("combobox", { name: "Protocol", exact: true }),
    ).toHaveValue(protocol);
    if (protocol === "FILE") {
      await expect(
        dialog.getByRole("textbox", { name: "Filename pattern", exact: true }),
      ).toHaveValue("*.csv");
      await expect(
        dialog.getByRole("group", { name: /^File column \d+$/ }),
      ).toHaveCount(3);
    } else {
      await expect(
        dialog.getByRole("combobox", {
          name: "Default connection role",
          exact: true,
        }),
      ).toHaveValue("SERVER");
    }
    // Use the existing UI's explicit rule authoring; never affirm absent controls by assumption.
    await dialog
      .getByText("Recognize controls using these conditions", { exact: true })
      .click();
    await dialog
      .getByRole("combobox", { name: "New condition", exact: true })
      .selectOption("SPECIMEN_ID_STARTS_WITH|");
    await dialog
      .getByRole("button", { name: "Add condition", exact: true })
      .click();
    await dialog
      .getByRole("textbox", { name: "Specimen ID prefix", exact: true })
      .fill("QC-");
    await dialog
      .getByRole("button", { name: "Save control recognition", exact: true })
      .click();
    const publish = dialog.getByRole("button", {
      name: "Publish Profile",
      exact: true,
    });
    await expect(publish).toBeEnabled({ timeout: LONG_TIMEOUT });
    if (protocol === "FILE") {
      await page.setViewportSize({ width: 390, height: 844 });
      await expect(publish).toBeInViewport();
      await expectNoPageHorizontalOverflow(
        page,
        "Profile authoring remains usable on a narrow viewport",
      );
    }
    await testInfo.attach(`saved-${protocol.toLowerCase()}-draft`, {
      body: await page.screenshot(),
      contentType: "image/png",
    });
    await publish.click();
    await expect(dialog).not.toBeVisible({ timeout: LONG_TIMEOUT });
    const row = page.getByRole("row", { name: new RegExp(name) });
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(row).toContainText("Site");
    await expect(row).toContainText("revision 1");
    await expect(row).toContainText(protocol);
    await testInfo.attach(`published-${protocol.toLowerCase()}-profile`, {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });
}
