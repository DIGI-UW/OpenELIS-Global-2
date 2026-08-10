import React from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PolymorphicResultCell, {
  ResultCellRow,
  worklistRowKey,
} from "./PolymorphicResultCell";
import {
  AttachmentsSection,
  OrderInfoSection,
  ProgrammeSection,
  StorageSection,
  useOrderContext,
} from "./orderContextSections";
import CriticalBanner from "./CriticalBanner";
import HistorySection from "./HistorySection";
import InterpretationSection from "./InterpretationSection";
import ReagentsQcSection from "./ReagentsQcSection";
import AliquotsSection from "./AliquotsSection";
import ReferralAction, {
  ReferralDraft,
  emptyReferralDraft,
} from "./ReferralAction";
// the shipped inline non-conformity form (FR-E1) — plain jsx, no types
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import { FlagChip, accentClass } from "./flags";
import { NceDisposition } from "./nceDisposition";
import { ResultsDomain, formatDomainMessage } from "./domainIntl";
import { dilutionApplies, computeReportedValue } from "./dilution";
import {
  SectionLayout,
  isSectionOpen,
  rememberSectionChoice,
  resetSectionLayout,
} from "./sectionLayout";

/**
 * OGC-1021 (R2 of OGC-811) — the expanded row panel.
 *
 * Work zone on top, always open (FR-C1): result value + Method + Analyzer
 * (FR-B1/B2) + dilution factor (FR-D5) + dual-axis notes (FR-J1/J2) + the row
 * actions. Below it the reference zone: collapsed-but-summarized sections
 * (FR-C3) that render only when they have content (FR-C5), with the layout
 * remembered per user and a Reset control (FR-C4). The context strip is one
 * compact line with no decorative leading icon (FR-C2, D19).
 */

export interface IdValue {
  id: string;
  value: string;
}

export interface AnalysisNote {
  text?: string;
  noteType?: string;
  subject?: string;
  author?: string;
  date?: string;
}

export interface PanelRow extends ResultCellRow {
  accessionNumber?: string;
  testName?: string;
  patientName?: string;
  patientInfo?: string;
  sampleType?: string;
  normalRange?: string;
  testDate?: string;
  receivedDate?: string;
  technician?: string;
  testMethod?: string;
  analyzerId?: string;
  referredOut?: boolean;
  analysisNotes?: AnalysisNote[];
  /** OGC-1022 (R3): NORMAL | ABNORMAL | CRITICAL | INVALID, computed server-side. */
  resultFlag?: string;
  /** OGC-1022 (R3): display string for the authored critical bounds. */
  criticalRange?: string;
  [key: string]: unknown;
}

export interface NoteDraft {
  text: string;
  visibility: "I" | "E";
}

export interface DilutionDraft {
  measuredValue: string;
  factor: string;
}

export interface RejectDraft {
  rejectReasonId: string;
}

/** dd/MM/yyyy — the app's date format; FR-F2's "defaults to now". */
export const todayForReferral = (): string => {
  const now = new Date();
  const dd = String(now.getDate()).padStart(2, "0");
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  return `${dd}/${mm}/${now.getFullYear()}`;
};

interface ExpandedPanelProps {
  row: PanelRow;
  domain: ResultsDomain;
  editable: boolean;
  editing: boolean;
  /** analyzerId as loaded from the server — drives the provenance tag (FR-B2). */
  loadedAnalyzerId?: string;
  methods: IdValue[];
  analyzers: IdValue[];
  noteDraft: NoteDraft;
  dilutionDraft: DilutionDraft;
  sectionLayout: SectionLayout;
  onSectionLayoutChange: (layout: SectionLayout) => void;
  onFieldChange: (field: "testMethod" | "analyzerId", value: string) => void;
  onValueChange: (
    field: "resultValue" | "multiSelectResultValues",
    value: string,
  ) => void;
  onNoteDraftChange: (draft: NoteDraft) => void;
  onDilutionDraftChange: (draft: DilutionDraft) => void;
  actions: React.ReactNode;
  /** OGC-1023 (R4): gates "Report Non-Conformity" and result rejection. */
  allowResultRejection: boolean;
  nceOpen: boolean;
  onNceOpenChange: (open: boolean) => void;
  referralOrganizations: IdValue[];
  referralReasons: IdValue[];
  referralDraft: ReferralDraft | null;
  onReferralDraftChange: (draft: ReferralDraft | null) => void;
  rejectReasons: IdValue[];
  rejectDraft: RejectDraft | null;
  onRejectDraftChange: (draft: RejectDraft | null) => void;
  interpretationDraft: string | null;
  onInterpretationDraftChange: (draft: string | null) => void;
  nceDisposition: NceDisposition;
  onNceDispositionChange: (disposition: NceDisposition) => void;
  nceRejectReasonId: string;
  onNceRejectReasonChange: (reasonId: string) => void;
  onNceApplyDisposition: (
    disposition: NceDisposition,
    rejectReasonId: string,
  ) => void;
}

const noteVisibilityTag = (noteType?: string) =>
  noteType === "E" ? (
    <Tag type="teal" size="sm">
      <FormattedMessage id="label.results.note.external" />
    </Tag>
  ) : (
    <Tag type="gray" size="sm">
      <FormattedMessage id="label.results.note.internal" />
    </Tag>
  );

const noteContextTag = (subject?: string) =>
  subject && subject.includes("Modification") ? (
    <Tag type="warm-gray" size="sm">
      <FormattedMessage id="label.results.note.context.modification" />
    </Tag>
  ) : (
    <Tag type="blue" size="sm">
      <FormattedMessage id="label.results.note.context.entry" />
    </Tag>
  );

const ExpandedPanel: React.FC<ExpandedPanelProps> = ({
  row,
  domain,
  editable,
  editing,
  loadedAnalyzerId,
  methods,
  analyzers,
  noteDraft,
  dilutionDraft,
  sectionLayout,
  onSectionLayoutChange,
  onFieldChange,
  onValueChange,
  onNoteDraftChange,
  onDilutionDraftChange,
  actions,
  allowResultRejection,
  nceOpen,
  onNceOpenChange,
  referralOrganizations,
  referralReasons,
  referralDraft,
  onReferralDraftChange,
  rejectReasons,
  rejectDraft,
  onRejectDraftChange,
  interpretationDraft,
  onInterpretationDraftChange,
  nceDisposition,
  onNceDispositionChange,
  nceRejectReasonId,
  onNceRejectReasonChange,
  onNceApplyDisposition,
}) => {
  const intl = useIntl();
  const rowKey = worklistRowKey(row);

  const methodName =
    methods.find((m) => m.id === row.testMethod)?.value || row.testMethod || "";
  const analyzerName =
    analyzers.find((a) => a.id === row.analyzerId)?.value ||
    row.analyzerId ||
    "";

  const reported = computeReportedValue(
    dilutionDraft.measuredValue,
    dilutionDraft.factor,
  );

  const applyDilution = (draft: DilutionDraft) => {
    onDilutionDraftChange(draft);
    const computed = computeReportedValue(draft.measuredValue, draft.factor);
    if (computed !== null) {
      onValueChange("resultValue", computed);
    }
  };

  // one order-record fetch per accession feeds Order info + Programme
  const orderContext = useOrderContext(row.accessionNumber);
  const notes = row.analysisNotes || [];
  const latestInterpretation = [...notes]
    .reverse()
    .find((note) => note.subject === "Interpretation")?.text;

  const toggleSection = (sectionId: string, open: boolean) =>
    onSectionLayoutChange(rememberSectionChoice(sectionId, open));

  return (
    <div className="unifiedExpandedPanel" data-testid={`panel-${rowKey}`}>
      {/* Context strip (FR-C2) — one compact line, no decorative icon */}
      <div className="unifiedContextStrip">
        {domain === "CLINICAL" ? (
          <>
            <span className="unifiedContextPrimary">
              {row.patientName || row.accessionNumber}
            </span>
            {row.patientInfo && <span>{row.patientInfo}</span>}
          </>
        ) : (
          <>
            <span className="unifiedContextPrimary">{row.accessionNumber}</span>
            {row.sampleType && <span>{row.sampleType}</span>}
          </>
        )}
        <span>{row.testName}</span>
        {row.referredOut && (
          <Tag type="cyan" size="sm">
            <FormattedMessage id="label.results.referredOut" />
          </Tag>
        )}
      </div>

      {/* WORK ZONE (FR-C1) — always open */}
      <div className="unifiedWorkZone">
        <div className="unifiedWorkZoneGrid">
          <div>
            <div className="cds--label">
              <FormattedMessage id="label.results.result" />
            </div>
            <div
              className={`unifiedWorkZoneValue ${accentClass(row.resultFlag)}`}
            >
              <PolymorphicResultCell
                row={row}
                editable={editable}
                onValueChange={onValueChange}
              />
              {row.unitsOfMeasure && <span>{row.unitsOfMeasure}</span>}
              <FlagChip flag={row.resultFlag} />
            </div>
            {row.normalRange && (
              <div className="unifiedWorkZoneRange">
                {formatDomainMessage(intl, "label.results.range", domain)}:{" "}
                {row.normalRange} {row.unitsOfMeasure || ""}
                {row.criticalRange && (
                  <span className="unifiedCriticalRange">
                    {" "}
                    <FormattedMessage id="label.results.critical.range" />:{" "}
                    {row.criticalRange}
                  </span>
                )}
              </div>
            )}
          </div>

          <div>
            <div className="cds--label">
              <FormattedMessage id="label.results.method" />
              {loadedAnalyzerId && (
                <Tag type="cool-gray" size="sm" className="unifiedProvenance">
                  <FormattedMessage id="label.results.fromAnalyzer" />
                </Tag>
              )}
            </div>
            {editable ? (
              <Select
                id={`method-${rowKey}`}
                noLabel
                value={row.testMethod || ""}
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                  onFieldChange("testMethod", e.target.value)
                }
              >
                <SelectItem text="" value="" />
                {methods.map((m) => (
                  <SelectItem text={m.value} value={m.id} key={m.id} />
                ))}
              </Select>
            ) : (
              <span>{methodName || "—"}</span>
            )}

            <div className="cds--label unifiedFieldSpacer">
              <FormattedMessage id="label.results.analyzer" />
            </div>
            {editable ? (
              <Select
                id={`analyzer-${rowKey}`}
                noLabel
                value={row.analyzerId || ""}
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                  onFieldChange("analyzerId", e.target.value)
                }
              >
                <SelectItem
                  text={intl.formatMessage({
                    id: "label.results.analyzer.none",
                  })}
                  value=""
                />
                {analyzers.map((a) => (
                  <SelectItem text={a.value} value={a.id} key={a.id} />
                ))}
              </Select>
            ) : (
              <span>
                {analyzerName || (
                  <FormattedMessage id="label.results.analyzer.none" />
                )}
              </span>
            )}
            <div className="unifiedFieldHint">
              <FormattedMessage id="label.results.methodAnalyzerHint" />
            </div>
          </div>
        </div>

        {/* Dual-axis notes (FR-J1/J2) */}
        <div className="unifiedNotes">
          <div className="cds--label">
            <FormattedMessage id="label.results.notes" /> ({notes.length})
          </div>
          {notes.map((note, index) => (
            <div className="unifiedNoteItem" key={index}>
              <div className="unifiedNoteMeta">
                <span>{note.date}</span>
                <span>{note.author}</span>
                {noteContextTag(note.subject)}
                {noteVisibilityTag(note.noteType)}
              </div>
              <div>{note.text}</div>
            </div>
          ))}
          {editable && (
            <div className="unifiedNoteComposer">
              <div className="unifiedNoteMeta">
                <FormattedMessage id="label.results.note.context" />:{" "}
                {editing ? (
                  <Tag type="warm-gray" size="sm">
                    <FormattedMessage id="label.results.note.context.modification" />
                  </Tag>
                ) : (
                  <Tag type="blue" size="sm">
                    <FormattedMessage id="label.results.note.context.entry" />
                  </Tag>
                )}
                <span className="unifiedFieldHint">
                  <FormattedMessage id="label.results.note.context.auto" />
                </span>
              </div>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "label.results.note.visibility",
                })}
                name={`note-visibility-${rowKey}`}
                valueSelected={noteDraft.visibility}
                onChange={(value: string) =>
                  onNoteDraftChange({
                    ...noteDraft,
                    visibility: value === "E" ? "E" : "I",
                  })
                }
              >
                <RadioButton
                  id={`note-internal-${rowKey}`}
                  labelText={intl.formatMessage({
                    id: "label.results.note.internal",
                  })}
                  value="I"
                />
                <RadioButton
                  id={`note-external-${rowKey}`}
                  labelText={intl.formatMessage({
                    id: "label.results.note.external",
                  })}
                  value="E"
                />
              </RadioButtonGroup>
              <TextArea
                id={`note-text-${rowKey}`}
                labelText=""
                rows={2}
                placeholder={intl.formatMessage({
                  id: "label.results.note.placeholder",
                })}
                value={noteDraft.text}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                  onNoteDraftChange({ ...noteDraft, text: e.target.value })
                }
              />
              {noteDraft.visibility === "E" && (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage({
                    id: "label.results.note.externalWarning",
                  })}
                />
              )}
            </div>
          )}
        </div>

        <div className="unifiedWorkZoneActions">
          {actions}
          {allowResultRejection && (
            <Button
              kind="tertiary"
              size="sm"
              className="unifiedNceButton"
              onClick={() => onNceOpenChange(!nceOpen)}
              data-testid={`nce-toggle-${rowKey}`}
            >
              <FormattedMessage id="label.results.nce.report" />
            </Button>
          )}
          {allowResultRejection && (
            <Button
              kind="ghost"
              size="sm"
              onClick={() =>
                onRejectDraftChange(rejectDraft ? null : { rejectReasonId: "" })
              }
              data-testid={`reject-toggle-${rowKey}`}
            >
              <FormattedMessage id="label.results.reject.result" />
            </Button>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() =>
              onReferralDraftChange(
                referralDraft ? null : emptyReferralDraft(todayForReferral()),
              )
            }
            data-testid={`referral-toggle-${rowKey}`}
          >
            {row.referredOut || referralDraft ? (
              <FormattedMessage id="label.results.referral.editing" />
            ) : (
              <FormattedMessage id="label.results.referral.refer" />
            )}
          </Button>
          <span className="unifiedFieldHint">
            <FormattedMessage id="label.results.saveHint" />
          </span>
        </div>

        {allowResultRejection && rejectReasons.length > 0 && rejectDraft && (
          <div className="unifiedRejectRow" data-testid={`reject-${rowKey}`}>
            <Select
              id={`reject-reason-${rowKey}`}
              labelText={intl.formatMessage({
                id: "label.results.reject.reason",
              })}
              value={rejectDraft.rejectReasonId}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                onRejectDraftChange({ rejectReasonId: e.target.value })
              }
            >
              <SelectItem value="" text="" />
              {rejectReasons.map((reason) => (
                <SelectItem
                  key={reason.id}
                  value={reason.id}
                  text={reason.value}
                />
              ))}
            </Select>
            <span className="unifiedFieldHint">
              <FormattedMessage id="label.results.reject.hint" />
            </span>
            <Button
              kind="ghost"
              size="sm"
              onClick={() => onRejectDraftChange(null)}
            >
              <FormattedMessage id="label.results.referral.cancel" />
            </Button>
          </div>
        )}

        {referralDraft && (
          <ReferralAction
            rowKey={rowKey}
            organizations={referralOrganizations}
            reasons={referralReasons}
            draft={referralDraft}
            onDraftChange={(draft) => onReferralDraftChange(draft)}
            onCancel={() => onReferralDraftChange(null)}
          />
        )}
      </div>

      {/* Inline NCE (FR-E1/E2) — the shipped form, embedded, auto-linked to
          this sample + result; gated by allowResultRejection */}
      {nceOpen && (
        <div className="unifiedNceEmbed" data-testid={`nce-${rowKey}`}>
          <InlineNceForm
            resultRow={row}
            onClose={() => onNceOpenChange(false)}
            onSubmitSuccess={() => {
              onNceApplyDisposition(nceDisposition, nceRejectReasonId);
              onNceOpenChange(false);
            }}
          />
          {/* FR-E3 — result disposition applied when the NCE is submitted;
              refer-out is a separate row action, never a disposition */}
          <div
            className="unifiedDisposition"
            data-testid={`disposition-${rowKey}`}
          >
            <span className="cds--label">
              <FormattedMessage id="label.results.nce.disposition" />
            </span>
            <div className="unifiedDispositionTiles">
              {(
                [
                  ["NONE", "label.results.nce.disposition.none"],
                  ["CANCEL", "label.results.nce.disposition.cancel"],
                  ["REJECT", "label.results.nce.disposition.reject"],
                  ["RETEST", "label.results.nce.disposition.retest"],
                ] as [NceDisposition, string][]
              ).map(([value, labelKey]) => (
                <button
                  type="button"
                  key={value}
                  className={`unifiedDispositionTile${
                    nceDisposition === value
                      ? " unifiedDispositionTile--selected"
                      : ""
                  }`}
                  onClick={() => onNceDispositionChange(value)}
                  data-testid={`disposition-${value}`}
                >
                  <strong>
                    <FormattedMessage id={labelKey} />
                  </strong>
                  <span className="unifiedBucketText">
                    <FormattedMessage id={`${labelKey}.detail`} />
                  </span>
                </button>
              ))}
            </div>
            {nceDisposition === "REJECT" && (
              <Select
                id={`nce-reject-reason-${rowKey}`}
                labelText={intl.formatMessage({
                  id: "label.results.reject.reason",
                })}
                value={nceRejectReasonId}
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                  onNceRejectReasonChange(e.target.value)
                }
              >
                <SelectItem value="" text="" />
                {rejectReasons.map((reason) => (
                  <SelectItem
                    key={reason.id}
                    value={reason.id}
                    text={reason.value}
                  />
                ))}
              </Select>
            )}
            <span className="unifiedFieldHint">
              <FormattedMessage id="label.results.nce.disposition.hint" />
            </span>
          </div>
        </div>
      )}

      {/* Critical banner (FR-C2) — the one full-width banner; ack never gates Save (FR-A4) */}
      {row.resultFlag === "CRITICAL" && (
        <CriticalBanner
          analysisId={row.analysisId as string | undefined}
          criticalRange={row.criticalRange}
        />
      )}

      {/* CONSUMABLES & QUALITY (R5/R6 — OGC-1024/OGC-1025) */}
      <ReagentsQcSection
        testId={row.testId as string | undefined}
        analysisId={row.analysisId as string | undefined}
        editable={editable}
        fromAnalyzerId={loadedAnalyzerId}
        analyzerName={
          analyzers.find((a) => a.id === loadedAnalyzerId)?.value as
            | string
            | undefined
        }
        open={isSectionOpen(sectionLayout, "combo", true)}
        onToggle={(open) => toggleSection("combo", open)}
        dilution={
          editable && dilutionApplies(row.resultType) ? (
            <div className="unifiedDilution">
              <TextInput
                id={`dilution-measured-${rowKey}`}
                labelText={intl.formatMessage({
                  id: "label.results.dilution.measured",
                })}
                type="number"
                value={dilutionDraft.measuredValue}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  applyDilution({
                    ...dilutionDraft,
                    measuredValue: e.target.value,
                  })
                }
              />
              <TextInput
                id={`dilution-factor-${rowKey}`}
                labelText={intl.formatMessage({
                  id: "label.results.dilution.factor",
                })}
                type="number"
                value={dilutionDraft.factor}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  applyDilution({ ...dilutionDraft, factor: e.target.value })
                }
              />
              {reported !== null && (
                <span className="unifiedDilutionComputed">
                  <FormattedMessage
                    id="label.results.dilution.reported"
                    values={{ 0: reported }}
                  />
                </span>
              )}
            </div>
          ) : undefined
        }
      />

      {/* REFERENCE ZONE (FR-C3/C4/C5) */}
      <div className="unifiedRefZone">
        <div className="unifiedRefZoneHeader">
          <span className="unifiedRefZoneLabel">
            <FormattedMessage id="label.results.referenceZone" />
          </span>
          {Object.keys(sectionLayout).length > 0 && (
            <Tag type="blue" size="sm">
              <FormattedMessage id="label.results.layoutRemembered" />
            </Tag>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() => onSectionLayoutChange(resetSectionLayout())}
          >
            <FormattedMessage id="label.results.resetLayout" />
          </Button>
        </div>

        <OrderInfoSection
          open={isSectionOpen(sectionLayout, "orderInfo", false)}
          onToggle={(open) => toggleSection("orderInfo", open)}
          order={orderContext}
          sampleType={row.sampleType}
          receivedDate={row.receivedDate}
          technician={row.technician}
        />

        <AliquotsSection
          accessionNumber={row.accessionNumber}
          sampleItemId={row.sampleItemId as string | undefined}
          open={isSectionOpen(sectionLayout, "aliquots", false)}
          onToggle={(open) => toggleSection("aliquots", open)}
        />

        {domain === "CLINICAL" && (
          <InterpretationSection
            testId={row.testId as string | undefined}
            componentId={row.testResultComponentId as string | undefined}
            resultValue={row.resultValue as string | undefined}
            latestInterpretation={latestInterpretation}
            draft={interpretationDraft}
            onDraftChange={onInterpretationDraftChange}
            editable={editable}
            openOverride={sectionLayout["interpretation"]}
            onToggle={(open) => toggleSection("interpretation", open)}
          />
        )}

        <ProgrammeSection
          open={isSectionOpen(sectionLayout, "program", false)}
          onToggle={(open) => toggleSection("program", open)}
          order={orderContext}
          eqaSample={Boolean(row.eqaSample)}
          eqaPriority={row.eqaPriority as string | undefined}
        />

        <StorageSection
          open={isSectionOpen(sectionLayout, "storage", false)}
          onToggle={(open) => toggleSection("storage", open)}
          sampleItemId={row.sampleItemId as string | undefined}
          editable={editable}
        />

        <AttachmentsSection
          open={isSectionOpen(sectionLayout, "attachments", false)}
          onToggle={(open) => toggleSection("attachments", open)}
          accessionNumber={row.accessionNumber}
          editable={editable}
          legacyResultFile={
            row.resultFile as
              | import("./orderContextSections").LegacyResultFile
              | undefined
          }
        />

        <HistorySection
          analysisId={row.analysisId as string | undefined}
          open={isSectionOpen(sectionLayout, "history", false)}
          onToggle={(open) => toggleSection("history", open)}
        />
      </div>
    </div>
  );
};

export default ExpandedPanel;
