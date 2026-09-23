package org.openelisglobal.inventory.projection;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * One row of the inventory items board: what is on hand, when it runs out, and
 * by when it has to be ordered.
 *
 * <p>
 * The run-out estimate is a window rather than a single date, and every field
 * that depends on consumption history carries enough context for the caller to
 * say how much to trust it: {@link #basisDate} is the newest usage the estimate
 * saw, and {@link #stale} says that usage has gone quiet. When there is too
 * little regular consumption to stand behind a date, the window, order-by date
 * and trend are all null and the status falls back to the item's own stock
 * threshold.
 */
/*
 * Dates are pinned to an ISO string shape on every field. Jackson's default for
 * java.time writes a LocalDate as a [year, month, day] array, which no caller
 * of this board parses, and which a service-level test never sees because it
 * never serialises.
 */
@Getter
@Setter
public class InventoryProjection {

    /** Where the effective lead time came from, so the board can show its basis. */
    public enum LeadTimeTier {
        /** The lab entered a lead time for this item. */
        SET,
        /** Learned from this item's own order-to-receipt history. */
        OBSERVED,
        /** Neither was available, so a global placeholder is in use. */
        DEFAULT
    }

    /**
     * Whether the item needs ordering, on the one rule the board and reorder
     * suggestions share.
     */
    public enum BoardStatus {
        /** On hand is at or below the item's stock threshold. */
        REORDER_NOW,
        /** Projected to reach the threshold within the item's own lead time. */
        REORDER_SOON,
        /** Neither test fires. */
        ADEQUATE,
        /**
         * Too little consumption history to project; threshold alone did not fire
         * either.
         */
        BUILDING_DATA
    }

    private Long itemId;
    private String code;
    private String name;
    private String itemType;
    private String units;

    /**
     * Quantity across lots that may actually be used, so this agrees with what
     * consumption sees.
     */
    private Double onHand;

    private Integer lowStockThreshold;

    /**
     * Median daily use over the trailing window. Median, not mean, so one busy day
     * cannot skew it.
     */
    private Double medianDailyUse;

    /**
     * Newest usage record the estimate is based on; null when the item has no usage
     * at all.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate basisDate;

    /**
     * True when usage has gone quiet for long enough that the estimate should be
     * hedged.
     */
    private boolean stale;

    /**
     * Early (conservative) end of the run-out window. Null when there is no
     * projection.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate runOutEarly;

    /**
     * Late end of the run-out window. Null both when there is no projection and
     * when consumption is erratic enough that the slow end is open-ended.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate runOutLate;

    private Integer leadTimeDays;
    private LeadTimeTier leadTimeTier;

    /**
     * Early end of the window minus the effective lead time. Null when there is no
     * projection.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderByDate;

    /**
     * Change in median daily use between the older and newer half of the window, as
     * a percentage. Null when the item is too low-volume for a percentage to mean
     * anything.
     */
    private Double trendPercent;

    private BoardStatus status;

    /**
     * When someone recorded that this item had been ordered, or null. An item on
     * order still shows its run-out window and its status — the point is that the
     * row stays visible and honest about the shortfall, while the page stops
     * demanding attention it has already been given.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderedOn;

    /** When the order is expected to arrive, if the lab said. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderExpectedDate;

    /** Free text kept with the mark. */
    private String orderNote;
}
