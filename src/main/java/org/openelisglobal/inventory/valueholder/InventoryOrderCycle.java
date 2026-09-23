package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One completed order-to-receipt cycle for an item: it was marked as ordered,
 * then stock arrived, and this is how long that took.
 *
 * <p>
 * Written only when a cycle closes, and never edited or deleted afterwards. A
 * cycle is not knowable while it is in flight —
 * {@code inventory_item.ordered_at} holds a single mark that the next receipt
 * replaces — so the moment of closing is the only chance to record it.
 */
@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_order_cycle")
public class InventoryOrderCycle extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_order_cycle_generator")
    @SequenceGenerator(name = "inventory_order_cycle_generator", sequenceName = "inventory_order_cycle_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @NotNull
    private InventoryItem inventoryItem;

    /** When the item was marked as ordered. */
    @Column(name = "ordered_at", nullable = false)
    @NotNull
    private Timestamp orderedAt;

    /** When the receipt that closed this cycle was recorded. */
    @Column(name = "received_at", nullable = false)
    @NotNull
    private Timestamp receivedAt;

    /**
     * Whole days between the two ends, stored rather than re-derived so a median
     * over many cycles is a single read.
     */
    @Column(name = "lead_time_days", nullable = false)
    @NotNull
    private Integer leadTimeDays;
}
