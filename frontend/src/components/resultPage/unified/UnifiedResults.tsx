import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  Column,
  DatePicker,
  DatePickerInput,
  Grid,
  Heading,
  InlineNotification,
  Pagination,
  Search,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import ESignatureButton, {
  SignatureMeaning,
} from "../../esignature/ESignatureButton";
import PolymorphicResultCell, { ResultCellRow } from "./PolymorphicResultCell";
import {
  RowEditState,
  initialRowState,
  isRowEditable,
  nextRowState,
  showEdit,
  showSave,
} from "./editState";
import {
  ResultsDomain,
  formatDomainMessage,
  normalizeDomain,
} from "./domainIntl";
import { usePresence } from "./usePresence";

/**
 * OGC-1020 (R1 of OGC-811) — unified /Results worklist.
 *
 * Consolidates the legacy result-entry routes behind the
 * `results.entry.unifiedRoute` site flag: one toolbar (search, Lab Unit,
 * date, status chips), a polymorphic result cell (FR-A1), a per-row
 * read-only→Edit→Save edit-state machine (FR-A2/A3), e-signature on Save
 * (FR-A4), per-analysis save scoping + optimistic version check + soft
 * presence (FR-O1–O3), and cross-domain rendering driven by the selected Lab
 * Unit (FR-M1–M4).
 */

interface LabUnit {
  id: string;
  value: string;
  domain?: string;
}

interface WorklistRow extends ResultCellRow {
  accessionNumber?: string;
  sequenceNumber?: string;
  testName?: string;
  patientInfo?: string;
  patientName?: string;
  sampleType?: string;
  normalRange?: string;
  analysisStatusId?: string;
  analysisLastupdated?: string;
  [key: string]: unknown;
}

interface StatusOption {
  id: string;
  value: string;
}

interface SaveResponse {
  status?: number;
  error?: string;
  modifiedBy?: string;
  modifiedAt?: string;
  analysisLastupdated?: string;
  reflex?: string[];
  calculated?: string[];
}

const UnifiedResults: React.FC = () => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [labUnits, setLabUnits] = useState<LabUnit[]>([]);
  const [selectedLabUnit, setSelectedLabUnit] = useState<string>("");
  const [statusOptions, setStatusOptions] = useState<StatusOption[]>([]);
  const [searchText, setSearchText] = useState<string>("");
  const [collectionDate, setCollectionDate] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [rows, setRows] = useState<WorklistRow[]>([]);
  const [rowStates, setRowStates] = useState<Record<string, RowEditState>>({});
  const [staleInfo, setStaleInfo] = useState<
    Record<string, { modifiedBy?: string; modifiedAt?: string }>
  >({});
  const [editingAnalysisId, setEditingAnalysisId] = useState<string | null>(
    null,
  );
  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(25);
  const [loading, setLoading] = useState<boolean>(false);

  const domain: ResultsDomain = useMemo(() => {
    const unit = labUnits.find((u) => u.id === selectedLabUnit);
    return normalizeDomain(unit?.domain);
  }, [labUnits, selectedLabUnit]);

  useEffect(() => {
    getFromOpenElisServer("/rest/results-entry/lab-units", (list: LabUnit[]) =>
      setLabUnits(list || []),
    );
    getFromOpenElisServer(
      "/rest/analysis-status-types",
      (list: StatusOption[]) =>
        setStatusOptions((list || []).filter((s) => s.id !== "0")),
    );
  }, []);

  const applyLoadedRows = useCallback(
    (results: { testResult?: WorklistRow[] }) => {
      const loaded = (results?.testResult || []).filter((r) => r.analysisId);
      setRows(loaded);
      const states: Record<string, RowEditState> = {};
      for (const row of loaded) {
        states[row.analysisId] = initialRowState(
          Boolean(row.resultValue) ||
            Boolean(
              row.multiSelectResultValues &&
              row.multiSelectResultValues !== "{}",
            ),
        );
      }
      setRowStates(states);
      setStaleInfo({});
      setEditingAnalysisId(null);
      setPage(1);
      setLoading(false);
    },
    [],
  );

  const loadWorklist = useCallback(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (searchText) {
      params.set("labNumber", searchText);
    }
    if (selectedLabUnit) {
      params.set("testSectionId", selectedLabUnit);
    }
    if (collectionDate) {
      params.set("collectionDate", collectionDate);
    }
    params.set("doRange", "false");
    params.set("finished", "false");
    getFromOpenElisServer(
      "/rest/LogbookResults?" + params.toString(),
      applyLoadedRows,
    );
  }, [searchText, selectedLabUnit, collectionDate, applyLoadedRows]);

  useEffect(() => {
    if (selectedLabUnit) {
      loadWorklist();
    }
  }, [selectedLabUnit]);

  const statusCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const row of rows) {
      const key = row.analysisStatusId || "";
      counts[key] = (counts[key] || 0) + 1;
    }
    return counts;
  }, [rows]);

  const filteredRows = useMemo(
    () =>
      statusFilter === "ALL"
        ? rows
        : rows.filter((row) => row.analysisStatusId === statusFilter),
    [rows, statusFilter],
  );

  const pagedRows = useMemo(
    () => filteredRows.slice((page - 1) * pageSize, page * pageSize),
    [filteredRows, page, pageSize],
  );

  const visibleAnalysisIds = useMemo(
    () => pagedRows.map((row) => row.analysisId),
    [pagedRows],
  );

  const presence = usePresence(editingAnalysisId, visibleAnalysisIds);

  const handleValueChange = useCallback(
    (
      analysisId: string,
      field: "resultValue" | "multiSelectResultValues",
      value: string,
    ) => {
      setRows((current) =>
        current.map((row) =>
          row.analysisId === analysisId ? { ...row, [field]: value } : row,
        ),
      );
      setRowStates((current) => ({
        ...current,
        [analysisId]: nextRowState(current[analysisId] || "EMPTY", {
          type: "VALUE_CHANGED",
        }),
      }));
      // FR-O3: entering a fresh result counts as having the analysis "open
      // in Edit" — colleagues should see the presence hint for this row too
      setEditingAnalysisId(analysisId);
    },
    [],
  );

  const handleEdit = useCallback((analysisId: string) => {
    setRowStates((current) => ({
      ...current,
      [analysisId]: nextRowState(current[analysisId] || "SAVED", {
        type: "EDIT_CLICKED",
      }),
    }));
    setEditingAnalysisId(analysisId);
  }, []);

  const handleSaveResponse = useCallback(
    (analysisId: string, response: SaveResponse | undefined) => {
      if (!response) {
        return;
      }
      if (response.status === 409) {
        // FR-O2: the stale editor loses — nothing merged, refresh offered.
        setStaleInfo((current) => ({
          ...current,
          [analysisId]: {
            modifiedBy: response.modifiedBy,
            modifiedAt: response.modifiedAt,
          },
        }));
        setRowStates((current) => ({
          ...current,
          [analysisId]: nextRowState(current[analysisId] || "EDITING", {
            type: "SAVE_REJECTED_STALE",
          }),
        }));
        return;
      }
      if (response.status && response.status >= 400) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            response.error || intl.formatMessage({ id: "error.save.msg" }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }
      setRowStates((current) => ({
        ...current,
        [analysisId]: nextRowState(current[analysisId] || "EDITING", {
          type: "SAVE_SUCCEEDED",
        }),
      }));
      if (response.analysisLastupdated) {
        setRows((current) =>
          current.map((row) =>
            row.analysisId === analysisId
              ? { ...row, analysisLastupdated: response.analysisLastupdated }
              : row,
          ),
        );
      }
      setStaleInfo((current) => {
        const next = { ...current };
        delete next[analysisId];
        return next;
      });
      setEditingAnalysisId((current) =>
        current === analysisId ? null : current,
      );
      const triggered = [
        ...(response.reflex || []),
        ...(response.calculated || []),
      ];
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          intl.formatMessage({ id: "success.save.msg" }) +
          (triggered.length
            ? " " +
              intl.formatMessage({ id: "label.results.reflexTriggered" }) +
              " " +
              triggered.join(", ")
            : ""),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
    },
    [addNotification, intl, setNotificationVisible],
  );

  const handleSave = useCallback(
    (row: WorklistRow) => {
      // FR-O1: the payload names and carries exactly this analysis — never
      // the page. Untouched rows cannot be re-submitted or defaulted.
      const item: Record<string, unknown> = { ...row, isModified: true };
      delete item.result;
      // TestResultItem serializes reportable as "Y"/"N" but deserializes it
      // as boolean — same normalization the legacy page applies before POST
      item.reportable = item.reportable !== "N";
      postToOpenElisServerJsonResponse(
        `/rest/results-entry/analysis/${row.analysisId}/result`,
        JSON.stringify({ testResult: item }),
        (response: SaveResponse | undefined) =>
          handleSaveResponse(row.analysisId, response),
      );
    },
    [handleSaveResponse],
  );

  const subjectCell = (row: WorklistRow): string => {
    const accession = row.accessionNumber || "";
    if (domain === "CLINICAL") {
      const patient = row.patientInfo || row.patientName || "";
      return patient ? `${accession} · ${patient}` : accession;
    }
    // FR-M2/M3: no patient identity outside CLINICAL; sample context instead
    return row.sampleType ? `${accession} · ${row.sampleType}` : accession;
  };

  const statusName = (statusId?: string): string =>
    statusOptions.find((s) => s.id === statusId)?.value || statusId || "";

  return (
    <>
      <AlertDialog />
      <Grid fullWidth className="unifiedResultsPage">
        <Column lg={16} md={8} sm={4}>
          <Breadcrumb>
            <BreadcrumbItem href="/">
              <FormattedMessage id="home.label" />
            </BreadcrumbItem>
            <BreadcrumbItem href="/WorkplanByTest">
              <FormattedMessage id="sidenav.label.workplan" />
            </BreadcrumbItem>
          </Breadcrumb>
          <Section>
            <Heading>
              <FormattedMessage id="sidenav.label.results" />
              {domain !== "CLINICAL" && (
                <Tag type="cyan" className="unifiedResultsDomainTag">
                  {formatDomainMessage(intl, "label.results.domain", domain)}
                </Tag>
              )}
            </Heading>
          </Section>
        </Column>

        {/* Toolbar: search + Lab Unit + date (FR worklist toolbar) */}
        <Column lg={4} md={4} sm={4}>
          <Search
            id="unifiedResultsSearch"
            labelText={intl.formatMessage({ id: "label.results.search" })}
            placeholder={intl.formatMessage({ id: "label.results.search" })}
            value={searchText}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setSearchText(e.target.value)
            }
            onKeyDown={(e: React.KeyboardEvent) => {
              if (e.key === "Enter") {
                loadWorklist();
              }
            }}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="unifiedResultsLabUnit"
            labelText={intl.formatMessage({ id: "label.results.labUnit" })}
            value={selectedLabUnit}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              setSelectedLabUnit(e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {labUnits.map((unit) => (
              <SelectItem text={unit.value} value={unit.id} key={unit.id} />
            ))}
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="d/m/Y"
            onChange={(dates: Date[]) => {
              if (dates && dates.length) {
                const d = dates[0];
                setCollectionDate(
                  `${String(d.getDate()).padStart(2, "0")}/${String(
                    d.getMonth() + 1,
                  ).padStart(2, "0")}/${d.getFullYear()}`,
                );
              } else {
                setCollectionDate("");
              }
            }}
          >
            <DatePickerInput
              id="unifiedResultsDate"
              labelText={intl.formatMessage({ id: "label.results.date" })}
              placeholder="dd/mm/yyyy"
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4} className="unifiedResultsLoadColumn">
          <Button onClick={loadWorklist} disabled={loading}>
            <FormattedMessage id="label.results.load" />
          </Button>
        </Column>

        {/* Status filter chips with counts */}
        <Column lg={16} md={8} sm={4} className="unifiedResultsChips">
          <Tag
            type={statusFilter === "ALL" ? "blue" : "gray"}
            onClick={() => setStatusFilter("ALL")}
            className="unifiedResultsChip"
          >
            <FormattedMessage id="label.results.status.all" /> ({rows.length})
          </Tag>
          {statusOptions
            .filter((status) => statusCounts[status.id])
            .map((status) => (
              <Tag
                key={status.id}
                type={statusFilter === status.id ? "blue" : "gray"}
                onClick={() => setStatusFilter(status.id)}
                className="unifiedResultsChip"
              >
                {status.value} ({statusCounts[status.id]})
              </Tag>
            ))}
        </Column>

        <Column lg={16} md={8} sm={4}>
          <TableContainer>
            <Table size="lg">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {formatDomainMessage(intl, "label.results.subject", domain)}
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.test" />
                  </TableHeader>
                  <TableHeader>
                    {formatDomainMessage(intl, "label.results.range", domain)}
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.result" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.status" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.actions" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {pagedRows.map((row) => {
                  const state = rowStates[row.analysisId] || "EMPTY";
                  const stale = staleInfo[row.analysisId];
                  const reviewer = presence[row.analysisId];
                  return (
                    <React.Fragment key={row.analysisId}>
                      <TableRow>
                        <TableCell>
                          {subjectCell(row)}
                          {reviewer && (
                            <Tag type="purple" className="unifiedResultsChip">
                              <FormattedMessage
                                id="label.results.inReviewBy"
                                values={{ 0: reviewer }}
                              />
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell>{row.testName}</TableCell>
                        <TableCell>
                          {row.normalRange}{" "}
                          {row.unitsOfMeasure ? row.unitsOfMeasure : ""}
                        </TableCell>
                        <TableCell>
                          <PolymorphicResultCell
                            row={row}
                            editable={isRowEditable(state)}
                            onValueChange={handleValueChange}
                          />
                        </TableCell>
                        <TableCell>
                          {statusName(row.analysisStatusId)}
                        </TableCell>
                        <TableCell>
                          {showEdit(state) && (
                            <Button
                              kind="tertiary"
                              size="sm"
                              onClick={() => handleEdit(row.analysisId)}
                            >
                              <FormattedMessage id="label.results.edit" />
                            </Button>
                          )}
                          {showSave(state) && (
                            <ESignatureButton
                              meaning={SignatureMeaning.AUTHORED}
                              context={`${intl.formatMessage({
                                id: "label.results.save",
                              })} ${row.accessionNumber} - ${row.testName}`}
                              recordType="RESULT"
                              recordId={row.analysisId}
                              onSign={() => handleSave(row)}
                              size="sm"
                            >
                              <FormattedMessage id="label.results.save" />
                            </ESignatureButton>
                          )}
                        </TableCell>
                      </TableRow>
                      {stale && (
                        <TableRow>
                          <TableCell colSpan={6}>
                            <InlineNotification
                              kind="error"
                              hideCloseButton
                              lowContrast
                              title={intl.formatMessage(
                                { id: "error.results.staleSave" },
                                {
                                  0:
                                    stale.modifiedBy ||
                                    intl.formatMessage({
                                      id: "label.results.anotherUser",
                                    }),
                                  1: stale.modifiedAt || "",
                                },
                              )}
                              actions={
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={loadWorklist}
                                >
                                  <FormattedMessage id="label.results.refresh" />
                                </Button>
                              }
                            />
                          </TableCell>
                        </TableRow>
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
          <Pagination
            page={page}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            totalItems={filteredRows.length}
            onChange={({
              page: newPage,
              pageSize: newPageSize,
            }: {
              page: number;
              pageSize: number;
            }) => {
              setPage(newPage);
              setPageSize(newPageSize);
            }}
          />
        </Column>
      </Grid>
    </>
  );
};

export default UnifiedResults;
