package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reconciles source identities without replacing laboratory choices. */
@Service
@Transactional
public class MicroReferenceConceptService {
    private static final String ACTOR = "1";
    private final MicroOrganismDAO organisms;
    private final MicroAntibioticDAO antibiotics;

    public MicroReferenceConceptService(MicroOrganismDAO organisms, MicroAntibioticDAO antibiotics) {
        this.organisms = organisms;
        this.antibiotics = antibiotics;
    }

    public void validateIdentities(java.util.List<CsvRow> rows) {
        var identities = new java.util.HashSet<java.util.List<String>>();
        for (CsvRow row : rows) {
            if (!identities.add(java.util.List.of(required(row, "source", 80), required(row, "code", 120)))) {
                throw new IllegalArgumentException("Duplicate source identity at line " + row.lineNumber());
            }
        }
    }

    /**
     * Each organism leaves the session once it is saved, so a file of tens of
     * thousands of rows costs the same per row as a short one.
     */
    public LoadedRow<String> loadOrganism(CsvRow row) {
        String source = required(row, "source", 80), code = required(row, "code", 120);
        String name = required(row, "displayName", 255), whonet = optional(row, "whonetCode", 50);
        var existing = organisms.findByReference(source, code);
        if (existing.isPresent()) {
            MicroOrganism known = existing.get();
            if (whonet != null && !whonet.equalsIgnoreCase(known.getWhonetCode())) {
                organisms.findByWhonetCodeIgnoreCase(whonet).ifPresent(holder -> {
                    throw new IllegalArgumentException(
                            "WHONET code " + whonet + " is already used by " + holder.getDisplayName());
                });
            }
            boolean changed = applyPackageValues(known, row, whonet);
            if (changed) {
                known.setLastUpdatedBy(ACTOR);
                organisms.update(known);
            }
            organisms.evict(known);
            return changed ? LoadedRow.updated(known.getId())
                    : LoadedRow.skipped("Existing source identity; local values preserved");
        }
        var byName = organisms.findByDisplayNameIgnoreCase(name);
        var byCode = whonet == null ? Optional.<MicroOrganism>empty() : organisms.findByWhonetCodeIgnoreCase(whonet);
        if (byName.isPresent() && byCode.isPresent() && !byName.get().getId().equals(byCode.get().getId())) {
            throw new IllegalArgumentException("Conflicting organism name and code: " + code);
        }
        MicroOrganism organism = byCode.or(() -> byName).orElseGet(MicroOrganism::new);
        if (organism.getReferenceCode() != null)
            throw new IllegalArgumentException("Organism already has a different source identity");
        boolean created = byName.isEmpty() && byCode.isEmpty();
        if (created) {
            organism.setDisplayName(name);
            organism.setGramStain(optional(row, "gramStain", 20));
            organism.setSeeded(true);
        }
        applyPackageValues(organism, row, whonet);
        organism.setReferenceSource(source);
        organism.setReferenceCode(code);
        organism.setLastUpdatedBy(ACTOR);
        organism.setSysUserId(ACTOR);
        if (created)
            organisms.insert(organism);
        else
            organisms.update(organism);
        organisms.evict(organism);
        return created ? LoadedRow.created(organism.getId()) : LoadedRow.updated(organism.getId());
    }

    /**
     * The package is the source of an organism's WHONET code and name, rank and
     * prevalence. A group is the laboratory's once it has set one.
     */
    private boolean applyPackageValues(MicroOrganism organism, CsvRow row, String whonet) {
        boolean changed = false;
        if (whonet != null && !whonet.equals(organism.getWhonetCode())) {
            organism.setWhonetCode(whonet);
            changed = true;
        }
        String whonetName = optional(row, "whonetName", 255);
        if (whonetName != null && !whonetName.equals(organism.getWhonetName())) {
            organism.setWhonetName(whonetName);
            changed = true;
        }
        String rank = optional(row, "rank", 40);
        if (rank != null && !rank.equals(organism.getTaxonomicRank())) {
            organism.setTaxonomicRank(rank);
            changed = true;
        }
        BigDecimal prevalence = prevalence(row);
        if (prevalence != null
                && (organism.getPrevalence() == null || prevalence.compareTo(organism.getPrevalence()) != 0)) {
            organism.setPrevalence(prevalence);
            changed = true;
        }
        String group = optional(row, "organism_group", 255);
        if (group != null && (organism.getOrganismGroup() == null || organism.getOrganismGroup().isBlank())) {
            organism.setOrganismGroup(group);
            changed = true;
        }
        return changed;
    }

    private static BigDecimal prevalence(CsvRow row) {
        String value = optional(row, "prevalence", 10);
        if (value == null)
            return null;
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.signum() >= 0 && number.compareTo(new BigDecimal("100")) < 0)
                return number;
        } catch (NumberFormatException e) {
            // Reported below with the value that failed.
        }
        throw new IllegalArgumentException("prevalence must be a number from 0 to under 100: " + value);
    }

    public LoadedRow<String> loadAntibiotic(CsvRow row) {
        String source = required(row, "source", 80), code = required(row, "code", 120);
        String name = required(row, "displayName", 255), whonet = required(row, "whonetCode", 50);
        var existing = antibiotics.findByReference(source, code);
        if (existing.isPresent())
            return LoadedRow.skipped("Existing source identity; local values preserved");
        var byName = antibiotics.findByDisplayNameIgnoreCase(name);
        var byCode = antibiotics.findByWhonetCodeIgnoreCase(whonet);
        if (byName.isPresent() && byCode.isPresent() && !byName.get().getId().equals(byCode.get().getId())) {
            throw new IllegalArgumentException("Conflicting antimicrobial name and code: " + code);
        }
        MicroAntibiotic antibiotic = byCode.or(() -> byName).orElseGet(MicroAntibiotic::new);
        if (antibiotic.getReferenceCode() != null)
            throw new IllegalArgumentException("Antimicrobial already has a different source identity");
        boolean created = byName.isEmpty() && byCode.isEmpty();
        if (created) {
            antibiotic.setDisplayName(name);
            antibiotic.setWhonetCode(whonet);
            antibiotic.setAntibioticClass(optional(row, "antibioticClass", 255));
            antibiotic.setSeeded(true);
        }
        antibiotic.setReferenceSource(source);
        antibiotic.setReferenceCode(code);
        antibiotic.setLastUpdatedBy(ACTOR);
        antibiotic.setSysUserId(ACTOR);
        if (created)
            antibiotics.insert(antibiotic);
        else
            antibiotics.update(antibiotic);
        return created ? LoadedRow.created(antibiotic.getId()) : LoadedRow.updated(antibiotic.getId());
    }

    private static String required(CsvRow row, String key, int max) {
        String value = optional(row, key, max);
        if (value == null)
            throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static String optional(CsvRow row, String key, int max) {
        String value = row.get(key).trim();
        if (value.length() > max)
            throw new IllegalArgumentException(key + " is too long");
        return value.isEmpty() ? null : value;
    }

}
