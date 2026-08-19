package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-609 [EQA V2.1] — panel lifecycle guards and the sealed-target read rule
 * (FR-V2.1-11 / FR-V2.1-16 / AC-V2.4-03). Runs against the
 * liquibase-provisioned schema, so the encryption converter and constraints are
 * live.
 */
public class EQAPanelLifecycleIntegrationTest extends EQASpineTestBase {

    private static final long ANALYTE = 9801L;

    @Autowired
    private EQAPanelService panelService;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    private EQAPanelSample insertSample(EQAPanel panel, String code, String target) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode(code);
        sample.setBlindCode("BLIND-" + code);
        sample.setAnalyteId(ANALYTE);
        sample.setTargetValue(target);
        sample.setTargetUnit("log10 c/mL");
        sample.setAcceptanceRangeLow(new BigDecimal("4.00000"));
        sample.setAcceptanceRangeHigh(new BigDecimal("5.00000"));
        sample.setSysUserId(USER);
        sample.setId(eqaPanelSampleDAO.insert(sample));
        return sample;
    }

    @Test
    public void seal_movesPreparingPanelToSealed() {
        EQAProgram scheme = insertScheme("Seal happy", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("P1"));
        insertSample(panel, "A01", "4.52");

        EQAPanel sealed = panelService.seal(panel.getId(), USER);

        assertEquals(EQAPanelStatus.SEALED, sealed.getStatus());
        assertEquals(EQAPanelStatus.SEALED,
                eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void seal_refusesPanelWithNoSamples() {
        EQAProgram scheme = insertScheme("Seal empty", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("Empty"));

        try {
            panelService.seal(panel.getId(), USER);
            fail("a panel with no samples must not seal");
        } catch (IllegalArgumentException expected) {
            assertEquals(EQAPanelStatus.PREPARING,
                    eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());
        }
    }

    @Test
    public void seal_refusesBlankTargetValue() {
        EQAProgram scheme = insertScheme("Seal blank", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("Blank target"));
        insertSample(panel, "A01", "  ");

        try {
            panelService.seal(panel.getId(), USER);
            fail("a blank target must not seal (the converter passes blanks through unencrypted)");
        } catch (IllegalArgumentException expected) {
            assertEquals(EQAPanelStatus.PREPARING,
                    eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());
        }
    }

    @Test
    public void seal_refusesInHousePanelWithoutUnblindDate() {
        EQAProgram scheme = insertScheme("Seal in-house", EQASchemeType.IN_HOUSE, null);
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("No unblind date"));
        insertSample(panel, "A01", "4.52");

        try {
            panelService.seal(panel.getId(), USER);
            fail("an in-house panel without an unblind date must not seal");
        } catch (IllegalArgumentException expected) {
            assertEquals(EQAPanelStatus.PREPARING,
                    eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());
        }
    }

    @Test
    public void seal_acceptsInHousePanelWithUnblindDate() {
        EQAProgram scheme = insertScheme("Seal in-house ok", EQASchemeType.IN_HOUSE, null);
        EQAPanel panel = insertPanel(scheme, p -> {
            p.setPanelName("Dated");
            p.setUnblindDate(Date.valueOf("2026-12-01"));
        });
        insertSample(panel, "A01", "4.52");

        assertEquals(EQAPanelStatus.SEALED, panelService.seal(panel.getId(), USER).getStatus());
    }

    @Test
    public void lifecycle_illegalJumpIsRefused() {
        EQAProgram scheme = insertScheme("Bad jump", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("Jump"));

        try {
            panelService.unblind(panel.getId(), USER);
            fail("PREPARING cannot jump straight to UNBLINDED");
        } catch (IllegalStateException expected) {
            assertEquals(EQAPanelStatus.PREPARING,
                    eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());
        }
    }

    @Test
    public void sealedTargets_hiddenFromUnprivilegedCaller() {
        EQAProgram scheme = insertScheme("Sealed read", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("Sealed"));
        insertSample(panel, "A01", "4.52");
        panelService.seal(panel.getId(), USER);

        List<Map<String, Object>> dtos = panelService.getSampleDtos(panel.getId(), false);

        assertEquals(1, dtos.size());
        Map<String, Object> dto = dtos.get(0);
        assertEquals("A01", dto.get("sampleCode"));
        assertEquals("BLIND-A01", dto.get("blindCode"));
        // T-25's pack list prints analyte names, not ids — and a name is not a target,
        // so it travels with a sealed panel.
        assertEquals("the analyte resolves to its name",
                jdbc.queryForObject("SELECT name FROM clinlims.analyte WHERE id = ?", String.class, ANALYTE),
                dto.get("analyteName"));
        assertNull("sealed target must not appear in the DTO", dto.get("targetValue"));
        assertNull("sealed unit must not appear in the DTO", dto.get("targetUnit"));
        assertNull("sealed range low must not appear in the DTO", dto.get("acceptanceRangeLow"));
        assertNull("sealed range high must not appear in the DTO", dto.get("acceptanceRangeHigh"));
        assertEquals(Boolean.FALSE, dto.get("targetsRevealed"));
    }

    @Test
    public void sealedTargets_visibleToPrivilegedCaller() {
        EQAProgram scheme = insertScheme("Privileged read", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQAPanel panel = insertPanel(scheme, p -> p.setPanelName("Sealed priv"));
        insertSample(panel, "A01", "4.52");
        panelService.seal(panel.getId(), USER);

        Map<String, Object> dto = panelService.getSampleDtos(panel.getId(), true).get(0);

        assertEquals("4.52", dto.get("targetValue"));
        assertEquals("log10 c/mL", dto.get("targetUnit"));
        assertEquals(0, new BigDecimal("4.00000").compareTo((BigDecimal) dto.get("acceptanceRangeLow")));
        assertEquals(Boolean.TRUE, dto.get("targetsRevealed"));
    }

    @Test
    public void unblindedTargets_visibleToEveryone() {
        EQAProgram scheme = insertScheme("Unblinded read", EQASchemeType.IN_HOUSE, null);
        EQAPanel panel = insertPanel(scheme, p -> {
            p.setPanelName("Unblinded");
            p.setUnblindDate(Date.valueOf("2026-12-01"));
        });
        insertSample(panel, "A01", "4.52");
        panelService.seal(panel.getId(), USER);
        panelService.distribute(panel.getId(), USER);
        panelService.unblind(panel.getId(), USER);

        Map<String, Object> dto = panelService.getSampleDtos(panel.getId(), false).get(0);

        assertEquals("4.52", dto.get("targetValue"));
        assertEquals(Boolean.TRUE, dto.get("targetsRevealed"));
    }
}
