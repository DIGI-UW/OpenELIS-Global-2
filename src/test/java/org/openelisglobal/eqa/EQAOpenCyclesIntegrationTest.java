package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQACycleService.OpenCycle;
import org.openelisglobal.eqa.service.EQAOrderEntryValidator;
import org.openelisglobal.eqa.service.EQAOrderEntryValidator.Refusal;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1483: an EQA order joins an open cycle of the scheme its enrollment
 * names, and order entry offers the panel codes the provider sent.
 */
public class EQAOpenCyclesIntegrationTest extends EQASpineTestBase {

    private static final long ANALYTE_HIV_VL = 9802L;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private EQAOrderEntryValidator validator;

    private static final long ENROLLMENT = 9921L;
    private static final long WITHDRAWN = 9922L;

    private void addPanelSample(EQACycle cycle, String code) {
        EQAPanel panel = insertPanel(cycle.getScheme(), p -> {
            p.setCycle(cycle);
            p.setPanelName("Panel " + code);
        });
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode(code);
        sample.setAnalyteId(ANALYTE_HIV_VL);
        sample.setSysUserId(USER);
        eqaPanelSampleDAO.insert(sample);
    }

    @Test
    public void openCycles_ofAScheme_carryTheirPanelCodes_andLeaveOutOtherSchemesAndClosedCycles() {
        EQAProgram malaria = insertScheme("Malaria microscopy", EQASchemeType.INTERNATIONAL_PT, "WHO");
        EQAProgram other = insertScheme("Hematology", EQASchemeType.INTERNATIONAL_PT, "WHO");
        Long open = insertCycle(malaria, 1);
        Long closed = insertCycle(malaria, 2);
        Long elsewhere = insertCycle(other, 1);
        addPanelSample(readBack(open), "MAL-02");
        addPanelSample(readBack(open), "MAL-01");
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = ? WHERE id = ?", EQACycleStatus.SCORED.name(), closed);

        List<OpenCycle> cycles = cycleService.findOpenCycles(" malaria MICROSCOPY ");

        assertEquals(1, cycles.size());
        assertEquals(open, cycles.get(0).id());
        assertEquals(List.of("MAL-01", "MAL-02"), cycles.get(0).panelCodes());
        assertFalse(cycles.stream().anyMatch(cycle -> cycle.id().equals(elsewhere)));
    }

    @Test
    public void aCycleBelongsOnlyToItsOwnScheme() {
        EQAProgram malaria = insertScheme("Malaria microscopy", EQASchemeType.INTERNATIONAL_PT, "WHO");
        EQAProgram other = insertScheme("Hematology", EQASchemeType.INTERNATIONAL_PT, "WHO");
        Long cycle = insertCycle(other, 1);

        assertTrue(cycleService.isOpenCycleOfScheme(cycle, "Hematology"));
        assertFalse(cycleService.isOpenCycleOfScheme(cycle, malaria.getName()));
        assertFalse(cycleService.isOpenCycleOfScheme(424242L, "Hematology"));
    }

    private String refusalCode(String enrollmentId, String cycleId) {
        return validator.check("EQA-NEW-1483", enrollmentId, cycleId).map(Refusal::code).orElse(null);
    }

    @Test
    public void aNewEqaOrder_namesAnActiveEnrollment_andACycleOfItsScheme() {
        seedEnrollment(ENROLLMENT, "Malaria microscopy");
        seedEnrollment(WITHDRAWN, "Hematology");
        jdbc.update("UPDATE clinlims.eqa_lab_program_enrollment SET is_active = false, status = 'Withdrawn'"
                + " WHERE id = ?", WITHDRAWN);
        Long malariaCycle = insertCycle(insertScheme("Malaria microscopy", EQASchemeType.INTERNATIONAL_PT, "WHO"), 1);
        Long otherCycle = insertCycle(insertScheme("Hematology", EQASchemeType.INTERNATIONAL_PT, "WHO"), 1);

        assertEquals("order.eqa.schemeRequired", refusalCode(" ", null));
        assertEquals("order.eqa.schemeUnknown", refusalCode(String.valueOf(WITHDRAWN), null));
        assertEquals("order.eqa.schemeUnknown", refusalCode("abc", null));
        assertEquals("order.eqa.cycleNotInScheme", refusalCode(String.valueOf(ENROLLMENT), String.valueOf(otherCycle)));
        assertEquals(null, refusalCode(String.valueOf(ENROLLMENT), null));
        assertEquals(null, refusalCode(String.valueOf(ENROLLMENT), String.valueOf(malariaCycle)));
        assertEquals("order.eqa.cycleNotInScheme", refusalCode(String.valueOf(ENROLLMENT), "99999999999999999999"));
        assertEquals("order.eqa.schemeUnknown", refusalCode("99999999999999999999", null));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = ? WHERE id = ?", EQACycleStatus.SCORED.name(),
                malariaCycle);
        assertEquals("a scored cycle takes no new samples", "order.eqa.cycleNotInScheme",
                refusalCode(String.valueOf(ENROLLMENT), String.valueOf(malariaCycle)));
    }
}
