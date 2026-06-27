package org.openelisglobal.testreagentlink.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 / OGC-986 (epic OGC-760) — links a test to a reagent it consumes.
 * Prerequisite for the v2 Reagents tab (OGC-762).
 *
 * <p>
 * A "reagent" is an {@code inventory_item} with {@code item_type = 'REAGENT'}
 * (see {@code org.openelisglobal.inventory}); there is no standalone reagent
 * table, so {@code reagentId} is a FK to {@code inventory_item.id}.
 * {@code test_id} (a {@code numeric(10)} FK to {@code test.id}) maps to String
 * via {@code LIMSStringNumberUserType}, the established OpenELIS idiom (see
 * {@code TestResultComponent}). {@code reagent_id} is a {@code bigint} FK to
 * {@code inventory_item.id} ({@code Long}) and is mapped as a plain
 * {@code Long} — {@code LIMSStringNumberUserType} is int-only and would
 * truncate it. The audit {@code @Version} column ({@code last_updated}) comes
 * from {@link BaseObject}; the table's separate {@code lastupdated} (DEFAULT
 * now()) is filled by the DB and not mapped here.
 */
@Entity
@Table(name = "test_reagent_link", schema = "clinlims")
public class TestReagentLink extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    // FK to inventory_item.id (Long, sequence-generated). Plain Long mapping —
    // NOT LIMSStringNumberUserType, which is int-only and would truncate a bigint.
    @Column(name = "reagent_id", nullable = false)
    private Long reagentId;

    @Column(name = "usage_type", nullable = false, length = 20)
    private String usageType;

    @Column(name = "quantity_per_test", precision = 15, scale = 6)
    private BigDecimal quantityPerTest;

    @Column(name = "quantity_unit", length = 50)
    private String quantityUnit;

    public TestReagentLink() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public Long getReagentId() {
        return reagentId;
    }

    public void setReagentId(Long reagentId) {
        this.reagentId = reagentId;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public BigDecimal getQuantityPerTest() {
        return quantityPerTest;
    }

    public void setQuantityPerTest(BigDecimal quantityPerTest) {
        this.quantityPerTest = quantityPerTest;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }
}
