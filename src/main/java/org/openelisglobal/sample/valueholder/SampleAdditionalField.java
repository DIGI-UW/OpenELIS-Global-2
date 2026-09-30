package org.openelisglobal.sample.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.SampleAdditionalFieldId;

@Setter
@Getter
@Entity
@Table(name = "sample_additional_fields")
public class SampleAdditionalField extends BaseObject<SampleAdditionalFieldId> {

    public enum AdditionalFieldName {
        CONTACT_TRACING_INDEX_NAME, CONTACT_TRACING_INDEX_RECORD_NUMBER
    }

    private static final long serialVersionUID = 932652572195268450L;

    @EmbeddedId
    private SampleAdditionalFieldId id;

    @MapsId("sampleId") // value corresponds to property in the ID class
    @ManyToOne
    @JoinColumn(name = "sample_id")
    private Sample sample;

    @Column(name = "field_value")
    private String fieldValue;

    @Override
    public SampleAdditionalFieldId getId() {
        return id;
    }

    @Override
    public void setId(SampleAdditionalFieldId id) {
        this.id = id;
    }

    public AdditionalFieldName getFieldName() {
        if (id == null) {
            id = new SampleAdditionalFieldId();
        }
        return id.getFieldName();
    }

    public void setFieldName(AdditionalFieldName fieldName) {
        if (id == null) {
            id = new SampleAdditionalFieldId();
        }
        this.id.setFieldName(fieldName);
    }

    @Setter
    @Getter
    @Embeddable
    public static class SampleAdditionalFieldId implements Serializable {
        private static final long serialVersionUID = -9097137007120585441L;

        @Column(name = "field_name")
        @Enumerated(value = EnumType.STRING)
        private AdditionalFieldName fieldName;

        @Convert(converter = StringToIntegerConverter.class)
        private String sampleId;

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            SampleAdditionalFieldId that = (SampleAdditionalFieldId) o;

            return Objects.equals(this.fieldName, that.fieldName) && Objects.equals(this.sampleId, that.sampleId);
        }

        public int hashCode() {
            return Objects.hash(fieldName, sampleId);
        }
    }
}
