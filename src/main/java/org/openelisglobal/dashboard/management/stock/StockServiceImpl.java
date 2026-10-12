package org.openelisglobal.dashboard.management.stock;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.openelisglobal.dashboard.management.stock.StockResponse.ExpiringLot;
import org.openelisglobal.dashboard.management.stock.StockResponse.Reagent;
import org.openelisglobal.dashboard.management.stock.StockResponse.Reorder;
import org.openelisglobal.dashboard.management.stock.StockResponse.State;
import org.openelisglobal.inventory.projection.InventoryProjection;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockServiceImpl implements StockService {

    static final int EXPIRY_WINDOW_DAYS = 30;
    static final int CRITICAL_COUNT = 5;
    private static final Set<BoardStatus> REORDER_STATUSES = EnumSet.of(BoardStatus.REORDER_NOW,
            BoardStatus.REORDER_SOON);

    @Autowired
    private InventoryProjectionService inventoryProjectionService;
    @Autowired
    private InventoryLotService inventoryLotService;

    @Override
    @Transactional(readOnly = true)
    public StockResponse getStock() {
        LocalDate today = LocalDate.now();
        List<InventoryProjection> board = inventoryProjectionService.getBoard();
        return new StockResponse(inventoryLotService.getCount() == 0 ? State.EMPTY : State.OK, criticalReagents(board),
                expiring(today), reorder(board));
    }

    private List<Reagent> criticalReagents(List<InventoryProjection> board) {
        return board.stream().filter(row -> row.getLowStockThreshold() != null && row.getLowStockThreshold() > 0)
                .sorted(Comparator.comparingDouble(row -> row.getOnHand() / row.getLowStockThreshold()))
                .limit(CRITICAL_COUNT).map(row -> {
                    InventoryLot next = inventoryLotService.getAvailableLotsByItemFEFO(row.getItemId()).stream()
                            .findFirst().orElse(null);
                    return new Reagent(row.getName(), next == null ? null : next.getLotNumber(),
                            next == null || next.getEffectiveExpirationDate() == null ? null
                                    : next.getEffectiveExpirationDate().toLocalDateTime().toLocalDate(),
                            row.getOnHand(), row.getLowStockThreshold(), row.getStatus());
                }).toList();
    }

    // A lot whose printed and after-opening expiries straddle today is in both
    // lists.
    private List<ExpiringLot> expiring(LocalDate today) {
        Set<Long> seen = new HashSet<>();
        return Stream
                .concat(inventoryLotService.getExpiredActiveLots().stream(),
                        inventoryLotService.getExpiringLots(EXPIRY_WINDOW_DAYS).stream())
                .filter(lot -> lot.getCurrentQuantity() != null && lot.getCurrentQuantity() > 0
                        && seen.add(lot.getId()))
                .map(lot -> new ExpiringLot(lot.getInventoryItem().getName(), lot.getLotNumber(),
                        ChronoUnit.DAYS.between(today,
                                lot.getEffectiveExpirationDate().toLocalDateTime().toLocalDate()),
                        lot.getCurrentQuantity()))
                .sorted(Comparator.comparingLong(ExpiringLot::daysLeft)).toList();
    }

    // Below uses the board's own threshold rule, so an item with no usage history
    // still counts; approaching is a reorder status above the threshold.
    private List<Reorder> reorder(List<InventoryProjection> board) {
        Stream<Reorder> below = board.stream().filter(StockServiceImpl::atOrBelowThreshold)
                .map(row -> reorder(row, Reorder.Kind.BELOW));
        Stream<Reorder> approaching = board.stream()
                .filter(row -> !atOrBelowThreshold(row) && REORDER_STATUSES.contains(row.getStatus()))
                .map(row -> reorder(row, Reorder.Kind.APPROACHING));
        return Stream.concat(below, approaching).toList();
    }

    private static boolean atOrBelowThreshold(InventoryProjection row) {
        return row.getOnHand() <= (row.getLowStockThreshold() == null ? 0 : row.getLowStockThreshold());
    }

    private static Reorder reorder(InventoryProjection row, Reorder.Kind kind) {
        return new Reorder(row.getName(), row.getOnHand(), row.getLowStockThreshold(), kind, row.getOrderByDate(),
                row.getOrderedOn());
    }
}
