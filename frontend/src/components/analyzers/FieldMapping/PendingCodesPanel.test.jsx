import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, test, vi } from "vitest";
import PendingCodesPanel from "./PendingCodesPanel";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";

vi.mock("../../../services/analyzerService", () => ({
  getTestMappingOptions: vi.fn(),
  resolvePendingCode: vi.fn(),
  updatePendingCodeStatus: vi.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("PendingCodesPanel", () => {
  test("requires a catalog test and resolves a real mapping before MAPPED", async () => {
    const onUpdated = vi.fn();
    analyzerService.getTestMappingOptions.mockImplementation(
      (analyzerId, callback) => {
        callback([
          {
            id: "501",
            name: "Xpert MTB/RIF",
            loinc: "38379-4",
          },
        ]);
      },
    );
    analyzerService.resolvePendingCode.mockImplementation(
      (analyzerId, pendingCodeId, openelisTestId, callback) => {
        callback({ ok: true, status: "MAPPED", openelisTestId });
      },
    );

    renderWithIntl(
      <PendingCodesPanel
        analyzerId="2013"
        onUpdated={onUpdated}
        pendingCodes={[
          {
            id: "pc-1",
            analyzerTestName: "MTB-RIF",
            seenCount: 2,
            status: "PENDING",
          },
        ]}
      />,
    );

    const testPicker = await screen.findByRole("combobox", {
      name: "OpenELIS test",
    });
    await userEvent.click(testPicker);
    await userEvent.click(await screen.findByText("Xpert MTB/RIF (38379-4)"));
    await userEvent.click(screen.getByTestId("pending-code-map-pc-1"));

    await waitFor(() => {
      expect(analyzerService.resolvePendingCode).toHaveBeenCalledWith(
        "2013",
        "pc-1",
        "501",
        expect.any(Function),
      );
      expect(onUpdated).toHaveBeenCalled();
    });
    expect(analyzerService.updatePendingCodeStatus).not.toHaveBeenCalledWith(
      "2013",
      "pc-1",
      "MAPPED",
      expect.any(Function),
    );
  });

  test("keeps a legacy status-only MAPPED row repairable", async () => {
    analyzerService.getTestMappingOptions.mockImplementation(
      (analyzerId, callback) => {
        callback([{ id: "501", name: "Xpert MTB/RIF", loinc: "38379-4" }]);
      },
    );

    renderWithIntl(
      <PendingCodesPanel
        analyzerId="2013"
        pendingCodes={[
          {
            id: "pc-legacy",
            analyzerTestName: "MTB-RIF",
            seenCount: 2,
            status: "MAPPED",
          },
        ]}
      />,
    );

    const testPicker = await screen.findByRole("combobox", {
      name: "OpenELIS test",
    });
    expect(testPicker).toBeEnabled();
    expect(screen.getByTestId("pending-code-map-pc-legacy")).toBeDisabled();

    await userEvent.click(testPicker);
    await userEvent.click(await screen.findByText("Xpert MTB/RIF (38379-4)"));

    expect(screen.getByTestId("pending-code-map-pc-legacy")).toBeEnabled();
  });
});
