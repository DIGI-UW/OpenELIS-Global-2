import React, { useCallback, useEffect, useState } from "react";
import { Button, NumberInput, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ReferenceSection from "./ReferenceSection";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

/**
 * OGC-1023 (R4, FR-I1/I2) — in-view aliquoting that reuses the shipped
 * sample-management aliquot function (D18): lists this sample item's existing
 * child aliquots and creates new ones. Numbering (LABNO.X) is server-side; the
 * child items inherit sample, type and collection metadata from the parent.
 * Storage assignment does not carry over automatically — that lives in the
 * storage module and is flagged in the PR rather than reinvented here.
 */
interface AliquotSummary {
  id?: string;
  externalId?: string;
  quantity?: number;
  remainingQuantity?: number;
  createdDate?: string | number;
}

/** the API serializes createdDate as epoch millis; render a local date */
const displayDate = (value?: string | number): string => {
  if (value === undefined || value === null || value === "") {
    return "";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return String(value);
  }
  return new Date(numeric).toLocaleDateString();
};

interface SampleItemDTO {
  id?: string;
  externalId?: string;
  remainingQuantity?: number;
  childAliquots?: AliquotSummary[];
}

interface SearchSamplesResponse {
  sampleItems?: SampleItemDTO[];
}

interface AliquotsSectionProps {
  accessionNumber?: string;
  sampleItemId?: string;
  open: boolean;
  onToggle: (open: boolean) => void;
}

const AliquotsSection: React.FC<AliquotsSectionProps> = ({
  accessionNumber,
  sampleItemId,
  open,
  onToggle,
}) => {
  const intl = useIntl();
  const [item, setItem] = useState<SampleItemDTO | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [count, setCount] = useState(1);
  const [quantity, setQuantity] = useState("");
  const [notes, setNotes] = useState("");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const fetchAliquots = useCallback(() => {
    if (!accessionNumber || !sampleItemId) {
      setLoaded(true);
      return;
    }
    getFromOpenElisServer(
      `/rest/sample-management/search?accessionNumber=${accessionNumber}&includeTests=false`,
      (body: SearchSamplesResponse) => {
        const items = body?.sampleItems || [];
        setItem(
          items.find((i) => String(i.id) === String(sampleItemId)) || null,
        );
        setLoaded(true);
      },
    );
  }, [accessionNumber, sampleItemId]);

  useEffect(() => {
    if (open && !loaded) {
      fetchAliquots();
    }
  }, [open, loaded, fetchAliquots]);

  const create = () => {
    if (!sampleItemId || !quantity) {
      return;
    }
    setSubmitting(true);
    setMessage("");
    postToOpenElisServerJsonResponse(
      `/rest/sample-management/aliquot`,
      JSON.stringify({
        parentSampleItemId: sampleItemId,
        quantityToTransfer: quantity,
        numberOfAliquots: count,
        notes: notes || undefined,
      }),
      (response: { aliquots?: AliquotSummary[]; message?: string }) => {
        setSubmitting(false);
        if (response?.aliquots) {
          setMessage(
            response.message ||
              intl.formatMessage({ id: "label.results.aliquots.created" }),
          );
          setQuantity("");
          setNotes("");
          setCount(1);
          fetchAliquots();
        } else {
          setMessage(
            response?.message ||
              intl.formatMessage({ id: "label.results.aliquots.failed" }),
          );
        }
      },
    );
  };

  const aliquots = item?.childAliquots || [];
  const summary = loaded
    ? intl.formatMessage(
        { id: "label.results.aliquots.summary" },
        { 0: aliquots.length },
      )
    : intl.formatMessage({ id: "label.results.aliquots.summary.unloaded" });

  if (!sampleItemId) {
    return null;
  }

  return (
    <ReferenceSection
      sectionId="aliquots"
      title={<FormattedMessage id="label.results.section.aliquots" />}
      summary={summary}
      open={open}
      onToggle={onToggle}
    >
      {aliquots.length > 0 && (
        <div className="unifiedHistoryTable" data-testid="aliquots-table">
          <div className="unifiedHistoryRow unifiedHistoryHead">
            <span className="unifiedHistoryWhen">
              <FormattedMessage id="label.results.aliquots.labNo" />
            </span>
            <span className="unifiedHistoryEvent">
              <FormattedMessage id="label.results.aliquots.quantity" />
            </span>
            <span className="unifiedHistoryDetail">
              <FormattedMessage id="label.results.aliquots.createdDate" />
            </span>
          </div>
          {aliquots.map((aliquot, index) => (
            <div className="unifiedHistoryRow" key={index}>
              <span className="unifiedHistoryWhen">{aliquot.externalId}</span>
              <span className="unifiedHistoryEvent">
                {aliquot.remainingQuantity ?? aliquot.quantity ?? ""}
              </span>
              <span className="unifiedHistoryDetail">
                {displayDate(aliquot.createdDate)}
              </span>
            </div>
          ))}
        </div>
      )}
      {loaded && aliquots.length === 0 && (
        <div className="unifiedHistoryEmpty">
          <FormattedMessage id="label.results.aliquots.empty" />
        </div>
      )}
      <div className="unifiedAliquotCreate">
        <NumberInput
          id={`aliquot-count-${sampleItemId}`}
          label={intl.formatMessage({ id: "label.results.aliquots.count" })}
          min={1}
          max={10}
          value={count}
          onChange={(_e: unknown, { value }: { value: number | string }) =>
            setCount(Number(value) || 1)
          }
        />
        <TextInput
          id={`aliquot-quantity-${sampleItemId}`}
          labelText={intl.formatMessage({
            id: "label.results.aliquots.totalQuantity",
          })}
          type="number"
          value={quantity}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            setQuantity(e.target.value)
          }
        />
        <TextInput
          id={`aliquot-notes-${sampleItemId}`}
          labelText={intl.formatMessage({ id: "label.results.aliquots.notes" })}
          value={notes}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            setNotes(e.target.value)
          }
        />
        <Button
          size="sm"
          disabled={!quantity || submitting}
          onClick={create}
          data-testid="aliquot-create"
        >
          <FormattedMessage id="label.results.aliquots.create" />
        </Button>
      </div>
      {message && <div className="unifiedHistoryFootnote">{message}</div>}
      {item?.remainingQuantity !== undefined && (
        <div className="unifiedHistoryFootnote">
          <FormattedMessage id="label.results.aliquots.remaining" />:{" "}
          {item.remainingQuantity}
        </div>
      )}
    </ReferenceSection>
  );
};

export default AliquotsSection;
