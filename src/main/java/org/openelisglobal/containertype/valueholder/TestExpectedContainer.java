package org.openelisglobal.containertype.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1611 FR-G5, FR-G6: a container a test expects its sample in. A test has
 * one Preferred and any number of Alternate containers. A container that yields
 * another sample type can be listed as an Alternate that is used as one of the
 * test's own sample types; the sample in it keeps its true sample type.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "test_expected_container", schema = "clinlims")
public class TestExpectedContainer extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public static final String PREFERRED = "PREFERRED";
    public static final String ALTERNATE = "ALTERNATE";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_expected_container_generator")
    @SequenceGenerator(name = "test_expected_container_generator", sequenceName = "test_expected_container_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @Column(name = "container_type_id", nullable = false)
    private Long containerTypeId;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "container_count", nullable = false)
    private int count = 1;

    @Column(name = "min_volume", precision = 10, scale = 2)
    private BigDecimal minVolume;

    @Column(name = "volume_unit", length = 10)
    private String volumeUnit;

    @Column(name = "used_as_type_of_sample_id")
    private Integer usedAsSampleTypeId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "sys_user_id")
    private Integer changedBy;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Long getContainerTypeId() {
        return containerTypeId;
    }

    public void setContainerTypeId(Long containerTypeId) {
        this.containerTypeId = containerTypeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public BigDecimal getMinVolume() {
        return minVolume;
    }

    public void setMinVolume(BigDecimal minVolume) {
        this.minVolume = minVolume;
    }

    public String getVolumeUnit() {
        return volumeUnit;
    }

    public void setVolumeUnit(String volumeUnit) {
        this.volumeUnit = volumeUnit;
    }

    /** The test's own sample type this container is used as, or null. */
    public Integer getUsedAsSampleTypeId() {
        return usedAsSampleTypeId;
    }

    public void setUsedAsSampleTypeId(Integer usedAsSampleTypeId) {
        this.usedAsSampleTypeId = usedAsSampleTypeId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        super.setSysUserId(sysUserId);
        this.changedBy = sysUserId != null && sysUserId.matches("\\d+") ? Integer.valueOf(sysUserId) : null;
    }
}
