import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import ReactDOM from "react-dom";
import {
  Button,
  Column,
  DataTable,
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  FilterableMultiSelect,
  Grid,
  InlineLoading,
  InlineNotification,
  Link,
  MultiSelect,
  Pagination,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  DismissibleTag,
  ClickableTile,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServerForBlob,
  putToOpenElisServer,
  toLocalIsoDate,
  parseIsoDate,
} from "../../utils/Utils";
import CustomLabNumberInput from "../../common/CustomLabNumberInput";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import SearchPatientForm from "../../patient/SearchPatientForm";
import config from "../../../config.json";
import "./ReportPrintQueue.scss";

const PAGE_SIZES = [10, 20, 50, 100];
const WINDOWS = ["24h", "7d", "30d", "all", "custom"];
const STATE_TAG = { PARTIAL: "warm-gray", FINAL: "teal", AMENDED: "magenta" };

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "common.reports", link: "/RoutineReports" },
  { label: "reportQueue.title", link: "/ReportPrintQueue" },
];

const PRINT_STATUSES = ["UNPRINTED", "PRINTED"];

/** Lab numbers of one format compare as text, digits by value. */
const REQUESTER_SEARCH_PAGE_SIZE = 200;
const REQUESTER_SEARCH_MAX_PAGES = 10;

/**
 * Requester options from provider search rows. A requester typed freely at
 * order entry gets a person record per order, so one name is offered once and
 * filters on all its records.
 */
export const requesterOptionsByName = (providers) => {
  const byName = new Map();
  (providers || []).forEach((provider) => {
    const text =
      provider.displayName ||
      provider.name ||
      `${provider.lastName || ""}, ${provider.firstName || ""}`;
    const id = String(provider.personId || provider.id);
    const option = byName.get(text) || { id, text, ids: [] };
    if (!option.ids.includes(id)) {
      option.ids.push(id);
    }
    byName.set(text, option);
  });
  return [...byName.values()];
};

/**
 * Whether a lab number range ends before it starts, compared as the server
 * compares and queries it: plain text, so DEV10 comes before DEV9.
 */
export const isRangeReversed = (from, to) =>
  Boolean(from) && Boolean(to) && String(to) < String(from);

function formatDateTime(iso) {
  if (!iso) {
    return "";
  }
  const date = new Date(iso);
  return isNaN(date.getTime()) ? iso : date.toLocaleString();
}

function formatDate(iso) {
  if (!iso) {
    return "";
  }
  const date = new Date(iso);
  return isNaN(date.getTime()) ? iso : date.toLocaleDateString();
}

/**
 * The Report Print Queue (OGC-431, FRS OGC-1031 r4 V1): every patient report
 * with a released result, with its Partial / Final / Amended state and whether
 * its current content has been printed, printed through one pipeline that
 * keeps every version as issued.
 */
export default function ReportPrintQueue() {
  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();
  const initialParams = useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );
  const initialLabNo = initialParams.get("labNo") || "";
  const initialPrintStatus = (
    initialParams.get("printStatus") || ""
  ).toUpperCase();

  const [labNoInput, setLabNoInput] = useState(initialLabNo);
  const [labNoToInput, setLabNoToInput] = useState("");
  const [rangeMode, setRangeMode] = useState(false);
  const [rangeReversed, setRangeReversed] = useState(false);
  const [lookup, setLookup] = useState(
    initialLabNo ? { labNo: initialLabNo, labNoTo: "", patient: null } : null,
  );
  const [showPatientSearch, setShowPatientSearch] = useState(false);
  const [states, setStates] = useState([]);
  const [printStatus, setPrintStatus] = useState(
    PRINT_STATUSES.includes(initialPrintStatus) ? initialPrintStatus : "ALL",
  );
  const [window_, setWindow] = useState(null);
  const [customFrom, setCustomFrom] = useState("");
  const [customTo, setCustomTo] = useState("");
  const [facilities, setFacilities] = useState([]);
  const [wards, setWards] = useState([]);
  const [requesters, setRequesters] = useState([]);
  const [facilityOptions, setFacilityOptions] = useState([]);
  const [wardOptions, setWardOptions] = useState([]);
  const [requesterOptions, setRequesterOptions] = useState([]);
  const [sort, setSort] = useState({ key: "released", dir: "desc" });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(null);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [notice, setNotice] = useState(null);
  const [printing, setPrinting] = useState(null);
  const [details, setDetails] = useState({});
  const requestId = useRef(0);
  const lastFacilityQuery = useRef("");
  const lastRequesterQuery = useRef("");

  const stateItems = useMemo(
    () => [
      {
        id: "PARTIAL",
        text: intl.formatMessage({ id: "reportQueue.state.partialName" }),
      },
      {
        id: "FINAL",
        text: intl.formatMessage({ id: "reportQueue.state.final" }),
      },
      {
        id: "AMENDED",
        text: intl.formatMessage({ id: "reportQueue.state.amended" }),
      },
    ],
    [intl],
  );
  const printStatusItems = useMemo(
    () => [
      {
        id: "ALL",
        text: intl.formatMessage({ id: "common.all", defaultMessage: "All" }),
      },
      {
        id: "UNPRINTED",
        text: intl.formatMessage({ id: "reportQueue.printStatus.unprinted" }),
      },
      {
        id: "PRINTED",
        text: intl.formatMessage({ id: "reportQueue.printStatus.printed" }),
      },
    ],
    [intl],
  );
  const windowItems = useMemo(
    () =>
      WINDOWS.map((id) => ({
        id,
        text: intl.formatMessage({ id: `reportQueue.filter.window.${id}` }),
      })),
    [intl],
  );

  const headers = useMemo(
    () => [
      {
        key: "labNumber",
        header: intl.formatMessage({ id: "reportQueue.col.labNoName" }),
        sortable: true,
      },
      {
        key: "subject",
        header: intl.formatMessage({ id: "reportQueue.col.subject" }),
        sortable: true,
      },
      {
        key: "details",
        header: intl.formatMessage({ id: "reportQueue.col.details" }),
      },
      {
        key: "released",
        header: intl.formatMessage({ id: "reportQueue.col.released" }),
        sortable: true,
      },
      {
        key: "state",
        header: intl.formatMessage({ id: "reportQueue.col.state" }),
        sortable: true,
      },
      {
        key: "printStatus",
        header: intl.formatMessage({ id: "reportQueue.col.printStatus" }),
        sortable: true,
      },
      {
        key: "version",
        header: intl.formatMessage({ id: "reportQueue.col.version" }),
      },
      {
        key: "action",
        header: intl.formatMessage({
          id: "common.action",
          defaultMessage: "Action",
        }),
      },
    ],
    [intl],
  );

  const load = useCallback(() => {
    if (!window_ || !pageSize) {
      return;
    }
    const params = new URLSearchParams();
    if (lookup?.labNo) {
      params.set("labNo", lookup.labNo);
      if (lookup.labNoTo) {
        params.set("labNoTo", lookup.labNoTo);
      }
    } else if (lookup?.patient) {
      params.set("patientId", lookup.patient.id);
    } else {
      states.forEach((state) => params.append("states", state));
      if (printStatus !== "ALL") {
        params.set("printStatus", printStatus);
      }
      if (window_) {
        params.set("window", window_);
        if (window_ === "custom") {
          if (customFrom) params.set("from", customFrom);
          if (customTo) params.set("to", customTo);
        }
      }
      facilities.forEach((item) => params.append("facilityIds", item.id));
      wards.forEach((item) => params.append("wardIds", item.id));
      requesters.forEach((item) =>
        (item.ids || [item.id]).forEach((id) =>
          params.append("requesterIds", id),
        ),
      );
    }
    params.set("sort", sort.key);
    params.set("sortDir", sort.dir);
    params.set("page", String(page));
    if (pageSize) {
      params.set("pageSize", String(pageSize));
    }
    const id = ++requestId.current;
    setLoading(true);
    setLoadError(false);
    getFromOpenElisServer(
      `/rest/reportQueue?${params.toString()}`,
      (response) => {
        if (id !== requestId.current) {
          return;
        }
        if (!response || !Array.isArray(response.rows)) {
          setLoadError(true);
          setLoading(false);
          return;
        }
        setData(response);
        setLoading(false);
      },
    );
  }, [
    lookup,
    states,
    printStatus,
    window_,
    customFrom,
    customTo,
    facilities,
    wards,
    requesters,
    sort,
    page,
    pageSize,
  ]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  useEffect(() => {
    getFromOpenElisServer("/rest/reportQueue/preferences", (preferences) => {
      ReactDOM.unstable_batchedUpdates(() => {
        setWindow(preferences?.timeWindow || "7d");
        setPageSize(
          PAGE_SIZES.includes(preferences?.pageSize)
            ? preferences.pageSize
            : 20,
        );
      });
    });
  }, []);

  useEffect(() => {
    if (facilities.length === 0) {
      return undefined;
    }
    let cancelled = false;
    const collected = [];
    let pending = facilities.length;
    facilities.forEach((facility) => {
      getFromOpenElisServer(
        `/rest/departments-for-site?refferingSiteId=${facility.id}`,
        (list) => {
          if (cancelled) return;
          (Array.isArray(list) ? list : []).forEach((item) =>
            collected.push({
              id: String(item.id),
              text: `${item.value} (${facility.text})`,
            }),
          );
          pending -= 1;
          if (pending === 0) {
            setWardOptions(collected);
            setWards((current) => {
              const kept = current.filter((ward) =>
                collected.some((option) => option.id === ward.id),
              );
              return kept.length === current.length ? current : kept;
            });
          }
        },
      );
    });
    return () => {
      cancelled = true;
    };
  }, [facilities]);

  const savePreferences = useCallback((timeWindow, size) => {
    putToOpenElisServer(
      "/rest/reportQueue/preferences",
      JSON.stringify({ timeWindow, pageSize: size }),
      () => {},
    );
  }, []);

  const applyLabNo = () => {
    const from = (labNoInput || "").trim();
    if (!from) {
      return;
    }
    const to = rangeMode ? (labNoToInput || "").trim() : "";
    if (isRangeReversed(from, to)) {
      setRangeReversed(true);
      return;
    }
    setRangeReversed(false);
    setPage(1);
    setLookup({ labNo: from, labNoTo: to, patient: null });
  };

  const clearSearch = () => {
    setLookup(null);
    setLabNoInput("");
    setLabNoToInput("");
    setRangeReversed(false);
    setPage(1);
    if (initialLabNo) {
      history.replace(location.pathname);
    }
  };

  const changeFacilities = (next) => {
    setPage(1);
    setFacilities(next);
    if (next.length === 0) {
      setWardOptions([]);
      setWards([]);
    }
  };

  const clearFilters = () => {
    setStates([]);
    setPrintStatus("ALL");
    setFacilities([]);
    setWardOptions([]);
    setWards([]);
    setRequesters([]);
    setPage(1);
  };

  const searchFacilities = (text) => {
    if (
      !text ||
      text.trim().length < 2 ||
      text.trim() === lastFacilityQuery.current
    ) {
      return;
    }
    lastFacilityQuery.current = text.trim();
    getFromOpenElisServer(
      `/rest/organization/search?search=${encodeURIComponent(text.trim())}&pageSize=20`,
      (response) => {
        const list = response?.organizations || response?.results || [];
        setFacilityOptions(
          list.map((org) => ({
            id: String(org.id),
            text: org.organizationName || org.name,
          })),
        );
      },
    );
  };

  const searchRequesters = (text) => {
    if (
      !text ||
      text.trim().length < 2 ||
      text.trim() === lastRequesterQuery.current
    ) {
      return;
    }
    const query = text.trim();
    lastRequesterQuery.current = query;
    const found = [];
    const loadPage = (pageNumber) => {
      getFromOpenElisServer(
        `/rest/provider/search?search=${encodeURIComponent(query)}&pageSize=${REQUESTER_SEARCH_PAGE_SIZE}&page=${pageNumber}`,
        (response) => {
          if (lastRequesterQuery.current !== query) {
            return;
          }
          const list = response?.providers || response?.results || [];
          found.push(...list);
          const total = Number(response?.totalCount) || 0;
          if (
            list.length === REQUESTER_SEARCH_PAGE_SIZE &&
            found.length < total &&
            pageNumber < REQUESTER_SEARCH_MAX_PAGES
          ) {
            loadPage(pageNumber + 1);
            return;
          }
          setRequesterOptions(requesterOptionsByName(found));
        },
      );
    };
    loadPage(1);
  };

  const loadDetail = useCallback((sampleId) => {
    setDetails((current) => ({ ...current, [sampleId]: { loading: true } }));
    getFromOpenElisServer(`/rest/reportQueue/orders/${sampleId}`, (detail) => {
      setDetails((current) => ({
        ...current,
        [sampleId]:
          detail && detail.sampleId
            ? { loading: false, detail }
            : { loading: false, error: true },
      }));
    });
  }, []);

  const handlePrint = (row) => {
    setPrinting(row.sampleId);
    setNotice(null);
    postToOpenElisServerForBlob(
      "/rest/reportQueue/print",
      JSON.stringify({ sampleId: row.sampleId }),
      (blob, response) => {
        const url = URL.createObjectURL(blob);
        const opened = window.open(url, "_blank");
        setTimeout(() => URL.revokeObjectURL(url), opened ? 60000 : 600000);
        const version = response?.headers?.get("X-Report-Version");
        const reprint = response?.headers?.get("X-Report-Reprint") === "true";
        const issued = intl.formatMessage(
          {
            id: reprint
              ? "reportQueue.print.reprinted"
              : "reportQueue.print.issued",
          },
          { n: version, labNumber: row.labNumber },
        );
        setNotice(
          opened
            ? {
                kind: "success",
                title: intl.formatMessage(
                  { id: "reportQueue.print.success" },
                  { count: 1 },
                ),
                subtitle: issued,
              }
            : {
                kind: "warning",
                title: intl.formatMessage(
                  { id: "reportQueue.print.popupBlocked" },
                  { labNumber: row.labNumber },
                ),
                subtitle: issued,
                link: url,
              },
        );
        setPrinting(null);
        if (details[row.sampleId]) {
          loadDetail(row.sampleId);
        }
        load();
      },
      () => {
        setNotice({
          kind: "error",
          title: intl.formatMessage(
            { id: "reportQueue.print.failed" },
            { count: 1, labNumbers: row.labNumber },
          ),
        });
        setPrinting(null);
      },
    );
  };

  const onHeaderClick = (header) => {
    if (!header.sortable) {
      return;
    }
    setPage(1);
    setSort((current) =>
      current.key === header.key
        ? { key: header.key, dir: current.dir === "desc" ? "asc" : "desc" }
        : { key: header.key, dir: header.key === "released" ? "desc" : "asc" },
    );
  };

  const rows = (data?.rows || []).map((row) => ({
    ...row,
    id: String(row.sampleId),
  }));
  const lookupActive = !!lookup;
  const filtersActive =
    states.length > 0 ||
    printStatus !== "ALL" ||
    facilities.length > 0 ||
    wards.length > 0 ||
    requesters.length > 0;

  const stateTag = (row) => {
    const partial = row.releasedCount < row.totalCount;
    let label;
    if (row.state === "AMENDED") {
      label = partial
        ? intl.formatMessage(
            { id: "reportQueue.state.amendedPartial" },
            { released: row.releasedCount, total: row.totalCount },
          )
        : intl.formatMessage({ id: "reportQueue.state.amended" });
    } else if (row.state === "PARTIAL") {
      label = intl.formatMessage(
        { id: "reportQueue.state.partial" },
        { released: row.releasedCount, total: row.totalCount },
      );
    } else {
      label = intl.formatMessage({ id: "reportQueue.state.final" });
    }
    return (
      <>
        <Tag
          type={STATE_TAG[row.state] || "gray"}
          size="sm"
          data-testid={`state-${row.sampleId}`}
        >
          {label}
        </Tag>
        {row.critical && (
          <Tag type="red" size="sm">
            <FormattedMessage id="common.critical" defaultMessage="Critical" />
          </Tag>
        )}
      </>
    );
  };

  const printStatusTag = (row) => (
    <Tag
      type={row.printStatus === "PRINTED" ? "green" : "purple"}
      size="sm"
      data-testid={`print-status-${row.sampleId}`}
    >
      <FormattedMessage
        id={
          row.printStatus === "PRINTED"
            ? "reportQueue.printStatus.printed"
            : "reportQueue.printStatus.unprinted"
        }
      />
    </Tag>
  );

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="orderLegendBody" data-testid="report-print-queue">
        <h2 style={{ marginBottom: "0.25rem" }}>
          <FormattedMessage id="reportQueue.title" />
        </h2>
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="reportQueue.subtitle" />
        </p>

        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "reportQueue.guidance.lead" })}
          subtitle={intl.formatMessage({ id: "reportQueue.guidance.body" })}
          style={{ maxWidth: "100%", marginBottom: "0.5rem" }}
        />
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "0.75rem",
            marginBottom: "0.75rem",
          }}
          data-testid="legend"
        >
          {[
            [
              "PARTIAL",
              "reportQueue.state.partialName",
              "reportQueue.legend.partial",
            ],
            ["FINAL", "reportQueue.state.final", "reportQueue.legend.final"],
            [
              "AMENDED",
              "reportQueue.state.amended",
              "reportQueue.legend.amended",
            ],
          ].map(([state, nameKey, legendKey]) => (
            <span
              key={state}
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "0.25rem",
              }}
            >
              <Tag type={STATE_TAG[state]} size="sm">
                <FormattedMessage id={nameKey} />
              </Tag>
              <small>
                <FormattedMessage id={legendKey} />
              </small>
            </span>
          ))}
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.25rem",
            }}
          >
            <Tag type="purple" size="sm">
              <FormattedMessage id="reportQueue.printStatus.unprinted" />
            </Tag>
            <small>
              <FormattedMessage id="reportQueue.legend.unprinted" />
            </small>
          </span>
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.25rem",
            }}
          >
            <Tag type="green" size="sm">
              <FormattedMessage id="reportQueue.printStatus.printed" />
            </Tag>
            <small>
              <FormattedMessage id="reportQueue.legend.printed" />
            </small>
          </span>
        </div>

        <Grid narrow style={{ marginBottom: "0.75rem" }}>
          <Column lg={4} md={4} sm={4}>
            <ClickableTile
              data-testid="tile-to-print"
              style={{ minHeight: "auto", padding: "0.5rem 1rem" }}
              onClick={() => {
                clearSearch();
                setPrintStatus("UNPRINTED");
                setStates([]);
              }}
            >
              <p style={{ fontSize: "1.5rem", fontWeight: "600" }}>
                {data ? data.toPrint : "–"}
              </p>
              <p>
                <FormattedMessage id="reportQueue.tile.toPrint" />
              </p>
            </ClickableTile>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <ClickableTile
              data-testid="tile-amended-to-print"
              style={{ minHeight: "auto", padding: "0.5rem 1rem" }}
              onClick={() => {
                clearSearch();
                setPrintStatus("UNPRINTED");
                setStates(["AMENDED"]);
              }}
            >
              <p style={{ fontSize: "1.5rem", fontWeight: "600" }}>
                {data ? data.amendedToPrint : "–"}
              </p>
              <p>
                <FormattedMessage id="reportQueue.tile.amendedToPrint" />
              </p>
            </ClickableTile>
          </Column>
        </Grid>

        <Grid narrow style={{ marginBottom: "0.5rem", alignItems: "flex-end" }}>
          <Column lg={4} md={4} sm={4}>
            <CustomLabNumberInput
              id="queue-lab-no"
              name="queueLabNo"
              labelText={intl.formatMessage({
                id: "common.labNumber",
                defaultMessage: "Lab Number",
              })}
              placeholder={intl.formatMessage({
                id: "reportQueue.filter.labNo.placeholder",
              })}
              helperText={intl.formatMessage({
                id: "reportQueue.filter.labNo.helper",
              })}
              value={labNoInput}
              onChange={(event, rawValue) => {
                setRangeReversed(false);
                setLabNoInput(rawValue ?? event.target.value);
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  applyLabNo();
                }
              }}
            />
          </Column>
          <Column lg={2} md={2} sm={4}>
            <Toggle
              id="queue-lab-range"
              labelText={intl.formatMessage({
                id: "reportQueue.filter.labNo.range",
              })}
              size="sm"
              toggled={rangeMode}
              onToggle={(value) => setRangeMode(value)}
            />
          </Column>
          {rangeMode && (
            <Column lg={4} md={4} sm={4}>
              <CustomLabNumberInput
                id="queue-lab-no-to"
                name="queueLabNoTo"
                labelText={intl.formatMessage({
                  id: "reportQueue.filter.labNo.to",
                })}
                value={labNoToInput}
                invalid={rangeReversed}
                invalidText={intl.formatMessage({
                  id: "reportQueue.filter.labNo.rangeReversed",
                })}
                onChange={(event, rawValue) => {
                  setRangeReversed(false);
                  setLabNoToInput(rawValue ?? event.target.value);
                }}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    applyLabNo();
                  }
                }}
              />
            </Column>
          )}
          <Column lg={3} md={4} sm={4}>
            <Button
              kind="ghost"
              size="md"
              onClick={() => setShowPatientSearch((open) => !open)}
            >
              <FormattedMessage id="reportQueue.filter.patient" />
            </Button>
          </Column>
        </Grid>

        {showPatientSearch && (
          <div
            style={{ marginBottom: "1rem" }}
            data-testid="queue-patient-search"
          >
            <SearchPatientForm
              idPrefix="queue"
              renderNotifications={false}
              followUrlLabNumber={false}
              getSelectedPatient={(patient) => {
                const id =
                  patient?.patientPK || patient?.id || patient?.patientId;
                if (!id) {
                  return;
                }
                setPage(1);
                setLookup({
                  labNo: "",
                  labNoTo: "",
                  patient: {
                    id,
                    name: [patient.lastName, patient.firstName]
                      .filter(Boolean)
                      .join(", "),
                  },
                });
                setShowPatientSearch(false);
              }}
            />
          </div>
        )}

        {lookupActive && (
          <div
            data-testid="active-search"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              marginBottom: "1rem",
            }}
          >
            <DismissibleTag
              type="blue"
              className="report-queue-active-search"
              style={{ maxInlineSize: "none" }}
              onClose={clearSearch}
              text={
                lookup.patient
                  ? intl.formatMessage(
                      { id: "reportQueue.search.active.patient" },
                      { name: lookup.patient.name },
                    )
                  : lookup.labNoTo
                    ? intl.formatMessage(
                        { id: "reportQueue.search.active.labRange" },
                        { from: lookup.labNo, to: lookup.labNoTo },
                      )
                    : intl.formatMessage(
                        { id: "reportQueue.search.active.labNo" },
                        { labNo: lookup.labNo },
                      )
              }
            />
            <Button kind="ghost" size="sm" onClick={clearSearch}>
              <FormattedMessage id="reportQueue.search.clear" />
            </Button>
            <small>
              <FormattedMessage id="reportQueue.search.disabledHelper" />
            </small>
          </div>
        )}

        <Grid
          narrow
          style={{ marginBottom: "0.75rem", alignItems: "flex-end" }}
        >
          <Column lg={3} md={4} sm={4}>
            <MultiSelect
              id="queue-state"
              titleText={intl.formatMessage({ id: "reportQueue.filter.state" })}
              label={intl.formatMessage({
                id: "common.all",
                defaultMessage: "All",
              })}
              items={stateItems}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={stateItems.filter((item) =>
                states.includes(item.id),
              )}
              disabled={lookupActive}
              onChange={({ selectedItems }) => {
                setPage(1);
                setStates(selectedItems.map((item) => item.id));
              }}
            />
          </Column>
          <Column lg={3} md={4} sm={4}>
            <Dropdown
              id="queue-print-status"
              titleText={intl.formatMessage({
                id: "reportQueue.filter.printStatus",
              })}
              label={intl.formatMessage({
                id: "common.all",
                defaultMessage: "All",
              })}
              items={printStatusItems}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={printStatusItems.find(
                (item) => item.id === printStatus,
              )}
              disabled={lookupActive}
              onChange={({ selectedItem }) => {
                setPage(1);
                setPrintStatus(selectedItem?.id ?? "ALL");
              }}
            />
          </Column>
          <Column lg={3} md={4} sm={4}>
            <Dropdown
              id="queue-window"
              titleText={intl.formatMessage({
                id: "reportQueue.filter.window",
              })}
              helperText={intl.formatMessage({
                id: "reportQueue.filter.window.helper",
              })}
              label={intl.formatMessage({ id: "reportQueue.filter.window.7d" })}
              items={windowItems}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                windowItems.find((item) => item.id === window_) || null
              }
              disabled={lookupActive}
              onChange={({ selectedItem }) => {
                const next = selectedItem?.id ?? "7d";
                setPage(1);
                setWindow(next);
                savePreferences(next, pageSize);
              }}
            />
          </Column>
          {window_ === "custom" && (
            <>
              <Column lg={2} md={2} sm={4}>
                <DatePicker
                  dateFormat="Y-m-d"
                  parseDate={parseIsoDate}
                  datePickerType="single"
                  onChange={(dates) => {
                    setPage(1);
                    setCustomFrom(dates[0] ? toLocalIsoDate(dates[0]) : "");
                  }}
                >
                  <DatePickerInput
                    id="queue-window-from"
                    placeholder="yyyy-mm-dd"
                    labelText={intl.formatMessage({
                      id: "common.from",
                      defaultMessage: "From",
                    })}
                  />
                </DatePicker>
              </Column>
              <Column lg={2} md={2} sm={4}>
                <DatePicker
                  dateFormat="Y-m-d"
                  parseDate={parseIsoDate}
                  datePickerType="single"
                  onChange={(dates) => {
                    setPage(1);
                    setCustomTo(dates[0] ? toLocalIsoDate(dates[0]) : "");
                  }}
                >
                  <DatePickerInput
                    id="queue-window-to"
                    placeholder="yyyy-mm-dd"
                    labelText={intl.formatMessage({
                      id: "reportQueue.filter.window.to",
                    })}
                  />
                </DatePicker>
              </Column>
            </>
          )}
        </Grid>

        <Grid
          narrow
          style={{ marginBottom: "0.75rem", alignItems: "flex-end" }}
        >
          <Column lg={4} md={4} sm={4}>
            <FilterableMultiSelect
              id="queue-facility"
              titleText={intl.formatMessage({
                id: "reportQueue.filter.facility",
              })}
              helperText={intl.formatMessage({
                id: "reportQueue.filter.typeahead.helper",
              })}
              placeholder={intl.formatMessage({
                id: "reportQueue.filter.typeahead.helper",
              })}
              items={facilityOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={facilities}
              disabled={lookupActive}
              onInputValueChange={searchFacilities}
              onChange={({ selectedItems }) => changeFacilities(selectedItems)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <FilterableMultiSelect
              id="queue-ward"
              titleText={intl.formatMessage({ id: "reportQueue.filter.ward" })}
              helperText={
                facilities.length === 0
                  ? intl.formatMessage({
                      id: "reportQueue.filter.ward.disabled",
                    })
                  : undefined
              }
              placeholder=""
              items={wardOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={wards}
              disabled={lookupActive || facilities.length === 0}
              onChange={({ selectedItems }) => {
                setPage(1);
                setWards(selectedItems);
              }}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <FilterableMultiSelect
              id="queue-requester"
              titleText={intl.formatMessage({
                id: "common.requester",
                defaultMessage: "Requester",
              })}
              helperText={intl.formatMessage({
                id: "reportQueue.filter.typeahead.helper",
              })}
              placeholder={intl.formatMessage({
                id: "reportQueue.filter.typeahead.helper",
              })}
              items={requesterOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItems={requesters}
              disabled={lookupActive}
              onInputValueChange={searchRequesters}
              onChange={({ selectedItems }) => {
                setPage(1);
                setRequesters(selectedItems);
              }}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            {filtersActive && !lookupActive && (
              <Button kind="ghost" size="md" onClick={clearFilters}>
                <FormattedMessage id="reportQueue.filter.clear" />
              </Button>
            )}
          </Column>
        </Grid>

        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "0.25rem",
            marginBottom: "0.5rem",
          }}
        >
          {facilities.map((item) => (
            <DismissibleTag
              key={`f-${item.id}`}
              type="cool-gray"
              size="sm"
              text={item.text}
              onClose={() =>
                changeFacilities(facilities.filter((f) => f.id !== item.id))
              }
            />
          ))}
          {wards.map((item) => (
            <DismissibleTag
              key={`w-${item.id}`}
              type="cool-gray"
              size="sm"
              text={item.text}
              onClose={() => setWards(wards.filter((w) => w.id !== item.id))}
            />
          ))}
          {requesters.map((item) => (
            <DismissibleTag
              key={`r-${item.id}`}
              type="cool-gray"
              size="sm"
              text={item.text}
              onClose={() =>
                setRequesters(requesters.filter((r) => r.id !== item.id))
              }
            />
          ))}
          {states.map((state) => (
            <DismissibleTag
              key={`s-${state}`}
              type={STATE_TAG[state]}
              size="sm"
              text={stateItems.find((item) => item.id === state)?.text || state}
              onClose={() => setStates(states.filter((s) => s !== state))}
            />
          ))}
        </div>

        {notice && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              marginBottom: "1rem",
            }}
          >
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={notice.title}
              subtitle={notice.subtitle}
              onCloseButtonClick={() => setNotice(null)}
              style={{ maxWidth: "100%", margin: 0 }}
            />
            {notice.link && (
              <Link
                href={notice.link}
                target="_blank"
                rel="noopener"
                data-testid="open-report"
              >
                <FormattedMessage id="reportQueue.print.open" />
              </Link>
            )}
          </div>
        )}

        {loadError && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              marginBottom: "1rem",
            }}
          >
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "reportQueue.error.load" })}
              style={{ maxWidth: "100%", margin: 0 }}
            />
            <Button
              kind="ghost"
              size="sm"
              onClick={load}
              data-testid="queue-retry"
            >
              <FormattedMessage id="common.retry" defaultMessage="Retry" />
            </Button>
          </div>
        )}

        {loading && !data ? (
          <DataTableSkeleton
            headers={headers}
            rowCount={5}
            showHeader={false}
            showToolbar={false}
          />
        ) : (
          <DataTable rows={rows} headers={headers} isSortable={false}>
            {({
              rows: tableRows,
              headers: tableHeaders,
              getHeaderProps,
              getRowProps,
              getTableProps,
              getExpandHeaderProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()} size="md">
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader {...getExpandHeaderProps()} />
                      {tableHeaders.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                          isSortable={!!header.sortable}
                          isSortHeader={sort.key === header.key}
                          sortDirection={
                            sort.key === header.key
                              ? sort.dir.toUpperCase()
                              : "NONE"
                          }
                          onClick={() => onHeaderClick(header)}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((tableRow) => {
                      const row = rows.find((r) => r.id === tableRow.id);
                      if (!row) {
                        return null;
                      }
                      const rowProps = getRowProps({ row: tableRow });
                      const detail = details[row.sampleId];
                      return (
                        <React.Fragment key={tableRow.id}>
                          <TableExpandRow
                            {...rowProps}
                            data-testid={`queue-row-${row.labNumber}`}
                            onExpand={(event) => {
                              if (
                                !tableRow.isExpanded &&
                                (!detail || detail.error)
                              ) {
                                loadDetail(row.sampleId);
                              }
                              rowProps.onExpand(event);
                            }}
                          >
                            <TableCell>
                              <span>{row.labNumber}</span>
                            </TableCell>
                            <TableCell>
                              <div>{row.patientName || "–"}</div>
                              <small>{row.patientCode || ""}</small>
                            </TableCell>
                            <TableCell>
                              <div>{row.requester || "–"}</div>
                              <small>
                                {[row.facility, row.ward]
                                  .filter(Boolean)
                                  .join(" / ")}
                              </small>
                            </TableCell>
                            <TableCell>
                              {row.nothingReleased ? (
                                <Tag
                                  type="gray"
                                  size="sm"
                                  data-testid={`nothing-released-${row.labNumber}`}
                                >
                                  <FormattedMessage id="reportQueue.print.disabled.nothingReleased" />
                                </Tag>
                              ) : (
                                formatDateTime(row.released)
                              )}
                              {row.awaitingValidation && (
                                <div>
                                  <Tag
                                    type="blue"
                                    size="sm"
                                    data-testid={`awaiting-validation-${row.labNumber}`}
                                  >
                                    <FormattedMessage id="reportQueue.awaitingValidation" />
                                  </Tag>
                                </div>
                              )}
                            </TableCell>
                            <TableCell>
                              {row.nothingReleased ? "–" : stateTag(row)}
                            </TableCell>
                            <TableCell>
                              {row.nothingReleased && !row.version
                                ? "–"
                                : printStatusTag(row)}
                            </TableCell>
                            <TableCell>
                              {row.version
                                ? intl.formatMessage(
                                    { id: "reportQueue.version.value" },
                                    { n: row.version },
                                  )
                                : intl.formatMessage({
                                    id: "reportQueue.version.none",
                                  })}
                            </TableCell>
                            <TableCell>
                              {row.nothingReleased ? (
                                <Button
                                  size="sm"
                                  kind="primary"
                                  style={{ paddingInlineEnd: "1rem" }}
                                  disabled
                                  title={intl.formatMessage({
                                    id: "reportQueue.print.disabled.nothingReleased",
                                  })}
                                >
                                  <FormattedMessage id="reportQueue.action.print" />
                                </Button>
                              ) : printing === row.sampleId ? (
                                <InlineLoading
                                  description={intl.formatMessage({
                                    id: "reportQueue.action.print",
                                  })}
                                />
                              ) : (
                                <Button
                                  size="sm"
                                  kind="primary"
                                  style={{ paddingInlineEnd: "1rem" }}
                                  data-testid={`print-${row.labNumber}`}
                                  onClick={() => handlePrint(row)}
                                >
                                  <FormattedMessage id="reportQueue.action.print" />
                                </Button>
                              )}
                            </TableCell>
                          </TableExpandRow>
                          <TableExpandedRow colSpan={tableHeaders.length + 1}>
                            {tableRow.isExpanded && (
                              <RowDetail
                                detail={detail}
                                onRetry={() => loadDetail(row.sampleId)}
                              />
                            )}
                          </TableExpandedRow>
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}

        {!loading && data && rows.length === 0 && !loadError && (
          <p style={{ marginTop: "1rem" }} data-testid="queue-empty">
            <FormattedMessage id="reportQueue.empty" />{" "}
            {(filtersActive || lookupActive) && (
              <Link
                href="#"
                onClick={(event) => {
                  event.preventDefault();
                  clearSearch();
                  clearFilters();
                }}
              >
                <FormattedMessage id="reportQueue.filter.clear" />
              </Link>
            )}
          </p>
        )}

        {data && pageSize && (
          <Pagination
            page={page}
            pageSize={pageSize}
            pageSizes={PAGE_SIZES}
            totalItems={data.total}
            onChange={({ page: nextPage, pageSize: nextSize }) => {
              if (nextSize !== pageSize) {
                setPageSize(nextSize);
                setPage(1);
                savePreferences(window_, nextSize);
              } else {
                setPage(nextPage);
              }
            }}
          />
        )}
      </div>
    </>
  );
}

function RowDetail({ detail, onRetry }) {
  const intl = useIntl();
  if (!detail || detail.loading) {
    return (
      <InlineLoading
        description={intl.formatMessage({
          id: "label.loading",
          defaultMessage: "Loading",
        })}
      />
    );
  }
  if (detail.error) {
    return (
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "reportQueue.error.load" })}
          style={{ margin: 0 }}
        />
        <Button
          kind="ghost"
          size="sm"
          onClick={onRetry}
          data-testid="detail-retry"
        >
          <FormattedMessage id="common.retry" defaultMessage="Retry" />
        </Button>
      </div>
    );
  }
  const data = detail.detail;
  const flagLabel = (flag) => {
    const labels = {
      B: { symbol: "↓", id: "report.patient.flag.low" },
      E: { symbol: "↑", id: "report.patient.flag.high" },
      BB: { symbol: "⇊", id: "report.patient.flag.criticalLow" },
      EE: { symbol: "⇈", id: "report.patient.flag.criticalHigh" },
      "*": { symbol: "", id: "report.patient.flag.abnormal" },
    };
    const label = labels[flag];
    if (!label) {
      return flag || "";
    }
    return `${label.symbol} ${intl.formatMessage({ id: label.id })}`.trim();
  };

  const statusLabel = (test) => {
    if (test.released) {
      return null;
    }
    return intl.formatMessage({
      id:
        test.status === "AWAITING_VALIDATION"
          ? "reportQueue.detail.awaitingValidation"
          : "reportQueue.detail.inProgress",
    });
  };
  return (
    <Grid narrow style={{ padding: "1rem 0" }}>
      <Column lg={9} md={8} sm={4}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="reportQueue.detail.tests" />
        </h5>
        <StructuredListWrapper>
          <StructuredListBody>
            <StructuredListRow head>
              {[
                "reportQueue.detail.col.test",
                "reportQueue.detail.col.result",
                "reportQueue.detail.col.flag",
                "reportQueue.detail.col.released",
                "reportQueue.detail.validatedBy",
              ].map((id) => (
                <StructuredListCell head key={id}>
                  <FormattedMessage id={id} />
                </StructuredListCell>
              ))}
            </StructuredListRow>
            {data.tests.map((test) => (
              <StructuredListRow key={test.analysisId}>
                <StructuredListCell noWrap>{test.testName}</StructuredListCell>
                <StructuredListCell>
                  {test.released ? test.result || "–" : statusLabel(test)}
                </StructuredListCell>
                <StructuredListCell>{flagLabel(test.flag)}</StructuredListCell>
                <StructuredListCell>
                  {test.released ? formatDateTime(test.releasedAt) : ""}
                </StructuredListCell>
                <StructuredListCell>
                  {test.validatedBy || ""}
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      </Column>
      <Column lg={7} md={8} sm={4}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="reportQueue.detail.order" />
        </h5>
        <StructuredListWrapper>
          <StructuredListBody>
            {[
              ["reportQueue.filter.facility", data.facility],
              ["reportQueue.filter.ward", data.ward],
              ["common.requester", data.requester],
              ["common.collected", formatDateTime(data.collected)],
              ["common.received", formatDateTime(data.received)],
            ].map(([id, value]) => (
              <StructuredListRow key={id}>
                <StructuredListCell noWrap>
                  <FormattedMessage id={id} />
                </StructuredListCell>
                <StructuredListCell>{value || "–"}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      </Column>
      <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage id="reportQueue.history.title" />
        </h5>
        {data.versions.length === 0 ? (
          <p data-testid="history-none">
            <FormattedMessage id="reportQueue.history.none" />
          </p>
        ) : (
          <StructuredListWrapper data-testid="history">
            <StructuredListBody>
              <StructuredListRow head>
                <StructuredListCell head>
                  <FormattedMessage id="reportQueue.col.version" />
                </StructuredListCell>
                <StructuredListCell head>
                  <FormattedMessage id="reportQueue.history.issued" />
                </StructuredListCell>
                <StructuredListCell head>
                  <FormattedMessage id="reportQueue.history.by" />
                </StructuredListCell>
                <StructuredListCell head>
                  <FormattedMessage id="reportQueue.history.change" />
                </StructuredListCell>
                <StructuredListCell head>
                  <FormattedMessage id="reportQueue.history.completeness" />
                </StructuredListCell>
                <StructuredListCell head>
                  <FormattedMessage id="reportQueue.history.fingerprint" />
                </StructuredListCell>
              </StructuredListRow>
              {data.versions.map((version) => (
                <StructuredListRow key={version.id}>
                  <StructuredListCell>
                    {intl.formatMessage(
                      { id: "reportQueue.version.value" },
                      { n: version.version },
                    )}
                    {version.amended && (
                      <>
                        {" "}
                        <Tag type="magenta" size="sm">
                          <FormattedMessage id="reportQueue.state.amended" />
                        </Tag>
                      </>
                    )}
                  </StructuredListCell>
                  <StructuredListCell>
                    <div>{formatDateTime(version.issuedAt)}</div>
                    {version.reprints.map((reprint, index) => (
                      <small key={index} style={{ display: "block" }}>
                        {intl.formatMessage(
                          { id: "reportQueue.history.reprinted" },
                          {
                            date: formatDateTime(reprint.printedAt),
                            user: reprint.printedBy || "",
                          },
                        )}
                      </small>
                    ))}
                  </StructuredListCell>
                  <StructuredListCell>
                    {version.issuedBy || ""}
                  </StructuredListCell>
                  <StructuredListCell>
                    {version.changeSummary ||
                      intl.formatMessage({
                        id: "reportQueue.history.firstIssue",
                      })}
                  </StructuredListCell>
                  <StructuredListCell>
                    {version.legacy
                      ? "–"
                      : `${version.releasedCount}/${version.totalCount}`}
                  </StructuredListCell>
                  <StructuredListCell>
                    {version.archived ? (
                      <>
                        <span
                          title={version.sha256}
                          style={{ fontFamily: "monospace" }}
                        >
                          {(version.sha256 || "").slice(0, 12)}
                        </span>{" "}
                        <Link
                          href={`${config.serverBaseUrl}/rest/reportQueue/versions/${version.id}/pdf`}
                          target="_blank"
                          rel="noopener noreferrer"
                          data-testid={`open-pdf-${version.version}`}
                        >
                          <FormattedMessage id="reportQueue.history.openPdf" />
                        </Link>
                      </>
                    ) : version.archiveFailed ? (
                      <Tag type="red" size="sm">
                        <FormattedMessage id="reportQueue.history.archiveFailed" />
                      </Tag>
                    ) : (
                      intl.formatMessage(
                        { id: "reportQueue.history.notArchived" },
                        { date: formatDate(data.installDate) },
                      )
                    )}
                  </StructuredListCell>
                </StructuredListRow>
              ))}
            </StructuredListBody>
          </StructuredListWrapper>
        )}
      </Column>
    </Grid>
  );
}
