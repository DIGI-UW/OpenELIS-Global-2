import React from "react";
import { render, screen, within } from "@testing-library/react";
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
 * The legacy Results screen tells a client sample from a QC sample in its
 * Sample Kind column. The expanded row of the unified screen says the same
 * thing, from the same field the worklist already carries.
 */
const baseRow = {
  id: "1",
  analysisId: "1",
  testName: "Glucose",
  resultValue: "5",
  resultType: "N",
};

const noop = () => {};

const renderPanel = (row: Record<string, unknown>) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ExpandedPanel
        row={row}
        domain="CLINICAL"
        editable={true}
        editing={false}
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

describe("ExpandedPanel sample kind", () => {
  it("names a client sample", () => {
    renderPanel(baseRow);
    const kind = screen.getByTestId("sample-kind-1-primary");
    expect(within(kind).getByText("Sample Kind")).toBeInTheDocument();
    expect(within(kind).getByText("Client sample")).toBeInTheDocument();
    expect(within(kind).queryByText("QC")).not.toBeInTheDocument();
  });

  it("names a QC sample and its kind", () => {
    renderPanel({ ...baseRow, qcType: "CONTROL" });
    const kind = screen.getByTestId("sample-kind-1-primary");
    expect(within(kind).getByText("QC")).toBeInTheDocument();
    expect(within(kind).getByText("CONTROL")).toBeInTheDocument();
    expect(within(kind).queryByText("Client sample")).not.toBeInTheDocument();
  });
});
