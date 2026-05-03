import React from "react";
import {
  Tile,
  ClickableTile,
  Loading,
  Grid,
  Button,
  Column,
  TextInput,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Link,
  Tab,
  Tabs,
  TabList,
} from "@carbon/react";
import "./Dashboard.css";
import { Minimize, Maximize, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { Copy, CheckmarkFilled } from "@carbon/icons-react";
import {
  useState,
  useEffect,
  useRef,
  useContext,
  useCallback,
  useMemo,
} from "react";
import config from "../../config.json";
import barcodeIcon from "./assets/barcode.png";
import resultIcon from "./assets/results.png";
import reportIcon from "./assets/report.png";
import validateIcon from "./assets/validate.png";
import {
  getFromOpenElisServer,
  getFromOpenElisServerV2,
  convertAlphaNumLabNumForDisplay,
  hasRole,
} from "../utils/Utils.js";
import { getFullPath } from "../utils/Navigation";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";

type DashBoardProps = Record<string, never>;

interface Tile {
  title: string | JSX.Element;
  subTitle: string | JSX.Element;
  type: MetricType;
  value: number;
  id?: number;
}

type MetricType =
  | "ORDERS_IN_PROGRESS"
  | "ON_GOING_ORDERS"
  | "ORDERS_READY_FOR_VALIDATION"
  | "ORDERS_COMPLETED_TODAY"
  | "ORDERS_PATIALLY_COMPLETED_TODAY"
  | "ORDERS_ENTERED_BY_USER_TODAY"
  | "ORDERS_REJECTED_TODAY"
  | "UN_PRINTED_RESULTS"
  | "INCOMING_ORDERS"
  | "AVERAGE_TURN_AROUND_TIME"
  | "DELAYED_TURN_AROUND"
  | "ORDERS_FOR_USER"
  | "SAMPLES_TO_COLLECT"
  | "BACKLOG_TILE";

interface UserSessionDetails {
  userSessionDetails: any;
}

interface Notification {
  notificationVisible: any;
  setNotificationVisible: any;
  addNotification: any;
}

type PanelView = "ACTIVE" | "BACKLOG";

const HomeDashBoard: React.FC<DashBoardProps> = () => {
  const intl = useIntl();

  const [counts, setCounts] = useState({
    ordersInProgress: 0,
    ordersReadyForValidation: 0,
    ordersCompletedToday: 0,
    patiallyCompletedToday: 0,
    orderEnterdByUserToday: 0,
    ordersRejectedToday: 0,
    unPritendResults: 0,
    incomigOrders: 0,
    averageTurnAroudTime: 0,
    delayedTurnAround: 0,
    samplesToCollect: 0,
  });

  const [timeMetrics, setTimeMetrics] = useState({
    receptionToResult: 0,
    resultToValidation: 0,
    receptionToValidation: 0,
  });

  const [data, setData] = useState([]);
  const [incomingOrdersData, setIncomingOrdersData] = useState<any[]>([]);
  const [testSections, setTestSections] = useState([]);
  const [selectedTestSection, setSelectedTestSection] = useState("");
  const [loading, setLoading] = useState(true);

  const [rightPage, setRightPage] = useState(1);
  const [rightPageSize, setRightPageSize] = useState(10);
  const [leftPage, setLeftPage] = useState(1);
  const [leftPageSize, setLeftPageSize] = useState(10);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);

  const [selectedTile, setSelectedTile] = useState<Tile>(null);

  const [rightSearch, setRightSearch] = useState("");
  const [leftSearch, setLeftSearch] = useState("");

  const [rightPanelView, setRightPanelView] = useState<PanelView>("ACTIVE");
  const [leftPanelView, setLeftPanelView] = useState<PanelView>("ACTIVE");
  const [dashboardTab, setDashboardTab] = useState<"LEFT" | "RIGHT">("RIGHT");

  const [isFetchingExternalOrder, setIsFetchingExternalOrder] = useState(false);

  const componentMounted = useRef(true);
  const tileLoadSequence = useRef(0);
  const patientNameCache = useRef<Record<string, string>>({});

  const { userSessionDetails } = useContext(
    UserSessionDetailsContext,
  ) as UserSessionDetails;
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as Notification;

  const message = useCallback(
    (id: string) => intl.formatMessage({ id }),
    [intl],
  );

  const isSplitLayout = (type?: MetricType | null) =>
    type === "ON_GOING_ORDERS" || type === "ORDERS_IN_PROGRESS";

  // ── DATA FETCHING ────────────────────────────────────────────────────────────
  const usesInProgressView = (type?: MetricType | null) =>
    type === "ORDERS_IN_PROGRESS" || type === "ON_GOING_ORDERS";

  const getTileEndpoint = (tile: Tile): string => {
    if (tile.type === "AVERAGE_TURN_AROUND_TIME")
      return "/rest/home-dashboard/turn-around-time-metrics";
    if (tile.type === "ORDERS_FOR_USER")
      return `/rest/home-dashboard/ORDERS_FOR_USER?systemUserId=${tile.id}`;
    if (usesInProgressView(tile.type))
      return "/rest/home-dashboard/ORDERS-Grouped";
    if (tile.type === "ORDERS_READY_FOR_VALIDATION")
      return "/rest/home-dashboard/VALIDATION-Grouped";
    return `/rest/home-dashboard/${tile.type}`;
  };

  const getPagedTileEndpoint = (tile: Tile, targetPage: number): string => {
    const endpoint = getTileEndpoint(tile);
    const sep = endpoint.includes("?") ? "&" : "?";
    return `${endpoint}${sep}page=${targetPage}`;
  };

  const isSameLocalDay = useCallback((left: Date, right = new Date()) => {
    return (
      left.getFullYear() === right.getFullYear() &&
      left.getMonth() === right.getMonth() &&
      left.getDate() === right.getDate()
    );
  }, []);

  const parseDisplayDate = useCallback(
    (value?: unknown) => {
      if (value == null || value === "") return null;

      if (value instanceof Date) {
        return Number.isNaN(value.getTime()) ? null : value;
      }

      if (typeof value === "number") {
        const parsed = new Date(value);
        return Number.isNaN(parsed.getTime()) ? null : parsed;
      }

      const raw = String(value).trim();
      if (!raw) return null;

      if (/^\d+$/.test(raw)) {
        const parsed = new Date(Number(raw));
        return Number.isNaN(parsed.getTime()) ? null : parsed;
      }

      // ── Slash-formatted dates (DD/MM/YYYY or MM/DD/YYYY) ──────────────────
      // MUST be checked BEFORE new Date(raw) because JS always parses these as
      // MM/DD/YYYY, which turns a DD/MM date like "03/05/2026" (May 3) into
      // March 5 — sending today's orders straight to backlog.
      const dateToken = raw.split(/\s+/)[0];
      const slashMatch = dateToken.match(/^(\d{1,2})\/(\d{1,2})\/(\d{2,4})$/);
      if (slashMatch) {
        const first = Number(slashMatch[1]);
        const second = Number(slashMatch[2]);
        const yearValue = Number(slashMatch[3]);
        const year = slashMatch[3].length === 2 ? 2000 + yearValue : yearValue;
        const preferMonthFirst = /(^|[-_])(en-US|en_US)([-_]|$)/i.test(
          String(intl.locale ?? ""),
        );

        const buildCandidate = (month: number, day: number) => {
          const candidate = new Date(year, month - 1, day);
          return candidate.getFullYear() === year &&
            candidate.getMonth() === month - 1 &&
            candidate.getDate() === day
            ? candidate
            : null;
        };

        const monthFirst = buildCandidate(first, second);
        const dayFirst = buildCandidate(second, first);

        if (monthFirst && !dayFirst) return monthFirst;
        if (dayFirst && !monthFirst) return dayFirst;
        if (monthFirst && dayFirst) {
          return preferMonthFirst ? monthFirst : dayFirst;
        }
        return null;
      }

      // ── ISO / RFC dates (YYYY-MM-DD, timestamps, etc.) ────────────────────
      const direct = new Date(raw);
      if (!Number.isNaN(direct.getTime())) return direct;

      return null;
    },
    [intl.locale],
  );

  const parseIncomingOrderTimestamp = useCallback(
    (timestamp?: any) => {
      return parseDisplayDate(timestamp);
    },
    [parseDisplayDate],
  );

  const formatIncomingOrderTimestamp = useCallback(
    (timestamp?: any) => {
      const parsed = parseIncomingOrderTimestamp(timestamp);
      if (!parsed) return "-";

      return parsed.toLocaleString([], {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    },
    [parseIncomingOrderTimestamp],
  );

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/home-dashboard/metrics", loadCount);
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Auto-land on "On Going Orders" as the default view
  useEffect(() => {
    if (selectedTile == null) {
      setSelectedTile({
        title: "On Going Orders",
        subTitle: (
          <FormattedMessage id="dashboard.in.progress.subtitle.label" />
        ),
        type: "ON_GOING_ORDERS",
        value: counts.ordersInProgress ?? 0,
      });
    }
  }, [counts.ordersInProgress]);

  useEffect(() => {
    setIsFetchingExternalOrder(true);
    getFromOpenElisServer("/rest/incoming-orders", (res) => {
      if (!componentMounted.current) return;
      const raw = Array.isArray(res) ? res : [];
      // Normalize field names to match table header keys + set row id
      const list = raw.map((item) => ({
        ...item,
        id: item.externalOrderNumber,
        received: item.receivedTimestamp
          ? new Date(item.receivedTimestamp).toLocaleString([], {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          })
          : "—",
        tests: item.testCount != null ? String(item.testCount) : "-",
        source: item.source ?? "-",
      }));
      setIncomingOrdersData(list);
      setCounts((prev) => ({ ...prev, samplesToCollect: list.length }));
      setIsFetchingExternalOrder(false);
    });
  }, []);

  useEffect(() => {
    let timeoutId: NodeJS.Timeout;
    const checkMidnight = () => {
      const now = new Date();
      const nextMidnight = new Date(now);
      nextMidnight.setDate(now.getDate() + 1);
      nextMidnight.setHours(0, 0, 0, 0);
      const msUntilMidnight = nextMidnight.getTime() - now.getTime();

      timeoutId = setTimeout(() => {
        if (!componentMounted.current) return;
        getFromOpenElisServer("/rest/home-dashboard/metrics", loadCount);
        if (selectedTile) {
          const seq = ++tileLoadSequence.current;
          if (selectedTile.type === "AVERAGE_TURN_AROUND_TIME") {
            getFromOpenElisServer(getTileEndpoint(selectedTile), (d) =>
              loadTimeMetrics(d, seq),
            );
          } else if (isSplitLayout(selectedTile.type)) {
            loadOngoingOrdersData(seq);
          } else {
            getFromOpenElisServer(getTileEndpoint(selectedTile), (res) =>
              loadData(res, false, seq),
            );
          }
        }
        checkMidnight();
      }, msUntilMidnight);
    };

    checkMidnight();
    return () => clearTimeout(timeoutId);
  }, [selectedTile]);

  useEffect(() => {
    if (selectedTile == null) return;
    const seq = ++tileLoadSequence.current;
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    setLoading(true);
    if (selectedTile.type === "AVERAGE_TURN_AROUND_TIME") {
      getFromOpenElisServer(getTileEndpoint(selectedTile), (d) =>
        loadTimeMetrics(d, seq),
      );
    } else if (isSplitLayout(selectedTile.type)) {
      loadOngoingOrdersData(seq);
    } else {
      getFromOpenElisServer(getTileEndpoint(selectedTile), (res) =>
        loadData(res, false, seq),
      );
    }
  }, [selectedTile]);

  useEffect(() => {
    getFromOpenElisServer("/rest/user-test-sections/ALL", fetchTestSections);
  }, []);

  useEffect(() => {
    setRightSearch("");
    setLeftSearch("");
    setRightPanelView("ACTIVE");
    setLeftPanelView("ACTIVE");
    setRightPage(1);
    setLeftPage(1);
    patientNameCache.current = {};
  }, [selectedTile?.type]);

  useEffect(() => {
    setRightPage(1);
  }, [rightSearch, rightPanelView]);
  useEffect(() => {
    setLeftPage(1);
  }, [leftSearch, leftPanelView]);

  const fetchTestSections = (res) => {
    setTestSections(res);
    hasRole(userSessionDetails, "Global Administrator")
      ? setSelectedTestSection("all")
      : setSelectedTestSection(res[0]?.id);
  };

  const loadNextResultsPage = () => {
    const seq = ++tileLoadSequence.current;
    setLoading(true);
    getFromOpenElisServer(getPagedTileEndpoint(selectedTile, nextPage), (res) =>
      loadData(res, false, seq),
    );
  };

  const loadPreviousResultsPage = () => {
    const seq = ++tileLoadSequence.current;
    setLoading(true);
    getFromOpenElisServer(
      getPagedTileEndpoint(selectedTile, previousPage),
      (res) => loadData(res, false, seq),
    );
  };

  const loadCount = (d) => {
    if (componentMounted.current) {
      setCounts((prev) => ({
        ...d,
        samplesToCollect:
          prev.samplesToCollect > 0
            ? prev.samplesToCollect
            : d.samplesToCollect || 0,
      }));
      setLoading(false);
    }
  };

  const getGroupedItemKey = (item) => String(item?.labNumber ?? item?.id ?? "");

  const formatPatientName = (last?: string, first?: string) =>
    [last, first]
      .map((v) => String(v ?? "").trim())
      .filter(Boolean)
      .join(", ");

  const getExactPatientSearchResult = (results = [], patientId = "") => {
    if (!Array.isArray(results) || results.length === 0) return null;
    const norm = String(patientId ?? "").trim();
    if (!norm) return results[0];
    return (
      results.find((r) => String(r?.nationalId ?? "").trim() === norm) ?? null
    );
  };

  const getPatientLookupKey = (item) => {
    const pid = String(item?.patientId ?? "").trim();
    if (pid) return `patientId:${pid}`;
    const ln = String(item?.labNumber ?? "").trim();
    if (ln) return `labNumber:${ln}`;
    return "";
  };

  const fetchPatientNameForItem = async (item) => {
    const pid = String(item?.patientId ?? "").trim();
    const ln = String(item?.labNumber ?? "").trim();
    if (ln) {
      const res = await getFromOpenElisServerV2(
        `/rest/patient-search-results?labNumber=${encodeURIComponent(ln)}&suppressExternalSearch=true`,
      );
      const r = getExactPatientSearchResult(res?.patientSearchResults, pid);
      const name = formatPatientName(r?.lastName, r?.firstName);
      if (name) return name;
    }
    if (pid) {
      const res = await getFromOpenElisServerV2(
        `/rest/patient-search-results?nationalID=${encodeURIComponent(pid)}&suppressExternalSearch=true`,
      );
      const r = getExactPatientSearchResult(res?.patientSearchResults, pid);
      return formatPatientName(r?.lastName, r?.firstName);
    }
    return "";
  };

  const enrichMissingPatientNames = async (
    displayItems = [],
    seq = tileLoadSequence.current,
  ) => {
    const missingKeys = Array.from(
      new Set(
        displayItems
          .filter(
            (item) =>
              !String(item?.patientName ?? "").trim() &&
              getPatientLookupKey(item) &&
              !Object.prototype.hasOwnProperty.call(
                patientNameCache.current,
                getPatientLookupKey(item),
              ),
          )
          .map((item) => getPatientLookupKey(item)),
      ),
    ).slice(0, 20);
    if (missingKeys.length === 0) return;
    await Promise.all(
      missingKeys.map(async (key) => {
        const item = displayItems.find(
          (candidate) => getPatientLookupKey(candidate) === key,
        );
        if (!item) return;
        try {
          patientNameCache.current[key] = await fetchPatientNameForItem(item);
        } catch {
          patientNameCache.current[key] = "";
        }
      }),
    );
    if (seq !== tileLoadSequence.current) return;
    setData((cur) =>
      cur.map((item) => {
        if (String(item?.patientName ?? "").trim()) return item;
        const name = patientNameCache.current[getPatientLookupKey(item)];
        return name ? { ...item, patientName: name } : item;
      }),
    );
  };

  const normalizeGroupedDisplayItems = (items = [], tileType?: MetricType) =>
    items.map((item) => {
      const prc = Number(item.pendingResultCount);
      const pvc = Number(item.pendingValidationCount);
      const tc = Number(item.testCount);
      const pendingResultCount = Number.isFinite(prc) ? prc : 0;
      const pendingValidationCount = Number.isFinite(pvc) ? pvc : 0;
      // Always trust the backend's testCount — it is the real total of ALL
      // analyses for the sample regardless of status. Only fall back to
      // prc+pvc if the backend didn't send a valid count.
      const testCount =
        Number.isFinite(tc) && tc > 0
          ? tc
          : pendingResultCount + pendingValidationCount;
      return {
        ...item,
        pendingResultCount,
        pendingValidationCount,
        testCount,
      };
    });

  const mergeGroupedDisplayItems = (pending = [], validation = []) => {
    const merged = new Map();
    normalizeGroupedDisplayItems(pending, "ORDERS_IN_PROGRESS").forEach(
      (item) => {
        const key = getGroupedItemKey(item);
        if (!key) return;
        merged.set(key, {
          ...item,
          id: item.id || key,
          pendingResultCount: Number(item.pendingResultCount) || 0,
          pendingValidationCount: Number(item.pendingValidationCount) || 0,
          testCount: Number(item.testCount) || 0,
        });
      },
    );
    normalizeGroupedDisplayItems(
      validation,
      "ORDERS_READY_FOR_VALIDATION",
    ).forEach((item) => {
      const key = getGroupedItemKey(item);
      if (!key) return;
      const ex = merged.get(key);
      const prc = Number(ex?.pendingResultCount) || 0;
      const pvc =
        (Number(ex?.pendingValidationCount) || 0) +
        (Number(item.pendingValidationCount) || 0);
      // Use the backend testCount from whichever side has it; fall back to prc+pvc.
      const tc = Number(ex?.testCount) || Number(item.testCount) || prc + pvc;
      merged.set(key, {
        ...ex,
        ...item,
        id: ex?.id || item.id || key,
        labNumber: ex?.labNumber || item.labNumber,
        orderDate: ex?.orderDate || item.orderDate,
        patientId: ex?.patientId || item.patientId,
        patientName: ex?.patientName || item.patientName,
        priority: ex?.priority || item.priority,
        testSection: ex?.testSection || item.testSection,
        pendingResultCount: prc,
        pendingValidationCount: pvc,
        testCount: tc,
      });
    });
    return Array.from(merged.values());
  };

  const loadOngoingOrdersData = async (seq: number) => {
    try {
      const orders = await getFromOpenElisServerV2(
        "/rest/home-dashboard/ORDERS-Grouped",
      );
      loadData(orders, false, seq);
    } catch {
      loadData({ displayItems: [] }, false, seq);
    }
  };

  const loadData = (
    res,
    disableServerPaging = false,
    seq = tileLoadSequence.current,
  ) => {
    if (seq !== tileLoadSequence.current) return;
    const normalised = Array.isArray(res?.displayItems)
      ? normalizeGroupedDisplayItems(res.displayItems, selectedTile?.type)
      : [];
    if (normalised.length > 0) {
      setData(normalised);
      enrichMissingPatientNames(normalised, seq);
    } else {
      setData([]);
    }
    if (!disableServerPaging && res?.paging) {
      const { totalPages, currentPage } = res.paging;
      if (totalPages > 1) {
        setPagination(true);
        setCurrentApiPage(currentPage);
        setTotalApiPages(totalPages);
        setNextPage(
          parseInt(currentPage) < parseInt(totalPages)
            ? parseInt(currentPage) + 1
            : null,
        );
        setPreviousPage(
          parseInt(currentPage) > 1 ? parseInt(currentPage) - 1 : null,
        );
      }
    } else {
      setPagination(false);
      setCurrentApiPage(null);
      setTotalApiPages(null);
      setNextPage(null);
      setPreviousPage(null);
    }
    setLoading(false);
  };

  const loadTimeMetrics = (d, seq = tileLoadSequence.current) => {
    if (seq !== tileLoadSequence.current) return;
    setTimeMetrics(d);
    setLoading(false);
  };

  // ── TILE LIST ────────────────────────────────────────────────────────────────
  const tileList: Array<Tile> = [
    {
      title: <FormattedMessage id="dashboard.in.progress.label" />,
      subTitle: <FormattedMessage id="dashboard.in.progress.subtitle.label" />,
      type: "ORDERS_IN_PROGRESS",
      value: counts.ordersInProgress,
    },
    {
      title: (
        <FormattedMessage id="dashboard.validation.ready.subtitle.label" />
      ),
      subTitle: (
        <FormattedMessage id="dashboard.validation.ready.subtitle.label" />
      ),
      type: "ORDERS_READY_FOR_VALIDATION",
      value: counts.ordersReadyForValidation,
    },
    {
      title: <FormattedMessage id="dashboard.complete.orders.label" />,
      subTitle: <FormattedMessage id="dashboard.orders.subtitle.label" />,
      type: "ORDERS_COMPLETED_TODAY",
      value: counts.ordersCompletedToday,
    },
    {
      title: <FormattedMessage id="dashboard.samplesToCollect.label" />,
      subTitle: (
        <FormattedMessage id="dashboard.samplesToCollect.subtitle.label" />
      ),
      type: "SAMPLES_TO_COLLECT",
      value: counts.samplesToCollect ?? 0,
    },
    {
      title: "On Going Orders",
      subTitle: <FormattedMessage id="dashboard.in.progress.subtitle.label" />,
      type: "ON_GOING_ORDERS",
      value: counts.ordersInProgress ?? 0,
    },
  ];

  const averageTimeTileList: Array<Tile> = [
    {
      title: message("dashboard.average.receptionToValidation"),
      subTitle: message("dashboard.average.receptionToValidation"),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToValidation,
    },
    {
      title: message("dashboard.average.receptionToResult"),
      subTitle: message("dashboard.average.receptionToResult"),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToResult,
    },
    {
      title: message("dashboard.average.resultToValidation"),
      subTitle: message("dashboard.average.resultToValidation"),
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.resultToValidation,
    },
  ];

  const tilesWithTabs = ["ORDERS_COMPLETED_TODAY", "ORDERS_FOR_USER"];

  // ── HANDLERS ─────────────────────────────────────────────────────────────────
  const handleMinimizeClick = () => {
    setSelectedTile(null);
    hasRole(userSessionDetails, "Global Administrator")
      ? setSelectedTestSection("all")
      : setSelectedTestSection(testSections[0]?.id);
  };

  const handleMaximizeClick = (tile) => {
    if (tile?.type === "SAMPLES_TO_COLLECT") {
      window.location.href = getFullPath("/IncomingOrders");
      return;
    }
    if (tile?.type === "BACKLOG_TILE") {
      const ongoingTile = tileList.find((t) => t.type === "ON_GOING_ORDERS");
      if (ongoingTile) {
        setSelectedTile(ongoingTile);
        setRightPanelView("BACKLOG");
      }
      return;
    }
    if (
      testSections?.length > 0 ||
      hasRole(userSessionDetails, "Global Administrator")
    ) {
      setSelectedTile(tile);
    } else {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "accessDenied.title" }),
        message: intl.formatMessage({ id: "accessDenied.message" }),
      });
    }
  };

  const viewUserOrders = (row) => {
    const firstName = row.cells.find(
      (e) => e.info.header === "userFirstName",
    ).value;
    const lastName = row.cells.find(
      (e) => e.info.header === "userLastName",
    ).value;
    const value = row.cells.find(
      (e) => e.info.header === "countOfOrdersEntered",
    ).value;
    setSelectedTile({
      title: <FormattedMessage id="dashboard.user.orders.today.label" />,
      subTitle: `${firstName} ${lastName}`,
      type: "ORDERS_FOR_USER",
      value,
      id: row.id,
    });
  };

  // ── MEMOISED DATA ─────────────────────────────────────────────────────────────

  const isToday = useCallback(
    (orderDate?: string) => {
      if (!orderDate) return true;
      const parsed = parseDisplayDate(orderDate);
      if (!parsed) return true;
      return isSameLocalDay(parsed);
    },
    [isSameLocalDay, parseDisplayDate],
  );

  const sectionFilteredData = useMemo(() => {
    return data.filter(
      (item) =>
        isToday(item.orderDate) &&
        (tilesWithTabs.includes(selectedTile?.type) &&
          selectedTestSection !== "all"
          ? item.testSection === selectedTestSection
          : true),
    );
  }, [data, selectedTile?.type, selectedTestSection, isToday]);

  const filteredRightData = useMemo(() => {
    const q = rightSearch.trim().toLowerCase();
    if (!q) return sectionFilteredData;
    return sectionFilteredData.filter((item) => {
      const pid = String(item.patientId ?? "").toLowerCase();
      const pname = String(item.patientName ?? "").toLowerCase();
      const raw = String(item.labNumber ?? "").toLowerCase();
      const fmt = item.labNumber
        ? convertAlphaNumLabNumForDisplay(String(item.labNumber)).toLowerCase()
        : "";
      return (
        pid.includes(q) ||
        pname.includes(q) ||
        raw.includes(q) ||
        fmt.includes(q)
      );
    });
  }, [sectionFilteredData, rightSearch]);

  const backlogTableData = useMemo(
    () =>
      data.filter(
        (item) =>
          !isToday(item.orderDate) &&
          (tilesWithTabs.includes(selectedTile?.type) &&
            selectedTestSection !== "all"
            ? item.testSection === selectedTestSection
            : true),
      ),
    [data, selectedTile?.type, selectedTestSection, isToday],
  );

  const filteredBacklogData = useMemo(() => {
    const q = rightSearch.trim().toLowerCase();
    if (!q) return backlogTableData;
    return backlogTableData.filter(
      (item) =>
        String(item.labNumber ?? "")
          .toLowerCase()
          .includes(q) ||
        String(item.patientName ?? "")
          .toLowerCase()
          .includes(q) ||
        String(item.patientId ?? "")
          .toLowerCase()
          .includes(q),
    );
  }, [backlogTableData, rightSearch]);

  const filteredLeftData = useMemo(() => {
    const q = leftSearch.trim().toLowerCase();
    let filtered = incomingOrdersData;
    if (leftPanelView === "ACTIVE") {
      filtered = incomingOrdersData;
    }
    if (!q) return filtered;
    return filtered.filter((item) => {
      const patientName = String(item.patientName ?? "").toLowerCase();
      const source = String(item.source ?? "").toLowerCase();
      const tests = String(item.tests ?? "").toLowerCase();
      const orderId = String(item.id ?? "").toLowerCase();
      const patientGuid = String(item.patientGuid ?? "").toLowerCase();
      const testGuids = (item.testGuids ?? []).some((guid: string) =>
        guid.toLowerCase().includes(q),
      );
      const testNameMatch = tests.includes(q);
      return (
        patientName.includes(q) ||
        source.includes(q) ||
        tests.includes(q) ||
        orderId.includes(q) ||
        patientGuid.includes(q) ||
        testGuids ||
        testNameMatch
      );
    });
  }, [incomingOrdersData, leftSearch, leftPanelView]);

  const leftBacklogData = useMemo(() => {
    const q = leftSearch.trim().toLowerCase();
    const filtered = incomingOrdersData.filter(() => false);
    if (!q) return filtered;
    return filtered;
  }, [incomingOrdersData, leftSearch]);

  // Workflow summary counters
  const workflowOrderCount = useMemo(
    () =>
      new Set(
        sectionFilteredData
          .map((i) => String(i.labNumber ?? i.id ?? ""))
          .filter(Boolean),
      ).size,
    [sectionFilteredData],
  );
  const workflowPatientCount = useMemo(
    () =>
      new Set(
        sectionFilteredData
          .map((i) => String(i.patientId ?? ""))
          .filter(Boolean),
      ).size,
    [sectionFilteredData],
  );
  const workflowPendingResultCount = useMemo(
    () =>
      sectionFilteredData.reduce(
        (t, i) => t + (Number(i.pendingResultCount) || 0),
        0,
      ),
    [sectionFilteredData],
  );
  const workflowPendingValidationCount = useMemo(
    () =>
      sectionFilteredData.reduce(
        (t, i) => t + (Number(i.pendingValidationCount) || 0),
        0,
      ),
    [sectionFilteredData],
  );
  const workflowTestCount = useMemo(
    () =>
      sectionFilteredData.reduce((t, i) => {
        const tc = Number(i.testCount);
        const prc = Number(i.pendingResultCount) || 0;
        const pvc = Number(i.pendingValidationCount) || 0;
        // Use testCount if it's a real positive number (covers both active and
        // completed orders now that the backend sets it correctly).
        // Fall back to prc+pvc for legacy data that may lack testCount.
        // No || 1 fallback — a 0 count is valid and should show as 0.
        return t + (Number.isFinite(tc) && tc > 0 ? tc : prc + pvc);
      }, 0),
    [sectionFilteredData],
  );

  const summaryCards = [
    {
      label: message("dashboard.in.progress.subtitle.label"),
      value: workflowPendingResultCount,
      color: "#5f7fa3",
    },
    {
      label: message("dashboard.workflow.awaitingValidation"),
      value: workflowPendingValidationCount,
      color: "#5b8a8b",
    },
    {
      label: message("dashboard.table.total"),
      value: workflowTestCount,
      color: "#6d88a8",
    },
    {
      label: message("dashboard.workflow.totalOrders"),
      value: workflowOrderCount,
      color: "#7f7b96",
    },
    {
      label: message("dashboard.workflow.totalPatients"),
      value: workflowPatientCount,
      color: "#6c8b74",
    },
    {
      label: message("dashboard.complete.orders.label"),
      value: counts.ordersCompletedToday ?? 0,
      color: "#9a8366",
    },
  ];

  // ── TABLE HEADERS ─────────────────────────────────────────────────────────────
  const groupedOrderHeaders = [
    { key: "priority", header: message("dashboard.table.priority") },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientName",
      header: <FormattedMessage id="incomingOrders.table.patientName" />,
    },
    { key: "actions", header: message("dashboard.table.actions") },
    { key: "labNumber", header: <FormattedMessage id="eorder.labNumber" /> },
    {
      key: "pendingResultCount",
      header: <FormattedMessage id="dashboard.table.pendingResult" />,
    },
    {
      key: "pendingValidationCount",
      header: <FormattedMessage id="dashboard.table.pendingValidation" />,
    },
    {
      key: "testCount",
      header: <FormattedMessage id="dashboard.table.total" />,
    },
  ];

  const orderHeaders = [
    { key: "priority", header: message("dashboard.table.priority") },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientName",
      header: <FormattedMessage id="incomingOrders.table.patientName" />,
    },
    { key: "actions", header: message("dashboard.table.actions") },
    { key: "labNumber", header: <FormattedMessage id="eorder.labNumber" /> },
    { key: "testName", header: <FormattedMessage id="eorder.test.name" /> },
  ];

  const incomingOrderHeaders = [
    { key: "patientName", header: message("incomingOrders.table.patientName") },
    { key: "received", header: message("dashboard.table.received") },
    { key: "tests", header: message("dashboard.panel.tests") },
    { key: "source", header: message("dashboard.table.source") },
    { key: "actions", header: message("dashboard.table.actions") },
  ];

  const userHeaders = [
    { key: "userFirstName", header: message("dashboard.table.firstName") },
    { key: "userLastName", header: message("dashboard.table.lastName") },
    {
      key: "countOfOrdersEntered",
      header: message("dashboard.table.ordersEntered"),
    },
  ];

  // ── CELL RENDERER ─────────────────────────────────────────────────────────────
  const renderCell = (cell, row) => {
    const rowPatientName =
      data.find((item) => String(item.id) === String(row.id))?.patientName ||
      "";
    const isCompleted =
      data.find((item) => String(item.id) === String(row.id))?.completed ===
      true;

    if (cell.info.header === "labNumber" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <div style={{ display: "flex", alignItems: "center" }}>
            <Button
              onClick={async () => {
                if ("clipboard" in navigator)
                  return await navigator.clipboard.writeText(cell.value);
                return document.execCommand("copy", true, cell.value);
              }}
              kind="ghost"
              iconDescription={intl.formatMessage({
                id: "instructions.copy.labnum",
              })}
              hasIconOnly
              renderIcon={Copy}
            />
            {usesInProgressView(selectedTile.type) ||
              selectedTile.type === "ORDERS_READY_FOR_VALIDATION" ? (
              <Link
                style={{ color: "blue" }}
                href={
                  usesInProgressView(selectedTile.type)
                    ? getFullPath(
                      "/result?type=order&doRange=false&accessionNumber=" +
                      cell.value,
                    )
                    : getFullPath(
                      "/validation?type=order&accessionNumber=" + cell.value,
                    )
                }
              >
                <u>{convertAlphaNumLabNumForDisplay(cell.value)}</u>
              </Link>
            ) : (
              <>{convertAlphaNumLabNumForDisplay(cell.value)}</>
            )}
          </div>
        </TableCell>
      );
    } else if (cell.info.header === "patientId") {
      return (
        <TableCell key={cell.id}>
          {cell.value ? (
            <span className="dashboard-patient-id">{cell.value}</span>
          ) : (
            ""
          )}
        </TableCell>
      );
    } else if (cell.info.header === "patientName") {
      return (
        <TableCell key={cell.id}>
          {cell.value || rowPatientName || ""}
        </TableCell>
      );
    } else if (cell.info.header === "actions") {
      const accessionNumber = row.cells.find(
        (c) => c.info.header === "labNumber",
      )?.value;
      if (!accessionNumber) return <TableCell key={cell.id} />;
      const resultUrl = getFullPath(
        "/result?type=order&doRange=false&accessionNumber=" + accessionNumber,
      );
      const validationUrl = getFullPath(
        "/validation?type=order&accessionNumber=" + accessionNumber,
      );
      const reportUrl =
        config.serverBaseUrl +
        "/ReportPrint?report=patientCILNSP_vreduit&type=patient&accessionDirect=" +
        accessionNumber +
        "&highAccessionDirect=" +
        accessionNumber +
        "&dateOfBirthSearchValue=&selPatient=&referringSiteId=&referringSiteDepartmentId=&onlyResults=false&_onlyResults=on&dateType=RESULT_DATE&lowerDateRange=&upperDateRange=";
      const barcodeUrl =
        config.serverBaseUrl +
        "/LabelMakerServlet?labNo=" +
        accessionNumber +
        "&type=order&quantity=1&override=true";
      return (
        <TableCell key={cell.id}>
          <div
            style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}
          >
            <a
              href={barcodeUrl}
              title="Print Barcode"
              target="_blank"
              rel="noreferrer"
              style={{ display: "inline-flex", alignItems: "center" }}
            >
              <img
                src={barcodeIcon}
                alt="Print Barcode"
                style={{ width: "1.1rem", height: "1.1rem" }}
              />
            </a>
            <a
              href={resultUrl}
              title="Results"
              target="_blank"
              rel="noreferrer"
              style={{ display: "inline-flex", alignItems: "center" }}
              onClick={(e) => {
                e.preventDefault();
                window.open(resultUrl, "_blank");
              }}
            >
              <img
                src={resultIcon}
                alt="Results"
                style={{ width: "1.1rem", height: "1.1rem" }}
              />
            </a>
            <a
              href={validationUrl}
              title="Validate"
              target="_blank"
              rel="noreferrer"
              style={{ display: "inline-flex", alignItems: "center" }}
              onClick={(e) => {
                e.preventDefault();
                window.open(validationUrl, "_blank");
              }}
            >
              <img
                src={validateIcon}
                alt="Validate"
                style={{ width: "1.1rem", height: "1.1rem" }}
              />
            </a>
            <a
              href={reportUrl}
              title="Report"
              target="_blank"
              rel="noreferrer"
              style={{ display: "inline-flex", alignItems: "center" }}
            >
              <img
                src={reportIcon}
                alt="Report"
                style={{ width: "1.1rem", height: "1.1rem" }}
              />
            </a>
          </div>
        </TableCell>
      );
    } else if (cell.info.header === "countOfOrdersEntered" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <Link style={{ color: "blue" }}>{cell.value}</Link>
        </TableCell>
      );
    } else if (cell.info.header === "priority" && cell.value) {
      const priority = String(cell.value).toUpperCase();
      let bgColor = "#8d8d8d";
      let textColor = "#ffffff";
      if (priority === "STAT") {
        bgColor = "#da1e28";
      } else if (priority === "ASAP") {
        bgColor = "#f1c21b";
        textColor = "#161616";
      } else if (priority === "TIMED") {
        bgColor = "#0f62fe";
      } else if (priority === "FUTURE STAT") {
        bgColor = "#8a3ffc";
      } else if (priority === "ROUTINE") {
        bgColor = "#24a148";
      }
      return (
        <TableCell key={cell.id}>
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              padding: "0.2rem 0.6rem",
              borderRadius: "0.875rem",
              fontSize: "0.75rem",
              fontWeight: 600,
              backgroundColor: bgColor,
              color: textColor,
              letterSpacing: "0.32px",
              textTransform: "uppercase",
            }}
          >
            {cell.value}
          </div>
        </TableCell>
      );
    } else if (cell.info.header === "testCount") {
      return (
        <TableCell key={cell.id}>
          {isCompleted ? (
            <CheckmarkFilled
              size={16}
              style={{ color: "#24a148" }}
              title="Completed"
            />
          ) : (
            cell.value
          )}
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  // ── PANEL TOGGLE COMPONENT ────────────────────────────────────────────────────
  // ── RENDER ───────────────────────────────────────────────────────────────────
  return (
    <>
      {loading && <Loading description={message("dashboard.loading")} />}
      {notificationVisible === true && <AlertDialog />}

      {selectedTile == null ? (
        <div className="home-dashboard-container">
          {tileList.map((tile, index) => (
            <ClickableTile
              key={index}
              className="dashboard-tile"
              onClick={() => handleMaximizeClick(tile)}
            >
              <h3 className="tile-title">{tile.title}</h3>
              <p className="tile-subtitle">{tile.subTitle}</p>
              <p className="tile-value">{tile.value}</p>
              <div className="tile-icon">
                <div
                  onClick={() => handleMaximizeClick(tile)}
                  className="icon-wrapper"
                >
                  <Maximize
                    id="maximizeIcon"
                    size={20}
                    className="clickable-icon"
                  />
                </div>
              </div>
            </ClickableTile>
          ))}
        </div>
      ) : selectedTile.type === "AVERAGE_TURN_AROUND_TIME" ? (
        <div className="dashboard-view">
          <Tile className="dashboard-tile">
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h3 className="tile-title-view">{selectedTile.title}</h3>
                <div className="tile-icon">
                  <div onClick={handleMinimizeClick} className="icon-wrapper">
                    <Minimize size={20} className="clickable-icon" />
                  </div>
                </div>
              </Column>
            </Grid>
            <div className="home-dashboard-container">
              {averageTimeTileList.map((tile, i) => (
                <Tile key={i} className="dashboard-tile">
                  <h3 className="tile-title">{tile.title}</h3>
                  <p className="tile-value">{tile.value}</p>
                </Tile>
              ))}
            </div>
          </Tile>
        </div>
      ) : isSplitLayout(selectedTile.type) ? (
        /* ── SPLIT-PANEL LAYOUT ── */
        <div className="dashboard-page-shell">
          <div className="split-dashboard-header">
            <div className="split-dashboard-header__left">
              <div className="split-dashboard-title-panel">
                <div className="split-dashboard-title-group">
                  <h2 className="split-dashboard-title">
                    {message("dashboard.title")}
                  </h2>
                  <p className="split-dashboard-subtitle">
                    {message("dashboard.subtitle")}
                  </p>
                </div>
              </div>
            </div>
            <div
              className="split-summary-strip"
              aria-label={message("dashboard.title")}
            >
              {summaryCards.map((c) => (
                <div key={c.label} className="split-summary-pill">
                  <div className="split-summary-pill__meta">
                    <span
                      className="split-summary-dot"
                      style={{ background: c.color }}
                    />
                    <span className="split-summary-label">{c.label}</span>
                  </div>
                  <strong className="split-summary-value">{c.value}</strong>
                </div>
              ))}
            </div>
          </div>

          <div className="tabbed-dashboard-body">
            <div className="dashboard-section-switcher">
              <div className="dashboard-section-group">
                <h3 className="dashboard-section-title">
                  {message("dashboard.panel.tests")}
                </h3>
                <div
                  className="dashboard-table-toggle"
                  role="group"
                  aria-label={message("dashboard.panel.tests")}
                >
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "LEFT" && leftPanelView === "ACTIVE" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("LEFT");
                      setLeftPanelView("ACTIVE");
                    }}
                  >
                    {message("dashboard.filter.today")}
                    <span className="dashboard-table-toggle-count">
                      {filteredLeftData.length}
                    </span>
                  </button>
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "LEFT" && leftPanelView === "BACKLOG" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("LEFT");
                      setLeftPanelView("BACKLOG");
                    }}
                  >
                    {message("dashboard.filter.backlog")}
                    <span className="dashboard-table-toggle-count">
                      {leftBacklogData.length}
                    </span>
                  </button>
                </div>
              </div>
              <div className="dashboard-section-group">
                <h3 className="dashboard-section-title">
                  {message("dashboard.panel.samplesCollected")}
                </h3>
                <div
                  className="dashboard-table-toggle"
                  role="group"
                  aria-label={message("dashboard.panel.samplesCollected")}
                >
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "RIGHT" && rightPanelView === "ACTIVE" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("RIGHT");
                      setRightPanelView("ACTIVE");
                    }}
                  >
                    {message("dashboard.filter.today")}
                    <span className="dashboard-table-toggle-count">
                      {filteredRightData.length}
                    </span>
                  </button>
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "RIGHT" && rightPanelView === "BACKLOG" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("RIGHT");
                      setRightPanelView("BACKLOG");
                    }}
                  >
                    {message("dashboard.filter.backlog")}
                    <span className="dashboard-table-toggle-count">
                      {filteredBacklogData.length}
                    </span>
                  </button>
                </div>
              </div>
            </div>

            <div className="tab-panel-content">
              {/* LEFT PANEL — Tests / Incoming Orders */}
              {dashboardTab === "LEFT" && (
                <div className="split-panel split-panel--left">
                  <div className="split-panel-inner">
                    <div className="split-panel-search split-panel-search--stacked">
                      <TextInput
                        id="left-panel-search"
                        labelText={message("dashboard.incomingSearch.label")}
                        placeholder={
                          leftPanelView === "ACTIVE"
                            ? message("dashboard.incomingSearch.placeholder")
                            : message(
                              "dashboard.incomingSearch.backlogPlaceholder",
                            )
                        }
                        value={leftSearch}
                        onChange={(e) => setLeftSearch(e.target.value)}
                      />
                    </div>
                    <div className="split-panel-table-wrap">
                      {leftPanelView === "ACTIVE" ? (
                        <>
                          {isFetchingExternalOrder && (
                            <Loading
                              description={message("dashboard.loading")}
                              small
                            />
                          )}
                          <DataTable
                            rows={filteredLeftData.slice(
                              (leftPage - 1) * leftPageSize,
                              leftPage * leftPageSize,
                            )}
                            headers={incomingOrderHeaders}
                            isSortable
                          >
                            {({
                              rows,
                              headers,
                              getHeaderProps,
                              getTableProps,
                            }) => (
                              <TableContainer title="" description="">
                                <Table {...getTableProps()}>
                                  <TableHead>
                                    <TableRow>
                                      {headers.map((h) => (
                                        <TableHeader
                                          key={h.key}
                                          {...getHeaderProps({ header: h })}
                                        >
                                          {h.header}
                                        </TableHeader>
                                      ))}
                                    </TableRow>
                                  </TableHead>
                                  <TableBody>
                                    {rows.length === 0 &&
                                      !isFetchingExternalOrder ? (
                                      <TableRow>
                                        <TableCell
                                          colSpan={incomingOrderHeaders.length}
                                        >
                                          <p className="split-empty-msg">
                                            {message(
                                              "dashboard.empty.samplesToCollect",
                                            )}
                                          </p>
                                        </TableCell>
                                      </TableRow>
                                    ) : (
                                      rows.map((row) => (
                                        <TableRow key={row.id}>
                                          {headers.map((h) => {
                                            const cell = row.cells.find(
                                              (c) => c.info.header === h.key,
                                            );
                                            if (h.key === "actions") {
                                              // row.id IS the externalOrderNumber
                                              const collectUrl = getFullPath(
                                                "/SamplePatientEntry?incomingOrderNumber=" +
                                                encodeURIComponent(row.id),
                                              );
                                              return (
                                                <TableCell
                                                  key={`${row.id}-actions`}
                                                >
                                                  <a
                                                    href={collectUrl}
                                                    className="dashboard-collect-link"
                                                  >
                                                    {message(
                                                      "dashboard.action.collect",
                                                    )}
                                                  </a>
                                                </TableCell>
                                              );
                                            }
                                            return (
                                              <TableCell
                                                key={
                                                  cell?.id ||
                                                  `${row.id}-${h.key}`
                                                }
                                              >
                                                {cell?.value ?? "-"}
                                              </TableCell>
                                            );
                                          })}
                                        </TableRow>
                                      ))
                                    )}
                                  </TableBody>
                                </Table>
                              </TableContainer>
                            )}
                          </DataTable>
                          <Pagination
                            onChange={({ page: p, pageSize: ps }) => {
                              setLeftPage(p);
                              setLeftPageSize(ps);
                            }}
                            page={leftPage}
                            pageSize={leftPageSize}
                            pageSizes={[10, 20, 50, 100]}
                            totalItems={filteredLeftData.length}
                            forwardText={intl.formatMessage({
                              id: "pagination.forward",
                            })}
                            backwardText={intl.formatMessage({
                              id: "pagination.backward",
                            })}
                            itemRangeText={(min, max, total) =>
                              intl.formatMessage(
                                { id: "pagination.item-range" },
                                { min, max, total },
                              )
                            }
                            itemsPerPageText={intl.formatMessage({
                              id: "pagination.items-per-page",
                            })}
                            itemText={(min, max) =>
                              intl.formatMessage(
                                { id: "pagination.item" },
                                { min, max },
                              )
                            }
                            pageNumberText={intl.formatMessage({
                              id: "pagination.page-number",
                            })}
                            pageRangeText={(_c, total) =>
                              intl.formatMessage(
                                { id: "pagination.page-range" },
                                { total },
                              )
                            }
                            pageText={(p, unk) =>
                              intl.formatMessage(
                                { id: "pagination.page" },
                                { page: unk ? "" : p },
                              )
                            }
                          />
                        </>
                      ) : (
                        <div className="split-empty-msg">
                          {message("dashboard.empty.backlog")}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* RIGHT PANEL — Active Orders / Backlog */}
              {dashboardTab === "RIGHT" && (
                <div className="split-panel split-panel--right">
                  <div className="split-panel-inner">
                    {rightPanelView === "ACTIVE" &&
                      tilesWithTabs.includes(selectedTile.type) && (
                        <div className="dashboard-department-tabs">
                          <Tabs>
                            {hasRole(
                              userSessionDetails,
                              "Global Administrator",
                            ) ? (
                              <TabList
                                className="dashboard-department-tab-list"
                                aria-label={message(
                                  "dashboard.departmentTabs.label",
                                )}
                                contained
                              >
                                <Tab
                                  onClick={() => setSelectedTestSection("all")}
                                >
                                  <FormattedMessage id="all.label" />
                                </Tab>
                                {testSections?.map((item, id) => (
                                  <Tab
                                    key={id}
                                    onClick={() =>
                                      setSelectedTestSection(item.id)
                                    }
                                  >
                                    {item.value}
                                  </Tab>
                                ))}
                              </TabList>
                            ) : (
                              <TabList
                                className="dashboard-department-tab-list"
                                aria-label={message(
                                  "dashboard.departmentTabs.label",
                                )}
                                contained
                              >
                                {testSections?.map((item, id) => (
                                  <Tab
                                    key={id}
                                    onClick={() =>
                                      setSelectedTestSection(item.id)
                                    }
                                  >
                                    {item.value}
                                  </Tab>
                                ))}
                              </TabList>
                            )}
                          </Tabs>
                        </div>
                      )}

                    {pagination && rightPanelView === "ACTIVE" && (
                      <div className="dashboard-server-pagination">
                        <Link>
                          {currentApiPage} / {totalApiPages}
                        </Link>
                        <Button
                          hasIconOnly
                          onClick={loadPreviousResultsPage}
                          disabled={!previousPage}
                          renderIcon={ArrowLeft}
                          iconDescription="previous"
                          size="sm"
                        />
                        <Button
                          hasIconOnly
                          onClick={loadNextResultsPage}
                          disabled={!nextPage}
                          renderIcon={ArrowRight}
                          iconDescription="next"
                          size="sm"
                        />
                      </div>
                    )}

                    <div className="split-panel-search">
                      <TextInput
                        id="right-panel-search"
                        labelText=""
                        placeholder={
                          rightPanelView === "ACTIVE"
                            ? message("dashboard.orders.search.placeholder")
                            : message("dashboard.backlog.search.placeholder")
                        }
                        value={rightSearch}
                        onChange={(e) => setRightSearch(e.target.value)}
                      />
                    </div>

                    <div className="split-panel-table-wrap">
                      {rightPanelView === "ACTIVE" ? (
                        <>
                          <DataTable
                            rows={filteredRightData.slice(
                              (rightPage - 1) * rightPageSize,
                              rightPage * rightPageSize,
                            )}
                            headers={
                              usesInProgressView(selectedTile.type) ||
                                selectedTile.type ===
                                "ORDERS_READY_FOR_VALIDATION"
                                ? groupedOrderHeaders
                                : selectedTile.type !==
                                  "ORDERS_ENTERED_BY_USER_TODAY"
                                  ? orderHeaders
                                  : userHeaders
                            }
                            isSortable
                          >
                            {({
                              rows,
                              headers,
                              getHeaderProps,
                              getTableProps,
                            }) => (
                              <TableContainer title="" description="">
                                <Table {...getTableProps()}>
                                  <TableHead>
                                    <TableRow>
                                      {headers.map((h) => (
                                        <TableHeader
                                          key={h.key}
                                          {...getHeaderProps({ header: h })}
                                        >
                                          {h.header}
                                        </TableHeader>
                                      ))}
                                    </TableRow>
                                  </TableHead>
                                  <TableBody>
                                    {rows.map((row) => (
                                      <TableRow
                                        key={row.id}
                                        onClick={() => {
                                          if (
                                            selectedTile.type ===
                                            "ORDERS_ENTERED_BY_USER_TODAY"
                                          )
                                            viewUserOrders(row);
                                        }}
                                      >
                                        {headers.map((h) => {
                                          const cell = row.cells.find(
                                            (c) => c.info.header === h.key,
                                          );
                                          return cell ? (
                                            renderCell(cell, row)
                                          ) : (
                                            <TableCell
                                              key={`${row.id}-${h.key}`}
                                            />
                                          );
                                        })}
                                      </TableRow>
                                    ))}
                                  </TableBody>
                                </Table>
                              </TableContainer>
                            )}
                          </DataTable>
                          <Pagination
                            onChange={({ page: p, pageSize: ps }) => {
                              setRightPage(p);
                              setRightPageSize(ps);
                            }}
                            page={rightPage}
                            pageSize={rightPageSize}
                            pageSizes={[10, 20, 50, 100]}
                            totalItems={filteredRightData.length}
                            forwardText={intl.formatMessage({
                              id: "pagination.forward",
                            })}
                            backwardText={intl.formatMessage({
                              id: "pagination.backward",
                            })}
                            itemRangeText={(min, max, total) =>
                              intl.formatMessage(
                                { id: "pagination.item-range" },
                                { min, max, total },
                              )
                            }
                            itemsPerPageText={intl.formatMessage({
                              id: "pagination.items-per-page",
                            })}
                            itemText={(min, max) =>
                              intl.formatMessage(
                                { id: "pagination.item" },
                                { min, max },
                              )
                            }
                            pageNumberText={intl.formatMessage({
                              id: "pagination.page-number",
                            })}
                            pageRangeText={(_c, total) =>
                              intl.formatMessage(
                                { id: "pagination.page-range" },
                                { total },
                              )
                            }
                            pageText={(p, unk) =>
                              intl.formatMessage(
                                { id: "pagination.page" },
                                { page: unk ? "" : p },
                              )
                            }
                          />
                        </>
                      ) : (
                        <>
                          <DataTable
                            rows={filteredBacklogData.slice(
                              (rightPage - 1) * rightPageSize,
                              rightPage * rightPageSize,
                            )}
                            headers={groupedOrderHeaders}
                            isSortable
                          >
                            {({
                              rows,
                              headers,
                              getHeaderProps,
                              getTableProps,
                            }) => (
                              <TableContainer
                                title=""
                                description={message(
                                  "dashboard.backlog.description",
                                )}
                              >
                                <Table {...getTableProps()}>
                                  <TableHead>
                                    <TableRow>
                                      {headers.map((h) => (
                                        <TableHeader
                                          key={h.key}
                                          {...getHeaderProps({ header: h })}
                                        >
                                          {h.header}
                                        </TableHeader>
                                      ))}
                                    </TableRow>
                                  </TableHead>
                                  <TableBody>
                                    {rows.length === 0 ? (
                                      <TableRow>
                                        <TableCell
                                          colSpan={groupedOrderHeaders.length}
                                        >
                                          <p className="split-empty-msg">
                                            {message(
                                              "dashboard.empty.ordersBacklog",
                                            )}
                                          </p>
                                        </TableCell>
                                      </TableRow>
                                    ) : (
                                      rows.map((row) => (
                                        <TableRow key={row.id}>
                                          {headers.map((h) => {
                                            const cell = row.cells.find(
                                              (c) => c.info.header === h.key,
                                            );
                                            return cell ? (
                                              renderCell(cell, row)
                                            ) : (
                                              <TableCell
                                                key={`${row.id}-${h.key}`}
                                              />
                                            );
                                          })}
                                        </TableRow>
                                      ))
                                    )}
                                  </TableBody>
                                </Table>
                              </TableContainer>
                            )}
                          </DataTable>
                          <Pagination
                            onChange={({ page: p, pageSize: ps }) => {
                              setRightPage(p);
                              setRightPageSize(ps);
                            }}
                            page={rightPage}
                            pageSize={rightPageSize}
                            pageSizes={[10, 20, 50, 100]}
                            totalItems={filteredBacklogData.length}
                            forwardText={intl.formatMessage({
                              id: "pagination.forward",
                            })}
                            backwardText={intl.formatMessage({
                              id: "pagination.backward",
                            })}
                            itemRangeText={(min, max, total) =>
                              intl.formatMessage(
                                { id: "pagination.item-range" },
                                { min, max, total },
                              )
                            }
                            itemsPerPageText={intl.formatMessage({
                              id: "pagination.items-per-page",
                            })}
                            itemText={(min, max) =>
                              intl.formatMessage(
                                { id: "pagination.item" },
                                { min, max },
                              )
                            }
                            pageNumberText={intl.formatMessage({
                              id: "pagination.page-number",
                            })}
                            pageRangeText={(_c, total) =>
                              intl.formatMessage(
                                { id: "pagination.page-range" },
                                { total },
                              )
                            }
                            pageText={(p, unk) =>
                              intl.formatMessage(
                                { id: "pagination.page" },
                                { page: unk ? "" : p },
                              )
                            }
                          />
                        </>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      ) : (
        /* ── STANDARD SINGLE-PANEL DETAIL VIEW ── */
        <div className="dashboard-view">
          <Tile className="dashboard-tile">
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <h3 className="tile-title-view">{selectedTile.title}</h3>
                <p className="tile-subtitle-view">{selectedTile.subTitle}</p>
                <p className="tile-value-view">{selectedTile.value}</p>
                <div className="tile-icon">
                  <div onClick={handleMinimizeClick} className="icon-wrapper">
                    <Minimize size={20} className="clickable-icon" />
                  </div>
                </div>
              </Column>
            </Grid>
            <div className="gridBoundary">
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  {pagination && (
                    <Grid>
                      <Column lg={14} />
                      <Column
                        lg={2}
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          alignItems: "center",
                          gap: "10px",
                        }}
                      >
                        <Link>
                          {currentApiPage} / {totalApiPages}
                        </Link>
                        <div style={{ display: "flex", gap: "10px" }}>
                          <Button
                            hasIconOnly
                            onClick={loadPreviousResultsPage}
                            disabled={!previousPage}
                            renderIcon={ArrowLeft}
                            iconDescription="previous"
                          />
                          <Button
                            hasIconOnly
                            onClick={loadNextResultsPage}
                            disabled={!nextPage}
                            renderIcon={ArrowRight}
                            iconDescription="next"
                          />
                        </div>
                      </Column>
                    </Grid>
                  )}
                  {tilesWithTabs.includes(selectedTile.type) && (
                    <Grid>
                      <Column lg={16} md={8} sm={4}>
                        <Tabs>
                          {hasRole(
                            userSessionDetails,
                            "Global Administrator",
                          ) ? (
                            <TabList
                              style={{ width: "100%" }}
                              aria-label="List of tabs"
                              contained
                            >
                              <Tab
                                onClick={() => setSelectedTestSection("all")}
                              >
                                <FormattedMessage id="all.label" />
                              </Tab>
                              {testSections?.map((item, id) => (
                                <Tab
                                  key={id}
                                  onClick={() =>
                                    setSelectedTestSection(item.id)
                                  }
                                >
                                  {item.value}
                                </Tab>
                              ))}
                            </TabList>
                          ) : (
                            <TabList
                              style={{ width: "100%" }}
                              aria-label="List of tabs"
                              contained
                            >
                              {testSections?.map((item, id) => (
                                <Tab
                                  key={id}
                                  onClick={() =>
                                    setSelectedTestSection(item.id)
                                  }
                                >
                                  {item.value}
                                </Tab>
                              ))}
                            </TabList>
                          )}
                        </Tabs>
                      </Column>
                    </Grid>
                  )}
                  <Grid className="dashboard-table-toolbar">
                    <Column lg={6} md={4} sm={4}>
                      <TextInput
                        id="std-order-search"
                        labelText={intl.formatMessage({
                          id: "dashboard.orders.search.label",
                        })}
                        placeholder={intl.formatMessage({
                          id: "dashboard.orders.search.placeholder",
                        })}
                        value={rightSearch}
                        onChange={(e) => setRightSearch(e.target.value)}
                      />
                    </Column>
                  </Grid>
                  <DataTable
                    rows={filteredRightData.slice(
                      (rightPage - 1) * rightPageSize,
                      rightPage * rightPageSize,
                    )}
                    headers={
                      selectedTile.type === "ORDERS_ENTERED_BY_USER_TODAY"
                        ? userHeaders
                        : usesInProgressView(selectedTile.type) ||
                          selectedTile.type === "ORDERS_READY_FOR_VALIDATION"
                          ? groupedOrderHeaders
                          : orderHeaders
                    }
                    isSortable
                  >
                    {({ rows, headers, getHeaderProps, getTableProps }) => (
                      <TableContainer title="" description="">
                        <Table {...getTableProps()}>
                          <TableHead>
                            <TableRow>
                              {headers.map((h) => (
                                <TableHeader
                                  key={h.key}
                                  {...getHeaderProps({ header: h })}
                                >
                                  {h.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row) => (
                              <TableRow
                                key={row.id}
                                onClick={() => {
                                  if (
                                    selectedTile.type ===
                                    "ORDERS_ENTERED_BY_USER_TODAY"
                                  )
                                    viewUserOrders(row);
                                }}
                              >
                                {headers.map((h) => {
                                  const cell = row.cells.find(
                                    (c) => c.info.header === h.key,
                                  );
                                  return cell ? (
                                    renderCell(cell, row)
                                  ) : (
                                    <TableCell key={`${row.id}-${h.key}`} />
                                  );
                                })}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                  <Pagination
                    onChange={({ page: p, pageSize: ps }) => {
                      setRightPage(p);
                      setRightPageSize(ps);
                    }}
                    page={rightPage}
                    pageSize={rightPageSize}
                    pageSizes={[10, 20, 30, 50, 100]}
                    totalItems={filteredRightData.length}
                    forwardText={intl.formatMessage({
                      id: "pagination.forward",
                    })}
                    backwardText={intl.formatMessage({
                      id: "pagination.backward",
                    })}
                    itemRangeText={(min, max, total) =>
                      intl.formatMessage(
                        { id: "pagination.item-range" },
                        { min, max, total },
                      )
                    }
                    itemsPerPageText={intl.formatMessage({
                      id: "pagination.items-per-page",
                    })}
                    itemText={(min, max) =>
                      intl.formatMessage(
                        { id: "pagination.item" },
                        { min, max },
                      )
                    }
                    pageNumberText={intl.formatMessage({
                      id: "pagination.page-number",
                    })}
                    pageRangeText={(_c, total) =>
                      intl.formatMessage(
                        { id: "pagination.page-range" },
                        { total },
                      )
                    }
                    pageText={(p, unk) =>
                      intl.formatMessage(
                        { id: "pagination.page" },
                        { page: unk ? "" : p },
                      )
                    }
                  />
                </Column>
              </Grid>
            </div>
          </Tile>
        </div>
      )}
    </>
  );
};

export default HomeDashBoard;
