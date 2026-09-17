import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  Table,
  TableContainer,
  TableHead,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Search,
  Select,
  SelectItem,
  Tag,
  Loading,
  InlineNotification,
  Button,
  OverflowMenu,
  OverflowMenuItem,
  ActionableNotification,
} from "@carbon/react";
import { ArrowUp, ArrowDown, Subtract } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  InventoryBoardAPI,
  InventoryItemAPI,
  InventoryLotAPI,
} from "./InventoryService";
import LotDetailsPanel from "./LotDetailsPanel";
import LotEntryModal from "./LotEntryModal";
import LotAdjustmentModal from "./LotAdjustmentModal";
import DisposeLotModal from "./DisposeLotModal";
import UpdateQCStatusModal from "./UpdateQCStatusModal";
import InventoryItemForm from "./InventoryItemForm";
import QuickLogUsageModal from "./QuickLogUsageModal";
import ReorderSuggestionsModal, {
  isSuggested,
} from "./ReorderSuggestionsModal";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import "./InventoryItemsBoard.css";

/** Trailing window the projection is computed over; matches WINDOW_DAYS server side. */
const USAGE_WINDOW_DAYS = 30;

/**
 * Below this the trend reads as steady. A percentage that small says more about
 * the granularity of the window halves than about a real change in consumption.
 */
const STEADY_TREND_PERCENT = 10;

const STATUS_TAGS = {
  REORDER_NOW: { type: "red", label: "inventory.reorderStatus.now" },
  REORDER_SOON: { type: "magenta", label: "inventory.reorderStatus.soon" },
  ADEQUATE: { type: "green", label: "inventory.reorderStatus.adequate" },
  BUILDING_DATA: {
    type: "gray",
    label: "inventory.reorderStatus.buildingData",
  },
};

const LEAD_TIER_LABELS = {
  SET: "inventory.orderBy.leadSet",
  OBSERVED: "inventory.orderBy.leadObserved",
  DEFAULT: "inventory.orderBy.leadDefault",
};

/** Matches the module's existing expiry-warning window on the old dashboard. */
const EXPIRING_SOON_DAYS = 30;

const QC_TAGS = {
  PASSED: "green",
  FAILED: "red",
  QUARANTINED: "magenta",
  PENDING: "gray",
};

/**
 * Enum values arrive as their Java names. Translate them through the lot.status
 * and lot.qcStatus keys the rest of the app already uses for these same two
 * enums, falling back to the raw name so an enum added server side shows up
 * rather than disappearing. A second set of keys for one enum would be a second
 * vocabulary to translate and to keep in step.
 */
const labelFor = (intl, prefix, value) =>
  value == null
    ? ""
    : intl.formatMessage({
        id: `${prefix}${value}`,
        defaultMessage: value,
      });

/**
 * The board's dates arrive as plain yyyy-MM-dd. `new Date(string)` reads those as
 * UTC midnight, which renders as the previous day anywhere west of Greenwich, so
 * build the date in local time instead.
 */
const parseBoardDate = (value) => {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
};

const startOfToday = () => {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
};

const compare = (a, b) => {
  // Rows with nothing to compare sort last in either direction: an item with no
  // projection is not "earliest", it is unknown.
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;
  if (typeof a === "number" && typeof b === "number") return a - b;
  return String(a).localeCompare(String(b));
};

const InventoryItemsBoard = () => {
  const intl = useIntl();
  const [rows, setRows] = useState([]);
  const [lots, setLots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [locationFilter, setLocationFilter] = useState("");
  const [expandedId, setExpandedId] = useState(null);
  const [sort, setSort] = useState({ key: null, ascending: true });
  const [detailLot, setDetailLot] = useState(null);

  // One action at a time, holding the row or lot it was opened against. The
  // modals seed their form state at construction, so they are mounted only
  // while active rather than kept mounted behind an `open` prop.
  const [action, setAction] = useState(null);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Both reads, always together: every write here changes on-hand, and on-hand
  // is what the projection is computed from, so refreshing the lots without the
  // board would leave the run-out dates describing the stock level before the
  // action.
  const refresh = useCallback(
    () =>
      Promise.all([InventoryBoardAPI.get(), InventoryLotAPI.getAll()])
        .then(([board, allLots]) => {
          setRows(Array.isArray(board) ? board : []);
          setLots(Array.isArray(allLots) ? allLots : []);
          setError(null);
        })
        .catch((err) => setError(err.message)),
    [],
  );

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  const closeAction = () => setAction(null);

  const onActionSaved = (messageId) => {
    setAction(null);
    refresh();
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      message: intl.formatMessage({ id: messageId }),
    });
  };

  // The item editor takes a whole item, and a board row is a projection: it
  // carries itemId rather than id and omits six editable fields, so handing the
  // row straight over would PUT to /items/undefined and blank whatever it does
  // not carry. Fetch the real item first.
  const openItemEditor = async (row) => {
    try {
      const item = await InventoryItemAPI.getById(row.itemId);
      setAction({ kind: "editItem", item, row });
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        message: err.message,
      });
    }
  };

  const lotsByItem = useMemo(() => {
    const grouped = new Map();
    lots.forEach((lot) => {
      const itemId = lot.inventoryItem?.id;
      if (itemId == null) return;
      if (!grouped.has(itemId)) grouped.set(itemId, []);
      grouped.get(itemId).push(lot);
    });
    return grouped;
  }, [lots]);

  const locations = useMemo(() => {
    const paths = new Set();
    lots.forEach((lot) => {
      const path = lot.location?.hierarchicalPath;
      if (path) paths.add(path);
    });
    return [...paths].sort();
  }, [lots]);

  const visibleRows = useMemo(() => {
    const term = search.trim().toLowerCase();
    const matched = rows.filter((row) => {
      const itemLots = lotsByItem.get(row.itemId) || [];
      if (statusFilter && row.status !== statusFilter) return false;
      if (
        locationFilter &&
        !itemLots.some(
          (lot) => lot.location?.hierarchicalPath === locationFilter,
        )
      ) {
        return false;
      }
      if (!term) return true;
      return (
        row.name?.toLowerCase().includes(term) ||
        row.code?.toLowerCase().includes(term) ||
        itemLots.some(
          (lot) =>
            lot.lotNumber?.toLowerCase().includes(term) ||
            // A scanner is the fastest way to reach a row, and the barcode is
            // what it emits. Storage's own lot search already matches on it.
            lot.barcode?.toLowerCase().includes(term),
        )
      );
    });
    // No sort key means the server's urgency ordering stands.
    if (!sort.key) return matched;
    // Reversing a sorted list would carry the rows with no value to the front,
    // and "unknown" is not the extreme of anything. compare() already puts them
    // last, so descending negates the comparison of two real values instead.
    return [...matched].sort((a, b) => {
      const left = a[sort.key];
      const right = b[sort.key];
      if (left == null || right == null) return compare(left, right);
      return sort.ascending ? compare(left, right) : compare(right, left);
    });
  }, [rows, lotsByItem, search, statusFilter, locationFilter, sort]);

  const toggleSort = (key) =>
    setSort((current) =>
      current.key === key
        ? { key, ascending: !current.ascending }
        : { key, ascending: true },
    );

  const sortableHeader = (key, labelId, extraProps = {}) => (
    <TableHeader
      key={key}
      isSortable
      isSortHeader={sort.key === key}
      sortDirection={sort.ascending ? "ASC" : "DESC"}
      onClick={() => toggleSort(key)}
      {...extraProps}
    >
      <FormattedMessage id={labelId} />
    </TableHeader>
  );

  const formatDay = (value) => {
    const date = parseBoardDate(value);
    return date
      ? intl.formatDate(date, { month: "short", day: "numeric" })
      : null;
  };

  const renderTrend = (trendPercent) => {
    if (trendPercent == null) return <span className="board-muted">—</span>;
    if (Math.abs(trendPercent) < STEADY_TREND_PERCENT) {
      return (
        <span className="board-trend">
          <Subtract size={16} />
          <FormattedMessage id="inventory.projection.trend.steady" />
        </span>
      );
    }
    const rising = trendPercent > 0;
    return (
      <span className={`board-trend ${rising ? "trend-up" : "trend-down"}`}>
        {rising ? <ArrowUp size={16} /> : <ArrowDown size={16} />}
        <FormattedMessage
          id="inventory.projection.trend.change"
          values={{
            percent: intl.formatNumber(Math.round(trendPercent), {
              signDisplay: "always",
            }),
            days: USAGE_WINDOW_DAYS,
          }}
        />
      </span>
    );
  };

  const renderRunsOut = (row) => {
    if (!row.runOutEarly) {
      // Only the explanation, not a "Building data" heading: the status column
      // already carries that label whenever it applies, and a row can lack a
      // projection while its status reads Reorder now off the threshold alone.
      return (
        <div className="board-subline">
          <FormattedMessage id="inventory.projection.insufficient" />
        </div>
      );
    }
    const window = row.runOutLate ? (
      <FormattedMessage
        id="inventory.projection.window"
        values={{
          early: formatDay(row.runOutEarly),
          late: formatDay(row.runOutLate),
        }}
      />
    ) : (
      <FormattedMessage
        id="inventory.projection.windowOpen"
        values={{ early: formatDay(row.runOutEarly) }}
      />
    );
    return (
      <>
        <div className={row.status === "REORDER_NOW" ? "board-urgent" : ""}>
          {window}
        </div>
        <div className="board-subline">
          {row.stale ? (
            <FormattedMessage id="inventory.projection.stale" />
          ) : (
            <FormattedMessage
              id="inventory.projection.basis"
              values={{ date: formatDay(row.basisDate) }}
            />
          )}
        </div>
      </>
    );
  };

  const renderOrderBy = (row) => {
    const orderBy = parseBoardDate(row.orderByDate);
    return (
      <>
        <div
          className={orderBy && orderBy < startOfToday() ? "board-urgent" : ""}
        >
          {orderBy ? (
            orderBy < startOfToday() ? (
              <FormattedMessage id="inventory.orderBy.pastDue" />
            ) : (
              formatDay(row.orderByDate)
            )
          ) : (
            <span className="board-muted">—</span>
          )}
        </div>
        {row.leadTimeTier && (
          <div className="board-subline">
            <FormattedMessage
              id={LEAD_TIER_LABELS[row.leadTimeTier]}
              values={{ days: row.leadTimeDays }}
            />
          </div>
        )}
      </>
    );
  };

  // The dashboard being retired warned per lot when a lot was expired or close
  // to it. The board's status column answers a different question — whether the
  // item needs reordering — so without this the warning would simply be gone.
  const renderExpiryTag = (lot) => {
    if (!lot.effectiveExpirationDate) return null;
    // Test the remaining milliseconds, not the rounded days. A lot that went off
    // earlier today leaves a fraction of a day, and Math.ceil turns that into -0,
    // which is not less than zero — so the lot read "Expires in 0d" on the day it
    // expired.
    const remainingMs = new Date(lot.effectiveExpirationDate) - Date.now();
    const days = Math.ceil(remainingMs / 86400000);
    if (remainingMs < 0) {
      return (
        <Tag size="sm" type="red">
          <FormattedMessage id="stock.status.expired" />
        </Tag>
      );
    }
    if (days <= EXPIRING_SOON_DAYS) {
      return (
        <Tag size="sm" type="magenta">
          <FormattedMessage
            id="inventory.lots.expiringInDays"
            values={{ days }}
          />
        </Tag>
      );
    }
    return null;
  };

  const lotActions = (lot) => (
    <OverflowMenu
      size="sm"
      flipped
      iconDescription={intl.formatMessage(
        { id: "inventory.actions.forLot" },
        { lot: lot.lotNumber },
      )}
    >
      <OverflowMenuItem
        itemText={intl.formatMessage({ id: "inventory.actions.editLot" })}
        onClick={() => setAction({ kind: "editLot", lot })}
      />
      <OverflowMenuItem
        itemText={intl.formatMessage({ id: "adjustment.button" })}
        onClick={() => setAction({ kind: "adjust", lot })}
      />
      <OverflowMenuItem
        itemText={intl.formatMessage({ id: "qc.status.update.button" })}
        onClick={() => setAction({ kind: "qc", lot })}
      />
      <OverflowMenuItem
        isDelete
        itemText={intl.formatMessage({ id: "disposal.button" })}
        onClick={() => setAction({ kind: "dispose", lot })}
      />
    </OverflowMenu>
  );

  const renderExpansion = (row) => {
    const itemLots = lotsByItem.get(row.itemId) || [];
    if (itemLots.length === 0) {
      return (
        <p className="board-muted">
          <FormattedMessage id="inventory.lots.none" />
        </p>
      );
    }
    const usable = itemLots
      .filter((lot) => lot.availableForUse)
      .sort((a, b) =>
        compare(a.effectiveExpirationDate, b.effectiveExpirationDate),
      );
    const useFirstId = usable[0]?.id;

    const byLocation = new Map();
    usable.forEach((lot) => {
      const path =
        lot.location?.hierarchicalPath ||
        intl.formatMessage({ id: "storage.location.notAssigned" });
      byLocation.set(path, (byLocation.get(path) || 0) + lot.currentQuantity);
    });

    return (
      <div className="board-expansion">
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="lot.number" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="lot.expirationDate" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="lot.currentQuantity" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="lot.status" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="lot.qcStatus" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="common.storageLocation" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="inventory.lots.flag" />
              </TableHeader>
              <TableHeader>
                <span className="board-visually-hidden">
                  <FormattedMessage id="common.actions" />
                </span>
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {itemLots.map((lot) => (
              <TableRow key={lot.id}>
                <TableCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    className="board-lot-link"
                    onClick={() => setDetailLot(lot)}
                  >
                    {lot.lotNumber}
                  </Button>
                </TableCell>
                <TableCell>
                  {lot.effectiveExpirationDate
                    ? intl.formatDate(lot.effectiveExpirationDate, {
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      })
                    : "—"}{" "}
                  {renderExpiryTag(lot)}
                </TableCell>
                <TableCell>
                  {intl.formatNumber(lot.currentQuantity)} {row.units}
                </TableCell>
                <TableCell>
                  {labelFor(intl, "lot.status.", lot.status)}
                </TableCell>
                <TableCell>
                  <Tag size="sm" type={QC_TAGS[lot.qcStatus] || "gray"}>
                    {labelFor(intl, "lot.qcStatus.", lot.qcStatus)}
                  </Tag>
                </TableCell>
                <TableCell>
                  {lot.location?.hierarchicalPath || (
                    <span className="board-muted">
                      <FormattedMessage id="storage.location.notAssigned" />
                    </span>
                  )}
                </TableCell>
                <TableCell>
                  {lot.id === useFirstId && (
                    <Tag size="sm" type="blue">
                      <FormattedMessage id="inventory.lots.useFirst" />
                    </Tag>
                  )}
                </TableCell>
                <TableCell className="board-actions-cell">
                  {lotActions(lot)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        <p className="board-working">
          {row.medianDailyUse != null ? (
            <FormattedMessage
              id="inventory.projection.medianDailyUse"
              values={{
                rate: intl.formatNumber(row.medianDailyUse, {
                  maximumFractionDigits: 1,
                }),
                units: row.units,
                days: USAGE_WINDOW_DAYS,
              }}
            />
          ) : (
            <FormattedMessage id="inventory.projection.insufficient" />
          )}
        </p>
        {byLocation.size > 0 && (
          <p className="board-working">
            <FormattedMessage id="inventory.board.byLocation" />{" "}
            {[...byLocation.entries()]
              .map(
                ([path, quantity]) =>
                  `${path}: ${intl.formatNumber(quantity)} ${row.units}`,
              )
              .join(" · ")}
          </p>
        )}
      </div>
    );
  };

  // "Critical" is not a field on an item — the module has no criticality flag —
  // so the board's own REORDER_NOW is what it means here: on hand is at or below
  // the item's threshold right now. Marking ordered is what quiets a row; the row
  // itself stays on the board, still tagged, because the stock is still short.
  const unaddressedCritical = rows.filter(
    (row) => row.status === "REORDER_NOW" && !row.orderedOn,
  );
  const suggestionCount = rows.filter(isSuggested).length;

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "common.loading" })}
        withOverlay={false}
      />
    );
  }

  return (
    <div className="inventory-items-board">
      <p className="board-purpose">
        <FormattedMessage id="inventory.board.purpose" />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "inventory.board.error" })}
          subtitle={error}
        />
      )}

      {unaddressedCritical.length > 0 && (
        <ActionableNotification
          kind="error"
          lowContrast
          inline
          hideCloseButton
          className="board-critical-banner"
          title={intl.formatMessage({ id: "inventory.reorderStatus.now" })}
          subtitle={unaddressedCritical.map((row) => row.name).join(" · ")}
          actionButtonLabel={intl.formatMessage({
            id: "inventory.reorder.reviewAndOrder",
          })}
          onActionButtonClick={() => setAction({ kind: "suggestions" })}
        />
      )}

      <div className="board-toolbar">
        <Search
          id="inventory-board-search"
          size="lg"
          labelText={intl.formatMessage({ id: "inventory.search.placeholder" })}
          placeholder={intl.formatMessage({
            id: "inventory.search.placeholder",
          })}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <Select
          id="inventory-board-status-filter"
          labelText={<FormattedMessage id="inventory.filter.status" />}
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "inventory.filter.all" })}
          />
          {Object.entries(STATUS_TAGS).map(([value, { label }]) => (
            <SelectItem
              key={value}
              value={value}
              text={intl.formatMessage({ id: label })}
            />
          ))}
        </Select>
        <Select
          id="inventory-board-location-filter"
          labelText={<FormattedMessage id="inventory.filter.location" />}
          value={locationFilter}
          onChange={(event) => setLocationFilter(event.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "inventory.filter.all" })}
          />
          {locations.map((path) => (
            <SelectItem key={path} value={path} text={path} />
          ))}
        </Select>
        <Button
          kind="tertiary"
          size="lg"
          className="board-log-usage"
          onClick={() => setAction({ kind: "quickLog" })}
        >
          <FormattedMessage id="inventory.logUsage.button" />
        </Button>
        <Button
          kind="tertiary"
          size="lg"
          className="board-suggestions-button"
          onClick={() => setAction({ kind: "suggestions" })}
        >
          <FormattedMessage id="inventory.reorder.suggestions" />
          {suggestionCount > 0 ? ` (${suggestionCount})` : ""}
        </Button>
      </div>

      <TableContainer>
        <Table size="md" useZebraStyles={false}>
          <TableHead>
            <TableRow>
              <TableExpandHeader />
              {sortableHeader("name", "inventory.board.column.item")}
              {sortableHeader("onHand", "inventory.board.column.onHand")}
              {sortableHeader("trendPercent", "inventory.projection.trend")}
              {sortableHeader("runOutEarly", "inventory.board.column.runsOut")}
              {sortableHeader("orderByDate", "inventory.orderBy.label")}
              {sortableHeader("status", "common.status")}
              <TableHeader>
                <span className="board-visually-hidden">
                  <FormattedMessage id="common.actions" />
                </span>
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {visibleRows.length === 0 && !error && (
              <TableRow>
                <TableCell colSpan={8}>
                  <p className="board-empty">
                    <FormattedMessage
                      id={
                        rows.length === 0
                          ? "inventory.board.empty"
                          : "inventory.board.noMatches"
                      }
                    />
                  </p>
                </TableCell>
              </TableRow>
            )}
            {visibleRows.map((row) => {
              const isOpen = expandedId === row.itemId;
              const statusTag = STATUS_TAGS[row.status] || STATUS_TAGS.ADEQUATE;
              return (
                <React.Fragment key={row.itemId}>
                  <TableExpandRow
                    isExpanded={isOpen}
                    onExpand={() => setExpandedId(isOpen ? null : row.itemId)}
                    ariaLabel={row.name}
                  >
                    <TableCell>
                      <div className="board-item-name">{row.name}</div>
                      <div className="board-subline">
                        {row.code}
                        {row.itemType &&
                          ` · ${labelFor(intl, "inventory.itemType.", row.itemType)}`}
                      </div>
                    </TableCell>
                    <TableCell>
                      {intl.formatNumber(row.onHand)}{" "}
                      <span className="board-units">{row.units}</span>
                    </TableCell>
                    <TableCell>{renderTrend(row.trendPercent)}</TableCell>
                    <TableCell>{renderRunsOut(row)}</TableCell>
                    <TableCell>{renderOrderBy(row)}</TableCell>
                    <TableCell>
                      <Tag type={statusTag.type}>
                        <FormattedMessage id={statusTag.label} />
                      </Tag>
                      {row.orderedOn && (
                        <Tag type="teal" title={row.orderNote || undefined}>
                          <FormattedMessage id="inventory.reorder.onOrder" />
                          {/* The date the lab entered when it marked the order.
                              Collecting it and then showing it nowhere would
                              leave the row saying only that something is on the
                              way, which is the question the date answers. */}
                          {row.orderExpectedDate &&
                            ` · ${formatDay(row.orderExpectedDate)}`}
                        </Tag>
                      )}
                    </TableCell>
                    <TableCell className="board-actions-cell">
                      <OverflowMenu
                        size="sm"
                        flipped
                        iconDescription={intl.formatMessage(
                          { id: "inventory.actions.forItem" },
                          { item: row.name },
                        )}
                      >
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "inventory.receiveStock.button",
                          })}
                          onClick={() => setAction({ kind: "receive", row })}
                        />
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "usage.record.button",
                          })}
                          onClick={() => setAction({ kind: "quickLog", row })}
                        />
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "inventory.actions.editItem",
                          })}
                          onClick={() => openItemEditor(row)}
                        />
                      </OverflowMenu>
                    </TableCell>
                  </TableExpandRow>
                  {isOpen && (
                    <TableExpandedRow colSpan={8}>
                      {renderExpansion(row)}
                    </TableExpandedRow>
                  )}
                </React.Fragment>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      <LotDetailsPanel
        open={detailLot !== null}
        lot={detailLot}
        onClose={() => setDetailLot(null)}
      />

      {/* Each action is mounted only while it is the active one. These modals
          seed their form state from their props at construction and never reset
          it, so keeping them mounted behind an `open` prop would show the
          previous row's values on the next open. */}
      {action?.kind === "receive" && (
        <LotEntryModal
          open
          lot={null}
          item={{ id: action.row.itemId }}
          onClose={closeAction}
          onSave={() => onActionSaved("lot.save.success")}
        />
      )}
      {action?.kind === "editLot" && (
        <LotEntryModal
          open
          lot={action.lot}
          onClose={closeAction}
          onSave={() => onActionSaved("lot.save.success")}
        />
      )}
      {action?.kind === "adjust" && (
        <LotAdjustmentModal
          open
          lot={action.lot}
          onClose={closeAction}
          onSave={() => onActionSaved("adjustment.success")}
        />
      )}
      {action?.kind === "qc" && (
        <UpdateQCStatusModal
          open
          lot={action.lot}
          onClose={closeAction}
          onSave={() => onActionSaved("qc.status.update.success")}
        />
      )}
      {action?.kind === "dispose" && (
        <DisposeLotModal
          open
          lot={action.lot}
          onClose={closeAction}
          onSave={() => onActionSaved("disposal.success")}
        />
      )}
      {action?.kind === "editItem" && (
        <InventoryItemForm
          open
          item={action.item}
          // The board already resolved this item's lead time; offer the learned
          // figure only when it is the one in use, never as a silent overwrite.
          observedLeadTime={
            action.row?.leadTimeTier === "OBSERVED"
              ? action.row.leadTimeDays
              : null
          }
          onClose={closeAction}
          onSave={() => onActionSaved("catalog.item.save.success")}
        />
      )}
      {action?.kind === "quickLog" && (
        <QuickLogUsageModal
          open
          items={rows}
          initialItemId={action.row?.itemId ?? null}
          onClose={closeAction}
          onSave={() => onActionSaved("usage.record.success")}
        />
      )}

      {action?.kind === "suggestions" && (
        <ReorderSuggestionsModal
          open
          rows={rows}
          onClose={closeAction}
          onMarked={(count, outcome) => {
            setAction(null);
            refresh();
            notify({
              kind: NotificationKinds.success,
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage(
                {
                  id:
                    outcome === "cleared"
                      ? "inventory.reorder.cleared"
                      : "inventory.reorder.marked",
                },
                { count },
              ),
            });
          }}
        />
      )}

      {notificationVisible === true ? <AlertDialog /> : ""}
    </div>
  );
};

export default InventoryItemsBoard;
