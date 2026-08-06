import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  ComboBox,
  ComposedModal,
  DataTable,
  InlineNotification,
  Loading,
  ModalBody,
  ModalFooter,
  ModalHeader,
  OverflowMenu,
  OverflowMenuItem,
  Pagination,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  TextInput,
} from "@carbon/react";
import { Add, ArrowDown, ArrowUp, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getAstPanel,
  getReferenceOptions,
  getReferencePage,
  publishAstPanel,
} from "./api";
import { buildReferenceRequestQuery } from "./queryState";

const emptyPanel = {
  name: "",
  workflowType: "BACTERIOLOGY",
  organismGroup: "",
  specimenTypeId: "",
  active: true,
  antibiotics: [],
};

const AstPanelPage = ({ query, setQuery }) => {
  const intl = useIntl();
  const [page, setPage] = useState({ rows: [], total: 0 });
  const [antibiotics, setAntibiotics] = useState([]);
  const [draft, setDraft] = useState(null);
  const [selectedAntibiotic, setSelectedAntibiotic] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [confirmingPublish, setConfirmingPublish] = useState(false);
  const [error, setError] = useState("");
  const requestQuery = buildReferenceRequestQuery(query);

  const load = useCallback(
    async (signal) => {
      setLoading(true);
      try {
        const [panels, antibioticOptions] = await Promise.all([
          getReferencePage("ast-panels", requestQuery, signal),
          getReferenceOptions("antibiotics", signal),
        ]);
        setPage(panels);
        setAntibiotics(
          (antibioticOptions || []).map((option) => ({
            id: option.id,
            displayName: option.label,
            whonetCode: option.code,
          })),
        );
        setError("");
      } catch (requestError) {
        if (requestError.name !== "AbortError") setError(requestError.message);
      } finally {
        setLoading(false);
      }
    },
    [requestQuery],
  );

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  useEffect(() => {
    if (!query.edit) {
      setDraft(null);
      return undefined;
    }
    if (query.edit === "new") {
      setDraft({ ...emptyPanel, antibiotics: [] });
      return undefined;
    }
    const controller = new AbortController();
    getAstPanel(query.edit, controller.signal)
      .then((panel) => setDraft(panel))
      .catch((requestError) => setError(requestError.message));
    return () => controller.abort();
  }, [query.edit]);

  const headers = useMemo(
    () => [
      {
        key: "name",
        header: intl.formatMessage({ id: "microbiology.admin.field.name" }),
      },
      {
        key: "workflowType",
        header: intl.formatMessage({
          id: "microbiology.admin.field.workflow",
        }),
      },
      {
        key: "version",
        header: intl.formatMessage({
          id: "microbiology.admin.astPanels.version",
        }),
      },
      {
        key: "status",
        header: intl.formatMessage({ id: "microbiology.admin.status" }),
      },
      { key: "actions", header: "" },
    ],
    [intl],
  );

  const rows = page.rows.map((panel) => ({
    id: panel.id,
    name: panel.name,
    workflowType: panel.workflowType,
    version: `v${panel.versionNumber}`,
    status: { current: panel.current, active: panel.active },
    actions: { id: panel.id, current: panel.current },
  }));

  const updateDraft = (updates) =>
    setDraft((current) => ({ ...current, ...updates }));

  const updateRow = (index, updates) =>
    updateDraft({
      antibiotics: draft.antibiotics.map((row, rowIndex) =>
        rowIndex === index ? { ...row, ...updates } : row,
      ),
    });

  const moveRow = (index, direction) => {
    const nextIndex = index + direction;
    if (nextIndex < 0 || nextIndex >= draft.antibiotics.length) return;
    const next = [...draft.antibiotics];
    [next[index], next[nextIndex]] = [next[nextIndex], next[index]];
    updateDraft({ antibiotics: next });
  };

  const addAntibiotic = () => {
    if (
      !selectedAntibiotic ||
      draft.antibiotics.some(
        (row) => row.antibioticId === selectedAntibiotic.id,
      )
    ) {
      return;
    }
    updateDraft({
      antibiotics: [
        ...draft.antibiotics,
        {
          antibioticId: selectedAntibiotic.id,
          antibioticName: selectedAntibiotic.displayName,
          whonetCode: selectedAntibiotic.whonetCode,
          tier: 1,
          reportBehavior: "ALWAYS",
        },
      ],
    });
    setSelectedAntibiotic(null);
  };

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      await publishAstPanel(draft);
      setConfirmingPublish(false);
      setQuery({ edit: "" }, { replace: true });
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  };

  const closeEditor = () => {
    setConfirmingPublish(false);
    setQuery({ edit: "" }, { replace: true });
  };

  const readOnly = !!draft?.id && draft.current === false;

  if (loading && page.rows.length === 0) return <Loading withOverlay={false} />;

  return (
    <div className="microbiology-admin__page">
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "microbiology.admin.error.title" })}
          subtitle={error}
        />
      )}
      <DataTable rows={rows} headers={headers}>
        {({ rows: tableRows, headers: tableHeaders, getRowProps }) => (
          <TableContainer
            title={intl.formatMessage({
              id: "microbiology.admin.astPanels.title",
            })}
          >
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  persistent
                  value={query.q}
                  placeholder={intl.formatMessage({
                    id: "microbiology.admin.search",
                  })}
                  onChange={(event) => setQuery({ q: event.target.value })}
                />
                <Select
                  id="microbiology-panel-status"
                  hideLabel
                  labelText={intl.formatMessage({
                    id: "microbiology.admin.status",
                  })}
                  value={query.status}
                  onChange={(event) => setQuery({ status: event.target.value })}
                >
                  <SelectItem
                    value="ALL"
                    text={intl.formatMessage({
                      id: "microbiology.admin.status.all",
                    })}
                  />
                  <SelectItem
                    value="ACTIVE"
                    text={intl.formatMessage({
                      id: "microbiology.admin.status.active",
                    })}
                  />
                  <SelectItem
                    value="INACTIVE"
                    text={intl.formatMessage({
                      id: "microbiology.admin.status.inactive",
                    })}
                  />
                </Select>
                <Select
                  id="microbiology-panel-workflow-filter"
                  hideLabel
                  labelText={intl.formatMessage({
                    id: "microbiology.admin.field.workflow",
                  })}
                  value={query.workflow}
                  onChange={(event) =>
                    setQuery({ workflow: event.target.value })
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "microbiology.admin.workflow.all",
                    })}
                  />
                  <SelectItem
                    value="BACTERIOLOGY"
                    text={intl.formatMessage({
                      id: "microbiology.workflow.bacteriology",
                    })}
                  />
                  <SelectItem
                    value="MYCOBACTERIOLOGY"
                    text={intl.formatMessage({
                      id: "microbiology.workflow.mycobacteriology",
                    })}
                  />
                </Select>
                <Select
                  id="microbiology-panel-sort"
                  hideLabel
                  labelText={intl.formatMessage({
                    id: "microbiology.admin.sort",
                  })}
                  value={query.sort}
                  onChange={(event) => setQuery({ sort: event.target.value })}
                >
                  <SelectItem
                    value="name"
                    text={intl.formatMessage({
                      id: "microbiology.admin.sort.nameAsc",
                    })}
                  />
                  <SelectItem
                    value="name-desc"
                    text={intl.formatMessage({
                      id: "microbiology.admin.sort.nameDesc",
                    })}
                  />
                </Select>
                <Button
                  renderIcon={Add}
                  onClick={() => setQuery({ edit: "new" })}
                >
                  {intl.formatMessage({
                    id: "microbiology.admin.astPanels.add",
                  })}
                </Button>
              </TableToolbarContent>
            </TableToolbar>
            <Table size="lg" useZebraStyles tabIndex={0}>
              <TableHead>
                <TableRow>
                  {tableHeaders.map((header) => (
                    <TableHeader key={header.key}>{header.header}</TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {tableRows.map((row) => (
                  <TableRow {...getRowProps({ row })} key={row.id}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>
                        {cell.info.header === "status" ? (
                          <div className="microbiology-admin__tag-stack">
                            <Tag type={cell.value.current ? "blue" : "gray"}>
                              {intl.formatMessage({
                                id: cell.value.current
                                  ? "microbiology.admin.astPanels.current"
                                  : "microbiology.admin.astPanels.historical",
                              })}
                            </Tag>
                            <Tag type={cell.value.active ? "green" : "red"}>
                              {intl.formatMessage({
                                id: cell.value.active
                                  ? "microbiology.admin.status.active"
                                  : "microbiology.admin.status.inactive",
                              })}
                            </Tag>
                          </div>
                        ) : cell.info.header === "actions" ? (
                          <OverflowMenu
                            flipped
                            aria-label={intl.formatMessage({
                              id: "microbiology.admin.actions",
                            })}
                          >
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: cell.value.current
                                  ? "microbiology.admin.astPanels.publishVersion"
                                  : "microbiology.admin.astPanels.viewVersion",
                              })}
                              onClick={() => setQuery({ edit: cell.value.id })}
                            />
                          </OverflowMenu>
                        ) : (
                          cell.value || "—"
                        )}
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Pagination
              page={query.page}
              pageSize={query.pageSize}
              pageSizes={[20, 50, 100]}
              totalItems={page.total}
              onChange={({ page: nextPage, pageSize }) =>
                setQuery({ page: nextPage, pageSize })
              }
            />
          </TableContainer>
        )}
      </DataTable>
      <ComposedModal
        open={!!draft && !confirmingPublish}
        size="lg"
        onClose={closeEditor}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: draft?.id
              ? "microbiology.admin.astPanels.publishVersion"
              : "microbiology.admin.astPanels.add",
          })}
          closeModal={closeEditor}
        />
        <ModalBody>
          {draft?.id && (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.admin.astPanels.versionNotice",
              })}
            />
          )}
          {readOnly && (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.admin.astPanels.readOnly",
              })}
            />
          )}
          {draft && (
            <div className="microbiology-admin__form">
              <TextInput
                id="microbiology-panel-name"
                labelText={intl.formatMessage({
                  id: "microbiology.admin.field.name",
                })}
                value={draft.name}
                disabled={readOnly}
                onChange={(event) => updateDraft({ name: event.target.value })}
              />
              <Select
                id="microbiology-panel-workflow"
                labelText={intl.formatMessage({
                  id: "microbiology.admin.field.workflow",
                })}
                value={draft.workflowType}
                disabled={readOnly}
                onChange={(event) =>
                  updateDraft({ workflowType: event.target.value })
                }
              >
                <SelectItem
                  value="BACTERIOLOGY"
                  text={intl.formatMessage({
                    id: "microbiology.workflow.bacteriology",
                  })}
                />
                <SelectItem
                  value="MYCOBACTERIOLOGY_TB"
                  text={intl.formatMessage({
                    id: "microbiology.workflow.mycobacteriology",
                  })}
                />
              </Select>
              <Checkbox
                id="microbiology-panel-active"
                labelText={intl.formatMessage({
                  id: "microbiology.admin.field.active",
                })}
                checked={draft.active}
                disabled={readOnly}
                onChange={(_, detail) =>
                  updateDraft({ active: detail.checked })
                }
              />
              <div className="microbiology-admin__panel-add">
                <ComboBox
                  id="microbiology-panel-antibiotic"
                  titleText={intl.formatMessage({
                    id: "microbiology.admin.astPanels.antibiotic",
                  })}
                  items={antibiotics}
                  selectedItem={selectedAntibiotic}
                  disabled={readOnly}
                  itemToString={(item) =>
                    item ? `${item.displayName} (${item.whonetCode})` : ""
                  }
                  onChange={({ selectedItem }) =>
                    setSelectedAntibiotic(selectedItem)
                  }
                />
                <Button
                  kind="secondary"
                  renderIcon={Add}
                  disabled={readOnly}
                  onClick={addAntibiotic}
                >
                  {intl.formatMessage({ id: "button.add" })}
                </Button>
              </div>
              <div className="microbiology-admin__panel-rows">
                {draft.antibiotics.map((row, index) => (
                  <div
                    className="microbiology-admin__panel-row"
                    key={row.antibioticId}
                  >
                    <div>
                      <strong>{row.antibioticName}</strong>
                      <div>{row.whonetCode}</div>
                    </div>
                    <Select
                      id={`microbiology-tier-${row.antibioticId}`}
                      labelText={intl.formatMessage({
                        id: "microbiology.admin.astPanels.tier",
                      })}
                      value={row.tier}
                      disabled={readOnly}
                      onChange={(event) =>
                        updateRow(index, { tier: Number(event.target.value) })
                      }
                    >
                      {[1, 2, 3].map((tier) => (
                        <SelectItem
                          key={tier}
                          value={tier}
                          text={String(tier)}
                        />
                      ))}
                    </Select>
                    <Select
                      id={`microbiology-report-${row.antibioticId}`}
                      labelText={intl.formatMessage({
                        id: "microbiology.admin.astPanels.reportBehavior",
                      })}
                      value={row.reportBehavior}
                      disabled={readOnly}
                      onChange={(event) =>
                        updateRow(index, { reportBehavior: event.target.value })
                      }
                    >
                      <SelectItem
                        value="ALWAYS"
                        text={intl.formatMessage({
                          id: "microbiology.admin.astPanels.behavior.always",
                        })}
                      />
                      <SelectItem
                        value="CASCADE"
                        text={intl.formatMessage({
                          id: "microbiology.admin.astPanels.behavior.cascade",
                        })}
                      />
                      <SelectItem
                        value="SUPPRESS_UNLESS_RESISTANT"
                        text={intl.formatMessage({
                          id: "microbiology.admin.astPanels.behavior.resistant",
                        })}
                      />
                    </Select>
                    <div className="microbiology-admin__row-actions">
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={ArrowUp}
                        iconDescription={intl.formatMessage({
                          id: "microbiology.admin.action.moveUp",
                        })}
                        disabled={readOnly || index === 0}
                        onClick={() => moveRow(index, -1)}
                      />
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={ArrowDown}
                        iconDescription={intl.formatMessage({
                          id: "microbiology.admin.action.moveDown",
                        })}
                        disabled={
                          readOnly || index === draft.antibiotics.length - 1
                        }
                        onClick={() => moveRow(index, 1)}
                      />
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={TrashCan}
                        iconDescription={intl.formatMessage({
                          id: "button.remove",
                        })}
                        disabled={readOnly}
                        onClick={() =>
                          updateDraft({
                            antibiotics: draft.antibiotics.filter(
                              (_, rowIndex) => rowIndex !== index,
                            ),
                          })
                        }
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </ModalBody>
        <ModalFooter
          primaryButtonText={intl.formatMessage({
            id: draft?.id
              ? "microbiology.admin.astPanels.publishVersion"
              : "button.save",
          })}
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          primaryButtonDisabled={
            readOnly ||
            saving ||
            !draft?.name ||
            draft?.antibiotics.length === 0
          }
          onRequestSubmit={() =>
            draft?.id ? setConfirmingPublish(true) : save()
          }
          onRequestClose={closeEditor}
        />
      </ComposedModal>
      <ComposedModal
        open={confirmingPublish}
        size="sm"
        danger={!draft?.active}
        onClose={() => setConfirmingPublish(false)}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "microbiology.admin.astPanels.confirmTitle",
          })}
          closeModal={() => setConfirmingPublish(false)}
        />
        <ModalBody>
          {intl.formatMessage(
            {
              id: draft?.active
                ? "microbiology.admin.astPanels.confirmPublish"
                : "microbiology.admin.astPanels.confirmDeactivate",
            },
            { version: (draft?.versionNumber || 0) + 1 },
          )}
        </ModalBody>
        <ModalFooter
          primaryButtonText={intl.formatMessage({
            id: "microbiology.admin.astPanels.publishVersion",
          })}
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          primaryButtonDisabled={saving}
          onRequestSubmit={save}
          onRequestClose={() => setConfirmingPublish(false)}
        />
      </ComposedModal>
    </div>
  );
};

export default AstPanelPage;
