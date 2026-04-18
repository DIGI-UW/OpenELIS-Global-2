package org.openelisglobal.vector.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "vector_organism_group", schema = "clinlims")
@DynamicUpdate
public class VectorOrganismGroup extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "vector_organism_group_seq_gen")
    @GenericGenerator(name = "vector_organism_group_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "vector_organism_group_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

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
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
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
