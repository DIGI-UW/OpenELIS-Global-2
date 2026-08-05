import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  OverflowMenu,
  OverflowMenuItem,
  Pagination,
  Section,
  Select,
  SelectItem,
  Table,
  TableBatchAction,
  TableBatchActions,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
} from "@carbon/react";
import { Add, Checkmark, Close, Download, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import ConfirmedBulkActionModal from "../../common/ConfirmedBulkActionModal";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  bulkAdminMacros,
  exportAdminMacros,
  getAdminMacro,
  getAdminMacroPage,
  saveAdminMacro,
} from "../../common/textMacro/TextMacroService";
import { downloadAttachment } from "../../utils/downloadAttachment";
import {
  buildMacroAdminRequestQuery,
  buildMacroLibraryQuery,
} from "./queryState";
import MacroEditorModal from "./MacroEditorModal";
import { createEmptyTextMacro, TEXT_MACRO_CONTEXTS } from "./textMacroConfig";
import { useMacroLibraryQuery } from "./useMacroLibraryQuery";
import "./textMacroAdmin.scss";

const MacroLibraryPage = () => {
  const intl = useIntl();
  const { query, setQuery, pathname } = useMacroLibraryQuery();
  const [page, setPage] = useState({ items: [], total: 0 });
  const [draft, setDraft] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [bulkSaving, setBulkSaving] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [bulkAction, setBulkAction] = useState(null);
  const [tableVersion, setTableVersion] = useState(0);
  const [error, setError] = useState("");

  const requestQuery = buildMacroAdminRequestQuery(query);
  const load = useCallback(
    (signal) => {
      setLoading(true);
      setError("");
      return getAdminMacroPage(requestQuery, signal)
        .then(setPage)
        .catch((requestError) => {
          if (requestError.name !== "AbortError")
            setError(requestError.message);
        })
        .finally(() => setLoading(false));
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
      setDraft(createEmptyTextMacro());
      return undefined;
    }
    const visible = page.items.find((item) => item.id === query.edit);
    if (visible) {
      setDraft(visible);
      return undefined;
    }
    const controller = new AbortController();
    getAdminMacro(query.edit, controller.signal)
      .then(setDraft)
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(requestError.message);
      });
    return () => controller.abort();
  }, [page.items, query.edit]);

  const closeEditor = () => setQuery({ edit: "" }, { replace: true });
  const save = async (editedMacro) => {
    setSaving(true);
    setError("");
    try {
      await saveAdminMacro(editedMacro);
      closeEditor();
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  };

  const beginBulkAction = (action, selectedMacros) => {
    const count = selectedMacros.length;
    const copy = {
      ACTIVATE: {
        title: intl.formatMessage(
          { id: "textMacro.bulk.confirmActivate" },
          { count },
        ),
        description: intl.formatMessage({
          id: "textMacro.bulk.activateDescription",
        }),
        confirmLabel: intl.formatMessage({
          id: "textMacro.bulk.activateConfirm",
        }),
      },
      DEACTIVATE: {
        title: intl.formatMessage(
          { id: "textMacro.bulk.confirmDeactivate" },
          { count },
        ),
        description: intl.formatMessage({
          id: "textMacro.bulk.deactivateDescription",
        }),
        confirmLabel: intl.formatMessage({
          id: "textMacro.bulk.deactivateConfirm",
        }),
      },
      DELETE_LOCAL: {
        title: intl.formatMessage(
          { id: "textMacro.bulk.confirmRemove" },
          { count },
        ),
        description: intl.formatMessage({
          id: "textMacro.bulk.removeDescription",
        }),
        confirmLabel: intl.formatMessage({
          id: "textMacro.bulk.removeConfirm",
        }),
        danger: true,
      },
    }[action];
    setBulkAction({
      action,
      ids: selectedMacros.map((macro) => macro.id),
      codes: selectedMacros.map((macro) => macro.code).sort(),
      ...copy,
    });
  };

  const confirmBulkAction = async () => {
    if (!bulkAction) return;
    setBulkSaving(true);
    setError("");
    try {
      await bulkAdminMacros({
        action: bulkAction.action,
        ids: bulkAction.ids,
      });
      setBulkAction(null);
      setTableVersion((version) => version + 1);
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBulkSaving(false);
    }
  };

  const exportMacros = async () => {
    setExporting(true);
    setError("");
    try {
      const { blob, filename } = await exportAdminMacros();
      downloadAttachment(blob, filename);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setExporting(false);
    }
  };

  const headers = [
    { key: "code", header: intl.formatMessage({ id: "textMacro.code" }) },
    {
      key: "expansionText",
      header: intl.formatMessage({ id: "textMacro.text" }),
    },
    {
      key: "contextDisplay",
      header: intl.formatMessage({ id: "textMacro.contexts" }),
    },
    {
      key: "provenance",
      header: intl.formatMessage({ id: "textMacro.provenance" }),
    },
    { key: "status", header: intl.formatMessage({ id: "textMacro.status" }) },
    { key: "actions", header: "" },
  ];
  const rows = page.items.map((macro) => ({
    ...macro,
    contextDisplay: macro.contexts
      .map((context) =>
        intl.formatMessage({ id: `textMacro.context.${context}` }),
      )
      .join(", "),
    status: macro.active ? "active" : "inactive",
    actions: macro.id,
  }));
  const breadcrumbs = [
    { label: "home.label", link: "/Dashboard" },
    { label: "breadcrums.admin.managment", link: "/admin" },
    {
      label: "textMacro.admin.title",
      link: `${pathname}?${buildMacroLibraryQuery({ ...query, edit: "" })}`,
      isCurrentPage: true,
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth className="text-macro-admin">
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {intl.formatMessage({ id: "textMacro.admin.title" })}
            </Heading>
            <p className="text-macro-admin__subtitle">
              {intl.formatMessage({ id: "textMacro.admin.subtitle" })}
            </p>
          </Section>
          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "textMacro.error" })}
              subtitle={error}
            />
          )}
          {loading && page.items.length === 0 ? (
            <Loading withOverlay={false} />
          ) : (
            <DataTable key={tableVersion} rows={rows} headers={headers}>
              {({
                rows: renderedRows,
                headers: renderedHeaders,
                getBatchActionProps,
                getSelectionProps,
                getTableProps,
                getRowProps,
                selectedRows,
              }) => {
                const selectedMacros = selectedRows
                  .map((selected) =>
                    page.items.find((item) => item.id === selected.id),
                  )
                  .filter(Boolean);
                const includesPackaged = selectedMacros.some(
                  (macro) => macro.provenance !== "LOCAL",
                );
                return (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "textMacro.admin.tableTitle",
                    })}
                    description={intl.formatMessage({
                      id: "textMacro.admin.tableDescription",
                    })}
                  >
                    <TableToolbar>
                      <TableBatchActions {...getBatchActionProps()}>
                        <TableBatchAction
                          renderIcon={Checkmark}
                          onClick={() =>
                            beginBulkAction("ACTIVATE", selectedMacros)
                          }
                        >
                          {intl.formatMessage({
                            id: "textMacro.bulk.activate",
                          })}
                        </TableBatchAction>
                        <TableBatchAction
                          renderIcon={Close}
                          onClick={() =>
                            beginBulkAction("DEACTIVATE", selectedMacros)
                          }
                        >
                          {intl.formatMessage({
                            id: "textMacro.bulk.deactivate",
                          })}
                        </TableBatchAction>
                        <TableBatchAction
                          renderIcon={TrashCan}
                          disabled={includesPackaged}
                          aria-label={intl.formatMessage({
                            id: includesPackaged
                              ? "textMacro.bulk.removeUnavailable"
                              : "textMacro.bulk.remove",
                          })}
                          onClick={() =>
                            beginBulkAction("DELETE_LOCAL", selectedMacros)
                          }
                        >
                          {intl.formatMessage({ id: "textMacro.bulk.remove" })}
                        </TableBatchAction>
                      </TableBatchActions>
                      <TableToolbarContent>
                        <TableToolbarSearch
                          persistent
                          value={query.q}
                          placeholder={intl.formatMessage({
                            id: "textMacro.search",
                          })}
                          onChange={(event) =>
                            setQuery({ q: event.target.value })
                          }
                        />
                        <Select
                          id="text-macro-context-filter"
                          hideLabel
                          labelText={intl.formatMessage({
                            id: "textMacro.contexts",
                          })}
                          value={query.context}
                          onChange={(event) =>
                            setQuery({ context: event.target.value })
                          }
                        >
                          <SelectItem
                            value="all"
                            text={intl.formatMessage({
                              id: "textMacro.context.all",
                            })}
                          />
                          {TEXT_MACRO_CONTEXTS.map((context) => (
                            <SelectItem
                              key={context}
                              value={context}
                              text={intl.formatMessage({
                                id: `textMacro.context.${context}`,
                              })}
                            />
                          ))}
                        </Select>
                        <Select
                          id="text-macro-status-filter"
                          hideLabel
                          labelText={intl.formatMessage({
                            id: "textMacro.status",
                          })}
                          value={query.status}
                          onChange={(event) =>
                            setQuery({ status: event.target.value })
                          }
                        >
                          <SelectItem
                            value="active"
                            text={intl.formatMessage({
                              id: "textMacro.status.active",
                            })}
                          />
                          <SelectItem
                            value="inactive"
                            text={intl.formatMessage({
                              id: "textMacro.status.inactive",
                            })}
                          />
                          <SelectItem
                            value="all"
                            text={intl.formatMessage({
                              id: "textMacro.status.all",
                            })}
                          />
                        </Select>
                        <Select
                          id="text-macro-sort"
                          hideLabel
                          labelText={intl.formatMessage({
                            id: "textMacro.sort",
                          })}
                          value={query.sort}
                          onChange={(event) =>
                            setQuery({ sort: event.target.value })
                          }
                        >
                          <SelectItem
                            value="code:asc"
                            text={intl.formatMessage({
                              id: "textMacro.sort.codeAsc",
                            })}
                          />
                          <SelectItem
                            value="code:desc"
                            text={intl.formatMessage({
                              id: "textMacro.sort.codeDesc",
                            })}
                          />
                          <SelectItem
                            value="updated:desc"
                            text={intl.formatMessage({
                              id: "textMacro.sort.updatedDesc",
                            })}
                          />
                          <SelectItem
                            value="updated:asc"
                            text={intl.formatMessage({
                              id: "textMacro.sort.updatedAsc",
                            })}
                          />
                        </Select>
                        <Button
                          kind="ghost"
                          renderIcon={Download}
                          iconDescription={intl.formatMessage({
                            id: "textMacro.export",
                          })}
                          disabled={exporting}
                          onClick={exportMacros}
                        >
                          {intl.formatMessage({ id: "textMacro.export" })}
                        </Button>
                        <Button
                          renderIcon={Add}
                          onClick={() => setQuery({ edit: "new" })}
                        >
                          {intl.formatMessage({ id: "textMacro.add" })}
                        </Button>
                      </TableToolbarContent>
                    </TableToolbar>
                    <Table
                      {...getTableProps()}
                      className="text-macro-admin__table"
                      size="lg"
                      useZebraStyles
                    >
                      <TableHead>
                        <TableRow>
                          <TableSelectAll
                            {...getSelectionProps()}
                            aria-label={intl.formatMessage({
                              id: "textMacro.selectAll",
                            })}
                          />
                          {renderedHeaders.map((header) => (
                            <TableHeader key={header.key}>
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {renderedRows.map((row) => {
                          const source = page.items.find(
                            (item) => item.id === row.id,
                          );
                          return (
                            <TableRow {...getRowProps({ row })} key={row.id}>
                              <TableSelectRow
                                {...getSelectionProps({ row })}
                                aria-label={intl.formatMessage(
                                  { id: "textMacro.selectRow" },
                                  { code: source.code },
                                )}
                              />
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.info.header === "status" ? (
                                    <Tag
                                      type={source.active ? "green" : "gray"}
                                    >
                                      {intl.formatMessage({
                                        id: source.active
                                          ? "textMacro.status.active"
                                          : "textMacro.status.inactive",
                                      })}
                                    </Tag>
                                  ) : cell.info.header === "provenance" ? (
                                    intl.formatMessage({
                                      id: `textMacro.provenance.${String(cell.value).toLowerCase()}`,
                                    })
                                  ) : cell.info.header === "actions" ? (
                                    <OverflowMenu
                                      aria-label={intl.formatMessage({
                                        id: "textMacro.actions",
                                      })}
                                      iconDescription={intl.formatMessage({
                                        id: "textMacro.actions",
                                      })}
                                      flipped
                                    >
                                      <OverflowMenuItem
                                        itemText={intl.formatMessage({
                                          id: "button.edit",
                                        })}
                                        onClick={() =>
                                          setQuery({ edit: source.id })
                                        }
                                      />
                                    </OverflowMenu>
                                  ) : (
                                    cell.value || "—"
                                  )}
                                </TableCell>
                              ))}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                    {page.items.length === 0 && (
                      <div className="text-macro-admin__empty">
                        {intl.formatMessage({ id: "textMacro.empty" })}
                      </div>
                    )}
                    <Pagination
                      page={query.page}
                      pageSize={query.pageSize}
                      pageSizes={[10, 20, 50, 100]}
                      totalItems={page.total}
                      onChange={({ page: nextPage, pageSize }) =>
                        setQuery({ page: nextPage, pageSize })
                      }
                    />
                  </TableContainer>
                );
              }}
            </DataTable>
          )}
        </Column>
      </Grid>
      <MacroEditorModal
        open={Boolean(query.edit && draft)}
        mode={query.edit === "new" ? "create" : "edit"}
        value={draft}
        saving={saving}
        onClose={closeEditor}
        onSave={save}
      />
      <ConfirmedBulkActionModal
        open={Boolean(bulkAction)}
        danger={Boolean(bulkAction?.danger)}
        title={bulkAction?.title || ""}
        description={bulkAction?.description || ""}
        items={bulkAction?.codes || []}
        confirmLabel={bulkAction?.confirmLabel || ""}
        cancelLabel={intl.formatMessage({ id: "button.cancel" })}
        closeLabel={intl.formatMessage({ id: "label.button.close" })}
        working={bulkSaving}
        onClose={() => setBulkAction(null)}
        onConfirm={confirmBulkAction}
      />
    </>
  );
};

export default MacroLibraryPage;
