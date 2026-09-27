package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Entity
@Table(name = "micro_patient_origin_default", schema = "clinlims")
public class MicroPatientOriginDefault extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "organization_id", nullable = false, unique = true)
    private String organizationId;

    @Column(name = "patient_origin_id", nullable = false, length = 36)
    private String patientOriginId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getPatientOriginId() {
        return patientOriginId;
    }

    public void setPatientOriginId(String patientOriginId) {
        this.patientOriginId = patientOriginId;
    }
}
