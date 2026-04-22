package org.openelisglobal.vector.valueholder;

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

@Entity
@Table(name = "vector_organism_group", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorOrganismGroup extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_organism_group_seq_gen")
    @SequenceGenerator(name = "vector_organism_group_seq_gen", sequenceName = "vector_organism_group_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    @Column(name = "label", length = 60, nullable = false)
    private String label;

    @Column(name = "color_tag", length = 30)
    private String colorTag;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "active")
    private Boolean active;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColorTag() {
        return colorTag;
    }

    public void setColorTag(String colorTag) {
        this.colorTag = colorTag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return label;
    }
}
