import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
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
 * Typing in a result that a reference laboratory reported: the date that
 * laboratory put on its own report belongs to the referral, and only the
 * electronic path used to record it. Without a field for it the External
 * Referrals report printed a blank report-date column for every manually
 * entered result.
 */
const baseRow = {
  id: "1",
  analysisId: "1",
  testName: "Glucose",
  resultValue: "5",
  resultType: "N",
};

const noop = () => {};

const renderPanel = (
  referredOut: boolean,
  onReferenceLabReportDateChange = noop,
  referenceLabReportDate = "",
) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ExpandedPanel
        row={{ ...baseRow, referredOut }}
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
        allowResultRejection={false}
        nceOpen={false}
        onNceOpenChange={noop}
        referralOrganizations={[]}
        referralReasons={[]}
        referralDraft={null}
        onReferralDraftChange={noop}
        referenceLabReportDate={referenceLabReportDate}
        onReferenceLabReportDateChange={onReferenceLabReportDateChange}
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

describe("ExpandedPanel reference lab report date", () => {
  it("offers the report date only on a referred test", () => {
    renderPanel(false);
    expect(
      screen.queryByTestId("referral-report-date-row-1-primary"),
    ).not.toBeInTheDocument();
  });

  it("shows the field with its stored value on a referred test", () => {
    renderPanel(true, noop, "18/09/2026");
    expect(
      screen.getByTestId("referral-report-date-row-1-primary"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Reference lab report date")).toHaveValue(
      "18/09/2026",
    );
  });

  it("reports what was typed so the save can carry it", () => {
    const onChange = vi.fn();
    renderPanel(true, onChange);

    fireEvent.change(screen.getByLabelText("Reference lab report date"), {
      target: { value: "19/09/2026" },
    });

    expect(onChange).toHaveBeenCalledWith("19/09/2026");
  });
});
