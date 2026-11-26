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
 * OGC-1200: a row resulted from a run (waiting in it, or held by its failed
 * control) takes no save from the worklist, so it offers no referral or
 * rejection form that could never be saved.
 */
const noop = () => {};

const renderPanel = (inRun: boolean) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ExpandedPanel
        row={{
          id: "1",
          analysisId: "1",
          testName: "Glucose",
          resultValue: "50",
          resultType: "N",
        }}
        domain="CLINICAL"
        editable={false}
        editing={false}
        inRun={inRun}
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
        allowResultRejection={true}
        nceOpen={false}
        onNceOpenChange={noop}
        referralOrganizations={[]}
        referralReasons={[]}
        referralDraft={null}
        onReferralDraftChange={noop}
        rejectReasons={[]}
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

describe("ExpandedPanel on a row resulted from a run", () => {
  it("offers no referral or rejection while the row is in a run", () => {
    renderPanel(true);
    expect(
      screen.queryByTestId("referral-toggle-1-primary"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("reject-toggle-1-primary"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("nce-toggle-1-primary")).toBeInTheDocument();
  });

  it("offers referral and rejection on a row outside any run", () => {
    renderPanel(false);
    expect(screen.getByTestId("referral-toggle-1-primary")).toBeInTheDocument();
    expect(screen.getByTestId("reject-toggle-1-primary")).toBeInTheDocument();
  });
});
