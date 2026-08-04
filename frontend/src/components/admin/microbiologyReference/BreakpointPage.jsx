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
import { Add, ArrowLeft, Download, Upload } from "@carbon/icons-react";
import { useHistory } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  activateBreakpointStandard,
  applyBreakpointImport,
  archiveBreakpointStandard,
  getBreakpointRule,
  getBreakpointRules,
  getBreakpointStandard,
  getBreakpointStandards,
  getReferenceOptions,
  previewBreakpointImport,
  saveBreakpointRule,
} from "./api";
import BreakpointRuleModal from "./BreakpointRuleModal";
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
  const [ruleDraft, setRuleDraft] = useState(null);
  const [savingRule, setSavingRule] = useState(false);
  const [options, setOptions] = useState({
    organisms: [],
    organismGroups: [],
    antibiotics: [],
    specimenTypes: [],
  });

  const ruleEditId = query.edit?.startsWith("rule:")
    ? query.edit.slice("rule:".length)
    : "";

  const load = useCallback(
    async (signal) => {
      setLoading(true);
      setError("");
      try {
        const standardPage = standardId
          ? {
              rows: [await getBreakpointStandard(standardId, signal)],
              total: 1,
            }
          : await getBreakpointStandards(buildReferenceQuery(query), signal);
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

  useEffect(() => {
    if (query.edit !== "import") {
      setImportPreview(null);
      setImporting(false);
    }
  }, [query.edit]);

  useEffect(() => {
    if (!standardId) return undefined;
    const controller = new AbortController();
    Promise.all([
      getReferenceOptions("organisms", controller.signal),
      getReferenceOptions("organism-groups", controller.signal),
      getReferenceOptions("antibiotics", controller.signal),
      getReferenceOptions("specimen-types", controller.signal),
    ])
      .then(([organisms, organismGroups, antibiotics, specimenTypes]) =>
        setOptions({ organisms, organismGroups, antibiotics, specimenTypes }),
      )
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(requestError.message);
      });
    return () => controller.abort();
  }, [standardId]);

  useEffect(() => {
    if (!ruleEditId) {
      setRuleDraft(null);
      return undefined;
    }
    if (ruleEditId === "new") {
      setRuleDraft({});
      return undefined;
    }
    const visibleRule = rules.rows.find((rule) => rule.id === ruleEditId);
    if (visibleRule) {
      setRuleDraft(visibleRule);
      return undefined;
    }
    const controller = new AbortController();
    getBreakpointRule(standardId, ruleEditId, controller.signal)
      .then(setRuleDraft)
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(requestError.message);
      });
    return () => controller.abort();
  }, [ruleEditId, rules.rows, standardId]);

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
      { key: "actions", header: "" },
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

  const saveRule = async (rule) => {
    setSavingRule(true);
    try {
      await saveBreakpointRule(standardId, {
        ...rule,
        ...(ruleEditId !== "new" ? { id: ruleEditId } : {}),
      });
      setQuery({ edit: "" }, { replace: true });
      await load();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSavingRule(false);
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

  const closeImport = () => {
    setImportPreview(null);
    setQuery({ edit: "" }, { replace: true });
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
    actions: rule.id,
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
                  <Select
                    id="microbiology-breakpoint-status"
                    hideLabel
                    labelText={intl.formatMessage({
                      id: "microbiology.admin.status",
                    })}
                    value={query.status}
                    onChange={(event) =>
                      setQuery({ status: event.target.value })
                    }
                  >
                    <SelectItem
                      value="ALL"
                      text={intl.formatMessage({
                        id: "microbiology.admin.status.all",
                      })}
                    />
                    {[
                      ["ACTIVE", "active"],
                      ["LOADED", "loaded"],
                      ["ARCHIVED", "archived"],
                    ].map(([value, key]) => (
                      <SelectItem
                        key={value}
                        value={value}
                        text={intl.formatMessage({
                          id: `microbiology.admin.breakpoints.status.${key}`,
                        })}
                      />
                    ))}
                  </Select>
                  <Select
                    id="microbiology-breakpoint-sort"
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
                    {selectedStandard?.lifecycleStatus !== "ARCHIVED" && (
                      <Button
                        renderIcon={Add}
                        onClick={() => setQuery({ edit: "rule:new" })}
                      >
                        {intl.formatMessage({
                          id: "microbiology.admin.breakpoints.correction.add",
                        })}
                      </Button>
                    )}
                  </TableToolbarContent>
                </TableToolbar>
                <div className="microbiology-admin__filters">
                  <Select
                    id="microbiology-breakpoint-organism"
                    labelText={intl.formatMessage({
                      id: "microbiology.admin.breakpoints.organism",
                    })}
                    value={query.organism}
                    onChange={(event) =>
                      setQuery({ organism: event.target.value })
                    }
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "microbiology.admin.filter.all",
                      })}
                    />
                    {options.organismGroups.map((item) => (
                      <SelectItem
                        key={`group-${item.id}`}
                        value={item.id}
                        text={intl.formatMessage(
                          {
                            id: "microbiology.admin.breakpoints.groupOption",
                          },
                          { name: item.label },
                        )}
                      />
                    ))}
                    {options.organisms.map((item) => (
                      <SelectItem
                        key={item.id}
                        value={item.id}
                        text={item.label}
                      />
                    ))}
                  </Select>
                  <Select
                    id="microbiology-breakpoint-antibiotic"
                    labelText={intl.formatMessage({
                      id: "microbiology.admin.astPanels.antibiotic",
                    })}
                    value={query.antibiotic}
                    onChange={(event) =>
                      setQuery({ antibiotic: event.target.value })
                    }
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "microbiology.admin.filter.all",
                      })}
                    />
                    {options.antibiotics.map((item) => (
                      <SelectItem
                        key={item.id}
                        value={item.id}
                        text={item.label}
                      />
                    ))}
                  </Select>
                  <Select
                    id="microbiology-breakpoint-method"
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
                  <Select
                    id="microbiology-breakpoint-specimen"
                    labelText={intl.formatMessage({
                      id: "microbiology.admin.breakpoints.specimen",
                    })}
                    value={query.specimenTypeId}
                    onChange={(event) =>
                      setQuery({ specimenTypeId: event.target.value })
                    }
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "microbiology.admin.filter.all",
                      })}
                    />
                    {options.specimenTypes.map((item) => (
                      <SelectItem
                        key={item.id}
                        value={item.id}
                        text={item.label}
                      />
                    ))}
                  </Select>
                </div>
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
                            ) : cell.info.header === "actions" ? (
                              selectedStandard?.lifecycleStatus !==
                                "ARCHIVED" && (
                                <OverflowMenu
                                  flipped
                                  aria-label={intl.formatMessage({
                                    id: "microbiology.admin.actions",
                                  })}
                                >
                                  <OverflowMenuItem
                                    itemText={intl.formatMessage({
                                      id: "microbiology.admin.breakpoints.correction.edit",
                                    })}
                                    onClick={() =>
                                      setQuery({ edit: `rule:${row.id}` })
                                    }
                                  />
                                </OverflowMenu>
                              )
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

      <BreakpointRuleModal
        value={ruleDraft}
        organisms={options.organisms}
        organismGroups={options.organismGroups}
        antibiotics={options.antibiotics}
        specimenTypes={options.specimenTypes}
        saving={savingRule}
        onClose={() => setQuery({ edit: "" }, { replace: true })}
        onSave={saveRule}
      />

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
        onClose={closeImport}
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "microbiology.admin.breakpoints.import",
          })}
          closeModal={closeImport}
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
          onRequestClose={closeImport}
        />
      </ComposedModal>
    </div>
  );
};

export default BreakpointPage;
