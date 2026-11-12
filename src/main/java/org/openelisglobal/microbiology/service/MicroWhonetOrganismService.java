package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWhonetOrganism;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps WHONET's organism table as published, and offers each of its current
 * entries as an organism under WHONET's name and code.
 */
@Service
@Transactional
public class MicroWhonetOrganismService {
    public static final String SOURCE = "WHONET";
    private static final String ACTOR = "1";

    private final MicroWhonetOrganismDAO whonetOrganisms;
    private final MicroOrganismDAO organisms;

    public MicroWhonetOrganismService(MicroWhonetOrganismDAO whonetOrganisms, MicroOrganismDAO organisms) {
        this.whonetOrganisms = whonetOrganisms;
        this.organisms = organisms;
    }

    /**
     * The table is the file: loading one replaces every row with the file's, so
     * every row is reported as created. A row is reported as updated when it gave
     * an existing organism WHONET's name or code.
     */
    public List<LoadedRow<String>> load(List<CsvRow> rows) {
        whonetOrganisms.deleteAllRows();
        List<LoadedRow<String>> loaded = new ArrayList<>();
        for (CsvRow row : rows) {
            for (String column : List.of("WHONET_ORG_CODE", "ORGANISM", "TAXONOMIC_STATUS")) {
                if (row.isBlank(column)) {
                    throw new IllegalArgumentException(column + " is required at line " + row.lineNumber());
                }
            }
            MicroWhonetOrganism entry = of(row);
            entry.setSysUserId(ACTOR);
            whonetOrganisms.insert(entry);
            whonetOrganisms.evict(entry);
            boolean renamed = MicroWhonetOrganism.CURRENT.equals(entry.getTaxonomicStatus()) && offer(entry);
            loaded.add(renamed ? LoadedRow.updated(entry.getId()) : LoadedRow.created(entry.getId()));
        }
        return loaded;
    }

    /**
     * The organism of a WHONET code carries the code's current name.
     *
     * @return whether an organism the laboratory already had took WHONET's name or
     *         code
     */
    private boolean offer(MicroWhonetOrganism entry) {
        String code = entry.getWhonetOrgCode(), name = entry.getOrganism();
        var named = organisms.findByDisplayNameIgnoreCase(name);
        var coded = organisms.findByWhonetCodeIgnoreCase(code);
        boolean sameOrganism = named.isPresent() && coded.isPresent()
                && named.get().getId().equals(coded.get().getId());
        if (named.isPresent() && !sameOrganism && (coded.isPresent() || named.get().getWhonetCode() != null)) {
            throw new IllegalArgumentException("WHONET names " + code + " \"" + name
                    + "\", but that name belongs to the organism with code " + named.get().getWhonetCode());
        }
        MicroOrganism organism = coded.or(() -> named).orElseGet(MicroOrganism::new);
        boolean created = coded.isEmpty() && named.isEmpty();
        if (!created && name.equals(organism.getDisplayName()) && code.equals(organism.getWhonetCode())
                && SOURCE.equals(organism.getReferenceSource()) && code.equals(organism.getReferenceCode())) {
            organisms.evict(organism);
            return false;
        }
        organism.setDisplayName(name);
        organism.setWhonetCode(code);
        organism.setReferenceSource(SOURCE);
        organism.setReferenceCode(code);
        organism.setLastUpdatedBy(ACTOR);
        organism.setSysUserId(ACTOR);
        if (created) {
            organism.setSeeded(true);
            organisms.insert(organism);
        } else {
            organisms.update(organism);
        }
        organisms.evict(organism);
        return !created;
    }

    private static MicroWhonetOrganism of(CsvRow row) {
        MicroWhonetOrganism organism = new MicroWhonetOrganism();
        organism.setWhonetOrgCode(value(row, "WHONET_ORG_CODE"));
        organism.setReplacedBy(value(row, "REPLACED_BY"));
        organism.setOrganism(value(row, "ORGANISM"));
        organism.setTaxonomicStatus(value(row, "TAXONOMIC_STATUS"));
        organism.setCommon(value(row, "COMMON"));
        organism.setCommonCommensal(value(row, "COMMON_COMMENSAL"));
        organism.setOrganismType(value(row, "ORGANISM_TYPE"));
        organism.setAnaerobe(value(row, "ANAEROBE"));
        organism.setMorphology(value(row, "MORPHOLOGY"));
        organism.setSubkingdomCode(value(row, "SUBKINGDOM_CODE"));
        organism.setFamilyCode(value(row, "FAMILY_CODE"));
        organism.setGenusGroup(value(row, "GENUS_GROUP"));
        organism.setGenusCode(value(row, "GENUS_CODE"));
        organism.setSpeciesGroup(value(row, "SPECIES_GROUP"));
        organism.setSerovarGroup(value(row, "SEROVAR_GROUP"));
        organism.setMsfGrpClin(value(row, "MSF_GRP_CLIN"));
        organism.setSctCode(value(row, "SCT_CODE"));
        organism.setSctText(value(row, "SCT_TEXT"));
        organism.setGbifTaxonId(value(row, "GBIF_TAXON_ID"));
        organism.setGbifDatasetId(value(row, "GBIF_DATASET_ID"));
        organism.setGbifTaxonomicStatus(value(row, "GBIF_TAXONOMIC_STATUS"));
        organism.setKingdom(value(row, "KINGDOM"));
        organism.setPhylum(value(row, "PHYLUM"));
        organism.setTaxonClass(value(row, "CLASS"));
        organism.setTaxonOrder(value(row, "ORDER"));
        organism.setFamily(value(row, "FAMILY"));
        organism.setGenus(value(row, "GENUS"));
        return organism;
    }

    private static String value(CsvRow row, String column) {
        String value = row.get(column);
        return value.isEmpty() ? null : value;
    }
}
