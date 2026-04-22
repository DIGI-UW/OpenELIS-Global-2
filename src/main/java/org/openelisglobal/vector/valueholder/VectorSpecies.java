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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.valueholder.Dictionary;

@Entity
@Table(name = "vector_species", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorSpecies extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_species_seq_gen")
    @SequenceGenerator(name = "vector_species_seq_gen", sequenceName = "vector_species_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "genus", length = 100, nullable = false)
    private String genus;

    @Column(name = "species", length = 100)
    private String species;

    @Column(name = "subspecies", length = 100)
    private String subspecies;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private VectorOrganismGroup group;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        schema = "clinlims",
        name = "vector_species_pathogen",
        joinColumns = @JoinColumn(name = "species_id"),
        inverseJoinColumns = @JoinColumn(name = "dictionary_id")
    )
    private Set<Dictionary> pathogensOfInterest = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        schema = "clinlims",
        name = "vector_species_lifecycle_stage",
        joinColumns = @JoinColumn(name = "species_id"),
        inverseJoinColumns = @JoinColumn(name = "dictionary_id")
    )
    private Set<Dictionary> lifecycleStages = new HashSet<>();

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

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSubspecies() {
        return subspecies;
    }

    public void setSubspecies(String subspecies) {
        this.subspecies = subspecies;
    }

    public VectorOrganismGroup getGroup() {
        return group;
    }

    public void setGroup(VectorOrganismGroup group) {
        this.group = group;
    }

    public Set<Dictionary> getPathogensOfInterest() {
        return pathogensOfInterest;
    }

    public void setPathogensOfInterest(Set<Dictionary> pathogensOfInterest) {
        this.pathogensOfInterest = pathogensOfInterest != null ? pathogensOfInterest : new HashSet<>();
    }

    public Set<Dictionary> getLifecycleStages() {
        return lifecycleStages;
    }

    public void setLifecycleStages(Set<Dictionary> lifecycleStages) {
        this.lifecycleStages = lifecycleStages != null ? lifecycleStages : new HashSet<>();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return genus + (species != null ? " " + species : "");
    }
}
