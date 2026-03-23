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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_usage")
public class InventoryUsage extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_usage_generator")
    @SequenceGenerator(name = "inventory_usage_generator", sequenceName = "inventory_usage_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @NotNull
    private InventoryItem inventoryItem;

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    @NotNull
    private InventoryLot lot;

    @Column(name = "test_result_id")
    private Long testResultId;

    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "quantity_used", nullable = false)
    @NotNull
    @Min(1)
    private Double quantityUsed;

    @Column(name = "usage_date", nullable = false)
    @NotNull
    private Timestamp usageDate;

    @Column(name = "performed_by_user", nullable = false)
    @NotNull
    private Integer performedByUser;
}
