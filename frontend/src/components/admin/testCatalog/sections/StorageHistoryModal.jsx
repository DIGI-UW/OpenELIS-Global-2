import React, { useEffect, useState } from "react";
import {
  Modal,
  Search,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  DataTableSkeleton,
  Tag,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * OGC-949 / OGC-766 (OGC-1005) — Sample Storage change-history modal.
 *
 * Fetches the audit rows (GET /rest/test-catalog/{testId}/storage/history) and
 * expands each snapshot diff into one display row per changed field
 * (Changed At / Changed By / Field / Old / New), newest first, filterable.
 */
const humanize = (key) =>
  key
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (c) => c.toUpperCase())
    .trim();

const parse = (json) => {
  if (!json) {
    return {};
  }
  try {
    return JSON.parse(json);
  } catch {
    return {};
  }
};

const StorageHistoryModal = ({ open, onClose, testId }) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [entries, setEntries] = useState([]);
  const [filter, setFilter] = useState("");

  useEffect(() => {
    if (!open || !testId) {
      return;
    }
    setLoading(true);
    setError(false);
    setFilter("");
    getFromOpenElisServer(
      `/rest/test-catalog/${testId}/storage/history`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setEntries(Array.isArray(res) ? res : []);
      },
    );
  }, [open, testId]);

  // Expand each audit snapshot into one row per changed field.
  const rows = [];
  entries.forEach((entry) => {
    const prev = parse(entry.previousValues);
    const next = parse(entry.newValues);
    Object.keys(next).forEach((key) => {
      const oldVal = prev[key];
      const newVal = next[key];
      if (String(oldVal ?? "") !== String(newVal ?? "")) {
        rows.push({
          key: `${entry.id}-${key}`,
          changedAt: entry.changedAt,
          changedBy: entry.changedBy,
          field: humanize(key),
          oldVal,
          newVal,
        });
      }
    });
  });

  const filtered = filter
    ? rows.filter((r) => r.field.toLowerCase().includes(filter.toLowerCase()))
    : rows;

  const fmt = (v) =>
    v === null || v === undefined || v === "" ? "—" : String(v);

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      passiveModal
      size="lg"
      modalHeading={intl.formatMessage({
        id: "label.testCatalog.storage.history.title",
      })}
    >
      {loading ? (
        <DataTableSkeleton
          columnCount={5}
          rowCount={4}
          showHeader={false}
          showToolbar={false}
        />
      ) : error ? (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "error.title" })}
          subtitle={intl.formatMessage({
            id: "label.testCatalog.storage.history.loadError",
          })}
        />
      ) : rows.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.storage.history.empty",
          })}
        />
      ) : (
        <>
          <Search
            size="sm"
            labelText={intl.formatMessage({
              id: "label.testCatalog.storage.history.filter",
            })}
            placeholder={intl.formatMessage({
              id: "label.testCatalog.storage.history.filter",
            })}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
          <TableContainer>
            <Table size="sm" aria-label="storage-history">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "label.testCatalog.storage.history.col.changedAt",
                    })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "label.testCatalog.storage.history.col.changedBy",
                    })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "label.testCatalog.storage.history.col.field",
                    })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "label.testCatalog.storage.history.col.old",
                    })}
                  </TableHeader>
                  <TableHeader>
                    {intl.formatMessage({
                      id: "label.testCatalog.storage.history.col.new",
                    })}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.map((r) => (
                  <TableRow key={r.key}>
                    <TableCell>{r.changedAt}</TableCell>
                    <TableCell>
                      <Tag type="gray">{`#${fmt(r.changedBy)}`}</Tag>
                    </TableCell>
                    <TableCell>{r.field}</TableCell>
                    <TableCell>
                      <del>{fmt(r.oldVal)}</del>
                    </TableCell>
                    <TableCell>
                      <ins>{fmt(r.newVal)}</ins>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Modal>
  );
};

export default StorageHistoryModal;
