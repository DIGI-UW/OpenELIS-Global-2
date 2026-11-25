package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.openelisglobal.microbiology.service.MicroTbResistanceClassifier.*;

import java.util.*;
import org.junit.Test;

public class MicroTbResistanceClassifierTest {
    private Drug drug(Role role, String value) {
        return new Drug(UUID.randomUUID().toString(), role, value);
    }

    private Category classify(String rif, String inh, String other, String fq, String groupA) {
        return MicroTbResistanceClassifier.classify(List.of(drug(Role.RIFAMPICIN, rif), drug(Role.ISONIAZID, inh),
                drug(Role.OTHER_FIRST_LINE, other), drug(Role.FLUOROQUINOLONE, fq), drug(Role.XDR_GROUP_A, groupA)));
    }

    @Test
    public void firstLineCategoriesRequireCompleteEvidenceAndCountDistinctDrugs() {
        assertEquals(Category.PAN_SUSCEPTIBLE,
                classify("SUSCEPTIBLE", "SUSCEPTIBLE", "SUSCEPTIBLE", "INDETERMINATE", "INDETERMINATE"));
        assertEquals(Category.MONO_RESISTANT,
                classify("SUSCEPTIBLE", "RESISTANT", "SUSCEPTIBLE", "SUSCEPTIBLE", "SUSCEPTIBLE"));
        assertEquals(Category.POLY_RESISTANT,
                classify("SUSCEPTIBLE", "RESISTANT", "RESISTANT", "SUSCEPTIBLE", "SUSCEPTIBLE"));
        assertEquals(Category.INCOMPLETE,
                classify("SUSCEPTIBLE", "SUSCEPTIBLE", "INDETERMINATE", "SUSCEPTIBLE", "SUSCEPTIBLE"));
        assertEquals(Category.INCOMPLETE, MicroTbResistanceClassifier.classify(List.of()));
    }

    @Test
    public void advancedResistanceUsesDrugRolesAndWhoRifampicinResistantDefinitions() {
        assertEquals(Category.RR,
                classify("RESISTANT", "SUSCEPTIBLE", "INDETERMINATE", "INDETERMINATE", "INDETERMINATE"));
        assertEquals(Category.MDR, classify("RESISTANT", "RESISTANT", "INDETERMINATE", "SUSCEPTIBLE", "SUSCEPTIBLE"));
        assertEquals(Category.PRE_XDR,
                classify("RESISTANT", "SUSCEPTIBLE", "INDETERMINATE", "RESISTANT", "SUSCEPTIBLE"));
        assertEquals(Category.XDR, classify("RESISTANT", "INDETERMINATE", "INDETERMINATE", "RESISTANT", "RESISTANT"));
        assertEquals(Category.DISCORDANT, classify("RESISTANT", "RESISTANT", "DISCORDANT", "RESISTANT", "RESISTANT"));
    }

    @Test
    public void GroupAResistanceAloneDoesNotPromoteMdrToXdr() {
        assertEquals(Category.MDR, classify("RESISTANT", "RESISTANT", "SUSCEPTIBLE", "SUSCEPTIBLE", "RESISTANT"));
    }
}
