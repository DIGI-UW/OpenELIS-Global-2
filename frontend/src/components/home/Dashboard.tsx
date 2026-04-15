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
  Tag,
} from "@carbon/react";
import "./Dashboard.css";
import { Minimize, Maximize, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { Copy } from "@carbon/icons-react";
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

interface DashBoardProps {}

interface Tile {
  title: string | JSX.Element;
  subTitle: string | JSX.Element;
  type: MetricType;
  value: number;
  id?: number;
}

interface BacklogOrder {
  orderId: string;
  labNumber: string;
  patientName: string;
  patientId: string;
  movedToBacklogDate: string;
  orderDate: string;
  lastActivityDate: string;
  priority?: string;
  pendingResultCount?: number;
  pendingValidationCount?: number;
  testCount?: number;
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
  const [backlogOrders, setBacklogOrders] = useState<BacklogOrder[]>([]);
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
  const [dashboardTab, setDashboardTab] = useState<"LEFT" | "RIGHT">("LEFT");

  const componentMounted = useRef(true);
  const tileLoadSequence = useRef(0);
  const patientNameCache = useRef<Record<string, string>>({});

  const { userSessionDetails } = useContext(
    UserSessionDetailsContext,
  ) as UserSessionDetails;
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as Notification;

  const isSplitLayout = (type?: MetricType | null) =>
    type === "ON_GOING_ORDERS" || type === "ORDERS_IN_PROGRESS";

  // ── BACKLOG HELPERS ──────────────────────────────────────────────────────────
  const loadBacklogOrders = useCallback(() => {
    const saved = localStorage.getItem("lab_backlog_orders");
    if (saved) {
      try {
        setBacklogOrders(JSON.parse(saved));
      } catch (e) {
        console.error("Failed to load backlog orders", e);
      }
    }
  }, []);

  const saveBacklogOrders = useCallback((orders: BacklogOrder[]) => {
    localStorage.setItem("lab_backlog_orders", JSON.stringify(orders));
    setBacklogOrders(orders);
  }, []);

  const isOrderInactive = useCallback((order: any) => {
    const lastActivity = order.lastActivityDate || order.orderDate;
    if (!lastActivity) return false;
    const secondsDiff =
      (new Date().getTime() - new Date(lastActivity).getTime()) / 1000;
    return secondsDiff >= 86400;
  }, []);

  const autoMoveToBacklog = useCallback(() => {
    if (data.length === 0) return;
    const currentBacklogIds = new Set(backlogOrders.map((bo) => bo.orderId));
    const ordersToMove = data.filter((order) => {
      const orderDateOk = order.orderDate
        ? (new Date().getTime() - new Date(order.orderDate).getTime()) / 1000 >=
          86400
        : true;
      return (
        isOrderInactive(order) &&
        orderDateOk &&
        !currentBacklogIds.has(order.id) &&
        order.status !== "COMPLETED" &&
        order.status !== "REJECTED"
      );
    });
    if (ordersToMove.length > 0) {
      const newBacklogOrders = ordersToMove.map((order) => ({
        ...order,
        orderId: order.id,
        labNumber: order.labNumber,
        patientName: order.patientName || "",
        patientId: order.patientId || "",
        movedToBacklogDate: new Date().toISOString(),
        orderDate: order.orderDate,
        lastActivityDate: order.lastActivityDate || order.orderDate,
        priority: order.priority || order.source || "",
        pendingResultCount: order.pendingResultCount ?? 0,
        pendingValidationCount: order.pendingValidationCount ?? 0,
        testCount: order.testCount ?? 0,
      }));
      saveBacklogOrders([...backlogOrders, ...newBacklogOrders]);
      addNotification?.({
        kind: NotificationKinds.info,
        title: "Orders Moved to Backlog",
        message: `${newBacklogOrders.length} order(s) moved due to 24 h inactivity.`,
      });
    }
  }, [
    data,
    backlogOrders,
    isOrderInactive,
    saveBacklogOrders,
    addNotification,
  ]);

  const removeFromBacklog = useCallback(
    (orderId: string) => {
      saveBacklogOrders(backlogOrders.filter((bo) => bo.orderId !== orderId));
      addNotification?.({
        kind: NotificationKinds.success,
        title: "Order Returned to Queue",
        message: "Order removed from backlog.",
      });
    },
    [backlogOrders, saveBacklogOrders, addNotification],
  );

  useEffect(() => {
    loadBacklogOrders();
  }, [loadBacklogOrders]);
  useEffect(() => {
    if (data.length > 0) autoMoveToBacklog();
  }, [data, autoMoveToBacklog]);
  useEffect(() => {
    const interval = setInterval(
      () => {
        if (data.length > 0) autoMoveToBacklog();
      },
      60 * 60 * 1000,
    );
    return () => clearInterval(interval);
  }, [data, autoMoveToBacklog]);

  // Enrich backlog patient names if missing
  useEffect(() => {
    if (!backlogOrders.length) return;
    let updated = false;
    const newBacklog = backlogOrders.map((backlogItem) => {
      const matchingOrder = data.find(
        (order) => order.id === backlogItem.orderId,
      );
      if (matchingOrder?.patientName && !backlogItem.patientName) {
        updated = true;
        return { ...backlogItem, patientName: matchingOrder.patientName };
      }
      return backlogItem;
    });
    if (updated) saveBacklogOrders(newBacklog);
  }, [data, backlogOrders, saveBacklogOrders]);

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

  useEffect(() => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
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
    getFromOpenElisServer("/rest/home-dashboard/metrics", loadCount);
    getFromOpenElisServer("/rest/incoming-orders", (res) => {
      if (!componentMounted.current) return;
      const list = Array.isArray(res) ? res : [];
      setIncomingOrdersData(list);
      setCounts((prev) => ({ ...prev, samplesToCollect: list.length }));
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

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
    return () => {
      componentMounted.current = false;
    };
  }, [selectedTile]);

  useEffect(() => {
    getFromOpenElisServer("/rest/user-test-sections/ALL", fetchTestSections);
    return () => {
      componentMounted.current = false;
    };
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

  const hasPendingValidationField = (items = []) =>
    items.some((i) =>
      Object.prototype.hasOwnProperty.call(i ?? {}, "pendingValidationCount"),
    );

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
    );
    if (missingKeys.length === 0) return;
    await Promise.all(
      displayItems
        .filter(
          (item) =>
            !String(item?.patientName ?? "").trim() &&
            missingKeys.includes(getPatientLookupKey(item)),
        )
        .map(async (item) => {
          const key = getPatientLookupKey(item);
          if (!key) return;
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
      if (Number.isFinite(prc) || Number.isFinite(pvc)) {
        const pendingResultCount = Number.isFinite(prc) ? prc : 0;
        const pendingValidationCount = Number.isFinite(pvc) ? pvc : 0;
        const testCount =
          pendingResultCount + pendingValidationCount > 0
            ? pendingResultCount + pendingValidationCount
            : Number.isFinite(tc)
              ? tc
              : 0;
        return {
          ...item,
          pendingResultCount,
          pendingValidationCount,
          testCount,
        };
      }
      if (tileType === "ORDERS_READY_FOR_VALIDATION") {
        return {
          ...item,
          pendingResultCount: 0,
          pendingValidationCount: Number.isFinite(tc) ? tc : 0,
          testCount: Number.isFinite(tc) ? tc : 0,
        };
      }
      if (usesInProgressView(tileType)) {
        return {
          ...item,
          pendingResultCount: Number.isFinite(tc) ? tc : 0,
          pendingValidationCount: 0,
          testCount: Number.isFinite(tc) ? tc : 0,
        };
      }
      return item;
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
          testCount:
            (Number(item.pendingResultCount) || 0) +
            (Number(item.pendingValidationCount) || 0),
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
        testCount: prc + pvc,
      });
    });
    return Array.from(merged.values());
  };

  const fetchAllGroupedPages = async (endpoint: string) => {
    const first = await getFromOpenElisServerV2(endpoint);
    const all = Array.isArray(first?.displayItems)
      ? [...first.displayItems]
      : [];
    const totalPages = Number(first?.paging?.totalPages);
    const sep = endpoint.includes("?") ? "&" : "?";
    if (Number.isFinite(totalPages) && totalPages > 1) {
      for (let p = 2; p <= totalPages; p++) {
        const pg = await getFromOpenElisServerV2(`${endpoint}${sep}page=${p}`);
        if (Array.isArray(pg?.displayItems)) all.push(...pg.displayItems);
      }
    }
    return { ...first, displayItems: all };
  };

  const loadOngoingOrdersData = async (seq: number) => {
    try {
      const orders = await fetchAllGroupedPages(
        "/rest/home-dashboard/ORDERS-Grouped",
      );
      if (hasPendingValidationField(orders?.displayItems)) {
        loadData(orders, true, seq);
        return;
      }
      const validation = await fetchAllGroupedPages(
        "/rest/home-dashboard/VALIDATION-Grouped",
      );
      loadData(
        {
          displayItems: mergeGroupedDisplayItems(
            orders?.displayItems,
            validation?.displayItems,
          ),
        },
        true,
        seq,
      );
    } catch {
      loadData({ displayItems: [] }, true, seq);
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
      title: "Reception To Validation Average Time",
      subTitle: "Reception To Validation Average Time",
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToValidation,
    },
    {
      title: "Reception To Result Average Time",
      subTitle: "Reception To Result Average Time",
      type: "AVERAGE_TURN_AROUND_TIME",
      value: timeMetrics.receptionToResult,
    },
    {
      title: "Result To Validation Average Time",
      subTitle: "Result To Validation Average Time",
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
  const backlogIds = useMemo(
    () => new Set(backlogOrders.map((bo) => bo.orderId)),
    [backlogOrders],
  );

  const sectionFilteredData = useMemo(() => {
    return data.filter(
      (item) =>
        !backlogIds.has(item.id) &&
        (tilesWithTabs.includes(selectedTile?.type) &&
        selectedTestSection !== "all"
          ? item.testSection === selectedTestSection
          : true),
    );
  }, [data, selectedTile?.type, selectedTestSection, backlogIds]);

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
      backlogOrders.map((bo) => ({
        id: bo.orderId,
        priority: bo.priority || "",
        orderDate: bo.orderDate || "",
        patientId: bo.patientId,
        patientName: bo.patientName,
        labNumber: bo.labNumber,
        movedToBacklogDate: new Date(bo.movedToBacklogDate).toLocaleString(),
        pendingResultCount: (bo as any).pendingResultCount ?? 0,
        pendingValidationCount: (bo as any).pendingValidationCount ?? 0,
        testCount: (bo as any).testCount ?? 0,
      })),
    [backlogOrders],
  );

  const filteredBacklogData = useMemo(() => {
    const q = rightSearch.trim().toLowerCase();
    if (!q) return backlogTableData;
    return backlogTableData.filter(
      (item) =>
        item.labNumber.toLowerCase().includes(q) ||
        item.patientName.toLowerCase().includes(q) ||
        item.patientId.toLowerCase().includes(q),
    );
  }, [backlogTableData, rightSearch]);

  const filteredLeftData = useMemo(() => {
    const q = leftSearch.trim().toLowerCase();
    if (!q) return incomingOrdersData;
    return incomingOrdersData.filter(
      (item) =>
        String(item.patientName ?? "")
          .toLowerCase()
          .includes(q) ||
        String(item.source ?? "")
          .toLowerCase()
          .includes(q) ||
        String(item.labNumber ?? "")
          .toLowerCase()
          .includes(q),
    );
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
        const prc = Number(i.pendingResultCount) || 0;
        const pvc = Number(i.pendingValidationCount) || 0;
        return t + (Number(i.testCount) || prc + pvc || 1);
      }, 0),
    [sectionFilteredData],
  );

  const leftBacklogOrderCount = useMemo(
    () => backlogOrders.length,
    [backlogOrders],
  );
  const leftBacklogPatientCount = useMemo(
    () => new Set(backlogOrders.map((bo) => bo.patientId).filter(Boolean)).size,
    [backlogOrders],
  );
  const leftBacklogSummaryCards = [
    { label: "Backlog Orders", value: leftBacklogOrderCount, color: "#5f7fa3" },
    {
      label: "Patients in Backlog",
      value: leftBacklogPatientCount,
      color: "#6c8b74",
    },
    {
      label: "Total Backlog Items",
      value: leftBacklogOrderCount,
      color: "#7f7b96",
    },
  ];

  const summaryCards = [
    {
      label: intl.formatMessage({ id: "dashboard.in.progress.subtitle.label" }),
      value: workflowPendingResultCount,
      color: "#5f7fa3",
    },
    {
      label: intl.formatMessage({
        id: "dashboard.workflow.awaitingValidation",
      }),
      value: workflowPendingValidationCount,
      color: "#5b8a8b",
    },
    {
      label: intl.formatMessage({ id: "dashboard.table.total" }),
      value: workflowTestCount,
      color: "#6d88a8",
    },
    {
      label: intl.formatMessage({ id: "dashboard.workflow.totalOrders" }),
      value: workflowOrderCount,
      color: "#7f7b96",
    },
    {
      label: intl.formatMessage({ id: "dashboard.workflow.totalPatients" }),
      value: workflowPatientCount,
      color: "#6c8b74",
    },
    {
      label: intl.formatMessage({ id: "dashboard.complete.orders.label" }),
      value: counts.ordersCompletedToday ?? 0,
      color: "#9a8366",
    },
  ];

  // ── TABLE HEADERS ─────────────────────────────────────────────────────────────
  const groupedOrderHeaders = [
    { key: "priority", header: "Priority" },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientName",
      header: <FormattedMessage id="incomingOrders.table.patientName" />,
    },
    { key: "actions", header: "Actions" },
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
    { key: "priority", header: "Priority" },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientName",
      header: <FormattedMessage id="incomingOrders.table.patientName" />,
    },
    { key: "actions", header: "Actions" },
    { key: "labNumber", header: <FormattedMessage id="eorder.labNumber" /> },
    { key: "testName", header: <FormattedMessage id="eorder.test.name" /> },
  ];

  const incomingOrderHeaders = [
    { key: "patientName", header: "Patient Name" },
    { key: "received", header: "Received" },
    { key: "tests", header: "Tests" },
    { key: "source", header: "Source" },
    { key: "actions", header: "Actions" },
  ];

  const userHeaders = [
    { key: "userFirstName", header: "First Name" },
    { key: "userLastName", header: "Last Name" },
    { key: "countOfOrdersEntered", header: "Orders Entered" },
  ];

  // ── CELL RENDERER ─────────────────────────────────────────────────────────────
  const renderCell = (cell, row) => {
    const rowPatientName =
      data.find((item) => String(item.id) === String(row.id))?.patientName ||
      "";
    const isInBacklog = backlogIds.has(row.id);

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
            {isInBacklog && (
              <Tag type="red" size="sm" style={{ marginLeft: "0.5rem" }}>
                Backlog
              </Tag>
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
        "&type=order&quantity=1";
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
            >
              <img
                src={resultIcon}
                alt="Results"
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
            <a
              href={validationUrl}
              title="Validate"
              target="_blank"
              rel="noreferrer"
              style={{ display: "inline-flex", alignItems: "center" }}
            >
              <img
                src={validateIcon}
                alt="Validate"
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
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  // ── PANEL TOGGLE COMPONENT ────────────────────────────────────────────────────
  const PanelToggle = ({
    view,
    setView,
    activeCount,
    backlogCount,
  }: {
    view: PanelView;
    setView: (v: PanelView) => void;
    activeCount: number;
    backlogCount?: number;
  }) => (
    <div className="dashboard-table-toggle" role="group">
      <button
        type="button"
        className={`dashboard-table-toggle-btn${view === "ACTIVE" ? " dashboard-table-toggle-btn--active" : ""}`}
        onClick={() => setView("ACTIVE")}
      >
        Active
        {view === "ACTIVE" && (
          <span className="dashboard-table-toggle-count">{activeCount}</span>
        )}
      </button>
      <button
        type="button"
        className={`dashboard-table-toggle-btn${view === "BACKLOG" ? " dashboard-table-toggle-btn--active" : ""}`}
        onClick={() => setView("BACKLOG")}
      >
        Backlog
        {(backlogCount ?? 0) > 0 && view !== "BACKLOG" ? (
          <span className="dashboard-table-toggle-count--badge dashboard-table-toggle-count">
            {backlogCount}
          </span>
        ) : view === "BACKLOG" ? (
          <span className="dashboard-table-toggle-count">
            {backlogCount ?? 0}
          </span>
        ) : null}
      </button>
    </div>
  );

  // ── RENDER ───────────────────────────────────────────────────────────────────
  return (
    <>
      {loading && <Loading description="Loading Dashboard..." />}
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
          <div
            className="split-dashboard-header"
            style={{ backgroundColor: "#295785" }}
          >
            <div className="split-dashboard-header__left">
              <div className="split-dashboard-title-group">
                <h2 className="split-dashboard-title">{selectedTile.title}</h2>
                <p className="split-dashboard-subtitle">
                  {selectedTile.subTitle}
                </p>
              </div>
            </div>
            <div className="split-summary-strip">
              {summaryCards.map((c) => (
                <div key={c.label} className="split-summary-pill">
                  <span
                    className="split-summary-dot"
                    style={{ background: c.color }}
                  />
                  <span className="split-summary-label">{c.label}</span>
                  <strong className="split-summary-value">{c.value}</strong>
                </div>
              ))}
            </div>
          </div>

          <div className="tabbed-dashboard-body" style={{ marginTop: "1rem" }}>
            <div
              style={{
                padding: "1rem 0",
                marginBottom: "1rem",
                borderBottom: "1px solid #e0e0e0",
                display: "flex",
                gap: "6rem",
                alignItems: "flex-end",
              }}
            >
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "0.75rem",
                }}
              >
                <h3
                  style={{
                    margin: 0,
                    fontSize: "1.2rem",
                    fontWeight: 700,
                    color: "#161616",
                  }}
                >
                  Tests
                </h3>
                <div
                  className="dashboard-table-toggle"
                  role="group"
                  style={{ display: "inline-flex" }}
                >
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "LEFT" && leftPanelView === "ACTIVE" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("LEFT");
                      setLeftPanelView("ACTIVE");
                    }}
                    style={{ padding: "8px 24px", fontSize: "0.95rem" }}
                  >
                    Today
                  </button>
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "LEFT" && leftPanelView === "BACKLOG" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("LEFT");
                      setLeftPanelView("BACKLOG");
                    }}
                    style={{ padding: "8px 24px", fontSize: "0.95rem" }}
                  >
                    Backlog
                  </button>
                </div>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "0.75rem",
                }}
              >
                <h3
                  style={{
                    margin: 0,
                    fontSize: "1.2rem",
                    fontWeight: 700,
                    color: "#161616",
                  }}
                >
                  Samples Collected
                </h3>
                <div
                  className="dashboard-table-toggle"
                  role="group"
                  style={{ display: "inline-flex" }}
                >
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "RIGHT" && rightPanelView === "ACTIVE" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("RIGHT");
                      setRightPanelView("ACTIVE");
                    }}
                    style={{ padding: "8px 24px", fontSize: "0.95rem" }}
                  >
                    Today
                  </button>
                  <button
                    type="button"
                    className={`dashboard-table-toggle-btn${dashboardTab === "RIGHT" && rightPanelView === "BACKLOG" ? " dashboard-table-toggle-btn--active" : ""}`}
                    onClick={() => {
                      setDashboardTab("RIGHT");
                      setRightPanelView("BACKLOG");
                    }}
                    style={{ padding: "8px 24px", fontSize: "0.95rem" }}
                  >
                    Backlog
                  </button>
                </div>
              </div>
            </div>

            <div className="tab-panel-content">
              {/* LEFT PANEL — Tests / Incoming Orders */}
              {dashboardTab === "LEFT" && (
                <div className="split-panel split-panel--left">
                  <div className="split-panel-inner">
                    <div className="split-panel-search">
                      <TextInput
                        id="left-panel-search"
                        labelText=""
                        placeholder={
                          leftPanelView === "ACTIVE"
                            ? "Search by patient name or source…"
                            : "Search backlog…"
                        }
                        value={leftSearch}
                        onChange={(e) => setLeftSearch(e.target.value)}
                      />
                    </div>
                    <div className="split-panel-table-wrap">
                      {leftPanelView === "ACTIVE" ? (
                        <>
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
                                    {rows.length === 0 ? (
                                      <TableRow>
                                        <TableCell
                                          colSpan={incomingOrderHeaders.length}
                                        >
                                          <p className="split-empty-msg">
                                            No samples awaiting collection.
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
                                            return (
                                              <TableCell
                                                key={
                                                  cell?.id ||
                                                  `${row.id}-${h.key}`
                                                }
                                              >
                                                {cell?.value ?? "—"}
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
                        <>
                          <DataTable
                            rows={backlogOrders
                              .filter((bo) => {
                                const q = leftSearch.trim().toLowerCase();
                                if (!q) return true;
                                return (
                                  bo.labNumber.toLowerCase().includes(q) ||
                                  bo.patientName.toLowerCase().includes(q) ||
                                  bo.patientId.toLowerCase().includes(q)
                                );
                              })
                              .slice(
                                (leftPage - 1) * leftPageSize,
                                leftPage * leftPageSize,
                              )
                              .map((bo) => ({
                                ...bo,
                                id: bo.orderId,
                                movedToBacklogDate: new Date(
                                  bo.movedToBacklogDate,
                                ).toLocaleString(),
                              }))}
                            headers={incomingOrderHeaders}
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
                                description="Orders inactive 24+ hours"
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
                                          colSpan={incomingOrderHeaders.length}
                                        >
                                          <p className="split-empty-msg">
                                            No orders in backlog.
                                          </p>
                                        </TableCell>
                                      </TableRow>
                                    ) : (
                                      rows.map((row) => (
                                        <TableRow key={row.id}>
                                          {headers.map((h) => {
                                            if (h.key === "actions") {
                                              const bo = backlogOrders.find(
                                                (b) => b.orderId === row.id,
                                              );
                                              const accNum = bo?.labNumber;
                                              if (!accNum)
                                                return (
                                                  <TableCell
                                                    key={`${row.id}-actions`}
                                                  />
                                                );
                                              const resultUrl = getFullPath(
                                                "/result?type=order&doRange=false&accessionNumber=" +
                                                  accNum,
                                              );
                                              const validationUrl = getFullPath(
                                                "/validation?type=order&accessionNumber=" +
                                                  accNum,
                                              );
                                              const reportUrl =
                                                config.serverBaseUrl +
                                                "/ReportPrint?report=patientCILNSP_vreduit&type=patient&accessionDirect=" +
                                                accNum +
                                                "&highAccessionDirect=" +
                                                accNum +
                                                "&dateOfBirthSearchValue=&selPatient=&referringSiteId=&referringSiteDepartmentId=&onlyResults=false&_onlyResults=on&dateType=RESULT_DATE&lowerDateRange=&upperDateRange=";
                                              const barcodeUrl =
                                                config.serverBaseUrl +
                                                "/LabelMakerServlet?labNo=" +
                                                accNum +
                                                "&type=order&quantity=1";
                                              return (
                                                <TableCell
                                                  key={`${row.id}-actions`}
                                                >
                                                  <div
                                                    style={{
                                                      display: "flex",
                                                      gap: "0.75rem",
                                                      alignItems: "center",
                                                    }}
                                                  >
                                                    <a
                                                      href={barcodeUrl}
                                                      title="Print Barcode"
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      style={{
                                                        display: "inline-flex",
                                                        alignItems: "center",
                                                      }}
                                                    >
                                                      <img
                                                        src={barcodeIcon}
                                                        alt="Print Barcode"
                                                        style={{
                                                          width: "1.1rem",
                                                          height: "1.1rem",
                                                        }}
                                                      />
                                                    </a>
                                                    <a
                                                      href={resultUrl}
                                                      title="Results"
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      style={{
                                                        display: "inline-flex",
                                                        alignItems: "center",
                                                      }}
                                                    >
                                                      <img
                                                        src={resultIcon}
                                                        alt="Results"
                                                        style={{
                                                          width: "1.1rem",
                                                          height: "1.1rem",
                                                        }}
                                                      />
                                                    </a>
                                                    <a
                                                      href={reportUrl}
                                                      title="Report"
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      style={{
                                                        display: "inline-flex",
                                                        alignItems: "center",
                                                      }}
                                                    >
                                                      <img
                                                        src={reportIcon}
                                                        alt="Report"
                                                        style={{
                                                          width: "1.1rem",
                                                          height: "1.1rem",
                                                        }}
                                                      />
                                                    </a>
                                                    <a
                                                      href={validationUrl}
                                                      title="Validate"
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      style={{
                                                        display: "inline-flex",
                                                        alignItems: "center",
                                                      }}
                                                    >
                                                      <img
                                                        src={validateIcon}
                                                        alt="Validate"
                                                        style={{
                                                          width: "1.1rem",
                                                          height: "1.1rem",
                                                        }}
                                                      />
                                                    </a>
                                                  </div>
                                                </TableCell>
                                              );
                                            }
                                            const cell = row.cells.find(
                                              (c) => c.info.header === h.key,
                                            );
                                            return (
                                              <TableCell
                                                key={
                                                  cell?.id ||
                                                  `${row.id}-${h.key}`
                                                }
                                              >
                                                {cell?.value ?? "—"}
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
                            totalItems={backlogOrders.length}
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

              {/* RIGHT PANEL — Active Orders / Backlog */}
              {dashboardTab === "RIGHT" && (
                <div className="split-panel split-panel--right">
                  <div className="split-panel-inner">
                    {rightPanelView === "ACTIVE" &&
                      tilesWithTabs.includes(selectedTile.type) && (
                        <div style={{ marginBottom: "0.75rem" }}>
                          <Tabs>
                            {hasRole(
                              userSessionDetails,
                              "Global Administrator",
                            ) ? (
                              <TabList
                                style={{ width: "100%" }}
                                aria-label="Department tabs"
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
                                aria-label="Department tabs"
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
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-end",
                          alignItems: "center",
                          gap: "0.5rem",
                          marginBottom: "0.5rem",
                        }}
                      >
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
                            ? intl.formatMessage({
                                id: "dashboard.orders.search.placeholder",
                              })
                            : "Search backlog by lab number, patient…"
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
                                description="Orders moved here after 24 h inactivity"
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
                                            No orders in backlog.
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
