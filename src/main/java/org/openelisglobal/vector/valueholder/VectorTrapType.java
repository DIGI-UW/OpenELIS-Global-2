package org.openelisglobal.vector.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "vector_trap_type", schema = "clinlims")
@DynamicUpdate
public class VectorTrapType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "vector_trap_type_seq_gen")
    @GenericGenerator(name = "vector_trap_type_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "vector_trap_type_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private VectorOrganismGroup group;

    @Column(name = "description", length = 255)
    private String description;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VectorOrganismGroup getGroup() {
        return group;
    }

    public void setGroup(VectorOrganismGroup group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return name;
    }
}
