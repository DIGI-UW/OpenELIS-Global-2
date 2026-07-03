import React, { useEffect, useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * OGC-649 (LO-N/A — follow-up to OGC-60): Audit-trail viewer for a SampleItem's
 * storage movements. Fetches /rest/storage/sample-items/{id}/movements on open
 * and renders a Carbon DataTable. Insert-only audit log per SampleStorageMovement
 * @Immutable entity, so this view is read-only.
 *
 * Props:
 * - open: boolean - whether the modal is open
 * - sample: object - { id, sampleItemId, sampleId, sampleItemExternalId, ... }
 * - onClose: () => void
 */
const ViewAuditModal = ({ open, sample, onClose }) => {
  const intl = useIntl();
  const [movements, setMovements] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open || !sample) {
      return;
    }
    const sampleItemId = String(sample.sampleItemId || sample.id || "");
    if (!sampleItemId) {
      return;
    }
    setLoading(true);
    setError(null);
    setMovements([]);
    getFromOpenElisServer(
      `/rest/storage/sample-items/${encodeURIComponent(sampleItemId)}/movements`,
      (data) => {
        setLoading(false);
        if (Array.isArray(data)) {
          setMovements(data);
        } else {
          setError(
            intl.formatMessage({
              id: "storage.audit.fetch.error",
              defaultMessage: "Could not load audit trail.",
            }),
          );
        }
      },
    );
  }, [open, sample, intl]);

  const headers = [
    {
      key: "movementDate",
      header: intl.formatMessage({
        id: "storage.audit.movementDate",
        defaultMessage: "Movement Date",
      }),
    },
    {
      key: "movedBy",
      header: intl.formatMessage({
        id: "storage.audit.movedBy",
        defaultMessage: "Moved By",
      }),
    },
    {
      key: "from",
      header: intl.formatMessage({
        id: "storage.audit.from",
        defaultMessage: "From",
      }),
    },
    {
      key: "to",
      header: intl.formatMessage({
        id: "storage.audit.to",
        defaultMessage: "To",
      }),
    },
    {
      key: "reason",
      header: intl.formatMessage({
        id: "storage.audit.reason",
        defaultMessage: "Reason",
      }),
    },
  ];

  const rows = movements.map((m) => ({
    id: String(m.id),
    movementDate: m.movementDate || "",
    // OGC-738a: backend now resolves the user name; fall back to the numeric
    // id when an older row predates user-resolution.
    movedBy:
      (m.movedByUserName && m.movedByUserName.trim()) ||
      (m.movedByUserId != null ? String(m.movedByUserId) : ""),
    from: formatLocation(
      m.previousLocationType,
      m.previousLocationId,
      m.previousPositionCoordinate,
    ),
    to: formatLocation(
      m.newLocationType,
      m.newLocationId,
      m.newPositionCoordinate,
    ),
    reason: m.reason || "",
  }));

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="lg"
      data-testid="view-audit-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.view.audit.title",
          defaultMessage: "Storage Audit Trail",
        })}
        subtitle={intl.formatMessage(
          {
            id: "storage.view.audit.subtitle",
            defaultMessage: "Movement history for sample {sampleId}",
          },
          {
            sampleId:
              sample?.sampleItemExternalId ||
              sample?.sampleId ||
              sample?.sampleItemId ||
              "",
          },
        )}
      />
      <ModalBody>
        {loading && <Loading withOverlay={false} />}
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "storage.audit.fetch.error.title",
              defaultMessage: "Error",
            })}
            subtitle={error}
            lowContrast
          />
        )}
        {!loading && !error && rows.length === 0 && (
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "storage.audit.empty.title",
              defaultMessage: "No movements",
            })}
            subtitle={intl.formatMessage({
              id: "storage.audit.empty",
              defaultMessage: "No movements recorded for this sample yet.",
            })}
            lowContrast
          />
        )}
        {!loading && !error && rows.length > 0 && (
          <DataTable rows={rows} headers={headers}>
            {({
              rows: dtRows,
              headers: dtHeaders,
              getHeaderProps,
              getTableProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {dtHeaders.map((h) => (
                        <TableHeader
                          key={h.key}
                          {...getHeaderProps({ header: h })}
                        >
                          {h.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {dtRows.map((r) => (
                      <TableRow key={r.id}>
                        {r.cells.map((c) => (
                          <TableCell key={c.id}>{c.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}
      </ModalBody>
      <ModalFooter>
        <Button kind="primary" onClick={onClose}>
          <FormattedMessage id="label.button.close" defaultMessage="Close" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

function formatLocation(type, id, coord) {
  const parts = [];
  if (type) parts.push(type);
  if (id != null) parts.push(`#${id}`);
  if (coord) parts.push(`(${coord})`);
  return parts.length > 0 ? parts.join(" ") : "—";
}

export default ViewAuditModal;
