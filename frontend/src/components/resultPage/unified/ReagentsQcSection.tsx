import React, { useCallback, useEffect, useState } from "react";
import { Button, Select, SelectItem, Tag, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ReferenceSection from "./ReferenceSection";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

/**
 * OGC-1024 (R5, D5/D11) — the "Reagents, QC & Controls" combo section:
 * reagent links come from the Test Catalog (new RESULTS-role read), lots ride
 * the shipped inventory FEFO endpoint, and consumption posts to the shipped
 * /rest/inventory/management/consume (FEFO across lots, analysis-linked).
 * Analyzer-loaded rows are read-only (capture mode: analyzer-reported).
 *
 * OGC-1025 — manual control-result capture, polymorphic by
 * test type: RDT rows record a control-line outcome (Valid/Invalid) with a
 * free-text kit lot; manual quantitative rows record measured/expected/
 * uncertainty + Pass/Fail against a bench control lot. POSTs to the OGC-1147
 * endpoint /rest/qc/results; a failing control auto-opens an NCE and holds
 * covered results at Validation server-side. Analyzer rows stay read-only
 * and link to the QC dashboard.
 */
interface ReagentLink {
  reagentId?: number;
  usageType?: string;
  quantityPerTest?: number | string;
  quantityUnit?: string;
  name?: string;
  units?: string;
}

interface InventoryLot {
  id?: number;
  lotNumber?: string;
  expirationDate?: string | number;
  currentQuantity?: number;
  qcStatus?: string;
  status?: string;
}

interface ControlLot {
  id?: string;
  productName?: string;
  lotNumber?: string;
  controlLevel?: string;
  status?: string;
}

interface ReagentsQcSectionProps {
  testId?: string;
  analysisId?: string;
  editable: boolean;
  /** analyzerId as loaded — analyzer-reported rows are read-only (FR-B2). */
  fromAnalyzerId?: string;
  analyzerName?: string;
  /** "N" = manual quantitative capture; anything else = RDT capture. */
  resultType?: string;
  /** Lab unit of the worklist — scopes the QC-fail signal. */
  testSectionId?: string;
  unitOfMeasure?: string;
  open: boolean;
  onToggle: (open: boolean) => void;
  /** the dilution inputs, relocated here from the work zone */
  dilution?: React.ReactNode;
}

interface QcCaptureDraft {
  outcome: string;
  kitLot: string;
  measured: string;
  expected: string;
  uncertainty: string;
  lotId: string;
}

const EMPTY_QC_DRAFT: QcCaptureDraft = {
  outcome: "",
  kitLot: "",
  measured: "",
  expected: "",
  uncertainty: "",
  lotId: "",
};

const lotExpiry = (value?: string | number): string => {
  if (value === undefined || value === null || value === "") {
    return "";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? String(value)
    : date.toLocaleDateString();
};

const ReagentsQcSection: React.FC<ReagentsQcSectionProps> = ({
  testId,
  analysisId,
  editable,
  fromAnalyzerId,
  analyzerName,
  resultType,
  testSectionId,
  unitOfMeasure,
  open,
  onToggle,
  dilution,
}) => {
  const intl = useIntl();
  const fromAnalyzer = Boolean(fromAnalyzerId);
  const [links, setLinks] = useState<ReagentLink[] | null>(null);
  const [lotsByItem, setLotsByItem] = useState<Record<string, InventoryLot[]>>(
    {},
  );
  const [amounts, setAmounts] = useState<Record<string, string>>({});
  const [controlLots, setControlLots] = useState<ControlLot[]>([]);
  const [busyItem, setBusyItem] = useState<string | null>(null);
  const [message, setMessage] = useState<{
    kind: "ok" | "error";
    text: string;
  } | null>(null);
  // ---- OGC-1025 control capture ----
  const quantitative = resultType === "N";
  // Deliberately NOT gated on `editable`: a control run is a QC record, not an
  // edit of this patient result, and the hold it raises covers results already
  // sitting at Technical Acceptance (±24 h). Gating on `editable` would mean a
  // tech who saves the result first can never record the control for that row.
  // Write authority is enforced server-side (RESULTS role on POST /rest/qc/results).
  const canCaptureControl =
    !fromAnalyzer && Boolean(testId) && Boolean(testSectionId);
  const [qcDraft, setQcDraft] = useState<QcCaptureDraft>(EMPTY_QC_DRAFT);
  const [qcBusy, setQcBusy] = useState(false);
  const [qcMessage, setQcMessage] = useState<{
    kind: "ok" | "error" | "blocked";
    text: string;
  } | null>(null);

  useEffect(() => {
    if (!testId) {
      setLinks([]);
      return;
    }
    getFromOpenElisServer(
      `/rest/results-entry/test/${testId}/reagents`,
      (body: ReagentLink[]) => setLinks(Array.isArray(body) ? body : []),
    );
  }, [testId]);

  const loadLots = useCallback((itemId: number) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/item/${itemId}/available`,
      (body: InventoryLot[]) =>
        setLotsByItem((prev) => ({
          ...prev,
          [String(itemId)]: Array.isArray(body) ? body : [],
        })),
    );
  }, []);

  useEffect(() => {
    if (!fromAnalyzer && editable) {
      (links || []).forEach((link) => {
        if (link.reagentId) {
          loadLots(link.reagentId);
        }
      });
    }
  }, [links, fromAnalyzer, editable, loadLots]);

  useEffect(() => {
    if (!testId || (!fromAnalyzerId && !quantitative)) {
      return;
    }
    // with an instrumentId: the analyzer's lots (read-only display); without:
    // active bench lots only — the manual capture's lot picker (the API
    // refuses analyzer lots on capture anyway). RDT capture needs no lot
    // record, so plain RDT rows skip the fetch.
    const query = fromAnalyzerId
      ? `testId=${testId}&instrumentId=${fromAnalyzerId}`
      : `testId=${testId}`;
    getFromOpenElisServer(
      `/rest/qc/controlLots?${query}`,
      (body: ControlLot[]) => setControlLots(Array.isArray(body) ? body : []),
    );
  }, [testId, fromAnalyzerId, quantitative]);

  const recordUse = (link: ReagentLink) => {
    if (!link.reagentId) {
      return;
    }
    const key = String(link.reagentId);
    const amount = amounts[key] ?? String(link.quantityPerTest ?? "");
    const quantity = Number(amount);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setMessage({
        kind: "error",
        text: intl.formatMessage({
          id: "label.results.reagents.amountInvalid",
        }),
      });
      return;
    }
    setBusyItem(key);
    setMessage(null);
    postToOpenElisServerJsonResponse(
      "/rest/inventory/management/consume",
      JSON.stringify({
        itemId: key,
        quantity,
        analysisId: analysisId || null,
      }),
      (response?: { message?: string; error?: string; status?: number }) => {
        setBusyItem(null);
        if (response && response.status && response.status >= 400) {
          setMessage({
            kind: "error",
            text:
              response.message ||
              response.error ||
              intl.formatMessage({
                id: "label.results.reagents.recordFailed",
              }),
          });
          return;
        }
        setMessage({
          kind: "ok",
          text: intl.formatMessage(
            { id: "label.results.reagents.recorded" },
            { 0: quantity, 1: link.units || link.quantityUnit || "" },
          ),
        });
        loadLots(link.reagentId as number);
      },
    );
  };

  // React 17 pools synthetic events — read the value before the updater runs
  const setQcField = (field: keyof QcCaptureDraft) => {
    return (
      e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
    ): void => {
      const value = e.target.value;
      setQcDraft((prev) => ({ ...prev, [field]: value }));
    };
  };

  const recordControl = () => {
    const failing = qcDraft.outcome === "INVALID" || qcDraft.outcome === "FAIL";
    let body: Record<string, unknown>;
    if (quantitative) {
      const measured = Number(qcDraft.measured);
      const expected = Number(qcDraft.expected);
      const uncertainty = Number(qcDraft.uncertainty);
      if (
        !qcDraft.outcome ||
        !qcDraft.lotId ||
        qcDraft.measured.trim() === "" ||
        qcDraft.expected.trim() === "" ||
        qcDraft.uncertainty.trim() === "" ||
        !Number.isFinite(measured) ||
        !Number.isFinite(expected) ||
        !Number.isFinite(uncertainty) ||
        uncertainty < 0
      ) {
        setQcMessage({
          kind: "error",
          text: intl.formatMessage({
            id: "label.results.control.capture.incomplete",
          }),
        });
        return;
      }
      body = {
        source: "MANUAL",
        qualitativeOutcome: qcDraft.outcome,
        resultValue: qcDraft.measured,
        // always tech-entered — prefill from a configured QC target is
        // OGC-1148; when that ships, seed these two fields from the target
        expectedValue: qcDraft.expected,
        uncertainty: qcDraft.uncertainty,
        controlLotId: qcDraft.lotId,
        testId,
        testSectionId,
        unitOfMeasure: unitOfMeasure || null,
      };
    } else {
      if (!qcDraft.outcome || qcDraft.kitLot.trim() === "") {
        setQcMessage({
          kind: "error",
          text: intl.formatMessage({
            id: "label.results.control.capture.incomplete",
          }),
        });
        return;
      }
      body = {
        source: "RDT",
        qualitativeOutcome: qcDraft.outcome,
        controlLabel: qcDraft.kitLot.trim(),
        testId,
        testSectionId,
      };
    }
    setQcBusy(true);
    setQcMessage(null);
    postToOpenElisServerJsonResponse(
      "/rest/qc/results",
      JSON.stringify(body),
      (response?: {
        id?: string;
        message?: string;
        error?: string;
        status?: number;
      }) => {
        setQcBusy(false);
        // A transport failure reaches this callback as {error, status: 0} — a
        // falsy status, so testing the status alone would report a control that
        // never reached the server as recorded. Only a body carrying the saved
        // row's id counts as success.
        if (!response || !response.id) {
          setQcMessage({
            kind: "error",
            text:
              response?.message ||
              response?.error ||
              intl.formatMessage({
                id: "label.results.control.capture.failed",
              }),
          });
          return;
        }
        setQcDraft(EMPTY_QC_DRAFT);
        setQcMessage({
          kind: failing ? "blocked" : "ok",
          text: intl.formatMessage({
            id: failing
              ? "label.results.control.capture.recordedBlocked"
              : "label.results.control.capture.recorded",
          }),
        });
      },
    );
  };

  const summary = fromAnalyzer
    ? intl.formatMessage({ id: "label.results.reagents.summary.analyzer" })
    : intl.formatMessage({ id: "label.results.reagents.summary.manual" });

  return (
    <ReferenceSection
      sectionId="combo"
      title={<FormattedMessage id="label.results.section.reagents" />}
      summary={summary}
      open={open}
      onToggle={onToggle}
    >
      <div className="unifiedReagentProvenance">
        {fromAnalyzer ? (
          <>
            <Tag type="gray" size="sm">
              <FormattedMessage id="label.results.reagents.mode.analyzer" />
            </Tag>
            <span className="unifiedHistoryFootnote">
              <FormattedMessage
                id="label.results.reagents.analyzerNote"
                values={{ 0: analyzerName || fromAnalyzerId }}
              />
            </span>
          </>
        ) : (
          <Tag type="blue" size="sm">
            <FormattedMessage id="label.results.reagents.mode.manual" />
          </Tag>
        )}
      </div>

      <div className="cds--label">
        <FormattedMessage id="label.results.reagents.lotsUsed" />
      </div>
      {links !== null && links.length === 0 && (
        <div className="unifiedHistoryFootnote">
          <FormattedMessage id="label.results.reagents.noneLinked" />
        </div>
      )}
      {(links || []).map((link) => {
        const key = String(link.reagentId ?? "");
        const lots = lotsByItem[key] || [];
        const unit = link.units || link.quantityUnit || "";
        return (
          <div
            className="unifiedReagent"
            key={key || link.name}
            data-testid={`reagent-${key}`}
          >
            <div className="unifiedReagentHead">
              <strong>{link.name || key}</strong>
              {link.usageType && (
                <Tag size="sm" type="cool-gray">
                  {link.usageType}
                </Tag>
              )}
              {link.quantityPerTest !== undefined &&
                link.quantityPerTest !== null && (
                  <span className="unifiedHistoryFootnote">
                    <FormattedMessage
                      id="label.results.reagents.perTest"
                      values={{ 0: link.quantityPerTest, 1: unit }}
                    />
                  </span>
                )}
            </div>
            {!fromAnalyzer && editable && (
              <>
                {lots.map((lot, index) => (
                  <div
                    className="unifiedLotCard"
                    key={lot.id ?? lot.lotNumber}
                    data-testid={`lot-${lot.lotNumber}`}
                  >
                    <span className="unifiedAccession">{lot.lotNumber}</span>
                    {index === 0 && (
                      <Tag size="sm" type="teal">
                        <FormattedMessage id="label.results.reagents.fefo" />
                      </Tag>
                    )}
                    <span className="unifiedHistoryFootnote">
                      {lotExpiry(lot.expirationDate) && (
                        <FormattedMessage
                          id="label.results.reagents.expires"
                          values={{ 0: lotExpiry(lot.expirationDate) }}
                        />
                      )}{" "}
                      <FormattedMessage
                        id="label.results.reagents.remaining"
                        values={{ 0: lot.currentQuantity ?? "—", 1: unit }}
                      />
                    </span>
                  </div>
                ))}
                {lots.length === 0 && (
                  <div className="unifiedHistoryFootnote">
                    <FormattedMessage id="label.results.reagents.noLots" />
                  </div>
                )}
                {lots.length > 0 && (
                  <div className="unifiedSampleStatusLine">
                    <TextInput
                      id={`reagent-qty-${key}`}
                      labelText={intl.formatMessage({
                        id: "label.results.reagents.quantityUsed",
                      })}
                      size="sm"
                      className="unifiedMiniNumber"
                      value={amounts[key] ?? String(link.quantityPerTest ?? "")}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                        setAmounts((prev) => ({
                          ...prev,
                          [key]: e.target.value,
                        }))
                      }
                    />
                    {unit && (
                      <span className="unifiedHistoryFootnote">{unit}</span>
                    )}
                    <Button
                      kind="secondary"
                      size="sm"
                      disabled={busyItem === key}
                      onClick={() => recordUse(link)}
                      data-testid={`record-use-${key}`}
                    >
                      <FormattedMessage id="label.results.reagents.recordUse" />
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        );
      })}
      {message && (
        <div
          className={
            message.kind === "error"
              ? "unifiedSampleStatusError"
              : "unifiedReagentOk"
          }
        >
          {message.text}
        </div>
      )}
      {!fromAnalyzer && (
        <div className="unifiedHistoryFootnote">
          <FormattedMessage id="label.results.reagents.consumeNote" />
        </div>
      )}

      {(fromAnalyzer || canCaptureControl) && (
        <div className="cds--label unifiedFieldSpacer">
          <FormattedMessage id="label.results.control.title" />
        </div>
      )}
      {fromAnalyzer ? (
        <>
          {controlLots.length > 0 ? (
            controlLots.map((lot) => (
              <div className="unifiedHistoryRow" key={lot.id}>
                <span className="unifiedHistoryDetail">
                  {lot.productName}{" "}
                  <span className="unifiedAccession">{lot.lotNumber}</span>
                </span>
                {lot.controlLevel && (
                  <Tag size="sm" type="cool-gray">
                    {lot.controlLevel}
                  </Tag>
                )}
                {lot.status && (
                  <Tag
                    size="sm"
                    type={lot.status === "ACTIVE" ? "green" : "gray"}
                  >
                    {lot.status}
                  </Tag>
                )}
              </div>
            ))
          ) : (
            <div className="unifiedHistoryFootnote">
              <FormattedMessage id="label.results.control.noneConfigured" />
            </div>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() => window.open("/analyzers/qc/db", "_blank")}
          >
            <FormattedMessage id="label.results.control.viewResults" />
          </Button>
        </>
      ) : canCaptureControl ? (
        <div className="unifiedQcCapture" data-testid="qc-capture">
          <Select
            id={`qc-outcome-${testId}`}
            labelText={intl.formatMessage({
              id: quantitative
                ? "label.results.control.capture.outcome"
                : "label.results.control.capture.controlLine",
            })}
            size="sm"
            value={qcDraft.outcome}
            onChange={setQcField("outcome")}
            data-testid="qc-outcome"
          >
            <SelectItem value="" text="" />
            {quantitative ? (
              <>
                <SelectItem
                  value="PASS"
                  text={intl.formatMessage({
                    id: "label.results.control.capture.pass",
                  })}
                />
                <SelectItem
                  value="FAIL"
                  text={intl.formatMessage({
                    id: "label.results.control.capture.fail",
                  })}
                />
              </>
            ) : (
              <>
                <SelectItem
                  value="VALID"
                  text={intl.formatMessage({
                    id: "label.results.control.capture.valid",
                  })}
                />
                <SelectItem
                  value="INVALID"
                  text={intl.formatMessage({
                    id: "label.results.control.capture.invalid",
                  })}
                />
              </>
            )}
          </Select>
          {quantitative ? (
            <>
              <Select
                id={`qc-lot-${testId}`}
                labelText={intl.formatMessage({
                  id: "label.results.control.capture.lot",
                })}
                size="sm"
                value={qcDraft.lotId}
                onChange={setQcField("lotId")}
                data-testid="qc-lot"
              >
                <SelectItem value="" text="" />
                {controlLots.map((lot) => (
                  <SelectItem
                    key={lot.id}
                    value={lot.id}
                    text={[lot.productName, lot.lotNumber, lot.controlLevel]
                      .filter(Boolean)
                      .join(" — ")}
                  />
                ))}
              </Select>
              {controlLots.length === 0 && (
                <div
                  className="unifiedHistoryFootnote"
                  data-testid="qc-no-bench-lots"
                >
                  <FormattedMessage id="label.results.control.capture.noBenchLots" />
                </div>
              )}
              <div className="unifiedSampleStatusLine">
                <TextInput
                  id={`qc-measured-${testId}`}
                  labelText={intl.formatMessage({
                    id: "label.results.control.capture.measured",
                  })}
                  size="sm"
                  className="unifiedMiniNumber"
                  value={qcDraft.measured}
                  onChange={setQcField("measured")}
                />
                <TextInput
                  id={`qc-expected-${testId}`}
                  labelText={intl.formatMessage({
                    id: "label.results.control.capture.expected",
                  })}
                  size="sm"
                  className="unifiedMiniNumber"
                  value={qcDraft.expected}
                  onChange={setQcField("expected")}
                />
                <TextInput
                  id={`qc-uncertainty-${testId}`}
                  labelText={intl.formatMessage({
                    id: "label.results.control.capture.uncertainty",
                  })}
                  size="sm"
                  className="unifiedMiniNumber"
                  value={qcDraft.uncertainty}
                  onChange={setQcField("uncertainty")}
                />
                {unitOfMeasure && (
                  <span className="unifiedHistoryFootnote">
                    {unitOfMeasure}
                  </span>
                )}
              </div>
            </>
          ) : (
            <TextInput
              id={`qc-kit-lot-${testId}`}
              labelText={intl.formatMessage({
                id: "label.results.control.capture.kitLot",
              })}
              size="sm"
              maxLength={120}
              value={qcDraft.kitLot}
              onChange={setQcField("kitLot")}
              data-testid="qc-kit-lot"
            />
          )}
          {(qcDraft.outcome === "INVALID" || qcDraft.outcome === "FAIL") && (
            <div
              className="unifiedSampleStatusError"
              data-testid="qc-blocked-hint"
            >
              <FormattedMessage id="label.results.control.capture.blockedHint" />
            </div>
          )}
          <Button
            kind="secondary"
            size="sm"
            disabled={qcBusy}
            onClick={recordControl}
            data-testid="qc-record"
          >
            <FormattedMessage id="label.results.control.capture.record" />
          </Button>
          {qcMessage && (
            <div
              className={
                qcMessage.kind === "ok"
                  ? "unifiedReagentOk"
                  : qcMessage.kind === "blocked"
                    ? "unifiedQcCaptureWarning"
                    : "unifiedSampleStatusError"
              }
              data-testid="qc-capture-message"
            >
              {qcMessage.text}
            </div>
          )}
        </div>
      ) : null}

      {dilution && (
        <>
          <div className="cds--label unifiedFieldSpacer">
            <FormattedMessage id="label.results.dilution.title" />
          </div>
          {dilution}
        </>
      )}
    </ReferenceSection>
  );
};

export default ReagentsQcSection;
