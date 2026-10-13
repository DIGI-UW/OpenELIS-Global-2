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
 * Append-only: written when a receipt closes an ordered cycle, never edited.
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

    @Column(name = "ordered_at", nullable = false)
    @NotNull
    private Timestamp orderedAt;

    @Column(name = "received_at", nullable = false)
    @NotNull
    private Timestamp receivedAt;

    @Column(name = "lead_time_days", nullable = false)
    @NotNull
    private Integer leadTimeDays;
}
