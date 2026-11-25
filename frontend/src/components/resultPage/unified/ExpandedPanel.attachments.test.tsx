import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServer: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));
vi.mock("../../nonconform/common/InlineNceForm", () => ({
  default: () => <div data-testid="inline-nce-form" />,
}));

import ExpandedPanel from "./ExpandedPanel";
import { getFromOpenElisServer } from "../../utils/Utils";

const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

const noop = vi.fn();

const baseRow = {
  id: "1",
  accessionNumber: "DEV012600000000000128",
  analysisId: "58",
  testName: "Anti-Pan Keratin",
  resultValue: "Positive",
  resultType: "D",
};

const renderPanel = (
  row: Record<string, unknown>,
  attachmentsEditable?: boolean,
) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ExpandedPanel
        row={row}
        domain="CLINICAL"
        editable={false}
        attachmentsEditable={attachmentsEditable}
        editing={false}
        methods={[]}
        analyzers={[]}
        noteDraft={{ text: "", visibility: "I" }}
        dilutionDraft={{ measuredValue: "", factor: "" }}
        sectionLayout={{ attachments: true }}
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

describe("ExpandedPanel attachment deletion gating", () => {
  beforeEach(() => {
    getMock.mockReset();
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (
        typeof url === "string" &&
        url.includes("/attachments") &&
        typeof cb === "function"
      ) {
        cb([
          {
            id: 1,
            fileName: "stain.png",
            fileSizeBytes: 1024,
            analysisId: "58",
          },
        ]);
      }
    });
  });

  it("hides attachment delete and upload when the analysis is Finalized (status 6)", () => {
    renderPanel({ ...baseRow, analysisStatusId: "6" });
    expect(screen.getByText("stain.png")).toBeInTheDocument();
    expect(screen.queryByTestId("attachment-delete-1")).toBeNull();
    expect(screen.queryByTestId("attachment-upload")).toBeNull();
  });

  it("allows attachment delete and upload on non-finalized saved rows", () => {
    renderPanel({ ...baseRow, analysisStatusId: "15" });
    expect(screen.getByText("stain.png")).toBeInTheDocument();
    expect(screen.getByTestId("attachment-delete-1")).toBeInTheDocument();
    expect(screen.getByTestId("attachment-upload")).toBeInTheDocument();
  });

  it("respects attachmentsEditable=false for custom finalized status IDs", () => {
    renderPanel({ ...baseRow, analysisStatusId: "99" }, false);
    expect(screen.getByText("stain.png")).toBeInTheDocument();
    expect(screen.queryByTestId("attachment-delete-1")).toBeNull();
    expect(screen.queryByTestId("attachment-upload")).toBeNull();
  });
});
