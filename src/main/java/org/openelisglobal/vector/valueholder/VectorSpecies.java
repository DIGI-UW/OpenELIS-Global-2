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
@Table(name = "vector_species", schema = "clinlims")
@DynamicUpdate
public class VectorSpecies extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "vector_species_seq_gen")
    @GenericGenerator(name = "vector_species_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "vector_species_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "genus", length = 100, nullable = false)
    private String genus;

    @Column(name = "species", length = 100)
    private String species;

    @Column(name = "subspecies", length = 100)
    private String subspecies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private VectorOrganismGroup group;

    @Column(name = "pathogens_of_interest", length = 255)
    private String pathogensOfInterest;

    @Column(name = "lifecycle_stages", length = 100)
    private String lifecycleStages;

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

    public String getPathogensOfInterest() {
        return pathogensOfInterest;
    }

    public void setPathogensOfInterest(String pathogensOfInterest) {
        this.pathogensOfInterest = pathogensOfInterest;
    }

    public String getLifecycleStages() {
        return lifecycleStages;
    }

    public void setLifecycleStages(String lifecycleStages) {
        this.lifecycleStages = lifecycleStages;
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
