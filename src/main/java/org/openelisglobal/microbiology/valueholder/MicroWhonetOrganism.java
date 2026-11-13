package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One row of WHONET's organism table, held as WHONET publishes it. A code has
 * one current row and may have older ones for names it has carried.
 */
@Entity
@Table(name = "micro_whonet_organism", schema = "clinlims")
public class MicroWhonetOrganism extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    /** The value of taxonomicStatus on the current row of a code. */
    public static final String CURRENT = "C";

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "whonet_org_code", nullable = false)
    private String whonetOrgCode;

    @Column(name = "replaced_by")
    private String replacedBy;

    @Column(name = "organism", nullable = false)
    private String organism;

    @Column(name = "taxonomic_status", nullable = false)
    private String taxonomicStatus;

    @Column(name = "common")
    private String common;

    @Column(name = "common_commensal")
    private String commonCommensal;

    @Column(name = "organism_type")
    private String organismType;

    @Column(name = "anaerobe")
    private String anaerobe;

    @Column(name = "morphology")
    private String morphology;

    @Column(name = "subkingdom_code")
    private String subkingdomCode;

    @Column(name = "family_code")
    private String familyCode;

    @Column(name = "genus_group")
    private String genusGroup;

    @Column(name = "genus_code")
    private String genusCode;

    @Column(name = "species_group")
    private String speciesGroup;

    @Column(name = "serovar_group")
    private String serovarGroup;

    @Column(name = "msf_grp_clin")
    private String msfGrpClin;

    @Column(name = "sct_code")
    private String sctCode;

    @Column(name = "sct_text")
    private String sctText;

    @Column(name = "gbif_taxon_id")
    private String gbifTaxonId;

    @Column(name = "gbif_dataset_id")
    private String gbifDatasetId;

    @Column(name = "gbif_taxonomic_status")
    private String gbifTaxonomicStatus;

    @Column(name = "kingdom")
    private String kingdom;

    @Column(name = "phylum")
    private String phylum;

    @Column(name = "taxon_class")
    private String taxonClass;

    @Column(name = "taxon_order")
    private String taxonOrder;

    @Column(name = "family")
    private String family;

    @Column(name = "genus")
    private String genus;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getWhonetOrgCode() {
        return whonetOrgCode;
    }

    public void setWhonetOrgCode(String whonetOrgCode) {
        this.whonetOrgCode = whonetOrgCode;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getTaxonomicStatus() {
        return taxonomicStatus;
    }

    public void setTaxonomicStatus(String taxonomicStatus) {
        this.taxonomicStatus = taxonomicStatus;
    }

    public String getCommon() {
        return common;
    }

    public void setCommon(String common) {
        this.common = common;
    }

    public String getCommonCommensal() {
        return commonCommensal;
    }

    public void setCommonCommensal(String commonCommensal) {
        this.commonCommensal = commonCommensal;
    }

    public String getOrganismType() {
        return organismType;
    }

    public void setOrganismType(String organismType) {
        this.organismType = organismType;
    }

    public String getAnaerobe() {
        return anaerobe;
    }

    public void setAnaerobe(String anaerobe) {
        this.anaerobe = anaerobe;
    }

    public String getMorphology() {
        return morphology;
    }

    public void setMorphology(String morphology) {
        this.morphology = morphology;
    }

    public String getSubkingdomCode() {
        return subkingdomCode;
    }

    public void setSubkingdomCode(String subkingdomCode) {
        this.subkingdomCode = subkingdomCode;
    }

    public String getFamilyCode() {
        return familyCode;
    }

    public void setFamilyCode(String familyCode) {
        this.familyCode = familyCode;
    }

    public String getGenusGroup() {
        return genusGroup;
    }

    public void setGenusGroup(String genusGroup) {
        this.genusGroup = genusGroup;
    }

    public String getGenusCode() {
        return genusCode;
    }

    public void setGenusCode(String genusCode) {
        this.genusCode = genusCode;
    }

    public String getSpeciesGroup() {
        return speciesGroup;
    }

    public void setSpeciesGroup(String speciesGroup) {
        this.speciesGroup = speciesGroup;
    }

    public String getSerovarGroup() {
        return serovarGroup;
    }

    public void setSerovarGroup(String serovarGroup) {
        this.serovarGroup = serovarGroup;
    }

    public String getMsfGrpClin() {
        return msfGrpClin;
    }

    public void setMsfGrpClin(String msfGrpClin) {
        this.msfGrpClin = msfGrpClin;
    }

    public String getSctCode() {
        return sctCode;
    }

    public void setSctCode(String sctCode) {
        this.sctCode = sctCode;
    }

    public String getSctText() {
        return sctText;
    }

    public void setSctText(String sctText) {
        this.sctText = sctText;
    }

    public String getGbifTaxonId() {
        return gbifTaxonId;
    }

    public void setGbifTaxonId(String gbifTaxonId) {
        this.gbifTaxonId = gbifTaxonId;
    }

    public String getGbifDatasetId() {
        return gbifDatasetId;
    }

    public void setGbifDatasetId(String gbifDatasetId) {
        this.gbifDatasetId = gbifDatasetId;
    }

    public String getGbifTaxonomicStatus() {
        return gbifTaxonomicStatus;
    }

    public void setGbifTaxonomicStatus(String gbifTaxonomicStatus) {
        this.gbifTaxonomicStatus = gbifTaxonomicStatus;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    public String getPhylum() {
        return phylum;
    }

    public void setPhylum(String phylum) {
        this.phylum = phylum;
    }

    public String getTaxonClass() {
        return taxonClass;
    }

    public void setTaxonClass(String taxonClass) {
        this.taxonClass = taxonClass;
    }

    public String getTaxonOrder() {
        return taxonOrder;
    }

    public void setTaxonOrder(String taxonOrder) {
        this.taxonOrder = taxonOrder;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }
}
