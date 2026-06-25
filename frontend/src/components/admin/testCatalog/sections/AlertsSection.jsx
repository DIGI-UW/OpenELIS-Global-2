import React, { useCallback, useEffect, useState } from "react";
import {
  Stack,
  Button,
  Toggle,
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
import { Add, Edit, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
  deleteFromOpenElisServer,
} from "../../../utils/Utils";
import AlertRuleModal from "./AlertRuleModal";

/**
 * OGC-949 / OGC-994 (epic OGC-763) — Alerts section: per-test alert rules.
 *
 * Lists the rules (GET /rest/test-catalog/{testId}/alerts) with trigger,
 * channels, acknowledgment flag and an inline enable/disable toggle; add/edit
 * via {@link AlertRuleModal} (OGC-995/996/997); delete with confirmation.
 */
const AlertsSection = ({ testId }) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [rules, setRules] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [notification, setNotification] = useState(null);

  const load = useCallback(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/test-catalog/${testId}/alerts`, (res) => {
      setLoading(false);
      if (!res) {
        setError(true);
        return;
      }
      setRules(res);
    });
  }, [testId]);

  useEffect(() => {
    load();
  }, [load]);

  const byId = {};
  rules.forEach((r) => {
    byId[r.id] = r;
  });

  const channelsText = (r) => {
    const ch = [];
    if (r.notifySms) {
      ch.push(
        intl.formatMessage({ id: "label.testCatalog.alerts.channel.sms" }),
      );
    }
    if (r.notifyEmail) {
      ch.push(
        intl.formatMessage({ id: "label.testCatalog.alerts.channel.email" }),
      );
    }
    return ch.length ? ch.join(", ") : "—";
  };

  const toggleEnabled = (rule, checked) => {
    putToOpenElisServer(
      `/rest/test-catalog/${testId}/alerts/${rule.id}`,
      JSON.stringify({ ...rule, enabled: checked }),
      (status) => {
        if (status >= 200 && status < 300) {
          setRules((prev) =>
            prev.map((r) =>
              r.id === rule.id ? { ...r, enabled: checked } : r,
            ),
          );
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.alerts.saveError",
            }),
          });
          load();
        }
      },
    );
  };

  const confirmDelete = () => {
    const target = deleteTarget;
    setDeleteTarget(null);
    if (!target) {
      return;
    }
    deleteFromOpenElisServer(
      `/rest/test-catalog/${testId}/alerts/${target.id}`,
      (status) => {
        if (status >= 200 && status < 300) {
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.alerts.deleted",
            }),
          });
          load();
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.alerts.deleteError",
            }),
          });
        }
      },
    );
  };

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "label.testCatalog.alerts.col.name" }),
    },
    {
      key: "trigger",
      header: intl.formatMessage({
        id: "label.testCatalog.alerts.col.trigger",
      }),
    },
    {
      key: "channels",
      header: intl.formatMessage({
        id: "label.testCatalog.alerts.col.channels",
      }),
    },
    {
      key: "ack",
      header: intl.formatMessage({ id: "label.testCatalog.alerts.col.ack" }),
    },
    {
      key: "enabled",
      header: intl.formatMessage({
        id: "label.testCatalog.alerts.col.enabled",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.testCatalog.alerts.col.actions",
      }),
    },
  ];

  const rows = rules.map((r) => ({
    id: r.id,
    name: r.name,
    trigger: r.triggerType,
    channels: channelsText(r),
    ack: r.acknowledgmentRequired ? "1" : "0",
    enabled: r.enabled ? "1" : "0",
    actions: "",
  }));

  if (loading) {
    return (
      <DataTableSkeleton
        columnCount={headers.length}
        rowCount={3}
        showHeader={false}
        showToolbar={false}
        data-testid="alerts-skeleton"
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
          id: "label.testCatalog.alerts.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="alerts-section">
      {notification && (
        <InlineNotification
          kind={notification.kind}
          lowContrast
          title={notification.text}
          onCloseButtonClick={() => setNotification(null)}
        />
      )}

      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          onClick={() => {
            setEditing(null);
            setModalOpen(true);
          }}
          data-testid="add-rule-button"
        >
          {intl.formatMessage({ id: "label.testCatalog.alerts.addRule" })}
        </Button>
      </div>

      {rules.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.testCatalog.alerts.empty" })}
        />
      ) : (
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows: dtRows, headers: hdrs, getHeaderProps, getTableProps }) => (
            <TableContainer>
              <Table {...getTableProps()} aria-label="alerts">
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
                  {dtRows.map((row) => {
                    const rule = byId[row.id];
                    if (!rule) {
                      return null;
                    }
                    return (
                      <TableRow
                        key={row.id}
                        data-testid={`alert-row-${row.id}`}
                      >
                        <TableCell>{rule.name}</TableCell>
                        <TableCell>
                          <Tag type="purple">
                            {intl.formatMessage({
                              id: `label.testCatalog.alerts.trigger.${rule.triggerType}`,
                            }) +
                              (rule.triggerType === "SPECIFIC_VALUE" &&
                              rule.triggerValue
                                ? `: ${rule.triggerValue}`
                                : "")}
                          </Tag>
                        </TableCell>
                        <TableCell>{channelsText(rule)}</TableCell>
                        <TableCell>
                          {rule.acknowledgmentRequired ? (
                            <Tag type="teal">
                              {intl.formatMessage({ id: "label.yes" })}
                            </Tag>
                          ) : (
                            <Tag type="gray">
                              {intl.formatMessage({ id: "label.no" })}
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell>
                          <Toggle
                            id={`enabled-${row.id}`}
                            size="sm"
                            hideLabel
                            labelText={intl.formatMessage({
                              id: "label.testCatalog.alerts.col.enabled",
                            })}
                            labelA=""
                            labelB=""
                            toggled={!!rule.enabled}
                            onToggle={(checked) => toggleEnabled(rule, checked)}
                          />
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={Edit}
                            iconDescription={intl.formatMessage({
                              id: "label.testCatalog.alerts.edit.action",
                            })}
                            onClick={() => {
                              setEditing(rule);
                              setModalOpen(true);
                            }}
                            data-testid={`edit-alert-${row.id}`}
                          />
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={TrashCan}
                            iconDescription={intl.formatMessage({
                              id: "label.testCatalog.alerts.delete.action",
                            })}
                            onClick={() => setDeleteTarget(rule)}
                            data-testid={`delete-alert-${row.id}`}
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

      <AlertRuleModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        testId={testId}
        rule={editing}
        onSaved={() => {
          setNotification({
            kind: "success",
            text: intl.formatMessage({ id: "label.testCatalog.alerts.saved" }),
          });
          load();
        }}
      />

      <Modal
        open={!!deleteTarget}
        danger
        modalHeading={intl.formatMessage({
          id: "label.testCatalog.alerts.delete.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.testCatalog.alerts.delete.confirm",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setDeleteTarget(null)}
        onRequestSubmit={confirmDelete}
      >
        {deleteTarget &&
          intl.formatMessage(
            { id: "label.testCatalog.alerts.delete.body" },
            { name: deleteTarget.name },
          )}
      </Modal>
    </Stack>
  );
};

export default AlertsSection;
