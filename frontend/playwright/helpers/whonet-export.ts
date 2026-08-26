import { expect, type Download, type Page } from "@playwright/test";
import { LONG_TIMEOUT } from "./timeouts";

type WhonetExportFilters = {
  specimen?: string[];
  organism?: string[];
  origin?: string[];
  significance?: string[];
};

export const buildWhonetExportQuery = (
  exportDate: string,
  filters: WhonetExportFilters = {},
) => {
  const params = new URLSearchParams({
    from: exportDate,
    to: exportDate,
    dedup: "FIRST_ISOLATE_7_DAY",
    step: "configure",
    page: "1",
    pageSize: "100",
  });
  [...(filters.specimen || [])]
    .sort()
    .forEach((id) => params.append("specimen", id));
  [...(filters.organism || [])]
    .sort()
    .forEach((id) => params.append("organism", id));
  [...(filters.origin || [])]
    .sort()
    .forEach((id) => params.append("origin", id));
  [...(filters.significance || ["CLINICALLY_SIGNIFICANT"])]
    .sort()
    .forEach((id) => params.append("significance", id));

  const canonical = new URLSearchParams();
  ["from", "to"].forEach((key) => canonical.set(key, params.get(key) || ""));
  params
    .getAll("specimen")
    .forEach((value) => canonical.append("specimen", value));
  params
    .getAll("organism")
    .forEach((value) => canonical.append("organism", value));
  params.getAll("origin").forEach((value) => canonical.append("origin", value));
  params
    .getAll("significance")
    .forEach((value) => canonical.append("significance", value));
  ["dedup", "step", "page", "pageSize"].forEach((key) =>
    canonical.set(key, params.get(key) || ""),
  );
  return canonical.toString();
};

export const selectWhonetFilterOption = async (
  page: Page,
  filterName: RegExp,
  optionName: string,
) => {
  const filter = page.getByRole("combobox", { name: filterName });
  await filter.click();
  await expect(filter).toHaveAttribute("aria-expanded", "true");
  const listboxId = await filter.getAttribute("aria-controls");
  if (!listboxId) {
    throw new Error(`WHONET filter ${filterName} has no controlled listbox`);
  }
  const listbox = page.locator(`[id="${listboxId}"]`);
  await expect(listbox).toBeVisible();
  const supportsTextEntry = await filter.evaluate((element) =>
    element.matches("input, textarea, [contenteditable='true']"),
  );
  if (supportsTextEntry) {
    await filter.fill(optionName);
  }
  const option = listbox.getByRole("option", {
    name: optionName,
    exact: true,
  });
  await expect(option).toBeVisible();
  await option.click();
  await filter.press("Escape");
  await expect(filter).toHaveAttribute("aria-expanded", "false");
};

export const expectWhonetExportReady = async (page: Page) => {
  await expect(page.getByTestId("whonet-export")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "WHONET export", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("combobox", { name: /^Specimen types/ }),
  ).toBeEnabled();
};

export const whonetFixtureLabels = {
  specimen: (accessionNumber: string) =>
    `UAT WHONET specimen ${accessionNumber.replace(/^UATMICRO/, "")}`,
  mappedOrganism: "Reference organism (UAT)",
  unmappedOrganism: (accessionNumber: string) =>
    `WHONET mapping pending (UAT ${accessionNumber.replace(/^UATMICRO/, "")})`,
  inpatient: "Inpatient",
};

export const readWhonetDownload = async (download: Download) => {
  const stream = await download.createReadStream();
  let content = "";
  for await (const chunk of stream) content += chunk.toString();
  return content;
};

export const parseWhonetCsvLine = (line: string) => {
  const fields: string[] = [];
  const pattern = /"((?:[^"]|"")*)"(?:,|$)/g;
  for (const match of line.matchAll(pattern)) {
    fields.push(match[1].replace(/""/g, '"'));
  }
  return fields;
};
