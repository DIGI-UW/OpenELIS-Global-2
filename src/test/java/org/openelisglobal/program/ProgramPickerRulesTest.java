package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.program.service.ProgramPickerRules;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.test.valueholder.TestSection;

public class ProgramPickerRulesTest {

    private static TestSection section(String id) {
        TestSection section = new TestSection();
        section.setId(id);
        return section;
    }

    private static Program program(String domain, String isActive, String legacySectionId, String... junctionIds) {
        Program program = new Program();
        program.setDomain(domain);
        program.setIsActive(isActive);
        if (legacySectionId != null) {
            program.setTestSection(section(legacySectionId));
        }
        program.setLabUnits(new HashSet<>());
        for (String id : junctionIds) {
            program.getLabUnits().add(section(id));
        }
        return program;
    }

    @Test
    public void isActive_onlyAnExplicitNIsDeactivated() {
        assertTrue(ProgramPickerRules.isActive(program("CLINICAL", "Y", null)));
        assertTrue(ProgramPickerRules.isActive(program("CLINICAL", null, null)));
        assertFalse(ProgramPickerRules.isActive(program("CLINICAL", "N", null)));
        assertFalse(ProgramPickerRules.isActive(program("CLINICAL", "n", null)));
        assertFalse(ProgramPickerRules.isActive(null));
    }

    @Test
    public void offerableForDomain_matchesTheOrderDomainAndAcceptsLegacyCodes() {
        Program environmental = program("ENVIRONMENTAL", "Y", null);
        assertTrue(ProgramPickerRules.offerableForDomain(environmental, Domain.ENVIRONMENTAL));
        assertFalse(ProgramPickerRules.offerableForDomain(environmental, Domain.CLINICAL));
        assertTrue("no order domain means every program is offerable",
                ProgramPickerRules.offerableForDomain(environmental, null));
        assertTrue("a legacy one-letter stored code still resolves",
                ProgramPickerRules.offerableForDomain(program("E", "Y", null), Domain.ENVIRONMENTAL));
        assertTrue("an unreadable stored domain is offered everywhere rather than nowhere",
                ProgramPickerRules.offerableForDomain(program("", "Y", null), Domain.VECTOR));
    }

    @Test
    public void labUnitIds_prefersTheJunctionAndSortsNumerically() {
        assertEquals(Arrays.asList("9", "10", "163"),
                ProgramPickerRules.labUnitIds(program("CLINICAL", "Y", "5", "163", "9", "10")));
        assertEquals("legacy single FK is the fallback while the junction is empty", Collections.singletonList("5"),
                ProgramPickerRules.labUnitIds(program("CLINICAL", "Y", "5")));
        assertTrue(ProgramPickerRules.labUnitIds(program("CLINICAL", "Y", null)).isEmpty());
    }

    @Test
    public void servesAnyLabUnit_offersUnassignedProgramsToEveryoneAndMatchesAnyJunctionRow() {
        List<String> userUnits = Arrays.asList("10", "11");
        assertTrue(ProgramPickerRules.servesAnyLabUnit(program("CLINICAL", "Y", null), userUnits));
        assertTrue(ProgramPickerRules.servesAnyLabUnit(program("CLINICAL", "Y", null, "3", "11"), userUnits));
        assertFalse(ProgramPickerRules.servesAnyLabUnit(program("CLINICAL", "Y", null, "3", "4"), userUnits));
        assertTrue("the legacy FK still counts",
                ProgramPickerRules.servesAnyLabUnit(program("CLINICAL", "Y", "10"), userUnits));
        assertFalse(ProgramPickerRules.servesAnyLabUnit(program("CLINICAL", "Y", "10"), null));
    }

    @Test
    public void firstLabUnit_isTheLowestIdSoTheLegacyFkIsDeterministic() {
        Program program = program("CLINICAL", "Y", null, "163", "9", "10");
        assertEquals("9", ProgramPickerRules.firstLabUnit(program.getLabUnits()).getId());
        assertNull(ProgramPickerRules.firstLabUnit(Collections.emptySet()));
        assertNull(ProgramPickerRules.firstLabUnit(null));
    }
}
