package org.openelisglobal.compliance.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * ComplianceStandard value holder representing regulatory compliance standards
 * for environmental and vector testing.
 *
 * Follows constitutional requirements: - Extends BaseObject for audit trail
 * support - Includes FHIR UUID for interoperability - Uses JPA annotations (no
 * XML mappings) - Implements validation annotations
 */
@Entity
@Table(name = "compliance_standard")
public class ComplianceStandard extends BaseObject<String> implements SimpleBaseEntity<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "compliance_standard_seq_gen")
    @GenericGenerator(name = "compliance_standard_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "compliance_standard_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @NotNull
    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;

    @NotBlank(message = "Standard name is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Issuing body is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "issuing_body", nullable = false)
    private String issuingBody;

    @NotBlank(message = "Regulation number is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "regulation_number", nullable = false)
    private String regulationNumber;

    @NotBlank(message = "Version is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "version", nullable = false)
    private String version;

    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @NotBlank(message = "Country/Region is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "country_region", nullable = false)
    private String countryRegion;

    /**
     * FRS data model: applicableSampleTypes is a Set&lt;String&gt; persisted via a
     * join table compliance_standard_sample_type(standard_id, sample_type). Order
     * is preserved with LinkedHashSet so the UI's chip layout stays stable. The
     * legacy comma-delimited String column on this table is dropped by the
     * migration that backfills this collection.
     *
     * Compatibility: getApplicableSampleTypes()/setApplicableSampleTypes(String)
     * remain as derived shims that join/split on commas, and the existing
     * getApplicableSampleTypesList()/setApplicableSampleTypesList(List) are thin
     * wrappers over this set.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "compliance_standard_sample_type", joinColumns = @JoinColumn(name = "standard_id"))
    @Column(name = "sample_type", nullable = false, length = 255)
    // Batch the join-table SELECT so listing N standards fires roughly
    // ceil(N/50) queries on compliance_standard_sample_type instead of N.
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<String> sampleTypes = new LinkedHashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplianceStandardStatus status = ComplianceStandardStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_id")
    @JsonIgnoreProperties({ "supersededByStandard", "parameterGroups" })
    private ComplianceStandard supersededByStandard;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "superseded_by_id", insertable = false, updatable = false)
    private String supersededByStandardId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "regulatory_context", columnDefinition = "TEXT")
    private String regulatoryContext;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "enforcement_authority")
    private String enforcementAuthority;

    @NotNull
    @Column(name = "is_pre_seeded", nullable = false)
    private Boolean isPreSeeded = false;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    // Bidirectional relationship with parameter groups
    @OneToMany(mappedBy = "standard", fetch = FetchType.LAZY)
    @JsonManagedReference("standard-groups")
    private List<ParameterGroup> parameterGroups = new ArrayList<>();

    public ComplianceStandard() {
        super();
        generateFhirUuid();
    }

    public ComplianceStandard(String name, String issuingBody, String regulationNumber, String version) {
        this();
        this.name = name;
        this.issuingBody = issuingBody;
        this.regulationNumber = regulationNumber;
        this.version = version;
    }

    private void generateFhirUuid() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuingBody() {
        return issuingBody;
    }

    public void setIssuingBody(String issuingBody) {
        this.issuingBody = issuingBody;
    }

    public String getRegulationNumber() {
        return regulationNumber;
    }

    public void setRegulationNumber(String regulationNumber) {
        this.regulationNumber = regulationNumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCountryRegion() {
        return countryRegion;
    }

    public void setCountryRegion(String countryRegion) {
        this.countryRegion = countryRegion;
    }

    public Set<String> getSampleTypes() {
        if (sampleTypes == null) {
            sampleTypes = new LinkedHashSet<>();
        }
        return sampleTypes;
    }

    public void setSampleTypes(Set<String> sampleTypes) {
        this.sampleTypes = sampleTypes != null ? sampleTypes : new LinkedHashSet<>();
    }

    /**
     * Compatibility shim: returns the sample types as a comma-delimited string.
     * Returns null when the set is empty so legacy callers see the same shape the
     * old String column produced.
     */
    public String getApplicableSampleTypes() {
        if (sampleTypes == null || sampleTypes.isEmpty()) {
            return null;
        }
        return String.join(",", sampleTypes);
    }

    /**
     * Compatibility shim: parses a comma-delimited string into the underlying set,
     * trimming whitespace and dropping empty tokens. Existing callers (CSV import,
     * controller copy paths) keep working unchanged.
     */
    public void setApplicableSampleTypes(String applicableSampleTypes) {
        if (this.sampleTypes == null) {
            this.sampleTypes = new LinkedHashSet<>();
        } else {
            this.sampleTypes.clear();
        }
        if (applicableSampleTypes == null || applicableSampleTypes.trim().isEmpty()) {
            return;
        }
        for (String token : applicableSampleTypes.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                this.sampleTypes.add(trimmed);
            }
        }
    }

    public ComplianceStandardStatus getStatus() {
        return status;
    }

    public void setStatus(ComplianceStandardStatus status) {
        this.status = status;
    }

    public ComplianceStandard getSupersededByStandard() {
        return supersededByStandard;
    }

    public void setSupersededByStandard(ComplianceStandard supersededByStandard) {
        this.supersededByStandard = supersededByStandard;
    }

    public String getSupersededById() {
        return supersededByStandard != null ? supersededByStandard.getId() : null;
    }

    public void setSupersededById(String supersededById) {
        if (supersededById != null && !supersededById.isBlank()) {
            this.supersededByStandard = new ComplianceStandard();
            this.supersededByStandard.setId(supersededById);
        } else {
            this.supersededByStandard = null;
        }
    }

    public String getSupersededByStandardId() {
        return supersededByStandardId;
    }

    public void setSupersededByStandardId(String supersededByStandardId) {
        this.supersededByStandardId = supersededByStandardId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegulatoryContext() {
        return regulatoryContext;
    }

    public void setRegulatoryContext(String regulatoryContext) {
        this.regulatoryContext = regulatoryContext;
    }

    public String getEnforcementAuthority() {
        return enforcementAuthority;
    }

    public void setEnforcementAuthority(String enforcementAuthority) {
        this.enforcementAuthority = enforcementAuthority;
    }

    public Boolean getIsPreSeeded() {
        return isPreSeeded;
    }

    public void setIsPreSeeded(Boolean isPreSeeded) {
        this.isPreSeeded = isPreSeeded;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.systemUserId = sysUserId != null ? Integer.parseInt(sysUserId) : null;
        super.setSysUserId(sysUserId);
    }

    public List<ParameterGroup> getParameterGroups() {
        return parameterGroups;
    }

    public void setParameterGroups(List<ParameterGroup> parameterGroups) {
        this.parameterGroups = parameterGroups;
    }

    // Helper methods

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isActive() {
        return ComplianceStandardStatus.ACTIVE.equals(status);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSuperseded() {
        return ComplianceStandardStatus.SUPERSEDED.equals(status);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isArchived() {
        return ComplianceStandardStatus.ARCHIVED.equals(status);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<String> getApplicableSampleTypesList() {
        if (sampleTypes == null || sampleTypes.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(sampleTypes);
    }

    public void setApplicableSampleTypesList(List<String> sampleTypes) {
        if (this.sampleTypes == null) {
            this.sampleTypes = new LinkedHashSet<>();
        } else {
            this.sampleTypes.clear();
        }
        if (sampleTypes == null || sampleTypes.isEmpty()) {
            return;
        }
        for (String type : sampleTypes) {
            if (type != null && !type.trim().isEmpty()) {
                this.sampleTypes.add(type.trim());
            }
        }
    }

    /**
     * Returns display name for UI purposes
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDisplayName() {
        return String.format("%s (%s %s)", name, regulationNumber, version);
    }

    /**
     * Returns short identifier for reference
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getShortIdentifier() {
        return String.format("%s-%s", regulationNumber, version);
    }

    // Convenience methods for test compatibility

    /**
     * Alias for setApplicableSampleTypes() for test compatibility
     */
    public void setSampleType(String sampleType) {
        setApplicableSampleTypes(sampleType);
    }

    /**
     * Alias for setExpiryDate() for test compatibility
     */
    public void setExpirationDate(LocalDate expirationDate) {
        setExpiryDate(expirationDate);
    }

    @Override
    public String toString() {
        return "ComplianceStandard{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", regulationNumber='"
                + regulationNumber + '\'' + ", version='" + version + '\'' + ", status=" + status + '}';
    }

    @Getter
    public enum ComplianceStandardStatus {
        DRAFT("Draft", "Standard is being drafted and reviewed"),
        ACTIVE("Active", "Standard is active and available for compliance evaluations"),
        SUPERSEDED("Superseded", "Standard has been replaced by a newer version"),
        ARCHIVED("Archived", "Standard is archived and no longer in active use"),
        SUSPENDED("Suspended", "Standard is temporarily suspended");

        private final String displayName;
        private final String description;

        ComplianceStandardStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

}