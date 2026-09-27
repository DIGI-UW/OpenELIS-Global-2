import { describe, expect, it } from "vitest";
import {
  canonicalReportingUrl,
  CUSTOM_DATA_EXPORT_PATH,
  LEGACY_CUSTOM_DATA_EXPORT_PATH,
  REPORTS_PATH,
} from "./routes";

describe("canonicalReportingUrl", () => {
  it("rewrites the legacy aliases", () => {
    expect(canonicalReportingUrl(LEGACY_CUSTOM_DATA_EXPORT_PATH)).toBe(
      CUSTOM_DATA_EXPORT_PATH,
    );
    expect(canonicalReportingUrl(REPORTS_PATH)).toBe(CUSTOM_DATA_EXPORT_PATH);
  });

  // The workspace opened at the canonical path with a trailing slash used to be
  // returned unchanged, so callers comparing against CUSTOM_DATA_EXPORT_PATH
  // missed and reportingMenuDestination skipped its query-merging branch,
  // dropping preserved review parameters from a sidebar click.
  it("normalizes a trailing slash on the canonical path", () => {
    expect(canonicalReportingUrl(`${CUSTOM_DATA_EXPORT_PATH}/`)).toBe(
      CUSTOM_DATA_EXPORT_PATH,
    );
    expect(canonicalReportingUrl(`${REPORTS_PATH}/`)).toBe(
      CUSTOM_DATA_EXPORT_PATH,
    );
    expect(canonicalReportingUrl(`${LEGACY_CUSTOM_DATA_EXPORT_PATH}/`)).toBe(
      CUSTOM_DATA_EXPORT_PATH,
    );
  });

  it("keeps the query string and hash when normalizing", () => {
    expect(canonicalReportingUrl(`${CUSTOM_DATA_EXPORT_PATH}/?view=saved`)).toBe(
      `${CUSTOM_DATA_EXPORT_PATH}?view=saved`,
    );
    expect(canonicalReportingUrl(`${REPORTS_PATH}/?view=queue#top`)).toBe(
      `${CUSTOM_DATA_EXPORT_PATH}?view=queue#top`,
    );
  });

  it("leaves unrelated urls and empty input alone", () => {
    expect(canonicalReportingUrl("/reports/other")).toBe("/reports/other");
    expect(canonicalReportingUrl("")).toBe("");
    expect(canonicalReportingUrl(undefined)).toBe(undefined);
  });
});
