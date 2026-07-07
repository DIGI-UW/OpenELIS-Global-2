import React, { useCallback, useEffect, useState } from "react";
import {
  Stack,
  Dropdown,
  Toggle,
  Checkbox,
  NumberInput,
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
  Button,
  InlineNotification,
} from "@carbon/react";
import { TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

/**
 * OGC-949 M14 / OGC-988 + OGC-989 (epic OGC-761) — Labels section.
 *
 * Per-test label presets (OGC-988): a table of the fixed system presets linked
 * to this test, with default/max quantity and an allow-override flag, plus the
 * global "allow override at order entry" toggle (OGC-989). Backed by the OGC-285
 * label-config API ({@code GET/PUT /rest/api/tests/{id}/labelConfig}) which
 * full-replaces the config, and the system presets from
 * {@code GET /api/labelPresets}.
 */
const LabelsSection = ({ testId }) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [allowOverride, setAllowOverride] = useState(true);
  const [links, setLinks] = useState([]);
  const [presets, setPresets] = useState([]);
  const [removeTarget, setRemoveTarget] = useState(null);
  const [notification, setNotification] = useState(null);

  const load = useCallback(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/api/tests/${testId}/labelConfig`, (res) => {
      setLoading(false);
      if (!res) {
        setError(true);
        return;
      }
      setAllowOverride(res.allowOrderEntryOverride !== false);
      setLinks(res.links || []);
    });
    // Fixed system presets feed the "Add Label Type" picker.
    getFromOpenElisServer("/api/labelPresets?status=ACTIVE", (res) => {
      setPresets((Array.isArray(res) ? res : []).filter((p) => p.isSystem));
    });
  }, [testId]);

  useEffect(() => {
    load();
  }, [load]);

  // Full-replace PUT of the whole config (toggle + links). Optimistic: callers
  // update local state first; on failure we reload from the server.
  const persist = (nextLinks, nextAllowOverride) => {
    const payload = {
      allowOrderEntryOverride: nextAllowOverride,
      links: nextLinks.map((l) => ({
        presetId: l.presetId,
        defaultQty: l.defaultQty,
        maxQty: l.maxQty,
        allowOverride: l.allowOverride,
      })),
    };
    putToOpenElisServer(
      `/rest/api/tests/${testId}/labelConfig`,
      JSON.stringify(payload),
      (status) => {
        if (status >= 200 && status < 300) {
          setNotification({
            kind: "success",
            text: intl.formatMessage({ id: "label.testCatalog.labels.saved" }),
          });
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.labels.saveError",
            }),
          });
          load();
        }
      },
    );
  };

  const linkedPresetIds = new Set(links.map((l) => l.presetId));
  const available = presets.filter((p) => !linkedPresetIds.has(p.id));

  const addPreset = (preset) => {
    if (!preset) {
      return;
    }
    const next = [
      ...links,
      {
        presetId: preset.id,
        presetName: preset.name,
        defaultQty: 1,
        maxQty: 1,
        allowOverride: true,
      },
    ];
    setLinks(next);
    persist(next, allowOverride);
  };

  const setLinkField = (presetId, field, value) => {
    setLinks((prev) =>
      prev.map((l) => (l.presetId === presetId ? { ...l, [field]: value } : l)),
    );
  };

  const saveRow = (presetId) => {
    const link = links.find((l) => l.presetId === presetId);
    if (!link) {
      return;
    }
    if (link.maxQty < link.defaultQty) {
      setNotification({
        kind: "error",
        text: intl.formatMessage({
          id: "label.testCatalog.labels.maxLtDefault",
        }),
      });
      return;
    }
    persist(links, allowOverride);
  };

  const confirmRemove = () => {
    const target = removeTarget;
    setRemoveTarget(null);
    if (!target) {
      return;
    }
    const next = links.filter((l) => l.presetId !== target.presetId);
    setLinks(next);
    persist(next, allowOverride);
  };

  const toggleOverride = (checked) => {
    setAllowOverride(checked);
    persist(links, checked);
  };

  const headers = [
    {
      key: "presetName",
      header: intl.formatMessage({ id: "label.testCatalog.labels.col.preset" }),
    },
    {
      key: "defaultQty",
      header: intl.formatMessage({
        id: "label.testCatalog.labels.col.defaultQty",
      }),
    },
    {
      key: "maxQty",
      header: intl.formatMessage({ id: "label.testCatalog.labels.col.maxQty" }),
    },
    {
      key: "allowOverride",
      header: intl.formatMessage({
        id: "label.testCatalog.labels.col.allowOverride",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.testCatalog.labels.col.actions",
      }),
    },
  ];

  const rows = links.map((l) => ({
    id: String(l.presetId),
    presetName: l.presetName,
    defaultQty: l.defaultQty,
    maxQty: l.maxQty,
    allowOverride: l.allowOverride ? "1" : "0",
    actions: "",
  }));

  if (loading) {
    return (
      <DataTableSkeleton
        columnCount={headers.length}
        rowCount={3}
        showHeader={false}
        showToolbar={false}
        data-testid="labels-skeleton"
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
          id: "label.testCatalog.labels.loadError",
        })}
      />
    );
  }

  const byPresetId = {};
  links.forEach((l) => {
    byPresetId[l.presetId] = l;
  });

  return (
    <Stack gap={6} data-testid="labels-section">
      {notification && (
        <InlineNotification
          kind={notification.kind}
          lowContrast
          title={notification.text}
          onCloseButtonClick={() => setNotification(null)}
        />
      )}

      <div style={{ maxWidth: "20rem" }}>
        <Dropdown
          id="add-label-preset"
          titleText={intl.formatMessage({
            id: "label.testCatalog.labels.addLabelType",
          })}
          label={intl.formatMessage({
            id: "label.testCatalog.labels.addLabelType.placeholder",
          })}
          items={available}
          itemToString={(item) => (item ? item.name : "")}
          selectedItem={null}
          disabled={available.length === 0}
          onChange={({ selectedItem }) => addPreset(selectedItem)}
        />
      </div>

      {links.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.testCatalog.labels.empty" })}
        />
      ) : (
        <DataTable rows={rows} headers={headers}>
          {({ rows: dtRows, headers: hdrs, getHeaderProps, getTableProps }) => (
            <TableContainer>
              <Table {...getTableProps()} aria-label="labels">
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
                    const link = byPresetId[row.id];
                    if (!link) {
                      return null;
                    }
                    return (
                      <TableRow
                        key={row.id}
                        data-testid={`label-row-${row.id}`}
                      >
                        <TableCell>
                          <Tag type="teal">{link.presetName}</Tag>
                        </TableCell>
                        <TableCell>
                          <NumberInput
                            id={`default-${row.id}`}
                            label=""
                            hideLabel
                            size="sm"
                            min={0}
                            max={999}
                            value={link.defaultQty}
                            onChange={(ev) =>
                              setLinkField(
                                link.presetId,
                                "defaultQty",
                                Number(ev.target.value || 0),
                              )
                            }
                            onBlur={() => saveRow(link.presetId)}
                          />
                        </TableCell>
                        <TableCell>
                          <NumberInput
                            id={`max-${row.id}`}
                            label=""
                            hideLabel
                            size="sm"
                            min={0}
                            max={999}
                            value={link.maxQty}
                            onChange={(ev) =>
                              setLinkField(
                                link.presetId,
                                "maxQty",
                                Number(ev.target.value || 0),
                              )
                            }
                            onBlur={() => saveRow(link.presetId)}
                          />
                        </TableCell>
                        <TableCell>
                          <Checkbox
                            id={`override-${row.id}`}
                            labelText=""
                            checked={!!link.allowOverride}
                            onChange={(_e, { checked }) => {
                              setLinkField(
                                link.presetId,
                                "allowOverride",
                                checked,
                              );
                              persist(
                                links.map((l) =>
                                  l.presetId === link.presetId
                                    ? { ...l, allowOverride: checked }
                                    : l,
                                ),
                                allowOverride,
                              );
                            }}
                          />
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            renderIcon={TrashCan}
                            iconDescription={intl.formatMessage({
                              id: "label.testCatalog.labels.remove.action",
                            })}
                            onClick={() => setRemoveTarget(link)}
                            data-testid={`remove-label-${row.id}`}
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

      <Toggle
        id="allow-order-entry-override"
        labelText={intl.formatMessage({
          id: "label.testCatalog.labels.allowOverride.label",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={allowOverride}
        onToggle={toggleOverride}
      />

      <Modal
        open={!!removeTarget}
        danger
        modalHeading={intl.formatMessage({
          id: "label.testCatalog.labels.remove.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.testCatalog.labels.remove.confirm",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setRemoveTarget(null)}
        onRequestSubmit={confirmRemove}
      >
        {removeTarget &&
          intl.formatMessage(
            { id: "label.testCatalog.labels.remove.body" },
            { name: removeTarget.presetName },
          )}
      </Modal>
    </Stack>
  );
};

export default LabelsSection;
