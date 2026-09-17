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
import { labNow } from "../utils/labClock";
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

const BANNER_NAMES = 5;

/** Must match WINDOW_DAYS server side. */
const USAGE_WINDOW_DAYS = 30;

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

const EXPIRING_SOON_DAYS = 30;

const QC_TAGS = {
  PASSED: "green",
  FAILED: "red",
  QUARANTINED: "magenta",
  PENDING: "gray",
};

const labelFor = (intl, prefix, value) =>
  value == null
    ? ""
    : intl.formatMessage({
        id: `${prefix}${value}`,
        defaultMessage: value,
      });

// Parsing "yyyy-MM-dd" directly gives UTC midnight, a day early west of Greenwich.
const parseBoardDate = (value) => {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
};

const startOfToday = () => {
  const now = labNow();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
};

const compare = (a, b) => {
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

  // A board row carries itemId and only some item fields; the editor needs the full item.
  const openItemEditor = async (row) => {
    try {
      const item = await InventoryItemAPI.getById(row.itemId);
      setAction({ kind: "editItem", item });
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
            lot.barcode?.toLowerCase().includes(term),
        )
      );
    });
    // No sort key keeps the server's urgency order.
    if (!sort.key) return matched;
    // Nulls stay last in both directions; descending flips only real values.
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
    if (row.onHand === 0) {
      return (
        <div className="board-urgent">
          <FormattedMessage id="inventory.projection.outOfStock" />
        </div>
      );
    }
    if (!row.runOutEarly) {
      return (
        <div className="board-subline">
          <FormattedMessage id="inventory.projection.insufficient" />
        </div>
      );
    }
    const window = !row.runOutLate ? (
      <FormattedMessage
        id="inventory.projection.windowOpen"
        values={{ early: formatDay(row.runOutEarly) }}
      />
    ) : row.runOutLate === row.runOutEarly ? (
      formatDay(row.runOutEarly)
    ) : (
      <FormattedMessage
        id="inventory.projection.window"
        values={{
          early: formatDay(row.runOutEarly),
          late: formatDay(row.runOutLate),
        }}
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

  const renderExpiryTag = (lot) => {
    if (!lot.effectiveExpirationDate) return null;
    // Compare milliseconds: Math.ceil of part of a past day gives -0, which is not < 0.
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
    itemLots
      .filter((lot) => lot.countsAsAvailableStock)
      .forEach((lot) => {
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
                  {lot.qcStatus === "FAILED" && lot.currentQuantity > 0 && (
                    <Tag size="sm" type="red">
                      <FormattedMessage id="inventory.lots.disposeFailedQc" />
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

  // Items have no criticality flag: "critical" means REORDER_NOW and not yet marked ordered.
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
          subtitle={[
            ...unaddressedCritical
              .slice(0, BANNER_NAMES)
              .map((row) => row.name),
            ...(unaddressedCritical.length > BANNER_NAMES
              ? [
                  intl.formatMessage(
                    { id: "inventory.board.andMore" },
                    { count: unaddressedCritical.length - BANNER_NAMES },
                  ),
                ]
              : []),
          ].join(" · ")}
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

      {/* Mounted only while active: these modals seed form state once and never reset it. */}
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
