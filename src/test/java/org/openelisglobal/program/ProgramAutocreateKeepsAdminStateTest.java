package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.program.service.ProgramAutocreateService;
import org.openelisglobal.program.service.ProgramPickerRules;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The startup seeding rebuilds each bundled programme from its JSON and
 * re-saves it. Before OGC-781 that reset the Programs V2 columns of every
 * programme an admin had not edited yet: domain back to Clinical, deactivated
 * programmes back to active, and every {@code program_lab_unit} row gone.
 * Seeding must carry those over.
 */
public class ProgramAutocreateKeepsAdminStateTest extends BaseWebContextSensitiveTest {

    private static final String[] QUESTIONNAIRE_TABLES = { "questionnaire_response_answer",
            "questionnaire_response_item", "questionnaire_response", "questionnaire_item_initial",
            "questionnaire_answer_option", "questionnaire_item", "questionnaire" };
    private static final String BUNDLED_CODE = "PATH";

    @Autowired
    private ProgramAutocreateService autocreateService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private DataSource dataSource;

    private ProgramAutocreateService target;
    private List<String> bundledCodes;
    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        target = AopTestUtils.getUltimateTargetObject(autocreateService);
        reset(fhirConfig, fhirUtil);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("");
        when(fhirUtil.getFhirParser()).thenReturn(FhirContext.forR4Cached().newJsonParser());
        bundledCodes = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:programs/*.json")) {
            try (InputStream in = resource.getInputStream()) {
                bundledCodes.add(mapper.readTree(in).path("program").path("code").asText());
            }
        }
        executeDataSetWithStateManagement("testdata/program.xml");
        resyncSequence("clinlims.program_seq", "clinlims.program");
        ensureReferenceTables("program");
        jdbc = new JdbcTemplate(dataSource);
        removeSeededRows();
    }

    @After
    public void tearDown() throws Exception {
        ReflectionTestUtils.setField(target, "autocreateOn", false);
        reset(fhirConfig, fhirUtil);
        removeSeededRows();
    }

    @Test
    public void reseeding_keepsTheDomainLifecycleAndLabUnitsOfAnUneditedProgramme() {
        assertTrue("the bundled " + BUNDLED_CODE + " programme must be on the classpath",
                bundledCodes.contains(BUNDLED_CODE));
        Program classified = new Program();
        classified.setCode(BUNDLED_CODE);
        classified.setProgramName("Histopathology");
        classified.setManuallyChanged(false);
        classified.setDomain("ENVIRONMENTAL");
        classified.setIsActive("N");
        classified.setLabUnits(new HashSet<>(List.of(testSectionService.get("2"))));
        classified.setTestSection(testSectionService.get("2"));
        classified.setQuestionnaireUUID(UUID.randomUUID());
        programService.save(classified);
        ReflectionTestUtils.setField(target, "autocreateOn", true);

        ReflectionTestUtils.invokeMethod(target, "doAutocreateProgram");

        Program after = programService.getMatch("code", BUNDLED_CODE).orElseThrow();
        assertEquals("ENVIRONMENTAL", after.getDomain());
        assertEquals("N", after.getIsActive());
        assertTrue("the junction row survives the reseed", ProgramPickerRules.labUnitIds(after).contains("2"));
        assertEquals(1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM clinlims.program_lab_unit WHERE program_id = ? AND test_section_id = 2",
                        Integer.class, Integer.valueOf(after.getId())).intValue());
    }

    /**
     * A bundled programme that declares its own domain is seeded with it, so a
     * fresh install offers Vector Field Survey on vector order entry instead of
     * filing it under Clinical like an unknown programme.
     */
    @Test
    public void seeding_appliesTheDomainDeclaredByTheBundledDefinition() {
        assertTrue("the bundled VFS programme must be on the classpath", bundledCodes.contains("VFS"));
        ReflectionTestUtils.setField(target, "autocreateOn", true);

        ReflectionTestUtils.invokeMethod(target, "doAutocreateProgram");

        assertEquals("VECTOR", programService.getMatch("code", "VFS").orElseThrow().getDomain());
        assertEquals("a programme that declares no domain stays on the Clinical default", "CLINICAL",
                programService.getMatch("code", BUNDLED_CODE).orElseThrow().getDomain());
    }

    private void removeSeededRows() throws Exception {
        cleanRowsInCurrentConnection(QUESTIONNAIRE_TABLES);
        for (String code : bundledCodes) {
            jdbc.update("DELETE FROM clinlims.program WHERE code = ?", code);
        }
    }
}
