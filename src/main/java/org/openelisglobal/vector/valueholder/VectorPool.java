package org.openelisglobal.vector.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

/**
 * A vector pool — a many-organism testing arrangement. Members are tracked via
 * the {@code vector_pool_member} M:N join table (see {@link VectorPoolMember}),
 * so a {@code sample_item} can participate in multiple pools — required for
 * V-03 deconvolution rounds. {@link #parentPool} supports deconvolution: a
 * positive pool splits into smaller sub-pools.
 */
@Entity
@Table(name = "vector_pool", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorPool extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_pool_seq_gen")
    @SequenceGenerator(name = "vector_pool_seq_gen", sequenceName = "vector_pool_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    /**
     * Stored as String on the Java side to match {@code Sample.id}'s
     * LIMSStringNumberUserType mapping; converted to/from the underlying
     * NUMERIC(10,0) column at the JPA boundary.
     */
    @Column(name = "sample_id", nullable = false)
    @Convert(converter = StringToIntegerConverter.class)
    private String sampleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_pool_id")
    private VectorPool parentPool;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;

    @Column(name = "sys_user_id", length = 255)
    private String sysUserId;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public VectorPool getParentPool() {
        return parentPool;
    }

    public void setParentPool(VectorPool parentPool) {
        this.parentPool = parentPool;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return "Pool #" + id;
    }
}
