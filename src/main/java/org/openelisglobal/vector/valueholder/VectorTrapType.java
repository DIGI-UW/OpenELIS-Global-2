package org.openelisglobal.vector.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "vector_trap_type", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorTrapType extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_trap_type_seq_gen")
    @SequenceGenerator(name = "vector_trap_type_seq_gen", sequenceName = "vector_trap_type_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        schema = "clinlims",
        name = "vector_trap_type_group",
        joinColumns = @JoinColumn(name = "trap_type_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<VectorOrganismGroup> groups = new HashSet<>();

    @Column(name = "description", length = 255)
    private String description;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<VectorOrganismGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<VectorOrganismGroup> groups) {
        this.groups = groups != null ? groups : new HashSet<>();
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
