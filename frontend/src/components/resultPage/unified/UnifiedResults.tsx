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
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import ESignatureButton, {
  SignatureMeaning,
} from "../../esignature/ESignatureButton";
import PolymorphicResultCell, {
  ResultCellRow,
  worklistRowKey,
} from "./PolymorphicResultCell";
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
import ExpandedPanel, {
  DilutionDraft,
  IdValue,
  NoteDraft,
  PanelRow,
  RejectDraft,
} from "./ExpandedPanel";
import { ReferralDraft } from "./ReferralAction";
import { SectionLayout, loadSectionLayout } from "./sectionLayout";
import { FlagChip, accentClass } from "./flags";
import Avatar from "./Avatar";
import "./unified-results.scss";

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

interface WorklistRow extends ResultCellRow, PanelRow {
  accessionNumber?: string;
  sequenceNumber?: string;
  testName?: string;
  patientInfo?: string;
  patientName?: string;
  sampleType?: string;
  normalRange?: string;
  analysisStatusId?: string;
  analysisLastupdated?: string;
  testResultComponentId?: string;
  testMethod?: string;
  analyzerId?: string;
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
  // ---- R2 (OGC-1021) panel state ----
  const [expandedRowKey, setExpandedRowKey] = useState<string | null>(null);
  const [methods, setMethods] = useState<IdValue[]>([]);
  const [analyzers, setAnalyzers] = useState<IdValue[]>([]);
  const [noteDrafts, setNoteDrafts] = useState<Record<string, NoteDraft>>({});
  const [dilutionDrafts, setDilutionDrafts] = useState<
    Record<string, DilutionDraft>
  >({});
  // analyzerId as loaded — the provenance snapshot for FR-B2
  const [loadedAnalyzers, setLoadedAnalyzers] = useState<
    Record<string, string>
  >({});
  const [sectionLayout, setSectionLayout] = useState<SectionLayout>(() =>
    loadSectionLayout(),
  );
  // ---- R4 (OGC-1023) NCE / referral / rejection state ----
  const { configurationProperties } = useContext(ConfigurationContext) as {
    configurationProperties?: Record<string, string>;
  };
  const allowResultRejection =
    configurationProperties?.allowResultRejection === "true";
  const [nceOpenKey, setNceOpenKey] = useState<string | null>(null);
  const [referralOrganizations, setReferralOrganizations] = useState<IdValue[]>(
    [],
  );
  const [referralReasons, setReferralReasons] = useState<IdValue[]>([]);
  const [rejectReasons, setRejectReasons] = useState<IdValue[]>([]);
  const [referralDrafts, setReferralDrafts] = useState<
    Record<string, ReferralDraft>
  >({});
  const [rejectDrafts, setRejectDrafts] = useState<Record<string, RejectDraft>>(
    {},
  );

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
    // R2 (FR-B1): Method and Analyzer are separate fields with their own lists
    getFromOpenElisServer("/rest/displayList/METHODS", (list: IdValue[]) =>
      setMethods(list || []),
    );
    getFromOpenElisServer(
      "/rest/displayList/ANALYZER_LIST",
      (list: IdValue[]) => setAnalyzers(list || []),
    );
    // R4 (FR-F2/E3): referral target + reason lists, rejection reasons
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (list: IdValue[]) => setReferralOrganizations(list || []),
    );
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_REASONS",
      (list: IdValue[]) => setReferralReasons(list || []),
    );
    getFromOpenElisServer(
      "/rest/displayList/REJECTION_REASONS",
      (list: IdValue[]) => setRejectReasons(list || []),
    );
  }, []);

  const applyLoadedRows = useCallback(
    (results: { testResult?: WorklistRow[] }) => {
      const loaded = (results?.testResult || []).filter((r) => r.analysisId);
      setRows(loaded);
      const states: Record<string, RowEditState> = {};
      for (const row of loaded) {
        // one analysis may render N component rows (FR-A′1) — each row keeps
        // its own edit state under its composite key
        states[worklistRowKey(row)] = initialRowState(
          Boolean(row.resultValue) ||
            Boolean(
              row.multiSelectResultValues &&
              row.multiSelectResultValues !== "{}",
            ),
        );
      }
      setRowStates(states);
      const loadedByKey: Record<string, string> = {};
      for (const row of loaded) {
        if (row.analyzerId) {
          loadedByKey[worklistRowKey(row)] = row.analyzerId;
        }
      }
      setLoadedAnalyzers(loadedByKey);
      setNoteDrafts({});
      setDilutionDrafts({});
      setReferralDrafts({});
      setRejectDrafts({});
      setNceOpenKey(null);
      setExpandedRowKey(null);
      setStaleInfo({});
      setEditingAnalysisId(null);
      setPage(1);
      setLoading(false);
    },
    [],
  );

  const loadWorklist = useCallback(
    (labNumberOverride?: string) => {
      setLoading(true);
      const params = new URLSearchParams();
      // guard: when wired directly to onClick the argument is the click
      // event — only a string counts as an override
      const labNumber =
        typeof labNumberOverride === "string" ? labNumberOverride : searchText;
      if (labNumber) {
        params.set("labNumber", labNumber);
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
      // FRS: the selected Lab Unit (and filters) are the page's primary
      // state — keep them in the URL so refresh and share links reproduce
      // the same worklist
      const urlState = new URLSearchParams(window.location.search);
      const setOrDrop = (key: string, value: string) =>
        value ? urlState.set(key, value) : urlState.delete(key);
      setOrDrop("accessionNumber", labNumber);
      setOrDrop("testSectionId", selectedLabUnit);
      setOrDrop("collectionDate", collectionDate);
      const query = urlState.toString();
      window.history.replaceState(
        null,
        "",
        query ? `/Results?${query}` : "/Results",
      );
    },
    [searchText, selectedLabUnit, collectionDate, applyLoadedRows],
  );

  useEffect(() => {
    if (selectedLabUnit) {
      loadWorklist();
    }
  }, [selectedLabUnit]);

  // Restore the worklist from the URL: deep links from the in-progress
  // dashboard (?accessionNumber=) and refreshes of a loaded page
  // (?testSectionId=&collectionDate=&status=) reproduce the same view
  useEffect(() => {
    const urlState = new URLSearchParams(window.location.search);
    const accession = urlState.get("accessionNumber");
    const unit = urlState.get("testSectionId");
    const date = urlState.get("collectionDate");
    const status = urlState.get("status");
    if (date) {
      setCollectionDate(date);
    }
    if (status) {
      setStatusFilter(status);
    }
    if (accession) {
      setSearchText(accession);
    }
    if (unit) {
      setSelectedLabUnit(unit); // triggers the worklist load effect
    } else if (accession) {
      loadWorklist(accession);
    }
  }, []);

  // Keep the status chip in the URL too (client-side filter, no refetch)
  useEffect(() => {
    const urlState = new URLSearchParams(window.location.search);
    if (statusFilter === "ALL") {
      urlState.delete("status");
    } else {
      urlState.set("status", statusFilter);
    }
    const query = urlState.toString();
    window.history.replaceState(
      null,
      "",
      query ? `/Results?${query}` : "/Results",
    );
  }, [statusFilter]);

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
      target: WorklistRow,
      field: "resultValue" | "multiSelectResultValues",
      value: string,
    ) => {
      // FR-A′3: a multi-component analysis renders one row per component —
      // update ONLY the edited row (keyed by analysisId + componentId), never
      // its sibling component rows
      const key = worklistRowKey(target);
      setRows((current) =>
        current.map((row) =>
          worklistRowKey(row) === key ? { ...row, [field]: value } : row,
        ),
      );
      setRowStates((current) => ({
        ...current,
        [key]: nextRowState(current[key] || "EMPTY", {
          type: "VALUE_CHANGED",
        }),
      }));
      // FR-O3: entering a fresh result counts as having the analysis "open
      // in Edit" — colleagues should see the presence hint for this row too
      setEditingAnalysisId(target.analysisId);
    },
    [],
  );

  // R2 (FR-B1): method/analyzer edits are part of the analysis record — they
  // participate in the same edit-state machine as the value itself
  const handleFieldChange = useCallback(
    (
      target: WorklistRow,
      field: "testMethod" | "analyzerId",
      value: string,
    ) => {
      const key = worklistRowKey(target);
      setRows((current) =>
        current.map((row) =>
          worklistRowKey(row) === key ? { ...row, [field]: value } : row,
        ),
      );
      setRowStates((current) => ({
        ...current,
        [key]: nextRowState(current[key] || "EMPTY", {
          type: "VALUE_CHANGED",
        }),
      }));
      setEditingAnalysisId(target.analysisId);
    },
    [],
  );

  // R4: a referral or rejection is part of the analysis record — setting a
  // draft dirties the row so it goes through the same Save (e-signature)
  const markRowDirty = useCallback((target: WorklistRow) => {
    const key = worklistRowKey(target);
    setRowStates((current) => ({
      ...current,
      [key]: nextRowState(current[key] || "EMPTY", { type: "VALUE_CHANGED" }),
    }));
    setEditingAnalysisId(target.analysisId);
  }, []);

  const handleReferralDraftChange = useCallback(
    (target: WorklistRow, draft: ReferralDraft | null) => {
      const key = worklistRowKey(target);
      setReferralDrafts((current) => {
        const next = { ...current };
        if (draft) {
          next[key] = draft;
        } else {
          delete next[key];
        }
        return next;
      });
      if (draft) {
        markRowDirty(target);
      }
    },
    [markRowDirty],
  );

  const handleRejectDraftChange = useCallback(
    (target: WorklistRow, draft: RejectDraft | null) => {
      const key = worklistRowKey(target);
      setRejectDrafts((current) => {
        const next = { ...current };
        if (draft) {
          next[key] = draft;
        } else {
          delete next[key];
        }
        return next;
      });
      if (draft) {
        markRowDirty(target);
      }
    },
    [markRowDirty],
  );

  const handleEdit = useCallback((target: WorklistRow) => {
    const key = worklistRowKey(target);
    setRowStates((current) => ({
      ...current,
      [key]: nextRowState(current[key] || "SAVED", {
        type: "EDIT_CLICKED",
      }),
    }));
    setEditingAnalysisId(target.analysisId);
  }, []);

  const handleSaveResponse = useCallback(
    (target: WorklistRow, response: SaveResponse | undefined) => {
      if (!response) {
        return;
      }
      const key = worklistRowKey(target);
      if (response.status === 409) {
        // FR-O2: the stale editor loses — nothing merged, refresh offered.
        setStaleInfo((current) => ({
          ...current,
          [key]: {
            modifiedBy: response.modifiedBy,
            modifiedAt: response.modifiedAt,
          },
        }));
        setRowStates((current) => ({
          ...current,
          [key]: nextRowState(current[key] || "EDITING", {
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
        [key]: nextRowState(current[key] || "EDITING", {
          type: "SAVE_SUCCEEDED",
        }),
      }));
      if (response.analysisLastupdated) {
        // the version token is per ANALYSIS — refresh it on every component
        // row of this analysis so a sibling save isn't falsely rejected
        setRows((current) =>
          current.map((row) =>
            row.analysisId === target.analysisId
              ? { ...row, analysisLastupdated: response.analysisLastupdated }
              : row,
          ),
        );
      }
      setStaleInfo((current) => {
        const next = { ...current };
        delete next[key];
        return next;
      });
      setEditingAnalysisId((current) =>
        current === target.analysisId ? null : current,
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
      delete item.analysisNotes;
      // TestResultItem serializes reportable as "Y"/"N" but deserializes it
      // as boolean — same normalization the legacy page applies before POST
      item.reportable = item.reportable !== "N";
      const key = worklistRowKey(row);
      // R2 (FR-J1/J2): the dual-axis note — visibility chosen, context auto-set
      // from the edit-state machine (Modification when editing a saved result)
      const noteDraft = noteDrafts[key];
      if (noteDraft && noteDraft.text.trim()) {
        item.note = noteDraft.text.trim();
        item.noteVisibility = noteDraft.visibility;
        item.noteContext =
          rowStates[key] === "EDITING" ? "MODIFICATION" : "ENTRY";
      }
      // R2 (FR-D5): dilution provenance — the reported value is already in
      // resultValue; factor + measured value ride along for the audit note
      const dilution = dilutionDrafts[key];
      if (dilution && dilution.factor.trim()) {
        item.dilutionFactor = dilution.factor.trim();
        item.measuredValue = dilution.measuredValue.trim();
      }
      // R4 (FR-F2/F3): the referral rides the row's save — legacy
      // handleReferrals path; the referred test is this row's own test (no
      // test-to-perform field per FRS)
      const referral = referralDrafts[key];
      if (
        referral &&
        referral.referredInstituteId &&
        referral.referralReasonId
      ) {
        item.refer = true;
        item.referredOut = true;
        item.referralItem = {
          referralReasonId: referral.referralReasonId,
          referredInstituteId: referral.referredInstituteId,
          referredSendDate: referral.referredSendDate,
          referredTestId: row.testId,
        };
      }
      // R4 (FR-E3): reject disposition — legacy shadowRejected mechanics
      // (clears the value, writes the rejection-reason note, TechnicalRejected)
      const reject = rejectDrafts[key];
      if (reject && reject.rejectReasonId) {
        item.rejected = true;
        item.shadowRejected = true;
        item.rejectReasonId = reject.rejectReasonId;
      }
      postToOpenElisServerJsonResponse(
        `/rest/results-entry/analysis/${row.analysisId}/result`,
        JSON.stringify({ testResult: item }),
        (response: SaveResponse | undefined) => {
          handleSaveResponse(row, response);
          if (response && (!response.status || response.status < 400)) {
            setNoteDrafts((current) => {
              const next = { ...current };
              delete next[key];
              return next;
            });
            setDilutionDrafts((current) => {
              const next = { ...current };
              delete next[key];
              return next;
            });
            setReferralDrafts((current) => {
              const next = { ...current };
              delete next[key];
              return next;
            });
            setRejectDrafts((current) => {
              const next = { ...current };
              delete next[key];
              return next;
            });
          }
        },
      );
    },
    [
      handleSaveResponse,
      noteDrafts,
      dilutionDrafts,
      referralDrafts,
      rejectDrafts,
      rowStates,
    ],
  );

  // Gallery parity: accession leads (mono accent), identity as a sub-line, a
  // patient initials avatar on clinical rows (FR-M2/M3: no patient identity
  // outside CLINICAL — sample context replaces it)
  const subjectCell = (row: WorklistRow): React.ReactNode => {
    const accession = row.accessionNumber || "";
    const subline =
      domain === "CLINICAL"
        ? [row.patientName, row.patientInfo].filter(Boolean).join(" · ")
        : row.sampleType || "";
    return (
      <div className="unifiedSubjectCell">
        {domain === "CLINICAL" && (
          <Avatar name={row.patientName} id={row.patientId as string} />
        )}
        <div>
          <div className="unifiedAccession">{accession}</div>
          {subline && <div className="unifiedSubjectSub">{subline}</div>}
        </div>
      </div>
    );
  };

  const statusName = (statusId?: string): string =>
    statusOptions.find((s) => s.id === statusId)?.value || statusId || "";

  /** Carbon tag color per status name — never conveys status by color alone. */
  const statusTagType = (statusId?: string): string => {
    const name = statusName(statusId).toLowerCase();
    if (name.includes("final")) return "green";
    if (name.includes("reject") || name.includes("cancel")) return "red";
    if (name.includes("acceptance")) return "teal";
    if (name.includes("not tested") || name.includes("pending")) return "gray";
    return "cool-gray";
  };

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
          {/* Carbon Search's labelText is visually hidden; render an explicit
              label so the toolbar fields align on one horizontal level */}
          <div className="cds--label">
            <FormattedMessage id="label.button.search" />
          </div>
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
          {/* spacer keeps the button on the same level as the labeled fields */}
          <div className="cds--label">&nbsp;</div>
          <Button onClick={() => loadWorklist()} disabled={loading}>
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
                  <TableHeader className="unifiedExpandHeader" />
                  <TableHeader>
                    {formatDomainMessage(intl, "label.results.subject", domain)}
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.test" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.method" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.analyzer" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.sample" />
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
                    <FormattedMessage id="label.results.flag" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.results.actions" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {pagedRows.map((row) => {
                  const key = worklistRowKey(row);
                  const state = rowStates[key] || "EMPTY";
                  const stale = staleInfo[key];
                  const reviewer = presence[row.analysisId];
                  const isExpanded = expandedRowKey === key;
                  return (
                    <React.Fragment key={key}>
                      <TableRow>
                        <TableCell className="unifiedExpandCell">
                          <Button
                            kind="ghost"
                            size="sm"
                            aria-expanded={isExpanded}
                            aria-label={intl.formatMessage({
                              id: isExpanded
                                ? "label.results.collapseRow"
                                : "label.results.expandRow",
                            })}
                            className="unifiedExpandButton"
                            onClick={() =>
                              setExpandedRowKey(isExpanded ? null : key)
                            }
                          >
                            {isExpanded ? "▼" : "▶"}
                          </Button>
                        </TableCell>
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
                        <TableCell className="unifiedTestCell">
                          {row.testName}
                        </TableCell>
                        <TableCell className="unifiedResultsSmallCell">
                          {methods.find((m) => m.id === row.testMethod)
                            ?.value ||
                            row.testMethod ||
                            "—"}
                          {loadedAnalyzers[key] && (
                            <Tag
                              type="cool-gray"
                              size="sm"
                              className="unifiedProvenance"
                            >
                              <FormattedMessage id="label.results.fromAnalyzer" />
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell className="unifiedResultsSmallCell">
                          {analyzers.find((a) => a.id === row.analyzerId)
                            ?.value ||
                            row.analyzerId ||
                            "—"}
                        </TableCell>
                        <TableCell className="unifiedResultsSmallCell">
                          {row.sampleType || "—"}
                        </TableCell>
                        <TableCell>
                          {row.normalRange}{" "}
                          {row.unitsOfMeasure ? row.unitsOfMeasure : ""}
                        </TableCell>
                        <TableCell>
                          <span className={accentClass(row.resultFlag)}>
                            <PolymorphicResultCell
                              row={row}
                              editable={isRowEditable(state)}
                              onValueChange={(field, value) =>
                                handleValueChange(row, field, value)
                              }
                            />
                          </span>
                        </TableCell>
                        <TableCell>
                          {statusName(row.analysisStatusId) ? (
                            <Tag
                              size="sm"
                              type={statusTagType(row.analysisStatusId)}
                            >
                              {statusName(row.analysisStatusId)}
                            </Tag>
                          ) : (
                            "—"
                          )}
                        </TableCell>
                        <TableCell>
                          <FlagChip flag={row.resultFlag} />
                        </TableCell>
                        <TableCell>
                          {showEdit(state) && (
                            <Button
                              kind="tertiary"
                              size="sm"
                              onClick={() => handleEdit(row)}
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
                      {isExpanded && (
                        <TableRow className="unifiedExpandedRow">
                          <TableCell colSpan={11}>
                            <ExpandedPanel
                              row={row}
                              domain={domain}
                              editable={isRowEditable(state)}
                              editing={state === "EDITING"}
                              loadedAnalyzerId={loadedAnalyzers[key]}
                              methods={methods}
                              analyzers={analyzers}
                              noteDraft={
                                noteDrafts[key] || {
                                  text: "",
                                  visibility: "I",
                                }
                              }
                              dilutionDraft={
                                dilutionDrafts[key] || {
                                  measuredValue: "",
                                  factor: "",
                                }
                              }
                              sectionLayout={sectionLayout}
                              onSectionLayoutChange={setSectionLayout}
                              onFieldChange={(field, value) =>
                                handleFieldChange(row, field, value)
                              }
                              onValueChange={(field, value) =>
                                handleValueChange(row, field, value)
                              }
                              onNoteDraftChange={(draft) =>
                                setNoteDrafts((current) => ({
                                  ...current,
                                  [key]: draft,
                                }))
                              }
                              onDilutionDraftChange={(draft) =>
                                setDilutionDrafts((current) => ({
                                  ...current,
                                  [key]: draft,
                                }))
                              }
                              allowResultRejection={allowResultRejection}
                              nceOpen={nceOpenKey === key}
                              onNceOpenChange={(open) =>
                                setNceOpenKey(open ? key : null)
                              }
                              referralOrganizations={referralOrganizations}
                              referralReasons={referralReasons}
                              referralDraft={referralDrafts[key] || null}
                              onReferralDraftChange={(draft) =>
                                handleReferralDraftChange(row, draft)
                              }
                              rejectReasons={rejectReasons}
                              rejectDraft={rejectDrafts[key] || null}
                              onRejectDraftChange={(draft) =>
                                handleRejectDraftChange(row, draft)
                              }
                              actions={
                                <>
                                  {showEdit(state) && (
                                    <Button
                                      kind="tertiary"
                                      size="sm"
                                      onClick={() => handleEdit(row)}
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
                                </>
                              }
                            />
                          </TableCell>
                        </TableRow>
                      )}
                      {stale && (
                        <TableRow>
                          <TableCell colSpan={11}>
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
                                  onClick={() => loadWorklist()}
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
