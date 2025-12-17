package org.openelisglobal.labunit.valueholder;

import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.organization.valueholder.Organization;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "lab_unit")
public class LabUnit extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "id", length = 36)
    private String id;

    @jakarta.persistence.Column(name = "name", length = 100, nullable = false)
    private String name;

    @jakarta.persistence.Column(name = "code", length = 50)
    private String code;

    @jakarta.persistence.Column(name = "description", length = 500)
    private String description;

    @jakarta.persistence.Column(name = "organization_id", length = 36)
    private String organizationId;

    @jakarta.persistence.Column(name = "parent_lab_unit_id", length = 36)
    private String parentLabUnitId;

    @jakarta.persistence.Column(name = "sort_order")
    private Integer sortOrder;

    @jakarta.persistence.Column(name = "is_active", length = 1)
    private String isActive = "Y";

    @jakarta.persistence.Column(name = "fhir_uuid", unique = true)
    private java.util.UUID fhirUuid;

    @jakarta.persistence.Column(name = "sys_user_id", length = 36)
    private String sysUserId;

    @jakarta.persistence.Column(name = "lastupdated")
    private Timestamp lastUpdated;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "organization_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Organization organization;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "parent_lab_unit_id", referencedColumnName = "id", insertable = false, updatable = false)
    private LabUnit parentLabUnit;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "localization_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Localization localization;

    public LabUnit() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getParentLabUnitId() {
        return parentLabUnitId;
    }

    public void setParentLabUnitId(String parentLabUnitId) {
        this.parentLabUnitId = parentLabUnitId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getActive() {
        return isActive;
    }

    public void setActive(String active) {
        this.isActive = active;
    }

    public java.util.UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(java.util.UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public LabUnit getParentLabUnit() {
        return parentLabUnit;
    }

    public void setParentLabUnit(LabUnit parentLabUnit) {
        this.parentLabUnit = parentLabUnit;
    }

    public Localization getLocalization() {
        return localization;
    }

    public void setLocalization(Localization localization) {
        this.localization = localization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LabUnit that = (LabUnit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isActive() {
        return "Y".equals(isActive);
    }
}