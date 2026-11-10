package org.openelisglobal.dashboard.management.stock;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;

public record StockResponse(State state, List<Reagent> criticalReagents, List<ExpiringLot> expiring,
        List<Reorder> reorder) {

    public enum State {
        OK, EMPTY
    }

    public record Reagent(Long itemId, String name, String lot,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate expiry, double onHand,
            int threshold, BoardStatus status) {
    }

    public record ExpiringLot(String name, String lot, long daysLeft, boolean expired, double remaining) {
    }

    public record Reorder(Long itemId, String name, double onHand, Integer threshold, Kind kind,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate orderBy,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate orderedOn) {

        public enum Kind {
            BELOW, APPROACHING
        }
    }
}
