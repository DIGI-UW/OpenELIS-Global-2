package org.openelisglobal.inventory.projection;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryProjection {

    public enum LeadTimeTier {
        SET, OBSERVED, DEFAULT
    }

    public enum BoardStatus {
        REORDER_NOW, REORDER_SOON, ADEQUATE, BUILDING_DATA
    }

    private Long itemId;
    private String code;
    private String name;
    private List<String> tags;
    private String units;

    private boolean trackLots;

    private String upc;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastCountedOn;

    private boolean active = true;

    private Double onHand;

    private Integer lowStockThreshold;

    private Double medianDailyUse;

    /** Newest usage date the estimate saw; null when the item has no usage. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate basisDate;

    private boolean stale;

    /** Null when there is no projection. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate runOutEarly;

    /** Null when there is no projection or the slow end is open-ended. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate runOutLate;

    private Integer leadTimeDays;
    private LeadTimeTier leadTimeTier;

    /** runOutEarly minus the lead time; null when there is no projection. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderByDate;

    /** Newer vs older half-window median use, in %; null for low volume. */
    private Double trendPercent;

    private BoardStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderedOn;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderExpectedDate;

    private String orderNote;
}
