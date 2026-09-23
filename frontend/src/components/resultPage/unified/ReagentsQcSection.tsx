import React, { useCallback, useEffect, useState } from "react";
import { Button, Tag, TextInput } from "@carbon/react";
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
 * OGC-1025 (R6) — manual control-result capture stays a documented gray
 * state: qc_result.result_value is NOT NULL NUMERIC(15,5) and the only write
 * path is the analyzer ingest, so qualitative (RDT Valid/Invalid) and bench
 * QC entry need a backend story first. Analyzer rows link to the QC
 * dashboard; manual rows show the blocker note.
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
  open: boolean;
  onToggle: (open: boolean) => void;
  /** the dilution inputs, relocated here from the work zone per the mockup */
  dilution?: React.ReactNode;
}

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
    if (!testId || !fromAnalyzerId) {
      return;
    }
    getFromOpenElisServer(
      `/rest/qc/controlLots?testId=${testId}&instrumentId=${fromAnalyzerId}`,
      (body: ControlLot[]) => setControlLots(Array.isArray(body) ? body : []),
    );
  }, [testId, fromAnalyzerId]);

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

      <div className="cds--label unifiedFieldSpacer">
        <FormattedMessage id="label.results.control.title" />
      </div>
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
      ) : (
        <div className="unifiedHistoryFootnote" data-testid="control-blocked">
          <FormattedMessage id="label.results.control.manualBlocked" />
        </div>
      )}

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
