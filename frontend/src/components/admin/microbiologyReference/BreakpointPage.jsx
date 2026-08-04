import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  ComposedModal,
  DataTable,
  FileUploader,
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
import { ArrowLeft, Download, Upload } from "@carbon/icons-react";
import { useHistory } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  activateBreakpointStandard,
  applyBreakpointImport,
  archiveBreakpointStandard,
  getBreakpointRules,
  getBreakpointStandards,
  previewBreakpointImport,
} from "./api";
import { buildReferenceQuery } from "./queryState";
import { sectionPath } from "./sectionConfig";

const tagType = (status) => {
  if (status === "ACTIVE") return "green";
  if (status === "ARCHIVED") return "gray";
  return "blue";
};

const lifecycleMessage = (status) =>
  `microbiology.admin.breakpoints.status.${status.toLowerCase()}`;

const BreakpointPage = ({ standardId, basePath, query, setQuery }) => {
  const intl = useIntl();
  const history = useHistory();
  const [standards, setStandards] = useState({ rows: [], total: 0 });
  const [rules, setRules] = useState({ rows: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [importPreview, setImportPreview] = useState(null);
  const [importing, setImporting] = useState(false);

  const load = useCallback(
    async (signal) => {
      setLoading(true);
      setError("");
      try {
        const standardPage = await getBreakpointStandards(
          standardId
            ? "status=ALL&sort=name&page=1&pageSize=100"
            : buildReferenceQuery(query),
          signal,
        );
        setStandards(standardPage);
        if (standardId) {
          setRules(
            await getBreakpointRules(
              standardId,
              buildReferenceQuery(query),
              signal,
            ),
          );
        }
      } catch (requestError) {
        if (requestError.name !== "AbortError") setError(requestError.message);
      } finally {
        setLoading(false);
      }
    },
    [query, standardId],
  );

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const selectedStandard = standards.rows.find(
    (standard) => standard.id === standardId,
  );

  const standardHeaders = useMemo(
    () => [
      {
        key: "authority",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.publisher",
        }),
      },
      {
        key: "version",
        header: intl.formatMessage({
          id: "microbiology.admin.astPanels.version",
        }),
      },
      {
        key: "lifecycleStatus",
        header: intl.formatMessage({ id: "microbiology.admin.status" }),
      },
      {
        key: "effectiveDate",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.effectiveDate",
        }),
      },
      {
        key: "ruleCount",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.rules",
        }),
      },
      { key: "actions", header: "" },
    ],
    [intl],
  );

  const ruleHeaders = useMemo(
    () => [
      {
        key: "organism",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.organism",
        }),
      },
      {
        key: "antibiotic",
        header: intl.formatMessage({
          id: "microbiology.admin.astPanels.antibiotic",
        }),
      },
      {
        key: "method",
        header: intl.formatMessage({ id: "microbiology.admin.field.method" }),
      },
      {
        key: "susceptible",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.susceptible",
        }),
      },
      {
        key: "intermediate",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.intermediate",
        }),
      },
      {
        key: "resistant",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.resistant",
        }),
      },
      {
        key: "source",
        header: intl.formatMessage({
          id: "microbiology.admin.breakpoints.source",
        }),
      },
    ],
    [intl],
  );

  const openDetail = (id) =>
    history.push({
      pathname: sectionPath(basePath, "breakpoints", id),
      search: buildReferenceQuery(query),
    });

  const activate = async () => {
    try {
      await activateBreakpointStandard(standardId, effectiveDate);
      setQuery({ edit: "" }, { replace: true });
      setEffectiveDate("");
      await load();
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const archive = async () => {
    try {
      await archiveBreakpointStandard(standardId);
      setQuery({ edit: "" }, { replace: true });
      await load();
    } catch (requestError) {
      setError(requestError.message);
    }
  };

  const readImport = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setImporting(true);
    file
      .text()
      .then(previewBreakpointImport)
      .then(setImportPreview)
      .catch((requestError) => setError(requestError.message))
      .finally(() => setImporting(false));
  };

  const applyImport = async () => {
    setImporting(true);
    try {
      setImportPreview(await applyBreakpointImport(importPreview.previewToken));
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setImporting(false);
    }
  };

  const downloadRejected = () => {
    const header = "row_number,message,source_row\n";
    const quote = (value) => `"${String(value || "").replaceAll('"', '""')}"`;
    const csv =
      header +
      importPreview.errors
        .map((item) =>
          [item.rowNumber, item.message, item.sourceRow].map(quote).join(","),
        )
        .join("\n");
    const href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    const link = document.createElement("a");
    link.href = href;
    link.download = "breakpoint-import-rejected.csv";
    link.click();
    URL.revokeObjectURL(href);
  };

  if (loading && standards.rows.length === 0) {
    return <Loading withOverlay={false} />;
  }

  const standardRows = standards.rows.map((standard) => ({
    ...standard,
    effectiveDate: standard.effectiveDate || "—",
    actions: standard.id,
  }));
  const ruleRows = rules.rows.map((rule) => ({
    id: rule.id,
    organism: rule.organismName || rule.organismGroup,
    antibiotic: `${rule.antibioticName || ""} (${rule.antibioticCode || ""})`,
    method: rule.method,
    susceptible: rule.susceptibleValue ?? "—",
    intermediate:
      rule.intermediateLowerValue == null && rule.intermediateUpperValue == null
        ? "—"
        : `${rule.intermediateLowerValue ?? ""}–${rule.intermediateUpperValue ?? ""}`,
    resistant: rule.resistantValue ?? "—",
    source: rule.locallyCustomized
      ? "LOCAL"
      : rule.seeded
        ? "IMPORTED"
        : "MANUAL",
  }));

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
      {!standardId ? (
        <DataTable rows={standardRows} headers={standardHeaders}>
          {({ rows, headers }) => (
            <TableContainer
              title={intl.formatMessage({
                id: "microbiology.admin.breakpoints.title",
              })}
              description={intl.formatMessage({
                id: "microbiology.admin.breakpoints.description",
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
                  <Button
                    kind="secondary"
                    renderIcon={Upload}
                    onClick={() => setQuery({ edit: "import" })}
                  >
                    {intl.formatMessage({
                      id: "microbiology.admin.breakpoints.import",
                    })}
                  </Button>
                </TableToolbarContent>
              </TableToolbar>
              <Table size="lg" useZebraStyles>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader key={header.key}>
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => {
                    const source = standards.rows.find(
                      (standard) => standard.id === row.id,
                    );
                    return (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "lifecycleStatus" ? (
                              <Tag type={tagType(source.lifecycleStatus)}>
                                {intl.formatMessage({
                                  id: lifecycleMessage(source.lifecycleStatus),
                                })}
                              </Tag>
                            ) : cell.info.header === "actions" ? (
                              <OverflowMenu
                                flipped
                                aria-label={intl.formatMessage({
                                  id: "microbiology.admin.actions",
                                })}
                              >
                                <OverflowMenuItem
                                  itemText={intl.formatMessage({
                                    id: "microbiology.admin.breakpoints.view",
                                  })}
                                  onClick={() => openDetail(source.id)}
                                />
                              </OverflowMenu>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
              <Pagination
                page={query.page}
                pageSize={query.pageSize}
                pageSizes={[20, 50, 100]}
                totalItems={standards.total}
                onChange={({ page, pageSize }) => setQuery({ page, pageSize })}
              />
            </TableContainer>
          )}
        </DataTable>
      ) : (
        <>
          <div className="microbiology-admin__detail-actions">
            <Button
              kind="ghost"
              renderIcon={ArrowLeft}
              onClick={() =>
                history.push({
                  pathname: sectionPath(basePath, "breakpoints"),
                  search: buildReferenceQuery(query),
                })
              }
            >
              {intl.formatMessage({
                id: "microbiology.admin.breakpoints.back",
              })}
            </Button>
            {selectedStandard?.lifecycleStatus === "LOADED" && (
              <Button onClick={() => setQuery({ edit: "activate" })}>
                {intl.formatMessage({
                  id: "microbiology.admin.breakpoints.activate",
                })}
              </Button>
            )}
            {selectedStandard?.lifecycleStatus !== "ACTIVE" &&
              selectedStandard?.lifecycleStatus !== "ARCHIVED" && (
                <Button
                  kind="danger--tertiary"
                  onClick={() => setQuery({ edit: "archive" })}
                >
                  {intl.formatMessage({
                    id: "microbiology.admin.breakpoints.archive",
                  })}
                </Button>
              )}
          </div>
          {selectedStandard && (
            <div className="microbiology-admin__standard-heading">
              <div>
                <h2>{`${selectedStandard.authority} ${selectedStandard.version}`}</h2>
                <span>{selectedStandard.effectiveDate || "—"}</span>
              </div>
              <Tag type={tagType(selectedStandard.lifecycleStatus)}>
                {intl.formatMessage({
                  id: lifecycleMessage(selectedStandard.lifecycleStatus),
                })}
              </Tag>
            </div>
          )}
          <DataTable rows={ruleRows} headers={ruleHeaders}>
            {({ rows, headers }) => (
              <TableContainer
                title={intl.formatMessage({
                  id: "microbiology.admin.breakpoints.rules",
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
                      id="microbiology-breakpoint-method"
                      hideLabel
                      labelText={intl.formatMessage({
                        id: "microbiology.admin.field.method",
                      })}
                      value={query.method}
                      onChange={(event) =>
                        setQuery({ method: event.target.value })
                      }
                    >
                      <SelectItem
                        value=""
                        text={intl.formatMessage({
                          id: "microbiology.admin.breakpoints.method.all",
                        })}
                      />
                      <SelectItem value="MIC" text="MIC" />
                      <SelectItem
                        value="ZONE"
                        text={intl.formatMessage({
                          id: "microbiology.admin.breakpoints.method.zone",
                        })}
                      />
                    </Select>
                  </TableToolbarContent>
                </TableToolbar>
                <Table size="lg" useZebraStyles>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader key={header.key}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "source" ? (
                              <Tag
                                type={
                                  cell.value === "LOCAL" ? "purple" : "cyan"
                                }
                              >
                                {intl.formatMessage({
                                  id: `microbiology.admin.breakpoints.source.${cell.value.toLowerCase()}`,
                                })}
                              </Tag>
                            ) : (
                              cell.value
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
                  totalItems={rules.total}
                  onChange={({ page, pageSize }) =>
                    setQuery({ page, pageSize })
                  }
                />
              </TableContainer>
            )}
          </DataTable>
        </>
      )}

      <ComposedModal
        open={query.edit === "activate"}
        size="sm"
        onClose={() => setQuery({ edit: "" }, { replace: true })}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "microbiology.admin.breakpoints.activate",
          })}
          closeModal={() => setQuery({ edit: "" }, { replace: true })}
        />
        <ModalBody>
          <TextInput
            id="microbiology-breakpoint-effective-date"
            type="date"
            labelText={intl.formatMessage({
              id: "microbiology.admin.breakpoints.effectiveDate",
            })}
            value={effectiveDate}
            onChange={(event) => setEffectiveDate(event.target.value)}
          />
        </ModalBody>
        <ModalFooter
          primaryButtonText={intl.formatMessage({
            id: "microbiology.admin.breakpoints.activate",
          })}
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          primaryButtonDisabled={!effectiveDate}
          onRequestSubmit={activate}
          onRequestClose={() => setQuery({ edit: "" }, { replace: true })}
        />
      </ComposedModal>

      <ComposedModal
        open={query.edit === "archive"}
        size="sm"
        danger
        onClose={() => setQuery({ edit: "" }, { replace: true })}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "microbiology.admin.breakpoints.archive",
          })}
          closeModal={() => setQuery({ edit: "" }, { replace: true })}
        />
        <ModalBody>
          {intl.formatMessage({
            id: "microbiology.admin.breakpoints.archiveConfirm",
          })}
        </ModalBody>
        <ModalFooter
          primaryButtonText={intl.formatMessage({
            id: "microbiology.admin.breakpoints.archive",
          })}
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          onRequestSubmit={archive}
          onRequestClose={() => setQuery({ edit: "" }, { replace: true })}
        />
      </ComposedModal>

      <ComposedModal
        open={query.edit === "import"}
        size="lg"
        onClose={() => {
          setImportPreview(null);
          setQuery({ edit: "" }, { replace: true });
        }}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "microbiology.admin.breakpoints.import",
          })}
          closeModal={() => setQuery({ edit: "" }, { replace: true })}
        />
        <ModalBody>
          {!importPreview && (
            <FileUploader
              accept={[".csv", "text/csv"]}
              buttonLabel={intl.formatMessage({
                id: "microbiology.admin.breakpoints.chooseFile",
              })}
              filenameStatus="edit"
              labelDescription={intl.formatMessage({
                id: "microbiology.admin.breakpoints.csv",
              })}
              onChange={readImport}
            />
          )}
          {importing && <Loading small withOverlay={false} />}
          {importPreview && (
            <div className="microbiology-admin__import-result">
              <div className="microbiology-admin__import-counts">
                <Tag type="green">
                  {intl.formatMessage(
                    { id: "microbiology.admin.breakpoints.count.valid" },
                    { count: importPreview.validRows },
                  )}
                </Tag>
                <Tag type="red">
                  {intl.formatMessage(
                    { id: "microbiology.admin.breakpoints.count.skipped" },
                    { count: importPreview.skippedRows },
                  )}
                </Tag>
                <Tag type="blue">
                  {intl.formatMessage(
                    { id: "microbiology.admin.breakpoints.count.unchanged" },
                    { count: importPreview.unchangedRows },
                  )}
                </Tag>
              </div>
              {importPreview.errors.map((item) => (
                <InlineNotification
                  key={`${item.rowNumber}-${item.message}`}
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage(
                    { id: "microbiology.admin.breakpoints.rowError" },
                    { row: item.rowNumber },
                  )}
                  subtitle={item.message}
                />
              ))}
              {importPreview.errors.length > 0 && (
                <Button
                  kind="ghost"
                  renderIcon={Download}
                  onClick={downloadRejected}
                >
                  {intl.formatMessage({
                    id: "microbiology.admin.breakpoints.downloadRejected",
                  })}
                </Button>
              )}
            </div>
          )}
        </ModalBody>
        <ModalFooter
          primaryButtonText={intl.formatMessage({
            id: "microbiology.admin.breakpoints.applyValid",
          })}
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          primaryButtonDisabled={
            !importPreview ||
            importPreview.validRows === 0 ||
            importPreview.importedRows > 0 ||
            importing
          }
          onRequestSubmit={applyImport}
          onRequestClose={() => setQuery({ edit: "" }, { replace: true })}
        />
      </ComposedModal>
    </div>
  );
};

export default BreakpointPage;
