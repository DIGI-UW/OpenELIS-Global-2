package org.openelisglobal.containertype.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1611 FR-G1, FR-G2: a container a sample is taken in. Its identity is its
 * additive and material; the cap colour is for display and each deployment may
 * edit it. A container type serves one or more domains and yields the sample
 * types listed for it. It is deactivated, never deleted.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "container_type", schema = "clinlims")
public class ContainerType extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "container_type_generator")
    @SequenceGenerator(name = "container_type_generator", sequenceName = "container_type_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "category", length = 60)
    private String category;

    @Column(name = "additive", length = 120)
    private String additive;

    @Column(name = "material", length = 60)
    private String material;

    @Column(name = "nominal_volume", precision = 10, scale = 2)
    private BigDecimal nominalVolume;

    @Column(name = "volume_unit", length = 10)
    private String volumeUnit;

    @Column(name = "cap_colour_hex", length = 7)
    private String capColourHex;

    @Column(name = "cap_colour_name", length = 40)
    private String capColourName;

    @Column(name = "standard_code", length = 60)
    private String standardCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "sys_user_id")
    private Integer changedBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "container_type_domain", schema = "clinlims", joinColumns = @JoinColumn(name = "container_type_id"))
    @Column(name = "domain", length = 20)
    private Set<String> domains = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "container_type_sample_type", schema = "clinlims", joinColumns = @JoinColumn(name = "container_type_id"))
    @Column(name = "type_of_sample_id")
    private Set<Integer> sampleTypeIds = new LinkedHashSet<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAdditive() {
        return additive;
    }

    public void setAdditive(String additive) {
        this.additive = additive;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public BigDecimal getNominalVolume() {
        return nominalVolume;
    }

    public void setNominalVolume(BigDecimal nominalVolume) {
        this.nominalVolume = nominalVolume;
    }

    public String getVolumeUnit() {
        return volumeUnit;
    }

    public void setVolumeUnit(String volumeUnit) {
        this.volumeUnit = volumeUnit;
    }

    public String getCapColourHex() {
        return capColourHex;
    }

    public void setCapColourHex(String capColourHex) {
        this.capColourHex = capColourHex;
    }

    public String getCapColourName() {
        return capColourName;
    }

    public void setCapColourName(String capColourName) {
        this.capColourName = capColourName;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    /** The user who last added, edited, deactivated or activated it. */
    public Integer getChangedBy() {
        return changedBy;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        super.setSysUserId(sysUserId);
        this.changedBy = sysUserId != null && sysUserId.matches("\\d+") ? Integer.valueOf(sysUserId) : null;
    }

    public Set<String> getDomains() {
        return domains;
    }

    public void setDomains(Set<String> domains) {
        this.domains = domains;
    }

    public Set<Integer> getSampleTypeIds() {
        return sampleTypeIds;
    }

    public void setSampleTypeIds(Set<Integer> sampleTypeIds) {
        this.sampleTypeIds = sampleTypeIds;
    }
}
