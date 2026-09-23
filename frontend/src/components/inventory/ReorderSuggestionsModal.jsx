import React, { useState } from "react";
import {
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  TextInput,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Tag,
  Button,
} from "@carbon/react";
import { Download } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryItemAPI } from "./InventoryService";
import { toIsoDate } from "./dates";

/**
 * What to order, and a way to say you have.
 *
 * <p>Suggestions are not a second rule. A row belongs here exactly when the
 * board already badges it Reorder now or Reorder soon, so the panel and the
 * badges cannot drift apart — the arithmetic stays in one place, server side,
 * and this reads its answer.
 *
 * <p>It is called suggestions rather than an order or a list because it places
 * nothing. Marking as ordered records that somebody else placed the order.
 */
const SUGGESTED = ["REORDER_NOW", "REORDER_SOON"];

export const isSuggested = (row) => SUGGESTED.includes(row.status);

/** RFC 4180: double the quotes, wrap anything that could confuse a parser. */
const csvCell = (value) => {
  const text = value == null ? "" : String(value);
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
};

const ReorderSuggestionsModal = ({ open, rows, onClose, onMarked }) => {
  const intl = useIntl();
  const [note, setNote] = useState("");
  const [expectedDate, setExpectedDate] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const suggestions = rows.filter(isSuggested);

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "inventory.board.column.item" }),
    },
    {
      key: "onHand",
      header: intl.formatMessage({ id: "inventory.board.column.onHand" }),
    },
    {
      key: "threshold",
      header: intl.formatMessage({ id: "catalog.item.lowStockThreshold" }),
    },
    {
      key: "orderBy",
      header: intl.formatMessage({ id: "inventory.orderBy.label" }),
    },
    { key: "status", header: intl.formatMessage({ id: "common.status" }) },
  ];

  const tableRows = suggestions.map((row) => ({
    id: String(row.itemId),
    name: row.code ? `${row.name} (${row.code})` : row.name,
    onHand: `${row.onHand} ${row.units ?? ""}`.trim(),
    threshold: row.lowStockThreshold ?? "—",
    orderBy: row.orderByDate ?? "—",
    status: row.status,
    onOrder: !!row.orderedOn,
  }));

  const isOnOrder = (rowId) => !!tableRows.find((r) => r.id === rowId)?.onOrder;

  const mark = async (selectedRows) => {
    setSaving(true);
    setError(null);
    try {
      await InventoryItemAPI.markOrdered({
        itemIds: selectedRows.map((r) => Number(r.id)),
        note: note || null,
        expectedDate: expectedDate || null,
      });
      onMarked(selectedRows.length, "marked");
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  // Marking the wrong row is easy and the consequence is silent: the item stops
  // being reported while nothing is actually on its way. So the way back is next
  // to the way in, not buried.
  const clearMark = async (selectedRows) => {
    setSaving(true);
    setError(null);
    try {
      await InventoryItemAPI.clearOrdered(
        selectedRows.map((r) => Number(r.id)),
      );
      onMarked(selectedRows.length, "cleared");
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  const exportCsv = (selectedRows) => {
    const chosen = selectedRows.length ? selectedRows : tableRows;
    const ids = new Set(chosen.map((r) => String(r.id)));
    const lines = [headers.map((h) => csvCell(h.header)).join(",")];
    tableRows
      .filter((r) => ids.has(r.id))
      .forEach((r) =>
        lines.push(headers.map((h) => csvCell(r[h.key])).join(",")),
      );
    const blob = new Blob([lines.join("\n")], {
      type: "text/csv;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "reorder-suggestions.csv";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({ id: "inventory.reorder.suggestions" })}
      passiveModal
      size="lg"
    >
      <p className="board-subline board-suggestions-help">
        <FormattedMessage id="inventory.reorder.help" />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "notification.error" })}
          subtitle={error}
        />
      )}

      {suggestions.length === 0 ? (
        <p className="board-empty">
          <FormattedMessage id="inventory.reorder.none" />
        </p>
      ) : (
        <>
          <div className="board-order-fields">
            <TextInput
              id="reorder-note"
              labelText={<FormattedMessage id="inventory.reorder.note" />}
              placeholder={intl.formatMessage({
                id: "inventory.reorder.note.placeholder",
              })}
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={expectedDate}
              onChange={(dates) =>
                setExpectedDate(dates?.[0] ? toIsoDate(dates[0]) : "")
              }
            >
              <DatePickerInput
                id="reorder-expected-date"
                placeholder="yyyy-mm-dd"
                labelText={
                  <FormattedMessage id="inventory.reorder.expectedDate" />
                }
              />
            </DatePicker>
          </div>

          <DataTable rows={tableRows} headers={headers}>
            {({
              rows: renderRows,
              headers: renderHeaders,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              getBatchActionProps,
              getTableProps,
              selectedRows,
            }) => (
              <TableContainer>
                <TableToolbar>
                  <TableBatchActions {...getBatchActionProps()}>
                    <TableBatchAction
                      disabled={saving}
                      onClick={() => mark(selectedRows)}
                    >
                      <FormattedMessage id="inventory.reorder.markOrdered" />
                    </TableBatchAction>
                    <TableBatchAction
                      disabled={
                        saving || !selectedRows.some((r) => isOnOrder(r.id))
                      }
                      onClick={() => clearMark(selectedRows)}
                    >
                      <FormattedMessage id="inventory.reorder.clearOrdered" />
                    </TableBatchAction>
                    <TableBatchAction
                      renderIcon={Download}
                      onClick={() => exportCsv(selectedRows)}
                    >
                      <FormattedMessage id="reports.tat.exportCsv" />
                    </TableBatchAction>
                  </TableBatchActions>
                  <TableToolbarContent>
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Download}
                      onClick={() => exportCsv([])}
                    >
                      <FormattedMessage id="reports.tat.exportCsv" />
                    </Button>
                  </TableToolbarContent>
                </TableToolbar>
                <Table {...getTableProps()} size="sm">
                  <TableHead>
                    <TableRow>
                      <TableSelectAll {...getSelectionProps()} />
                      {renderHeaders.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {renderRows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableSelectRow {...getSelectionProps({ row })} />
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "status" ? (
                              <>
                                <Tag
                                  size="sm"
                                  type={
                                    cell.value === "REORDER_NOW"
                                      ? "red"
                                      : "magenta"
                                  }
                                >
                                  <FormattedMessage
                                    id={
                                      cell.value === "REORDER_NOW"
                                        ? "inventory.reorderStatus.now"
                                        : "inventory.reorderStatus.soon"
                                    }
                                  />
                                </Tag>
                                {tableRows.find((r) => r.id === row.id)
                                  ?.onOrder && (
                                  <Tag size="sm" type="teal">
                                    <FormattedMessage id="inventory.reorder.onOrder" />
                                  </Tag>
                                )}
                              </>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </>
      )}
    </Modal>
  );
};

export default ReorderSuggestionsModal;
