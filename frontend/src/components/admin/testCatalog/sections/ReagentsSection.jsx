import React, { useCallback, useEffect, useState } from "react";
import {
  Stack,
  Button,
  Select,
  SelectItem,
  NumberInput,
  ComboBox,
  Modal,
  DataTable,
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
import { Add, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
  deleteFromOpenElisServer,
} from "../../../utils/Utils";
import LinkReagentModal from "./LinkReagentModal";

/**
 * OGC-949 M15 / OGC-991 + OGC-992 + OGC-993 (epic OGC-762) — Reagents section.
 *
 * Lists reagents linked to this test (GET /rest/test-catalog/{testId}/reagents,
 * OGC-987) with current stock from inventory (OGC-991); links reagents via the
 * Link Reagent modal (OGC-992); and edits usage type / quantity / unit inline
 * with auto-save on blur, plus per-row unlink with confirmation (OGC-993).
 */

// Common reagent-consumption units; the ComboBox also accepts custom values.
const UNIT_OPTIONS = ["mL", "µL", "L", "mg", "g", "count", "drops", "units"];

const ReagentsSection = ({ testId }) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [reagents, setReagents] = useState([]);
  const [edits, setEdits] = useState({});
  const [linkModalOpen, setLinkModalOpen] = useState(false);
  const [unlinkTarget, setUnlinkTarget] = useState(null);
  const [notification, setNotification] = useState(null);

  const load = useCallback(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/test-catalog/${testId}/reagents`, (res) => {
      setLoading(false);
      if (!res) {
        setError(true);
        return;
      }
      setReagents(res);
    });
  }, [testId]);

  useEffect(() => {
    load();
  }, [load]);

  // Seed the per-row editable model whenever the server list changes.
  useEffect(() => {
    const next = {};
    reagents.forEach((r) => {
      next[r.id] = {
        usageType: r.usageType || "PRIMARY",
        quantityPerTest:
          r.quantityPerTest != null ? String(r.quantityPerTest) : "",
        quantityUnit: r.quantityUnit || "",
      };
    });
    setEdits(next);
  }, [reagents]);

  const byId = {};
  reagents.forEach((r) => {
    byId[r.id] = r;
  });

  const setEdit = (id, field, value) => {
    setEdits((prev) => ({ ...prev, [id]: { ...prev[id], [field]: value } }));
  };

  const stockTagType = (link) => {
    const stock = link.currentStock != null ? link.currentStock : 0;
    if (stock <= 0) {
      return "red";
    }
    if (link.lowStockThreshold != null && stock <= link.lowStockThreshold) {
      return "yellow";
    }
    return "green";
  };

  // PUT the row (usage type / quantity / unit). Optimistic: the UI already shows
  // the edited values; on failure we roll the row back to the server snapshot.
  const saveRow = (id, overrides = {}) => {
    const link = byId[id];
    if (!link) {
      return;
    }
    const current = { ...edits[id], ...overrides };
    const unchanged =
      current.usageType === (link.usageType || "PRIMARY") &&
      String(current.quantityPerTest ?? "") ===
        (link.quantityPerTest != null ? String(link.quantityPerTest) : "") &&
      (current.quantityUnit || "") === (link.quantityUnit || "");
    if (unchanged) {
      return;
    }
    const payload = {
      usageType: current.usageType,
      quantityPerTest:
        current.quantityPerTest === "" || current.quantityPerTest == null
          ? null
          : Number(current.quantityPerTest),
      quantityUnit: current.quantityUnit || null,
    };
    putToOpenElisServer(
      `/rest/test-catalog/${testId}/reagents/${link.reagentId}`,
      JSON.stringify(payload),
      (status) => {
        if (status >= 200 && status < 300) {
          setReagents((prev) =>
            prev.map((r) => (r.id === id ? { ...r, ...payload } : r)),
          );
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.reagents.updated",
            }),
          });
        } else {
          // Roll the edit back to the last saved server values.
          setEdits((prev) => ({
            ...prev,
            [id]: {
              usageType: link.usageType || "PRIMARY",
              quantityPerTest:
                link.quantityPerTest != null
                  ? String(link.quantityPerTest)
                  : "",
              quantityUnit: link.quantityUnit || "",
            },
          }));
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.reagents.saveError",
            }),
            retryId: id,
          });
        }
      },
    );
  };

  const confirmUnlink = () => {
    const link = unlinkTarget;
    if (!link) {
      return;
    }
    deleteFromOpenElisServer(
      `/rest/test-catalog/${testId}/reagents/${link.reagentId}`,
      (status) => {
        setUnlinkTarget(null);
        if (status >= 200 && status < 300) {
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.reagents.unlinked",
            }),
          });
          load();
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.reagents.unlinkError",
            }),
          });
        }
      },
    );
  };

  const headers = [
    {
      key: "reagentName",
      header: intl.formatMessage({ id: "label.testCatalog.reagents.col.name" }),
    },
    {
      key: "usageType",
      header: intl.formatMessage({
        id: "label.testCatalog.reagents.col.usage",
      }),
    },
    {
      key: "quantityPerTest",
      header: intl.formatMessage({ id: "label.testCatalog.reagents.col.qty" }),
    },
    {
      key: "stock",
      header: intl.formatMessage({
        id: "label.testCatalog.reagents.col.stock",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.testCatalog.reagents.col.actions",
      }),
    },
  ];

  // DataTable drives column sorting; sort keys are the server-side values.
  // Editable cells below read from `edits`/`byId`, keyed by the stable row id.
  const rows = reagents.map((link) => ({
    id: link.id,
    reagentName: link.reagentName,
    usageType: link.usageType,
    quantityPerTest: link.quantityPerTest != null ? link.quantityPerTest : "",
    stock: link.currentStock != null ? link.currentStock : 0,
    actions: "",
  }));

  if (loading) {
    return (
      <DataTableSkeleton
        columnCount={headers.length}
        rowCount={3}
        showHeader={false}
        showToolbar={false}
        data-testid="reagents-skeleton"
      />
    );
  }

  if (error) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.reagents.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="reagents-section">
      {notification && (
        <InlineNotification
          kind={notification.kind}
          lowContrast
          title={notification.text}
          actionButtonLabel={
            notification.retryId
              ? intl.formatMessage({ id: "label.testCatalog.reagents.retry" })
              : undefined
          }
          onActionButtonClick={
            notification.retryId
              ? () => {
                  const id = notification.retryId;
                  setNotification(null);
                  saveRow(id);
                }
              : undefined
          }
          onCloseButtonClick={() => setNotification(null)}
        />
      )}

      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          onClick={() => setLinkModalOpen(true)}
          data-testid="link-reagent-button"
        >
          {intl.formatMessage({ id: "label.testCatalog.reagents.linkButton" })}
        </Button>
      </div>

      {reagents.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.testCatalog.reagents.empty" })}
        />
      ) : (
        <DataTable rows={rows} headers={headers} isSortable>
          {({
            rows: sortedRows,
            headers: hdrs,
            getHeaderProps,
            getTableProps,
          }) => (
            <TableContainer>
              <Table {...getTableProps()} aria-label="reagents">
                <TableHead>
                  <TableRow>
                    {hdrs.map((header) => {
                      const { key, ...headerProps } = getHeaderProps({
                        header,
                      });
                      return (
                        <TableHeader key={key} {...headerProps}>
                          {header.header}
                        </TableHeader>
                      );
                    })}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sortedRows.map((row) => {
                    const link = byId[row.id];
                    const e = edits[row.id] || {};
                    if (!link) {
                      return null;
                    }
                    return (
                      <TableRow
                        key={row.id}
                        data-testid={`reagent-row-${link.id}`}
                      >
                        <TableCell>{link.reagentName}</TableCell>
                        <TableCell>
                          <Select
                            id={`usage-${link.id}`}
                            labelText=""
                            size="sm"
                            value={e.usageType || "PRIMARY"}
                            onChange={(ev) => {
                              setEdit(link.id, "usageType", ev.target.value);
                              saveRow(link.id, { usageType: ev.target.value });
                            }}
                          >
                            <SelectItem value="PRIMARY" text="PRIMARY" />
                            <SelectItem value="SECONDARY" text="SECONDARY" />
                          </Select>
                        </TableCell>
                        <TableCell>
                          <div
                            style={{ display: "flex", gap: "0.5rem" }}
                            onBlur={() => saveRow(link.id)}
                          >
                            <NumberInput
                              id={`qty-${link.id}`}
                              label=""
                              hideLabel
                              size="sm"
                              min={0}
                              step={0.000001}
                              allowEmpty
                              value={e.quantityPerTest ?? ""}
                              onChange={(ev) =>
                                setEdit(
                                  link.id,
                                  "quantityPerTest",
                                  ev.target.value,
                                )
                              }
                            />
                            <ComboBox
                              id={`unit-${link.id}`}
                              titleText=""
                              size="sm"
                              allowCustomValue
                              placeholder={intl.formatMessage({
                                id: "label.testCatalog.reagents.unitPlaceholder",
                              })}
                              items={UNIT_OPTIONS}
                              selectedItem={e.quantityUnit || null}
                              onChange={({ selectedItem }) =>
                                setEdit(
                                  link.id,
                                  "quantityUnit",
                                  selectedItem || "",
                                )
                              }
                              onInputChange={(text) =>
                                setEdit(link.id, "quantityUnit", text || "")
                              }
                            />
                          </div>
                        </TableCell>
                        <TableCell>
                          <Tag type={stockTagType(link)}>
                            {link.currentStock != null ? link.currentStock : 0}
                          </Tag>
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={TrashCan}
                            iconDescription={intl.formatMessage({
                              id: "label.testCatalog.reagents.unlink.action",
                            })}
                            onClick={() => setUnlinkTarget(link)}
                            data-testid={`unlink-${link.id}`}
                          />
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}

      <LinkReagentModal
        open={linkModalOpen}
        onClose={() => setLinkModalOpen(false)}
        testId={testId}
        linkedReagentIds={reagents.map((r) => r.reagentId)}
        onLinked={load}
      />

      <Modal
        open={!!unlinkTarget}
        danger
        modalHeading={intl.formatMessage({
          id: "label.testCatalog.reagents.unlink.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.testCatalog.reagents.unlink.confirm",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setUnlinkTarget(null)}
        onRequestSubmit={confirmUnlink}
      >
        {unlinkTarget &&
          intl.formatMessage(
            { id: "label.testCatalog.reagents.unlink.body" },
            { name: unlinkTarget.reagentName },
          )}
      </Modal>
    </Stack>
  );
};

export default ReagentsSection;
