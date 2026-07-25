import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, test, vi } from "vitest";
import SetupVerificationPanel from "./SetupVerificationPanel";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";

vi.mock("../../../services/analyzerService", () => ({
  getSetupVerification: vi.fn(),
  verifyAnalyzerSetup: vi.fn(),
}));

const renderPanel = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SetupVerificationPanel analyzerId="2013" />
    </IntlProvider>,
  );

describe("SetupVerificationPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("confirms the exact current mapping and QC records", async () => {
    const unverified = {
      verificationState: "UNVERIFIED",
      mappingReady: true,
      qcApplicable: true,
      qcReady: true,
      currentlyVerified: false,
      readyForActivation: false,
      mappingIds: ["RESULT:MTB:DETECTED", "TEST:MTB"],
      qcIds: ["LOT:lot-1", "RULE:rule-1"],
      mappingFingerprint: "mapping-fingerprint-1",
      qcFingerprint: "qc-fingerprint-1",
      blockers: ["SETUP_NOT_VERIFIED"],
    };
    analyzerService.getSetupVerification.mockImplementation(
      (analyzerId, callback) => callback(unverified),
    );
    analyzerService.verifyAnalyzerSetup.mockImplementation(
      (analyzerId, payload, callback) =>
        callback({
          ...unverified,
          verificationState: "CURRENT",
          currentlyVerified: true,
          readyForActivation: true,
          blockers: [],
          verifiedBy: "77",
          verifiedAt: "2026-07-24T12:00:00Z",
        }),
    );

    renderPanel();
    await userEvent.click(
      await screen.findByRole("button", { name: "Verify current setup" }),
    );

    await waitFor(() => {
      expect(analyzerService.verifyAnalyzerSetup).toHaveBeenCalledWith(
        "2013",
        {
          mappingIds: ["RESULT:MTB:DETECTED", "TEST:MTB"],
          qcIds: ["LOT:lot-1", "RULE:rule-1"],
          mappingFingerprint: "mapping-fingerprint-1",
          qcFingerprint: "qc-fingerprint-1",
        },
        expect.any(Function),
      );
    });
    expect(await screen.findByText("Currently verified")).toBeInTheDocument();
    expect(screen.getByText(/Verified by 77/)).toBeInTheDocument();
  });

  test("explains stale verification and links to the QC setup surfaces", async () => {
    analyzerService.getSetupVerification.mockImplementation(
      (analyzerId, callback) =>
        callback({
          verificationState: "STALE",
          mappingReady: true,
          qcApplicable: true,
          qcReady: false,
          currentlyVerified: false,
          readyForActivation: false,
          mappingIds: ["TEST:MTB"],
          qcIds: [],
          blockers: ["QC_CHANGED", "NO_ACTIVE_CONTROL_LOT"],
          verifiedBy: "77",
          verifiedAt: "2026-07-24T12:00:00Z",
        }),
    );

    renderPanel();

    expect(await screen.findByText("QC setup changed")).toBeInTheDocument();
    expect(
      screen.getByText("An active control lot is required."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Manage QC rules" }),
    ).toHaveAttribute("href", "/analyzers/2013/qc-rules");
    expect(
      screen.getByRole("link", { name: "Add or select control lot" }),
    ).toHaveAttribute("href", "/analyzers/qc/control-lots/new?analyzerId=2013");
    expect(document.querySelector(".setup-verification-actions")).toHaveClass(
      "cds--stack-horizontal",
      "cds--stack-scale-4",
    );
    expect(
      screen.getByRole("button", { name: "Verify current setup" }),
    ).toBeDisabled();
  });
});
