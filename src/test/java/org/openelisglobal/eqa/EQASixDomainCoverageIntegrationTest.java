package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-609 [EQA V2.1 / T-08] — AC-V2.1-23: all six ePT-validated test domains
 * must be expressible with standard test-catalog entries plus one scheme row,
 * with no domain-specific schema branches.
 *
 * <p>
 * The invariant this protects is a design one: the ePT crosswalk (§3 item A)
 * calls fork-per-scheme the anti-pattern to avoid. If a domain could only be
 * represented by adding a column or a table, that would show up here as a test
 * that cannot be written against the shared schema. Every domain below —
 * qualitative, quantitative, categorical and semi-quantitative alike — travels
 * through the same eqa_program → eqa_cycle → eqa_round → eqa_participant_result
 * path, differing only in data.
 *
 * <p>
 * The result-value shapes are why eqa_participant_result.result_value is a
 * VARCHAR rather than V1's DECIMAL(15,5): "Reactive", "Recent" and "3+" are not
 * numbers, and three of the six domains report exactly that way.
 */
public class EQASixDomainCoverageIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String USER = "1";
    private static final long ADMIN_USER_ID = 1L;
    private static final long ENROLLMENT_ID = 9902L;

    /** One row per domain scenario: analyte, catalog test, and how it reports. */
    private static final List<Domain> DOMAINS = Arrays.asList(
            new Domain("HIV serology (rapid)", 9801L, 9701L, EQASchemeType.INTERNATIONAL_PT, "Reactive", null),
            new Domain("HIV viral load", 9802L, 9702L, EQASchemeType.INTERNATIONAL_PT, "4.52", "log10 c/mL"),
            new Domain("Early infant diagnosis", 9803L, 9703L, EQASchemeType.REGIONAL_PT, "DETECTED", null),
            new Domain("HIV recency", 9804L, 9704L, EQASchemeType.REGIONAL_PT, "Recent", null),
            new Domain("COVID-19 antigen", 9805L, 9705L, EQASchemeType.INTER_LAB_SPLIT, "Positive", null),
            new Domain("COVID-19 RT-PCR", 9806L, 9706L, EQASchemeType.INTER_LAB_SPLIT, "28.4", "Ct"),
            new Domain("TB microscopy", 9807L, 9707L, EQASchemeType.IN_HOUSE, "3+", "AFB/HPF"),
            new Domain("TB molecular (Xpert)", 9808L, 9708L, EQASchemeType.IN_HOUSE, "MTB DETECTED", null));

    @Autowired
    private EQAProgramService eqaProgramService;

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

    @Autowired
    private EQAParticipantResultDAO eqaParticipantResultDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/eqa-cycle-spine.xml");
        clean();
        jdbc.update("INSERT INTO clinlims.eqa_lab_program_enrollment"
                + " (id, program_name, provider, is_active, created_date, sys_user_id, lastupdated)"
                + " VALUES (?, 'Six domain enrollment', 'NHLS', true, now(), ?, now())", ENROLLMENT_ID, USER);
    }

    @After
    public void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.eqa_round");
        jdbc.update("DELETE FROM clinlims.eqa_cycle");
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE id = ?", ENROLLMENT_ID);
        jdbc.update("DELETE FROM clinlims.eqa_program_test");
        jdbc.update("DELETE FROM clinlims.eqa_program");
    }

    @Test
    public void everyDomainIsConfigurableWithCatalogEntriesAndOneSchemeRow() {
        List<Long> resultIds = new ArrayList<>();

        for (Domain domain : DOMAINS) {
            EQAProgram scheme = new EQAProgram();
            scheme.setName(domain.name + " PT");
            scheme.setSchemeType(domain.schemeType);
            // BR-004: only the in-house domains may omit a provider.
            scheme.setProvider(domain.schemeType == EQASchemeType.IN_HOUSE ? null : "NHLS");
            scheme.setSysUserId(USER);
            Long schemeId = eqaProgramService.insert(scheme);
            scheme.setId(schemeId);

            // The domain's analyte reaches the scheme through the standard test
            // catalog — no EQA-side test/analyte table exists or is needed.
            EQAProgramTest assignment = eqaProgramService.assignTest(schemeId, domain.testId);
            assertEquals("assignment must point at the catalog test", Long.valueOf(domain.testId),
                    assignment.getTestId());

            EQACycle cycle = new EQACycle();
            cycle.setScheme(scheme);
            cycle.setCycleNumber(1);
            cycle.setCycleName(domain.name + " 2026");
            cycle.setCreatedBy(systemUser());
            cycle.setSysUserId(USER);
            EQACycle persistedCycle = eqaCycleDAO.get(eqaCycleDAO.insert(cycle)).orElseThrow(AssertionError::new);

            EQARound round = new EQARound();
            round.setCycle(persistedCycle);
            round.setRoundNumber(1);
            round.setSysUserId(USER);
            EQARound persistedRound = eqaRoundDAO.get(eqaRoundDAO.insert(round)).orElseThrow(AssertionError::new);

            EQAParticipantResult result = new EQAParticipantResult();
            result.setCycle(persistedCycle);
            result.setRound(persistedRound);
            result.setLabEnrollmentId(ENROLLMENT_ID);
            result.setAnalyteId(domain.analyteId);
            result.setResultValue(domain.resultValue);
            result.setResultUnit(domain.resultUnit);
            result.setSysUserId(USER);
            resultIds.add(eqaParticipantResultDAO.insert(result));
        }

        assertEquals("one scenario per domain row", DOMAINS.size(), resultIds.size());

        for (int i = 0; i < DOMAINS.size(); i++) {
            Domain domain = DOMAINS.get(i);
            EQAParticipantResult read = eqaParticipantResultDAO.get(resultIds.get(i)).orElseThrow(AssertionError::new);
            assertEquals(domain.name + " result value must survive round-trip verbatim", domain.resultValue,
                    read.getResultValue());
            assertEquals(domain.name + " unit must survive round-trip verbatim", domain.resultUnit,
                    read.getResultUnit());
            assertEquals(domain.name + " must resolve to its own analyte", Long.valueOf(domain.analyteId),
                    read.getAnalyteId());
        }

        // The point of the AC: the eight scenarios above needed exactly the
        // shared tables, one scheme row each, and zero domain-specific columns.
        Integer schemeCount = jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_program", Integer.class);
        assertEquals(Integer.valueOf(DOMAINS.size()), schemeCount);
        assertTrue("in-house domains coexist with external ones in one table", jdbc
                .queryForObject("SELECT count(DISTINCT scheme_type) FROM clinlims.eqa_program", Integer.class) == 4);
    }

    @Test
    public void quantitativeAndQualitativeResultsShareOneColumn() {
        // Fixture-integrity guard, not the schema guard: it pins DOMAINS so the
        // sibling test above keeps exercising non-numeric shapes. A DECIMAL
        // result_value is caught there, by the insert itself.
        List<String> nonNumeric = new ArrayList<>();
        for (Domain domain : DOMAINS) {
            if (!domain.resultValue.matches("-?\\d+(\\.\\d+)?")) {
                nonNumeric.add(domain.resultValue);
            }
        }
        assertEquals("qualitative, categorical and semi-quantitative shapes must all be represented",
                Arrays.asList("Reactive", "DETECTED", "Recent", "Positive", "3+", "MTB DETECTED"), nonNumeric);
    }

    private SystemUser systemUser() {
        return systemUserService.get(String.valueOf(ADMIN_USER_ID));
    }

    private static final class Domain {
        private final String name;
        private final long analyteId;
        private final long testId;
        private final EQASchemeType schemeType;
        private final String resultValue;
        private final String resultUnit;

        private Domain(String name, long analyteId, long testId, EQASchemeType schemeType, String resultValue,
                String resultUnit) {
            this.name = name;
            this.analyteId = analyteId;
            this.testId = testId;
            this.schemeType = schemeType;
            this.resultValue = resultValue;
            this.resultUnit = resultUnit;
        }
    }
}
