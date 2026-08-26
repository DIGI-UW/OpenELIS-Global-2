import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Layer,
  Link as CarbonLink,
  Loading,
  Pagination,
  ProgressIndicator,
  ProgressStep,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tile,
} from "@carbon/react";
import { Download, Edit, View, WarningAlt } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { Link as RouterLink, useHistory, useLocation } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import MicrobiologySurveillanceFilters from "./MicrobiologySurveillanceFilters";
import * as defaultService from "./WhonetService";
import {
  buildWhonetSearch,
  clearWhonetWorklistScope,
  getWhonetMappingRepairUrl,
  parseWhonetSearch,
  toWhonetRequest,
} from "./WhonetRoutes";
import "./WhonetExport.scss";

const formatRequestError = (intl, error) => {
  if (error?.code === "MICROBIOLOGY_REFERENCE_INVALID") {
    return intl.formatMessage({
      id: "microbiology.whonet.error.invalidRequest",
    });
  }
  if (error?.code === "MICROBIOLOGY_WHONET_EXPORT_BLOCKED") {
    return intl.formatMessage({ id: "microbiology.whonet.error.blocked" });
  }
  if (!error?.status) {
    return intl.formatMessage({ id: "microbiology.whonet.error.network" });
  }
  return intl.formatMessage({ id: "microbiology.whonet.error.generic" });
};

const mappingRepairMessage = {
  organisms: "microbiology.whonet.mapping.fixOrganism",
  antibiotics: "microbiology.whonet.mapping.fixAntibiotic",
  "specimen-types": "microbiology.whonet.mapping.fixSpecimen",
};

const emptyFilterOptions = {
  specimenTypes: [],
  organisms: [],
  patientOrigins: [],
  significance: [],
};

const WhonetExport = ({ service = defaultService, now }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [generated, setGenerated] = useState(false);
  const [error, setError] = useState("");
  const [filterOptionsError, setFilterOptionsError] = useState("");
  const [filterOptions, setFilterOptions] = useState(emptyFilterOptions);
  const [filterOptionsLoading, setFilterOptionsLoading] = useState(false);
  const referenceNow = useMemo(() => now || new Date(), [now]);

  const state = useMemo(
    () => parseWhonetSearch(location.search, referenceNow),
    [location.search, referenceNow],
  );
  const canonicalSearch = useMemo(
    () => buildWhonetSearch(state, referenceNow),
    [referenceNow, state],
  );

  useEffect(() => {
    if (location.search.replace(/^\?/, "") !== canonicalSearch) {
      history.replace({ pathname: location.pathname, search: canonicalSearch });
    }
  }, [canonicalSearch, history, location.pathname, location.search]);

  const setQuery = useCallback(
    (updates, { replace = false } = {}) => {
      const next = { ...state, ...updates };
      const destination = {
        pathname: location.pathname,
        search: buildWhonetSearch(next, referenceNow),
      };
      if (replace) history.replace(destination);
      else history.push(destination);
    },
    [history, location.pathname, referenceNow, state],
  );

  const request = useMemo(() => toWhonetRequest(state), [state]);
  const invalidPeriod = state.to < state.from;

  useEffect(() => {
    if (invalidPeriod) {
      setFilterOptionsError("");
      return undefined;
    }
    let active = true;
    setFilterOptionsLoading(true);
    service
      .getWhonetFilterOptions(request)
      .then((response) => {
        if (active) {
          setFilterOptions(response);
          setFilterOptionsError("");
        }
      })
      .catch((requestError) => {
        if (active)
          setFilterOptionsError(formatRequestError(intl, requestError));
      })
      .finally(() => {
        if (active) setFilterOptionsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [intl, invalidPeriod, request.from, request.to, service]);

  useEffect(() => {
    if (state.step !== "preview") {
      setPreview(null);
      return undefined;
    }
    let active = true;
    setPreview(null);
    setLoading(true);
    setError("");
    service
      .getWhonetPreview(request)
      .then((response) => {
        if (active) setPreview(response);
      })
      .catch((requestError) => {
        if (active) setError(formatRequestError(intl, requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [intl, request, service, state.step]);

  const updateConfiguration = (updates) => {
    setGenerated(false);
    setQuery({ ...updates, step: "configure", page: 1 });
  };

  const clearWorklistScope = () => {
    const directState = clearWhonetWorklistScope(state, referenceNow);
    history.push({
      pathname: location.pathname,
      search: buildWhonetSearch(directState, referenceNow),
    });
  };

  const activeError = error || filterOptionsError;

  const generate = async () => {
    setGenerating(true);
    setGenerated(false);
    setError("");
    try {
      const result = await service.generateWhonetExport(request);
      const href = URL.createObjectURL(result.blob);
      const link = document.createElement("a");
      link.href = href;
      link.download = result.filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(href);
      setGenerated(true);
    } catch (requestError) {
      setError(formatRequestError(intl, requestError));
    } finally {
      setGenerating(false);
    }
  };

  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "microbiology.whonet.table.accession" }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({ id: "microbiology.whonet.table.specimen" }),
    },
    {
      key: "organismCode",
      header: intl.formatMessage({ id: "microbiology.whonet.table.organism" }),
    },
    {
      key: "antibioticCode",
      header: intl.formatMessage({
        id: "microbiology.whonet.table.antibiotic",
      }),
    },
    {
      key: "interpretation",
      header: intl.formatMessage({
        id: "microbiology.whonet.table.interpretation",
      }),
    },
    {
      key: "method",
      header: intl.formatMessage({ id: "microbiology.whonet.table.method" }),
    },
  ];
  const rows = (preview?.rows || []).map((row, index) => ({
    ...row,
    id: `${row.caseId}-${row.isolateId}-${row.antibioticCode}-${index}`,
  }));
  const metrics = preview
    ? [
        ["cases", preview.totalCases, "microbiology.whonet.count.cases"],
        [
          "isolates",
          preview.totalIsolates,
          "microbiology.whonet.count.isolates",
        ],
        [
          "after-specimen",
          preview.afterSpecimen,
          "microbiology.whonet.count.specimen",
        ],
        [
          "after-organism",
          preview.afterOrganism,
          "microbiology.whonet.count.organism",
        ],
        [
          "after-origin",
          preview.afterPatientOrigin,
          "microbiology.whonet.count.origin",
        ],
        [
          "clinical-purpose",
          preview.clinicalPurposeCases,
          "microbiology.whonet.count.clinicalPurpose",
        ],
        [
          "screening-purpose",
          preview.screeningPurposeCases,
          "microbiology.whonet.count.screeningPurpose",
        ],
        [
          "unspecified-purpose",
          preview.unspecifiedPurposeCases,
          "microbiology.whonet.count.unspecifiedPurpose",
        ],
        [
          "after-purpose",
          preview.afterCulturePurpose,
          "microbiology.whonet.count.purposeIncluded",
        ],
        [
          "after-inclusion",
          preview.afterSignificance,
          "microbiology.whonet.count.included",
        ],
        [
          "after-deduplication",
          preview.afterDeduplication,
          "microbiology.whonet.count.deduplicated",
        ],
        [
          "mappable",
          preview.exportableIsolates,
          "microbiology.whonet.count.mappable",
        ],
        ["eligible", preview.exportedRows, "microbiology.whonet.count.rows"],
        [
          "excluded",
          preview.excludedRows,
          "microbiology.whonet.count.excluded",
        ],
      ]
    : [];

  return (
    <main className="whonet-export" data-testid="whonet-export">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/Dashboard" },
          { label: "sidenav.label.reports", link: "/Report" },
          {
            label: "microbiology.whonet.title",
            link: `${location.pathname}?${canonicalSearch}`,
            isCurrentPage: true,
          },
        ]}
      />
      <div className="cds--visually-hidden" role="status" aria-live="polite">
        {loading
          ? intl.formatMessage({ id: "microbiology.whonet.preview.loading" })
          : preview
            ? intl.formatMessage(
                { id: "microbiology.whonet.preview.announced" },
                { count: preview.exportedRows },
              )
            : ""}
      </div>
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="whonet-export__header">
            <div>
              <p className="whonet-export__eyebrow">
                {intl.formatMessage({ id: "microbiology.whonet.eyebrow" })}
              </p>
              <h1>{intl.formatMessage({ id: "microbiology.whonet.title" })}</h1>
              <p>
                {intl.formatMessage({ id: "microbiology.whonet.subtitle" })}
              </p>
            </div>
          </div>

          <ProgressIndicator
            currentIndex={generated ? 2 : state.step === "preview" ? 1 : 0}
            spaceEqually
            aria-label={intl.formatMessage({
              id: "microbiology.whonet.progress",
            })}
          >
            <ProgressStep
              label={intl.formatMessage({
                id: "microbiology.whonet.step.configure",
              })}
            />
            <ProgressStep
              label={intl.formatMessage({
                id: "microbiology.whonet.step.preview",
              })}
            />
            <ProgressStep
              label={intl.formatMessage({
                id: "microbiology.whonet.step.generate",
              })}
            />
          </ProgressIndicator>

          {state.source === "ast-worklist" && (
            <div className="whonet-export__source">
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "microbiology.whonet.source.worklist.title",
                })}
                subtitle={intl.formatMessage({
                  id: "microbiology.whonet.source.worklist.description",
                })}
              />
              <Button kind="ghost" size="sm" onClick={clearWorklistScope}>
                {intl.formatMessage({
                  id: "microbiology.whonet.source.worklist.clear",
                })}
              </Button>
            </div>
          )}

          <Layer className="whonet-export__configure">
            <div className="whonet-export__section-heading">
              <h2>
                {intl.formatMessage({
                  id: "microbiology.whonet.configure.title",
                })}
              </h2>
              <p>
                {intl.formatMessage({
                  id: "microbiology.whonet.configure.description",
                })}
              </p>
            </div>
            <MicrobiologySurveillanceFilters
              state={state}
              filterOptions={filterOptions}
              onChange={updateConfiguration}
              now={referenceNow}
              disabled={filterOptionsLoading}
              idPrefix="whonet"
            />
            <div className="whonet-export__controls">
              <Select
                id="whonet-dedup"
                labelText={intl.formatMessage({
                  id: "microbiology.whonet.dedup",
                })}
                value={state.dedup}
                onChange={(event) =>
                  updateConfiguration({ dedup: event.target.value })
                }
              >
                <SelectItem
                  value="FIRST_ISOLATE_7_DAY"
                  text={intl.formatMessage({
                    id: "microbiology.whonet.dedup.sevenDay",
                  })}
                />
                <SelectItem
                  value="NONE"
                  text={intl.formatMessage({
                    id: "microbiology.whonet.dedup.none",
                  })}
                />
              </Select>
            </div>
            <fieldset className="whonet-export__purpose-filters">
              <legend className="cds--label">
                {intl.formatMessage({
                  id: "microbiology.whonet.culturePurpose.title",
                })}
              </legend>
              <p>
                {intl.formatMessage({
                  id: "microbiology.whonet.culturePurpose.description",
                })}
              </p>
              <Checkbox
                id="whonet-include-screening"
                aria-label={intl.formatMessage({
                  id: "microbiology.whonet.culturePurpose.includeScreening",
                })}
                labelText={intl.formatMessage({
                  id: "microbiology.whonet.culturePurpose.includeScreening",
                })}
                checked={state.includeScreening}
                onChange={(_, { checked }) =>
                  updateConfiguration({ includeScreening: checked })
                }
              />
              <Checkbox
                id="whonet-include-unspecified"
                aria-label={intl.formatMessage({
                  id: "microbiology.whonet.culturePurpose.includeUnspecified",
                })}
                labelText={intl.formatMessage({
                  id: "microbiology.whonet.culturePurpose.includeUnspecified",
                })}
                checked={state.includeUnspecified}
                onChange={(_, { checked }) =>
                  updateConfiguration({ includeUnspecified: checked })
                }
              />
            </fieldset>
            <div className="whonet-export__configure-actions">
              <Button
                renderIcon={View}
                disabled={invalidPeriod}
                onClick={() => setQuery({ step: "preview", page: 1 })}
              >
                {intl.formatMessage({
                  id: "microbiology.whonet.preview.action",
                })}
              </Button>
            </div>
          </Layer>

          {activeError && (
            <InlineNotification
              kind="error"
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.whonet.error.title",
              })}
              subtitle={activeError}
            />
          )}
          {generated && (
            <InlineNotification
              kind="success"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.whonet.generated.title",
              })}
              subtitle={intl.formatMessage({
                id: "microbiology.whonet.generated.subtitle",
              })}
            />
          )}

          {loading && <Loading withOverlay={false} />}
          {!loading && state.step === "preview" && preview && (
            <section
              className="whonet-export__preview"
              aria-labelledby="whonet-preview-heading"
            >
              <div className="whonet-export__section-heading whonet-export__section-heading--split">
                <div>
                  <h2 id="whonet-preview-heading">
                    {intl.formatMessage({
                      id: "microbiology.whonet.preview.title",
                    })}
                  </h2>
                  <p>
                    {intl.formatMessage({
                      id: "microbiology.whonet.preview.description",
                    })}
                  </p>
                </div>
                <Button
                  renderIcon={Download}
                  disabled={!preview.canGenerate || generating}
                  onClick={generate}
                >
                  {intl.formatMessage({
                    id: "microbiology.whonet.generate.action",
                  })}
                </Button>
              </div>

              <div className="whonet-export__metrics">
                {metrics.map(([key, value, label]) => (
                  <Tile key={key} className="whonet-export__metric">
                    <strong>{value}</strong>
                    <span>{intl.formatMessage({ id: label })}</span>
                  </Tile>
                ))}
              </div>

              {preview.warnings?.length > 0 && (
                <div
                  className="whonet-export__warnings"
                  aria-label={intl.formatMessage({
                    id: "microbiology.whonet.mapping.title",
                  })}
                >
                  <h3>
                    {intl.formatMessage({
                      id: "microbiology.whonet.mapping.title",
                    })}
                  </h3>
                  {preview.warnings.map((warning) => {
                    const repairUrl = getWhonetMappingRepairUrl(
                      warning.resource,
                      warning.resourceId,
                      `${location.pathname}?${canonicalSearch}`,
                    );
                    return (
                      <div
                        key={`${warning.code}-${warning.resourceId || warning.itemLabel}`}
                        className="whonet-export__warning"
                      >
                        <WarningAlt size={20} aria-hidden="true" />
                        <div>
                          <strong>{warning.itemLabel}</strong>
                          <p>
                            {intl.formatMessage(
                              { id: "microbiology.whonet.mapping.excluded" },
                              { count: warning.excludedRows },
                            )}
                          </p>
                        </div>
                        {repairUrl && (
                          <CarbonLink
                            as={RouterLink}
                            to={repairUrl}
                            renderIcon={Edit}
                          >
                            {intl.formatMessage({
                              id: mappingRepairMessage[warning.resource],
                            })}
                          </CarbonLink>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}

              <DataTable rows={rows} headers={headers} size="sm">
                {({
                  rows: tableRows,
                  headers: tableHeaders,
                  getRowProps,
                  getTableProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "microbiology.whonet.table.title",
                    })}
                    description={intl.formatMessage({
                      id: "microbiology.whonet.table.description",
                    })}
                  >
                    <div
                      className="whonet-export__table-scroll"
                      role="region"
                      aria-label={intl.formatMessage({
                        id: "microbiology.whonet.table.title",
                      })}
                      tabIndex={0}
                    >
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            {tableHeaders.map((header) => (
                              <TableHeader key={header.key}>
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {tableRows.map((row) => (
                            <TableRow key={row.id} {...getRowProps({ row })}>
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  </TableContainer>
                )}
              </DataTable>
              <Pagination
                page={state.page}
                pageSize={state.pageSize}
                pageSizes={[20, 50, 100]}
                totalItems={preview.exportedRows}
                onChange={({ page, pageSize }) => setQuery({ page, pageSize })}
              />
            </section>
          )}
        </Column>
      </Grid>
    </main>
  );
};

export default WhonetExport;
