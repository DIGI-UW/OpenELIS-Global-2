import {
  Button,
  Column,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Grid,
  InlineNotification,
  Link as CarbonLink,
  Loading,
  MultiSelect,
  Select,
  SelectItem,
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
} from "@carbon/react";
import {
  CheckmarkOutline,
  Send,
  Warning,
  WarningAlt,
} from "@carbon/icons-react";
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import ShipmentNavigation from "../shipment/ShipmentNavigation";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../utils/Utils";
import MarkLostModal from "./MarkLostModal";
import RejectModal from "./RejectModal";
import "./ReferenceLabResults.css";

const VIEWS = ["outstanding", "returned", "history"];
const PRIORITIES = ["Routine", "Urgent", "STAT"];

const STATUS_TAG_KIND = {
  draft: "gray",
  requested: "blue",
  received: "purple",
  "in-progress": "warm-gray",
  completed: "teal",
  rejected: "red",
  cancelled: "gray",
  reconciled: "teal",
};

const PRIORITY_TAG_KIND = {
  Routine: "gray",
  Urgent: "warm-gray",
  STAT: "red",
};

const OUTCOME_TAG_KIND = {
  Reconciled: "teal",
  Rejected: "red",
  Cancelled: "gray",
  Lost: "red",
};

const METRIC_COLOR = {
  outstanding: "#24a148",
  returned: "#da1e28",
  reconciledToday: "#005d5d",
  rejectedThisWeek: "#8a3ffc",
};

const ReferenceLabResults = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const componentMounted = useRef(true);
  const { addNotification } = useContext(NotificationContext);

  const activeView = useMemo(() => {
    const params = new URLSearchParams(location.search);
    const raw = params.get("view");
    return VIEWS.includes(raw) ? raw : "outstanding";
  }, [location.search]);

  const [referrals, setReferrals] = useState([]);
  const [metrics, setMetrics] = useState({
    outstanding: 0,
    returned: 0,
    reconciledToday: 0,
    rejectedThisWeek: 0,
    referralStuckThresholdDays: 7,
  });
  const [referenceLabs, setReferenceLabs] = useState([]);
  const [loading, setLoading] = useState(true);

  const [filterRefLab, setFilterRefLab] = useState(null);
  const [filterDateFrom, setFilterDateFrom] = useState(null);
  const [filterDateTo, setFilterDateTo] = useState(null);
  const [filterPriority, setFilterPriority] = useState([]);
  const [filterDaysBucket, setFilterDaysBucket] = useState("all");

  const [expandedRow, setExpandedRow] = useState(null);
  const [markLostTarget, setMarkLostTarget] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);

  const toggleExpand = useCallback((id) => {
    setExpandedRow((prev) => (prev === id ? null : id));
  }, []);

  const refetchReferrals = useCallback(() => {
    getFromOpenElisServer(
      `/rest/reference-lab-results/referrals?view=${activeView}`,
      (response) => {
        if (!componentMounted.current) return;
        setReferrals(Array.isArray(response) ? response : []);
      },
    );
    getFromOpenElisServer("/rest/reference-lab-results/metrics", (response) => {
      if (componentMounted.current && response) {
        setMetrics(response);
      }
    });
  }, [activeView]);

  const acceptReferral = useCallback(
    (row) => {
      putToOpenElisServer(
        `/rest/reference-lab-results/referrals/${row.id}/accept`,
        null,
        (status) => {
          if (status === 204 || status === 200) {
            addNotification({
              kind: "success",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage({
                id: "referral.notification.accepted",
              }),
            });
            setExpandedRow(null);
            refetchReferrals();
          } else {
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({ id: "referral.accept.error" }),
            });
          }
        },
      );
    },
    [addNotification, intl, refetchReferrals],
  );

  const notifyReferenceLab = useCallback(
    (row) => {
      postToOpenElisServer(
        `/rest/reference-lab-results/referrals/${row.id}/notify`,
        null,
        (status) => {
          if (status === 204 || status === 200) {
            addNotification({
              kind: "success",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage({ id: "referral.notify.toast" }),
            });
          } else {
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({ id: "referral.notify.errorRetry" }),
            });
          }
        },
      );
    },
    [addNotification, intl],
  );

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (response) => {
        if (componentMounted.current && response) {
          setReferenceLabs(
            response.map((org) => ({ id: org.id, name: org.value })),
          );
        }
      },
    );
    getFromOpenElisServer("/rest/reference-lab-results/metrics", (response) => {
      if (componentMounted.current && response) {
        setMetrics(response);
      }
    });
  }, []);

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/reference-lab-results/referrals?view=${activeView}`,
      (response) => {
        if (!componentMounted.current) return;
        setReferrals(Array.isArray(response) ? response : []);
        setLoading(false);
      },
    );
  }, [activeView]);

  const clearFilters = useCallback(() => {
    setFilterRefLab(null);
    setFilterDateFrom(null);
    setFilterDateTo(null);
    setFilterPriority([]);
    setFilterDaysBucket("all");
  }, []);

  const switchView = useCallback(
    (next) => {
      if (next === activeView) return;
      const params = new URLSearchParams(location.search);
      params.set("view", next);
      history.push({ pathname: location.pathname, search: params.toString() });
      setLoading(true);
      clearFilters();
    },
    [activeView, history, location.pathname, location.search, clearFilters],
  );

  const filteredRows = useMemo(() => {
    let rows = referrals;
    if (filterRefLab) {
      rows = rows.filter((r) => r.referenceLabId === filterRefLab);
    }
    if (filterPriority.length > 0) {
      rows = rows.filter((r) => filterPriority.includes(r.priority));
    }
    if (filterDateFrom) {
      const from = new Date(filterDateFrom);
      from.setHours(0, 0, 0, 0);
      rows = rows.filter((r) => {
        const ref = activeView === "history" ? r.closedDate : r.sentDate;
        return ref && new Date(ref) >= from;
      });
    }
    if (filterDateTo) {
      const to = new Date(filterDateTo);
      to.setHours(23, 59, 59, 999);
      rows = rows.filter((r) => {
        const ref = activeView === "history" ? r.closedDate : r.sentDate;
        return ref && new Date(ref) <= to;
      });
    }
    if (activeView === "outstanding" && filterDaysBucket !== "all") {
      const threshold = metrics?.referralStuckThresholdDays ?? 7;
      rows = rows.filter((r) => {
        const d = r.daysOutstanding ?? 0;
        if (filterDaysBucket === "0-7") return d <= 7;
        if (filterDaysBucket === "7-30") return d > 7 && d <= 30;
        if (filterDaysBucket === ">30") return d > 30;
        if (filterDaysBucket === "stuck") return d > threshold;
        return true;
      });
    }
    return rows;
  }, [
    referrals,
    filterRefLab,
    filterPriority,
    filterDateFrom,
    filterDateTo,
    filterDaysBucket,
    activeView,
    metrics?.referralStuckThresholdDays,
  ]);

  const stuckThreshold = metrics?.referralStuckThresholdDays ?? 7;
  const stuckCount = useMemo(() => {
    if (activeView !== "outstanding") return 0;
    return referrals.filter((r) => (r.daysOutstanding ?? 0) > stuckThreshold)
      .length;
  }, [referrals, stuckThreshold, activeView]);

  return (
    <div className="reference-lab-results">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
          { label: "referral.breadcrumb.referenceLabResults" },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h1 className="reference-lab-results__title">
            <FormattedMessage id="referral.page.title" />
          </h1>
        </Column>
      </Grid>

      {activeView === "outstanding" && stuckCount > 0 && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div className="reference-lab-results__aging-banner">
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "referral.banner.stuckReferralsTitle",
                })}
                subtitle={intl.formatMessage(
                  { id: "referral.banner.stuckReferralsSubtitle" },
                  { count: stuckCount, threshold: stuckThreshold },
                )}
              />
              <Button
                kind="ghost"
                size="sm"
                onClick={() => setFilterDaysBucket("stuck")}
              >
                {intl.formatMessage({ id: "referral.banner.filterToStuck" })}
              </Button>
            </div>
          </Column>
        </Grid>
      )}

      <Grid fullWidth className="reference-lab-results__metrics">
        <Column lg={4} md={4} sm={2}>
          <MetricTile
            color={METRIC_COLOR.outstanding}
            label={intl.formatMessage({ id: "referral.metric.outstanding" })}
            count={metrics.outstanding}
            active={activeView === "outstanding"}
            onClick={() => switchView("outstanding")}
          />
        </Column>
        <Column lg={4} md={4} sm={2}>
          <MetricTile
            color={METRIC_COLOR.returned}
            label={intl.formatMessage({ id: "referral.metric.returned" })}
            count={metrics.returned}
            active={activeView === "returned"}
            onClick={() => switchView("returned")}
          />
        </Column>
        <Column lg={4} md={4} sm={2}>
          <MetricTile
            color={METRIC_COLOR.reconciledToday}
            label={intl.formatMessage({
              id: "referral.metric.reconciledToday",
            })}
            count={metrics.reconciledToday}
            active={activeView === "history"}
            onClick={() => switchView("history")}
          />
        </Column>
        <Column lg={4} md={4} sm={2}>
          <MetricTile
            color={METRIC_COLOR.rejectedThisWeek}
            label={intl.formatMessage({
              id: "referral.metric.rejectedThisWeek",
            })}
            count={metrics.rejectedThisWeek}
            active={activeView === "history"}
            onClick={() => switchView("history")}
          />
        </Column>
      </Grid>

      <div
        className="reference-lab-results__chips"
        role="radiogroup"
        aria-label={intl.formatMessage({ id: "referral.chip.ariaLabel" })}
      >
        <FilterChip
          active={activeView === "outstanding"}
          onClick={() => switchView("outstanding")}
        >
          <FormattedMessage id="referral.chip.outstanding" /> (
          {metrics.outstanding})
        </FilterChip>
        <FilterChip
          active={activeView === "returned"}
          onClick={() => switchView("returned")}
        >
          <FormattedMessage id="referral.chip.returned" /> ({metrics.returned})
        </FilterChip>
        <FilterChip
          active={activeView === "history"}
          onClick={() => switchView("history")}
        >
          <FormattedMessage id="referral.chip.history" />
        </FilterChip>
      </div>

      <div className="reference-lab-results__filters">
        <Grid narrow>
          <Column lg={4} md={4} sm={4}>
            <Dropdown
              id="ref-lab-filter"
              titleText={intl.formatMessage({
                id: "referral.filter.referenceLab",
              })}
              label={intl.formatMessage({ id: "referral.filter.allRefLabs" })}
              items={[
                {
                  id: null,
                  name: intl.formatMessage({
                    id: "referral.filter.allRefLabs",
                  }),
                },
                ...referenceLabs,
              ]}
              itemToString={(item) => (item ? item.name : "")}
              selectedItem={
                referenceLabs.find((r) => r.id === filterRefLab) || {
                  id: null,
                  name: intl.formatMessage({
                    id: "referral.filter.allRefLabs",
                  }),
                }
              }
              onChange={({ selectedItem }) =>
                setFilterRefLab(selectedItem?.id || null)
              }
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="range"
              onChange={([from, to]) => {
                setFilterDateFrom(from || null);
                setFilterDateTo(to || null);
              }}
              value={
                filterDateFrom && filterDateTo
                  ? [filterDateFrom, filterDateTo]
                  : []
              }
            >
              <DatePickerInput
                id="date-from"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "referral.filter.dateRange",
                })}
              />
              <DatePickerInput
                id="date-to"
                placeholder="mm/dd/yyyy"
                labelText={" "}
              />
            </DatePicker>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MultiSelect
              id="priority-filter"
              titleText={intl.formatMessage({ id: "referral.filter.priority" })}
              label={intl.formatMessage({
                id: "referral.filter.allPriorities",
              })}
              items={PRIORITIES}
              itemToString={(i) => i}
              selectedItems={filterPriority}
              onChange={({ selectedItems }) =>
                setFilterPriority(selectedItems || [])
              }
            />
          </Column>
          {activeView === "outstanding" && (
            <Column lg={3} md={4} sm={4}>
              <Select
                id="days-bucket-filter"
                labelText={intl.formatMessage({
                  id: "referral.filter.daysOutstandingBucket",
                })}
                value={filterDaysBucket}
                onChange={(e) => setFilterDaysBucket(e.target.value)}
              >
                <SelectItem
                  value="all"
                  text={intl.formatMessage({ id: "label.all" })}
                />
                <SelectItem value="0-7" text="0-7" />
                <SelectItem value="7-30" text="7-30" />
                <SelectItem value=">30" text=">30" />
                <SelectItem
                  value="stuck"
                  text={intl.formatMessage(
                    { id: "referral.filter.daysOutstandingBucket.stuck" },
                    { threshold: stuckThreshold },
                  )}
                />
              </Select>
            </Column>
          )}
          <Column lg={1} md={4} sm={4}>
            <div className="reference-lab-results__clear">
              <Button kind="ghost" size="sm" onClick={clearFilters}>
                <FormattedMessage id="referral.action.clearFilters" />
              </Button>
            </div>
          </Column>
        </Grid>
      </div>

      {loading ? (
        <Loading withOverlay={false} />
      ) : (
        <ReferralTable
          view={activeView}
          rows={filteredRows}
          intl={intl}
          expandedRow={expandedRow}
          toggleExpand={toggleExpand}
          onMarkLost={setMarkLostTarget}
          onNotify={notifyReferenceLab}
          onAccept={acceptReferral}
          onReject={setRejectTarget}
        />
      )}

      {markLostTarget && (
        <MarkLostModal
          open
          referral={markLostTarget}
          onClose={() => setMarkLostTarget(null)}
          onSuccess={() => {
            setMarkLostTarget(null);
            setExpandedRow(null);
            refetchReferrals();
          }}
        />
      )}

      {rejectTarget && (
        <RejectModal
          open
          referral={rejectTarget}
          onClose={() => setRejectTarget(null)}
          onSuccess={() => {
            setRejectTarget(null);
            setExpandedRow(null);
            refetchReferrals();
          }}
        />
      )}
    </div>
  );
};

const MetricTile = ({ color, label, count, onClick, active }) => (
  <button
    type="button"
    className={`reference-lab-results__metric${active ? " reference-lab-results__metric--active" : ""}`}
    style={{ borderLeftColor: color, borderColor: active ? color : undefined }}
    onClick={onClick}
  >
    <div className="reference-lab-results__metric-count">{count}</div>
    <div className="reference-lab-results__metric-label">{label}</div>
  </button>
);

const FilterChip = ({ active, onClick, children }) => (
  <button
    type="button"
    role="radio"
    aria-checked={active}
    className={`reference-lab-results__chip${active ? " reference-lab-results__chip--active" : ""}`}
    onClick={onClick}
  >
    {children}
  </button>
);

const ReferralTable = ({
  view,
  rows,
  intl,
  expandedRow,
  toggleExpand,
  onMarkLost,
  onNotify,
  onAccept,
  onReject,
}) => {
  const shared = { rows, intl, expandedRow, toggleExpand };
  if (view === "outstanding") {
    return (
      <OutstandingTable
        {...shared}
        onMarkLost={onMarkLost}
        onNotify={onNotify}
      />
    );
  }
  if (view === "returned") {
    return (
      <ReturnedTable {...shared} onAccept={onAccept} onReject={onReject} />
    );
  }
  return <HistoryTable {...shared} />;
};

const renderStatus = (status) => {
  if (!status) return null;
  const kind = STATUS_TAG_KIND[status] || "gray";
  return (
    <Tag type={kind} size="sm">
      <FormattedMessage
        id={`referral.status.${status === "in-progress" ? "inProgress" : status}`}
      />
    </Tag>
  );
};

const renderPriority = (priority) => {
  if (!priority) return null;
  return (
    <Tag type={PRIORITY_TAG_KIND[priority] || "gray"} size="sm">
      {priority}
    </Tag>
  );
};

const renderOutcome = (outcome) => {
  if (!outcome) return null;
  return (
    <Tag type={OUTCOME_TAG_KIND[outcome] || "gray"} size="sm">
      {outcome}
    </Tag>
  );
};

const renderTests = (tests) => {
  if (!tests || tests.length === 0) return "—";
  const head = tests.slice(0, 2).join(", ");
  return tests.length > 2 ? `${head} +${tests.length - 2}` : head;
};

const renderDate = (iso) => {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleDateString();
};

const renderDays = (days) => {
  if (days === null || days === undefined) return "—";
  let color = "#161616";
  let icon = null;
  if (days > 30) {
    color = "#da1e28";
    icon = <Warning size={16} aria-hidden="true" />;
  } else if (days > 7) {
    color = "#f1c21b";
    icon = <Warning size={16} aria-hidden="true" />;
  }
  return (
    <span
      style={{
        color,
        display: "inline-flex",
        alignItems: "center",
        gap: "0.25rem",
      }}
    >
      {icon}
      {days}
    </span>
  );
};

const INTERP_TAG_KIND = {
  Normal: "green",
  Abnormal: "magenta",
  Critical: "red",
};

const interpLabel = (interpretation, intl) =>
  ["Normal", "Abnormal", "Critical"].includes(interpretation)
    ? intl.formatMessage({ id: `referral.result.interp.${interpretation}` })
    : interpretation;

const isCritical = (row) =>
  (row.results || []).some((r) => r.interpretation === "Critical");

const ResultCard = ({ result, intl }) => (
  <div className="reference-lab-results__result-card">
    <div className="reference-lab-results__result-card-head">
      <span className="reference-lab-results__result-test">
        {result.testName || "—"}
      </span>
      {result.interpretation && (
        <Tag type={INTERP_TAG_KIND[result.interpretation] || "gray"} size="sm">
          {interpLabel(result.interpretation, intl)}
        </Tag>
      )}
    </div>
    <div className="reference-lab-results__result-value">
      {result.value || "—"}
      {result.units ? ` ${result.units}` : ""}
    </div>
    {result.referenceRange && (
      <div className="reference-lab-results__result-range">
        <FormattedMessage id="referral.result.referenceRange" />:{" "}
        {result.referenceRange}
      </div>
    )}
    {result.note && (
      <div className="reference-lab-results__result-note">{result.note}</div>
    )}
  </div>
);

const renderResultSummary = (row, intl) => {
  if (!row.resultSummary) return "—";
  return (
    <span className="reference-lab-results__summary-cell">
      {isCritical(row) && (
        <Tag type="red" size="sm">
          {intl.formatMessage({ id: "referral.expand.criticalBadge" })}
        </Tag>
      )}
      {row.resultSummary}
    </span>
  );
};

const TableShell = ({ title, count, headers, intl, expandable, children }) => (
  <TableContainer
    title={title}
    description={intl.formatMessage({ id: "referral.table.count" }, { count })}
  >
    <Table size="md">
      <TableHead>
        <TableRow>
          {expandable && <TableExpandHeader />}
          {headers.map((h) => (
            <TableHeader key={h}>{h}</TableHeader>
          ))}
        </TableRow>
      </TableHead>
      <TableBody>{children}</TableBody>
    </Table>
  </TableContainer>
);

const EmptyRow = ({ colSpan, intl }) => (
  <TableRow>
    <TableCell colSpan={colSpan} className="reference-lab-results__empty">
      {intl.formatMessage({ id: "referral.emptyState" })}
    </TableCell>
  </TableRow>
);

const OutstandingTable = ({
  rows,
  intl,
  expandedRow,
  toggleExpand,
  onMarkLost,
  onNotify,
}) => {
  const headers = [
    intl.formatMessage({ id: "referral.column.labNumber" }),
    intl.formatMessage({ id: "referral.column.patient" }),
    intl.formatMessage({ id: "referral.column.tests" }),
    intl.formatMessage({ id: "referral.column.referenceLab" }),
    intl.formatMessage({ id: "referral.column.boxId" }),
    intl.formatMessage({ id: "referral.column.sentDate" }),
    intl.formatMessage({ id: "referral.column.status" }),
    intl.formatMessage({ id: "referral.column.daysOutstanding" }),
    intl.formatMessage({ id: "referral.column.priority" }),
    "",
  ];
  return (
    <TableShell
      title={intl.formatMessage({ id: "referral.table.outstanding.title" })}
      count={rows.length}
      headers={headers}
      intl={intl}
      expandable
    >
      {rows.length === 0 ? (
        <EmptyRow colSpan={headers.length + 1} intl={intl} />
      ) : (
        rows.map((row) => (
          <React.Fragment key={row.id}>
            <TableExpandRow
              isExpanded={expandedRow === row.id}
              onExpand={() => toggleExpand(row.id)}
            >
              <TableCell>{row.labNumber || "—"}</TableCell>
              <TableCell>{row.patientDisplay || "—"}</TableCell>
              <TableCell>{renderTests(row.tests)}</TableCell>
              <TableCell>{row.referenceLabName || "—"}</TableCell>
              <TableCell>
                {row.boxId ? (
                  <CarbonLink href={`/SampleShipment/box/${row.boxId}`}>
                    {row.boxId}
                  </CarbonLink>
                ) : (
                  "—"
                )}
              </TableCell>
              <TableCell>{renderDate(row.sentDate)}</TableCell>
              <TableCell>{renderStatus(row.status)}</TableCell>
              <TableCell>{renderDays(row.daysOutstanding)}</TableCell>
              <TableCell>{renderPriority(row.priority)}</TableCell>
              <TableCell onClick={(e) => e.stopPropagation()}>
                <Button
                  kind="tertiary"
                  size="sm"
                  href={`/result?accessionNumber=${encodeURIComponent(row.labNumber || "")}`}
                  className="reference-lab-results__enter-result"
                >
                  <FormattedMessage id="referral.action.enterResult" />
                </Button>
              </TableCell>
            </TableExpandRow>
            {expandedRow === row.id && (
              <TableExpandedRow colSpan={headers.length + 1}>
                <ExpandPanel
                  row={row}
                  mode="outstanding"
                  intl={intl}
                  onMarkLost={onMarkLost}
                  onNotify={onNotify}
                />
              </TableExpandedRow>
            )}
          </React.Fragment>
        ))
      )}
    </TableShell>
  );
};

const ReturnedTable = ({
  rows,
  intl,
  expandedRow,
  toggleExpand,
  onAccept,
  onReject,
}) => {
  const headers = [
    intl.formatMessage({ id: "referral.column.labNumber" }),
    intl.formatMessage({ id: "referral.column.patient" }),
    intl.formatMessage({ id: "referral.column.tests" }),
    intl.formatMessage({ id: "referral.column.referenceLab" }),
    intl.formatMessage({ id: "referral.column.resultSummary" }),
    intl.formatMessage({ id: "referral.column.returnedDate" }),
    intl.formatMessage({ id: "referral.column.requestor" }),
    intl.formatMessage({ id: "referral.column.actions" }),
  ];
  return (
    <TableShell
      title={intl.formatMessage({ id: "referral.table.returned.title" })}
      count={rows.length}
      headers={headers}
      intl={intl}
      expandable
    >
      {rows.length === 0 ? (
        <EmptyRow colSpan={headers.length + 1} intl={intl} />
      ) : (
        rows.map((row) => (
          <React.Fragment key={row.id}>
            <TableExpandRow
              isExpanded={expandedRow === row.id}
              onExpand={() => toggleExpand(row.id)}
            >
              <TableCell>{row.labNumber || "—"}</TableCell>
              <TableCell>{row.patientDisplay || "—"}</TableCell>
              <TableCell>{renderTests(row.tests)}</TableCell>
              <TableCell>{row.referenceLabName || "—"}</TableCell>
              <TableCell>{renderResultSummary(row, intl)}</TableCell>
              <TableCell>{renderDate(row.returnedDate)}</TableCell>
              <TableCell>{row.requestor || "—"}</TableCell>
              <TableCell>
                <div className="reference-lab-results__row-actions">
                  <Button
                    kind="primary"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      onAccept && onAccept(row);
                    }}
                  >
                    <FormattedMessage id="referral.action.accept" />
                  </Button>
                  <Button
                    kind="danger--ghost"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      onReject && onReject(row);
                    }}
                  >
                    <FormattedMessage id="referral.action.reject" />
                  </Button>
                </div>
              </TableCell>
            </TableExpandRow>
            {expandedRow === row.id && (
              <TableExpandedRow colSpan={headers.length + 1}>
                <ExpandPanel
                  row={row}
                  mode="returned"
                  intl={intl}
                  onAccept={onAccept}
                  onReject={onReject}
                />
              </TableExpandedRow>
            )}
          </React.Fragment>
        ))
      )}
    </TableShell>
  );
};

const HistoryTable = ({ rows, intl, expandedRow, toggleExpand }) => {
  const headers = [
    intl.formatMessage({ id: "referral.column.labNumber" }),
    intl.formatMessage({ id: "referral.column.patient" }),
    intl.formatMessage({ id: "referral.column.tests" }),
    intl.formatMessage({ id: "referral.column.referenceLab" }),
    intl.formatMessage({ id: "referral.column.outcome" }),
    intl.formatMessage({ id: "referral.column.closedDate" }),
    intl.formatMessage({ id: "referral.column.boxId" }),
    intl.formatMessage({ id: "referral.column.daysTotal" }),
  ];
  return (
    <TableShell
      title={intl.formatMessage({ id: "referral.table.history.title" })}
      count={rows.length}
      headers={headers}
      intl={intl}
      expandable
    >
      {rows.length === 0 ? (
        <EmptyRow colSpan={headers.length + 1} intl={intl} />
      ) : (
        rows.map((row) => (
          <React.Fragment key={row.id}>
            <TableExpandRow
              isExpanded={expandedRow === row.id}
              onExpand={() => toggleExpand(row.id)}
            >
              <TableCell>{row.labNumber || "—"}</TableCell>
              <TableCell>{row.patientDisplay || "—"}</TableCell>
              <TableCell>{renderTests(row.tests)}</TableCell>
              <TableCell>{row.referenceLabName || "—"}</TableCell>
              <TableCell>
                {renderOutcome(row.outcome)}
                {row.manuallyEntered && (
                  <Tag
                    type="warm-gray"
                    size="sm"
                    style={{ marginLeft: "0.25rem" }}
                  >
                    <FormattedMessage id="referral.tag.manuallyEntered" />
                  </Tag>
                )}
              </TableCell>
              <TableCell>{renderDate(row.closedDate)}</TableCell>
              <TableCell>{row.boxId || "—"}</TableCell>
              <TableCell>{row.daysTotal ?? "—"}</TableCell>
            </TableExpandRow>
            {expandedRow === row.id && (
              <TableExpandedRow colSpan={headers.length + 1}>
                <ExpandPanel row={row} mode="history" intl={intl} />
              </TableExpandedRow>
            )}
          </React.Fragment>
        ))
      )}
    </TableShell>
  );
};

const formatDateTime = (iso) => {
  if (!iso) return "—";
  const d = new Date(iso);
  return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  })}`;
};

const ExpandPanel = ({
  row,
  mode,
  intl,
  onMarkLost,
  onNotify,
  onAccept,
  onReject,
}) => (
  <div className="reference-lab-results__expand">
    <Grid narrow>
      <Column lg={5} md={4} sm={4}>
        <h5 className="reference-lab-results__expand-heading">
          <FormattedMessage id="referral.expand.originalOrderContext" />
        </h5>
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.labNumber" })}
          value={row.labNumber}
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.patient" })}
          value={[
            row.patientDisplay,
            row.patientGender,
            row.patientAge != null
              ? intl.formatMessage(
                  { id: "referral.expand.detail.ageYears" },
                  { years: row.patientAge },
                )
              : null,
          ]
            .filter(Boolean)
            .join(" · ")}
        />
        <DetailRow
          label={intl.formatMessage({
            id: "referral.expand.detail.sampleType",
          })}
          value={row.sampleType}
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.collected" })}
          value={formatDateTime(row.collectedDate)}
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.tests" })}
          value={
            row.tests && row.tests.length > 0 ? row.tests.join(", ") : null
          }
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.requestor" })}
          value={row.requestor}
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.priority" })}
          value={renderPriority(row.priority)}
        />
      </Column>

      <Column lg={5} md={4} sm={4}>
        <h5 className="reference-lab-results__expand-heading">
          <FormattedMessage id="referral.expand.referenceLabTransit" />
        </h5>
        <DetailRow
          label={intl.formatMessage({
            id: "referral.expand.detail.referenceLab",
          })}
          value={row.referenceLabName}
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.boxId" })}
          value={
            row.boxId ? (
              <CarbonLink href={`/SampleShipment/box/${row.boxId}`}>
                {row.boxId}
              </CarbonLink>
            ) : null
          }
        />
        <DetailRow
          label={intl.formatMessage({
            id: "referral.expand.detail.dispatched",
          })}
          value={formatDateTime(row.sentDate)}
        />
        <DetailRow
          label={intl.formatMessage({
            id: "referral.expand.detail.boxReceivedDate",
          })}
          value={formatDateTime(row.boxReceivedDate)}
        />
        <DetailRow
          label={intl.formatMessage({ id: "referral.expand.detail.fhirTask" })}
          value={
            row.fhirTaskUuid ? (
              <code className="reference-lab-results__code">
                {row.fhirTaskUuid}
              </code>
            ) : null
          }
        />
      </Column>

      <Column lg={6} md={8} sm={4}>
        {mode === "outstanding" && (
          <>
            <h5 className="reference-lab-results__expand-heading">
              <FormattedMessage id="referral.expand.statusDetail" />
            </h5>
            <DetailRow
              label={intl.formatMessage({
                id: "referral.expand.detail.currentStatus",
              })}
              value={renderStatus(row.status)}
            />
            <DetailRow
              label={intl.formatMessage({
                id: "referral.expand.detail.daysOutstanding",
              })}
              value={renderDays(row.daysOutstanding)}
            />
            <div className="reference-lab-results__expand-actions">
              <Button
                kind="primary"
                size="sm"
                href={`/result?accessionNumber=${encodeURIComponent(row.labNumber || "")}`}
              >
                <FormattedMessage id="referral.action.enterResult" />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={WarningAlt}
                onClick={() => onMarkLost && onMarkLost(row)}
              >
                <FormattedMessage id="referral.action.markLost" />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Send}
                onClick={() => onNotify && onNotify(row)}
              >
                <FormattedMessage id="referral.action.notifyReferenceLab" />
              </Button>
            </div>
          </>
        )}
        {mode === "returned" && (
          <>
            <h5 className="reference-lab-results__expand-heading">
              <FormattedMessage id="referral.expand.result" />
            </h5>
            <p className="reference-lab-results__expand-hint">
              <FormattedMessage id="referral.expand.receptionHint" />
            </p>
            {row.results && row.results.length > 0 ? (
              <div className="reference-lab-results__result-cards">
                {row.results.map((result, index) => (
                  <ResultCard key={index} result={result} intl={intl} />
                ))}
              </div>
            ) : (
              <p className="reference-lab-results__expand-hint">
                <FormattedMessage id="referral.expand.noResultPayload" />
              </p>
            )}
            <div className="reference-lab-results__expand-actions">
              <Button
                kind="primary"
                size="sm"
                renderIcon={CheckmarkOutline}
                onClick={() => onAccept && onAccept(row)}
              >
                <FormattedMessage id="referral.action.acceptToAnalysis" />
              </Button>
              <Button
                kind="danger--ghost"
                size="sm"
                renderIcon={WarningAlt}
                onClick={() => onReject && onReject(row)}
              >
                <FormattedMessage id="referral.action.reject" />
              </Button>
            </div>
            {row.labNumber && (
              <CarbonLink
                href={`/result?accessionNumber=${row.labNumber}`}
                className="reference-lab-results__result-entry-link"
              >
                <FormattedMessage id="referral.action.openResultEntry" />
              </CarbonLink>
            )}
          </>
        )}
        {mode === "history" && (
          <>
            <h5 className="reference-lab-results__expand-heading">
              <FormattedMessage id="referral.expand.outcomeDetail" />
            </h5>
            <DetailRow
              label={intl.formatMessage({ id: "referral.column.outcome" })}
              value={renderOutcome(row.outcome)}
            />
            <DetailRow
              label={intl.formatMessage({ id: "referral.column.closedDate" })}
              value={formatDateTime(row.closedDate)}
            />
            <DetailRow
              label={intl.formatMessage({ id: "referral.column.daysTotal" })}
              value={row.daysTotal ?? null}
            />
          </>
        )}
      </Column>
    </Grid>
  </div>
);

const DetailRow = ({ label, value }) => (
  <div className="reference-lab-results__detail-row">
    <span className="reference-lab-results__detail-label">{label}</span>
    <span className="reference-lab-results__detail-value">
      {value == null || value === "" ? "—" : value}
    </span>
  </div>
);

export default ReferenceLabResults;
