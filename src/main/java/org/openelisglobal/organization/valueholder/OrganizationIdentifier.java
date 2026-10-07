package org.openelisglobal.organization.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1363 (section I): one labelled identifier of an organization, modelled
 * the way FHIR models them: a label the admin types ("DHIS2 ID", "CLIA",
 * "Code") and a value. Exactly one identifier per organization is the reporting
 * code, the one lists, exports and reports show as Code.
 */
@Entity
@Table(name = "organization_identifier", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class OrganizationIdentifier extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    public static final String CODE_LABEL = "Code";
    public static final String CLIA_LABEL = "CLIA";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_identifier_seq_gen")
    @SequenceGenerator(name = "organization_identifier_seq_gen", sequenceName = "organization_identifier_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "value", length = 100, nullable = false)
    private String value;

    @Column(name = "is_reporting", nullable = false)
    private boolean reporting;

    public OrganizationIdentifier() {
    }

    public OrganizationIdentifier(Integer organizationId, String label, String value, boolean reporting) {
        this.organizationId = organizationId;
        this.label = label;
        this.value = value;
        this.reporting = reporting;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organizationId = organizationId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isReporting() {
        return reporting;
    }

    public void setReporting(boolean reporting) {
        this.reporting = reporting;
    }
}
