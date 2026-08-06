import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
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
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getReferenceItem,
  getReferenceOptions,
  getReferencePage,
  saveReference,
  setReferenceActive,
} from "./api";
import { buildReferenceRequestQuery } from "./queryState";
import ReferenceEditModal from "./ReferenceEditModal";

const ReferenceDataPage = ({ definition, query, setQuery }) => {
  const intl = useIntl();
  const [page, setPage] = useState({ rows: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [detachedValue, setDetachedValue] = useState(null);
  const [optionLists, setOptionLists] = useState({});
  const readOnly = definition.readOnly === true;

  const deactivationId = query.edit?.startsWith("deactivate:")
    ? query.edit.slice("deactivate:".length)
    : "";
  const requestQuery = buildReferenceRequestQuery(query);

  const load = useCallback(
    (signal) => {
      return getReferencePage(definition.resource, requestQuery, signal)
        .then((response) => {
          setPage(response);
          setError("");
        })
        .catch((requestError) => {
          if (requestError.name !== "AbortError")
            setError(requestError.message);
        })
        .finally(() => setLoading(false));
    },
    [definition.resource, requestQuery],
  );

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  useEffect(() => {
    const optionResources = [
      ...new Set(
        definition.fields.map((field) => field.optionsResource).filter(Boolean),
      ),
    ];
    if (optionResources.length === 0) return undefined;
    const controller = new AbortController();
    Promise.all(
      optionResources.map(async (resource) => [
        resource,
        await getReferenceOptions(resource, controller.signal),
      ]),
    )
      .then((entries) => setOptionLists(Object.fromEntries(entries)))
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(requestError.message);
      });
    return () => controller.abort();
  }, [definition.fields]);

  const requestedId =
    !readOnly && query.edit && query.edit !== "new"
      ? deactivationId || query.edit
      : "";

  useEffect(() => {
    if (!requestedId || page.rows.some((row) => row.id === requestedId)) {
      return undefined;
    }
    const controller = new AbortController();
    getReferenceItem(definition.resource, requestedId, controller.signal)
      .then(setDetachedValue)
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(requestError.message);
      });
    return () => controller.abort();
  }, [definition.resource, page.rows, requestedId]);

  const editedValue = useMemo(() => {
    if (!query.edit) return null;
    if (query.edit === "new") return {};
    if (deactivationId) return null;
    return (
      page.rows.find((row) => row.id === query.edit) ||
      (detachedValue?.id === query.edit ? detachedValue : null)
    );
  }, [deactivationId, detachedValue, page.rows, query.edit]);

  const deactivationTarget = deactivationId
    ? page.rows.find((row) => row.id === deactivationId) || detachedValue
    : null;

  const modalFields = useMemo(
    () =>
      definition.fields.map((field) => ({
        ...field,
        options: field.optionsResource
          ? optionLists[field.optionsResource] || []
          : field.options,
      })),
    [definition.fields, optionLists],
  );

  const tableRows = page.rows.map((row) => ({
    ...row,
    id: row.id,
    status: row.active ? "ACTIVE" : "INACTIVE",
    ...(readOnly ? {} : { actions: row.id }),
  }));

  const headers = [
    ...definition.columns.map((column) => ({
      key: column.key,
      header: intl.formatMessage({ id: column.label }),
    })),
    {
      key: "status",
      header: intl.formatMessage({ id: "microbiology.admin.status" }),
    },
    ...(readOnly ? [] : [{ key: "actions", header: "" }]),
  ];

  const closeEditor = () => setQuery({ edit: "" }, { replace: true });

  const save = async (value) => {
    await saveReference(definition.resource, value);
    setNotice(intl.formatMessage({ id: "microbiology.admin.saved" }));
    closeEditor();
    await load();
  };

  const toggleActive = async (row) => {
    await setReferenceActive(definition.resource, row.id, !row.active);
    setNotice(
      intl.formatMessage({
        id: row.active
          ? "microbiology.admin.deactivated"
          : "microbiology.admin.reactivated",
      }),
    );
    await load();
  };

  if (loading && page.rows.length === 0) {
    return <Loading withOverlay={false} />;
  }

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
      {notice && (
        <InlineNotification
          kind="success"
          lowContrast
          title={notice}
          onClose={() => setNotice("")}
        />
      )}
      <DataTable rows={tableRows} headers={headers}>
        {({ rows, headers: renderedHeaders, getRowProps }) => (
          <TableContainer
            title={intl.formatMessage({ id: definition.title })}
            description={intl.formatMessage({ id: definition.description })}
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
                  id={`microbiology-${definition.resource}-status`}
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
                  id={`microbiology-${definition.resource}-sort`}
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
                {!readOnly && (
                  <Button
                    renderIcon={Add}
                    onClick={() => setQuery({ edit: "new" })}
                  >
                    {intl.formatMessage({ id: definition.addLabel })}
                  </Button>
                )}
              </TableToolbarContent>
            </TableToolbar>
            <Table size="lg" useZebraStyles tabIndex={0}>
              <TableHead>
                <TableRow>
                  {renderedHeaders.map((header) => (
                    <TableHeader key={header.key}>{header.header}</TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => {
                  const source = page.rows.find((item) => item.id === row.id);
                  return (
                    <TableRow {...getRowProps({ row })} key={row.id}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            <Tag
                              type={cell.value === "ACTIVE" ? "green" : "gray"}
                            >
                              {intl.formatMessage({
                                id:
                                  cell.value === "ACTIVE"
                                    ? "microbiology.admin.status.active"
                                    : "microbiology.admin.status.inactive",
                              })}
                            </Tag>
                          ) : cell.info.header === "actions" ? (
                            source ? (
                              <OverflowMenu
                                aria-label={intl.formatMessage({
                                  id: "microbiology.admin.actions",
                                })}
                                flipped
                              >
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "button.edit",
                                  })}
                                  onClick={() => setQuery({ edit: source.id })}
                                />
                                {definition.canToggle !== false && (
                                  <OverflowMenuItem
                                    isDelete={source.active}
                                    itemText={intl.formatMessage({
                                      id: source.active
                                        ? "microbiology.admin.action.deactivate"
                                        : "microbiology.admin.action.reactivate",
                                    })}
                                    onClick={() =>
                                      source.active
                                        ? setQuery({
                                            edit: `deactivate:${source.id}`,
                                          })
                                        : toggleActive(source)
                                    }
                                  />
                                )}
                              </OverflowMenu>
                            ) : null
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
            {page.rows.length === 0 && !error && (
              <div className="microbiology-admin__empty">
                {intl.formatMessage({ id: "microbiology.admin.empty" })}
              </div>
            )}
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
      {!readOnly && !!query.edit && !!editedValue && (
        <ReferenceEditModal
          key={`${definition.resource}-${query.edit}`}
          titleId={definition.editTitle}
          fields={modalFields}
          value={editedValue}
          onClose={closeEditor}
          onSave={save}
        />
      )}
      <ComposedModal
        open={!readOnly && !!deactivationTarget}
        size="sm"
        danger
        onClose={closeEditor}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "microbiology.admin.deactivate.title",
          })}
          closeModal={closeEditor}
        />
        <ModalBody>
          {deactivationTarget &&
            intl.formatMessage(
              { id: "microbiology.admin.deactivate.impact" },
              {
                name: deactivationTarget.displayName || deactivationTarget.name,
                count: deactivationTarget.referenceCount || 0,
              },
            )}
        </ModalBody>
        <ModalFooter
          primaryButtonText={intl.formatMessage({
            id: "microbiology.admin.action.deactivate",
          })}
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          onRequestSubmit={async () => {
            await toggleActive(deactivationTarget);
            closeEditor();
          }}
          onRequestClose={closeEditor}
        />
      </ComposedModal>
    </div>
  );
};

export default ReferenceDataPage;
