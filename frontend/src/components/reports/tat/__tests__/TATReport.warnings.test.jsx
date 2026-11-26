import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import TATFilterBar from "../TATFilterBar";
import TATExport from "../TATExport";
import TATTrendsTab from "../TATTrendsTab";

vi.mock("../../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn((url, callback) => callback([])),
}));

/**
 * React reports a warning once per run, so the TAT report's controls render
 * here in a file of their own: neither the filter bar nor the export menu
 * passes a prop its Carbon component rejects, and neither does the trends tab.
 */
describe("TAT report controls", () => {
  it("render without React prop warnings", () => {
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    render(
      <IntlProvider locale="en" messages={messages}>
        <TATFilterBar onGenerate={vi.fn()} />
        <TATExport filters={{}} buildQueryString={() => ""} />
        <TATTrendsTab filters={null} buildQueryString={() => ""} />
      </IntlProvider>,
    );

    expect(errors.mock.calls.map((call) => String(call[0]))).toEqual([]);
    errors.mockRestore();
  });
});
