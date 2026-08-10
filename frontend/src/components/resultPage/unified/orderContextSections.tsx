import React, { useEffect, useState } from "react";
import { Button, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ReferenceSection from "./ReferenceSection";
import SampleStatusBlock, { QuantitySnapshot } from "./SampleStatusBlock";
import { getFromOpenElisServer } from "../../utils/Utils";
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import config from "../../../config.json";

/**
 * OGC-811 gallery parity — the reference-zone sections fed by the order
 * record: Order info (clinician / priority / collection), Programme info, and
 * Attachments, plus the per-item Storage section. Everything loads lazily from
 * shipped endpoints when the panel expands; sections with no content are not
 * mounted (FR-C5).
 */

interface SampleOrderItems {
  providerFirstName?: string;
  providerLastName?: string;
  providerWorkPhone?: string;
  referringSiteName?: string;
  referringSiteDepartmentName?: string;
  priority?: string;
  collectionDate?: string;
  receivedDateForDisplay?: string;
  program?: string;
}

export interface OrderContext {
  sampleOrderItems?: SampleOrderItems;
  loaded: boolean;
}

/** one fetch per accession, shared by the order-fed sections */
export const useOrderContext = (accessionNumber?: string): OrderContext => {
  const [state, setState] = useState<OrderContext>({ loaded: false });
  useEffect(() => {
    if (!accessionNumber) {
      setState({ loaded: true });
      return;
    }
    getFromOpenElisServer(
      `/rest/order/search?labNumber=${accessionNumber}`,
      (body: { sampleOrderItems?: SampleOrderItems }) =>
        setState({ sampleOrderItems: body?.sampleOrderItems, loaded: true }),
    );
  }, [accessionNumber]);
  return state;
};

const KV: React.FC<{ labelKey: string; value?: React.ReactNode }> = ({
  labelKey,
  value,
}) =>
  value ? (
    <div>
      <span className="cds--label">
        <FormattedMessage id={labelKey} />
      </span>
      <span>{value}</span>
    </div>
  ) : null;

interface SectionProps {
  open: boolean;
  onToggle: (open: boolean) => void;
}

export const OrderInfoSection: React.FC<
  SectionProps & {
    order: OrderContext;
    sampleType?: string;
    receivedDate?: string;
    technician?: string;
  }
> = ({ open, onToggle, order, sampleType, receivedDate, technician }) => {
  const items = order.sampleOrderItems || {};
  const clinician = [items.providerFirstName, items.providerLastName]
    .filter(Boolean)
    .join(" ");
  const site = [items.referringSiteName, items.referringSiteDepartmentName]
    .filter(Boolean)
    .join(" — ");
  const summary = [clinician, items.priority, site].filter(Boolean).join(" · ");
  return (
    <ReferenceSection
      sectionId="orderInfo"
      title={<FormattedMessage id="label.results.section.orderInfo" />}
      summary={
        summary || [sampleType, receivedDate].filter(Boolean).join(" · ")
      }
      open={open}
      onToggle={onToggle}
    >
      <div className="unifiedRefGrid">
        <KV labelKey="label.results.order.clinician" value={clinician} />
        <KV
          labelKey="label.results.order.phone"
          value={items.providerWorkPhone}
        />
        <KV labelKey="label.results.order.site" value={site} />
        <KV labelKey="label.results.order.priority" value={items.priority} />
        <KV
          labelKey="label.results.order.collection"
          value={items.collectionDate}
        />
        <KV labelKey="label.results.receivedDate" value={receivedDate} />
        <KV labelKey="label.results.sampleType" value={sampleType} />
        <KV labelKey="label.results.technician" value={technician} />
      </div>
    </ReferenceSection>
  );
};

export const ProgrammeSection: React.FC<
  SectionProps & {
    order: OrderContext;
    eqaSample?: boolean;
    eqaPriority?: string;
  }
> = ({ open, onToggle, order, eqaSample, eqaPriority }) => {
  const program = order.sampleOrderItems?.program;
  if (!order.loaded || (!program && !eqaSample)) {
    return null;
  }
  return (
    <ReferenceSection
      sectionId="program"
      title={<FormattedMessage id="label.results.section.program" />}
      summary={program || (eqaSample ? "EQA" : "")}
      open={open}
      onToggle={onToggle}
    >
      <div className="unifiedRefGrid">
        <KV labelKey="label.results.program.name" value={program} />
        {eqaSample && (
          <div>
            <span className="cds--label">
              <FormattedMessage id="label.results.program.eqa" />
            </span>
            <Tag type="purple" size="sm">
              EQA{eqaPriority ? ` · ${eqaPriority}` : ""}
            </Tag>
          </div>
        )}
      </div>
      <div className="unifiedHistoryFootnote">
        <FormattedMessage id="label.results.program.readonly" />
      </div>
    </ReferenceSection>
  );
};

interface StorageLocation extends QuantitySnapshot {
  hierarchicalPath?: string;
  positionCoordinate?: string;
  notes?: string;
}

export const StorageSection: React.FC<
  SectionProps & {
    sampleItemId?: string;
    editable?: boolean;
    actions?: React.ReactNode;
  }
> = ({ open, onToggle, sampleItemId, editable, actions }) => {
  const intl = useIntl();
  const [location, setLocation] = useState<StorageLocation | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  useEffect(() => {
    if (!sampleItemId) {
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/sample-items/${sampleItemId}`,
      (body: StorageLocation) => setLocation(body || {}),
    );
  }, [sampleItemId, refreshKey]);
  if (!sampleItemId) {
    return null;
  }
  const path = location?.hierarchicalPath || "";
  return (
    <ReferenceSection
      sectionId="storage"
      title={<FormattedMessage id="label.results.section.storage" />}
      summary={
        path || intl.formatMessage({ id: "label.results.storage.unassigned" })
      }
      open={open}
      onToggle={onToggle}
    >
      <div className="unifiedRefGrid">
        <KV
          labelKey="label.results.storage.location"
          value={
            path ||
            intl.formatMessage({ id: "label.results.storage.unassigned" })
          }
        />
        <KV
          labelKey="label.results.storage.position"
          value={location?.positionCoordinate}
        />
        <KV labelKey="label.results.storage.notes" value={location?.notes} />
      </div>
      <Button
        kind="secondary"
        size="sm"
        className="unifiedFieldSpacer"
        onClick={() => window.open("/Storage/sample-items", "_blank")}
      >
        <FormattedMessage id="label.results.storage.move" />
      </Button>
      {location && (
        <SampleStatusBlock
          sampleItemId={sampleItemId}
          snapshot={location}
          editable={Boolean(editable)}
          onChanged={() => setRefreshKey((k) => k + 1)}
        />
      )}
      {actions}
    </ReferenceSection>
  );
};

interface AttachmentDto {
  id: number;
  fileName?: string;
  fileType?: string;
  fileSizeBytes?: number;
  uploadedAt?: string;
}

export const AttachmentsSection: React.FC<
  SectionProps & { accessionNumber?: string }
> = ({ open, onToggle, accessionNumber }) => {
  const intl = useIntl();
  const [files, setFiles] = useState<AttachmentDto[] | null>(null);
  useEffect(() => {
    if (!accessionNumber) {
      return;
    }
    getFromOpenElisServer(
      `/rest/order/${accessionNumber}/attachments`,
      (body: AttachmentDto[]) => setFiles(Array.isArray(body) ? body : []),
    );
  }, [accessionNumber]);
  if (!files || files.length === 0) {
    return null;
  }
  const sizeLabel = (bytes?: number) =>
    bytes ? `${Math.max(1, Math.round(bytes / 1024))} KB` : "";
  return (
    <ReferenceSection
      sectionId="attachments"
      title={<FormattedMessage id="label.results.section.attachments" />}
      summary={intl.formatMessage(
        { id: "label.results.attachments.summary" },
        { 0: files.length },
      )}
      open={open}
      onToggle={onToggle}
    >
      {files.map((file) => (
        <div className="unifiedHistoryRow" key={file.id}>
          <span className="unifiedHistoryDetail">
            {file.fileName}
            <span className="unifiedHistoryFootnote">
              {" "}
              {sizeLabel(file.fileSizeBytes)}
              {file.uploadedAt ? ` · ${file.uploadedAt}` : ""}
            </span>
          </span>
          <Button
            kind="ghost"
            size="sm"
            onClick={() =>
              window.open(
                `${config.serverBaseUrl}/rest/order/attachments/${file.id}/view`,
                "_blank",
              )
            }
          >
            <FormattedMessage id="label.results.attachments.view" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            onClick={() =>
              window.open(
                `${config.serverBaseUrl}/rest/order/attachments/${file.id}/download`,
                "_blank",
              )
            }
          >
            <FormattedMessage id="label.results.attachments.download" />
          </Button>
        </div>
      ))}
    </ReferenceSection>
  );
};
