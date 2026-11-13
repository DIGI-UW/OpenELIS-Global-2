package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroWhonetAntibiotic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps WHONET's antibiotic table as published, and offers each of its codes as
 * one antibiotic under WHONET's name.
 */
@Service
@Transactional
public class MicroWhonetAntibioticService {
    private static final String ACTOR = "1";

    private final MicroWhonetAntibioticDAO whonetAntibiotics;
    private final MicroAntibioticDAO antibiotics;

    public MicroWhonetAntibioticService(MicroWhonetAntibioticDAO whonetAntibiotics, MicroAntibioticDAO antibiotics) {
        this.whonetAntibiotics = whonetAntibiotics;
        this.antibiotics = antibiotics;
    }

    /**
     * Replaces the table with the file. A row is reported as updated only when the
     * antibiotic named after it changed one the laboratory already had.
     */
    public List<LoadedRow<String>> load(List<CsvRow> rows) {
        whonetAntibiotics.deleteAllRows();
        List<MicroWhonetAntibiotic> entries = new ArrayList<>();
        for (CsvRow row : rows) {
            for (String column : List.of("WHONET_ABX_CODE", "ANTIBIOTIC")) {
                if (row.isBlank(column)) {
                    throw new IllegalArgumentException(column + " is required at line " + row.lineNumber());
                }
            }
            MicroWhonetAntibiotic entry = of(row);
            entry.setSysUserId(ACTOR);
            whonetAntibiotics.insert(entry);
            whonetAntibiotics.evict(entry);
            entries.add(entry);
        }
        // A code listed at several potencies is named after its first WHONET entry.
        Map<String, MicroWhonetAntibiotic> first = new HashMap<>();
        for (MicroWhonetAntibiotic entry : entries) {
            first.merge(entry.getWhonetAbxCode(), entry, (kept, next) -> number(next) < number(kept) ? next : kept);
        }
        List<LoadedRow<String>> loaded = new ArrayList<>();
        for (MicroWhonetAntibiotic entry : entries) {
            boolean changed = first.get(entry.getWhonetAbxCode()) == entry && offer(entry);
            loaded.add(changed ? LoadedRow.updated(entry.getId()) : LoadedRow.created(entry.getId()));
        }
        return loaded;
    }

    /** @return whether a laboratory antibiotic took WHONET's name or code */
    private boolean offer(MicroWhonetAntibiotic entry) {
        String code = entry.getWhonetAbxCode(), name = entry.getAntibiotic();
        var named = antibiotics.findByDisplayNameIgnoreCase(name);
        var coded = antibiotics.findByWhonetCodeIgnoreCase(code);
        boolean sameAntibiotic = named.isPresent() && coded.isPresent()
                && named.get().getId().equals(coded.get().getId());
        if (named.isPresent() && !sameAntibiotic && (coded.isPresent() || named.get().getWhonetCode() != null)) {
            throw new IllegalArgumentException("WHONET names " + code + " \"" + name
                    + "\", but that name belongs to the antibiotic with code " + named.get().getWhonetCode());
        }
        MicroAntibiotic antibiotic = coded.or(() -> named).orElseGet(MicroAntibiotic::new);
        boolean created = coded.isEmpty() && named.isEmpty();
        boolean classMissing = antibiotic.getAntibioticClass() == null || antibiotic.getAntibioticClass().isBlank();
        if (!created && !classMissing && name.equals(antibiotic.getDisplayName())
                && code.equals(antibiotic.getWhonetCode())
                && MicroWhonetOrganismService.SOURCE.equals(antibiotic.getReferenceSource())
                && code.equals(antibiotic.getReferenceCode())) {
            antibiotics.evict(antibiotic);
            return false;
        }
        antibiotic.setDisplayName(name);
        antibiotic.setWhonetCode(code);
        if (classMissing) {
            antibiotic.setAntibioticClass(entry.getAntibioticClass());
        }
        antibiotic.setReferenceSource(MicroWhonetOrganismService.SOURCE);
        antibiotic.setReferenceCode(code);
        antibiotic.setLastUpdatedBy(ACTOR);
        antibiotic.setSysUserId(ACTOR);
        if (created) {
            antibiotic.setSeeded(true);
            antibiotics.insert(antibiotic);
        } else {
            antibiotics.update(antibiotic);
        }
        antibiotics.evict(antibiotic);
        return !created;
    }

    private static int number(MicroWhonetAntibiotic entry) {
        try {
            return Integer.parseInt(entry.getAbxNumber());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static MicroWhonetAntibiotic of(CsvRow row) {
        MicroWhonetAntibiotic antibiotic = new MicroWhonetAntibiotic();
        antibiotic.setWhonetAbxCode(value(row, "WHONET_ABX_CODE"));
        antibiotic.setWhoCode(value(row, "WHO_CODE"));
        antibiotic.setDinCode(value(row, "DIN_CODE"));
        antibiotic.setJacCode(value(row, "JAC_CODE"));
        antibiotic.setEucastCode(value(row, "EUCAST_CODE"));
        antibiotic.setUserCode(value(row, "USER_CODE"));
        antibiotic.setAntibiotic(value(row, "ANTIBIOTIC"));
        antibiotic.setGuidelines(value(row, "GUIDELINES"));
        antibiotic.setClsi(value(row, "CLSI"));
        antibiotic.setEucast(value(row, "EUCAST"));
        antibiotic.setSfm(value(row, "SFM"));
        antibiotic.setSrga(value(row, "SRGA"));
        antibiotic.setBsac(value(row, "BSAC"));
        antibiotic.setDin(value(row, "DIN"));
        antibiotic.setNeo(value(row, "NEO"));
        antibiotic.setAfa(value(row, "AFA"));
        antibiotic.setAbxNumber(value(row, "ABX_NUMBER"));
        antibiotic.setPotency(value(row, "POTENCY"));
        antibiotic.setAtcCode(value(row, "ATC_CODE"));
        antibiotic.setAntibioticClass(value(row, "CLASS"));
        antibiotic.setSubclass(value(row, "SUBCLASS"));
        antibiotic.setProfClass(value(row, "PROF_CLASS"));
        antibiotic.setWhoAware(value(row, "WHO_AWARE"));
        antibiotic.setCiaCategory(value(row, "CIA_CATEGORY"));
        antibiotic.setClsiOrder(value(row, "CLSI_ORDER"));
        antibiotic.setEucastOrder(value(row, "EUCAST_ORDER"));
        antibiotic.setHuman(value(row, "HUMAN"));
        antibiotic.setVeterinary(value(row, "VETERINARY"));
        antibiotic.setAnimalGp(value(row, "ANIMAL_GP"));
        antibiotic.setLoincComp(value(row, "LOINCCOMP"));
        antibiotic.setLoincGen(value(row, "LOINCGEN"));
        antibiotic.setLoincDisk(value(row, "LOINCDISK"));
        antibiotic.setLoincMic(value(row, "LOINCMIC"));
        antibiotic.setLoincEtest(value(row, "LOINCETEST"));
        antibiotic.setLoincSlow(value(row, "LOINCSLOW"));
        antibiotic.setLoincAfb(value(row, "LOINCAFB"));
        antibiotic.setLoincSbt(value(row, "LOINCSBT"));
        antibiotic.setLoincMlc(value(row, "LOINCMLC"));
        antibiotic.setDateEntered(value(row, "DATE_ENTERED"));
        antibiotic.setDateModified(value(row, "DATE_MODIFIED"));
        antibiotic.setComments(value(row, "COMMENTS"));
        return antibiotic;
    }

    private static String value(CsvRow row, String column) {
        String value = row.get(column);
        return value.isEmpty() ? null : value;
    }
}
