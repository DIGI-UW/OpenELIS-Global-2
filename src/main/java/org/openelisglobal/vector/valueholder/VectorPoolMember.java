package org.openelisglobal.vector.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Pure M:N join between {@link VectorPool} and
 * {@link org.openelisglobal.sampleitem.valueholder.SampleItem}. Two foreign-key
 * columns + a timestamp; no per-row metadata. Modeled as a join table so the
 * same organism can belong to multiple pools across V-03 deconvolution rounds
 * without schema churn.
 */
@Entity
@Table(name = "vector_pool_member", schema = "clinlims")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class VectorPoolMember {

    @EmbeddedId
    private VectorPoolMemberId id = new VectorPoolMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("vectorPoolId")
    @JoinColumn(name = "vector_pool_id")
    private VectorPool pool;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sampleItemId")
    @JoinColumn(name = "sample_item_id")
    private SampleItem sampleItem;

    @Column(name = "lastupdated", insertable = false, updatable = false)
    private Timestamp lastupdated;

    public VectorPoolMember() {
    }

    public VectorPoolMember(VectorPool pool, SampleItem sampleItem) {
        this.pool = pool;
        this.sampleItem = sampleItem;
        if (pool != null && pool.getId() != null) {
            this.id.setVectorPoolId(pool.getId());
        }
        if (sampleItem != null && sampleItem.getId() != null) {
            // SampleItem.id is a String (LIMSStringNumberUserType maps it to the
            // numeric column); @MapsId copies that String onto the composite key,
            // so the key field has to be String too.
            this.id.setSampleItemId(sampleItem.getId());
        }
    }

    public VectorPoolMemberId getId() {
        return id;
    }

    public void setId(VectorPoolMemberId id) {
        this.id = id;
    }

    public VectorPool getPool() {
        return pool;
    }

    public void setPool(VectorPool pool) {
        this.pool = pool;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    @Embeddable
    public static class VectorPoolMemberId implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "vector_pool_id")
        private Integer vectorPoolId;

        /**
         * String on the Java side to match {@code SampleItem.id} (mapped via
         * LIMSStringNumberUserType); converted to/from the NUMERIC column at the JPA
         * boundary.
         */
        @Column(name = "sample_item_id")
        @Convert(converter = StringToIntegerConverter.class)
        private String sampleItemId;

        public VectorPoolMemberId() {
        }

        public VectorPoolMemberId(Integer vectorPoolId, String sampleItemId) {
            this.vectorPoolId = vectorPoolId;
            this.sampleItemId = sampleItemId;
        }

        public Integer getVectorPoolId() {
            return vectorPoolId;
        }

        public void setVectorPoolId(Integer vectorPoolId) {
            this.vectorPoolId = vectorPoolId;
        }

        public String getSampleItemId() {
            return sampleItemId;
        }

        public void setSampleItemId(String sampleItemId) {
            this.sampleItemId = sampleItemId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof VectorPoolMemberId))
                return false;
            VectorPoolMemberId other = (VectorPoolMemberId) o;
            return Objects.equals(vectorPoolId, other.vectorPoolId) && Objects.equals(sampleItemId, other.sampleItemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vectorPoolId, sampleItemId);
        }
    }
}
