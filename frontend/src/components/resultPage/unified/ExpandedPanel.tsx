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
import ReferenceSection from "./ReferenceSection";
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

  const orderInfoAvailable = Boolean(
    row.testDate || row.receivedDate || row.sampleType || row.technician,
  );
  const notes = row.analysisNotes || [];

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
            <div className="unifiedWorkZoneValue">
              <PolymorphicResultCell
                row={row}
                editable={editable}
                onValueChange={onValueChange}
              />
              {row.unitsOfMeasure && <span>{row.unitsOfMeasure}</span>}
            </div>
            {row.normalRange && (
              <div className="unifiedWorkZoneRange">
                {formatDomainMessage(intl, "label.results.range", domain)}:{" "}
                {row.normalRange} {row.unitsOfMeasure || ""}
              </div>
            )}
            {editable && dilutionApplies(row.resultType) && (
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
          <span className="unifiedFieldHint">
            <FormattedMessage id="label.results.saveHint" />
          </span>
        </div>
      </div>

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

        {orderInfoAvailable && (
          <ReferenceSection
            sectionId="orderInfo"
            title={<FormattedMessage id="label.results.section.orderInfo" />}
            summary={[row.sampleType, row.receivedDate]
              .filter(Boolean)
              .join(" · ")}
            open={isSectionOpen(sectionLayout, "orderInfo", false)}
            onToggle={(open) => toggleSection("orderInfo", open)}
          >
            <div className="unifiedRefGrid">
              {row.sampleType && (
                <div>
                  <span className="cds--label">
                    <FormattedMessage id="label.results.sampleType" />
                  </span>
                  <span>{row.sampleType}</span>
                </div>
              )}
              {row.testDate && (
                <div>
                  <span className="cds--label">
                    <FormattedMessage id="label.results.testDate" />
                  </span>
                  <span>{row.testDate}</span>
                </div>
              )}
              {row.receivedDate && (
                <div>
                  <span className="cds--label">
                    <FormattedMessage id="label.results.receivedDate" />
                  </span>
                  <span>{row.receivedDate}</span>
                </div>
              )}
              {row.technician && (
                <div>
                  <span className="cds--label">
                    <FormattedMessage id="label.results.technician" />
                  </span>
                  <span>{row.technician}</span>
                </div>
              )}
            </div>
          </ReferenceSection>
        )}
      </div>
    </div>
  );
};

export default ExpandedPanel;
