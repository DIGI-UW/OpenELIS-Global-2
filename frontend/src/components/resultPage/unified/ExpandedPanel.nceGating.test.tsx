import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServer: vi.fn(),
}));
vi.mock("../../nonconform/common/InlineNceForm", () => ({
  default: () => <div data-testid="inline-nce-form" />,
}));

// eslint-disable-next-line import/first
import ExpandedPanel from "./ExpandedPanel";

/**
 * "Allow Result Rejection" governs the Reject Result action and nothing else.
 * Reporting a non-conformity stays available either way, as it always has on
 * the legacy Results page, where the configuration only adds or removes the
 * reject column.
 */
const row = {
  id: "1",
  analysisId: "1",
  testName: "Glucose",
  resultValue: "5",
  resultType: "N",
};

const noop = () => {};

const renderPanel = (allowResultRejection: boolean) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ExpandedPanel
        row={row}
        domain="CLINICAL"
        editable={true}
        editing={true}
        methods={[]}
        analyzers={[]}
        noteDraft={{ text: "", visibility: "I" }}
        dilutionDraft={{ measuredValue: "", factor: "" }}
        sectionLayout={{}}
        onSectionLayoutChange={noop}
        onFieldChange={noop}
        onValueChange={noop}
        onNoteDraftChange={noop}
        onDilutionDraftChange={noop}
        actions={null}
        allowResultRejection={allowResultRejection}
        nceOpen={false}
        onNceOpenChange={noop}
        referralOrganizations={[]}
        referralReasons={[]}
        referralDraft={null}
        onReferralDraftChange={noop}
        rejectReasons={[{ id: "1", value: "Insufficient sample" }]}
        rejectDraft={null}
        onRejectDraftChange={noop}
        interpretationDraft={null}
        onInterpretationDraftChange={noop}
        nceDisposition="NONE"
        onNceDispositionChange={noop}
        nceRejectReasonId=""
        onNceRejectReasonChange={noop}
        onNceApplyDisposition={noop}
      />
    </IntlProvider>,
  );

describe("ExpandedPanel non-conformity gating", () => {
  it("offers Report Non-Conformity when result rejection is disabled", () => {
    renderPanel(false);
    expect(screen.getByTestId("nce-toggle-1-primary")).toBeInTheDocument();
    expect(
      screen.queryByTestId("reject-toggle-1-primary"),
    ).not.toBeInTheDocument();
  });

  it("offers both actions when result rejection is enabled", () => {
    renderPanel(true);
    expect(screen.getByTestId("nce-toggle-1-primary")).toBeInTheDocument();
    expect(screen.getByTestId("reject-toggle-1-primary")).toBeInTheDocument();
  });
});
