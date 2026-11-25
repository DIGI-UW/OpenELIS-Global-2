package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.microbiology.service.MicroProgramReportingTrackConfigurationHandler;
import org.openelisglobal.microbiology.service.MicroReportingTrackConfigurationHandler;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroProgramReportingTrackConfigurationIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicroProgramReportingTrackConfigurationHandler handler;
    @Autowired
    private MicroReportingTrackConfigurationHandler tracks;
    @Autowired
    private DictionaryService dictionary;
    @Autowired
    private ProgramService programs;

    private String programId;

    @Before
    public void aProgramOnTheBacterialTrack() throws Exception {
        try (InputStream csv = getClass()
                .getResourceAsStream("/configuration/microbiology-reporting-tracks/default.csv")) {
            tracks.processConfiguration(csv, "default.csv");
        }
        Program program = new Program();
        program.setCode("FRESH_SITE");
        program.setProgramName("Fresh site program");
        program.setManuallyChanged(true);
        program.setReportingTrackId(track("Bacterial"));
        program.setSysUserId("1");
        programId = programs.insert(program);
    }

    private String track(String name) {
        return dictionary.getDictionaryEntryByNameAndCategoryName(name, "Reporting track").getId();
    }

    private Program program() {
        return programs.get(programId);
    }

    private void load(String rows) throws Exception {
        handler.processConfiguration(
                new ByteArrayInputStream(
                        ("programCode,reportingTrack,showOnMicroCase\n" + rows).getBytes(StandardCharsets.UTF_8)),
                "test.csv");
    }

    @Test
    public void aRowWithoutATrackMarksAndUnmarksAProgramForMicrobiologyCases() throws Exception {
        load("FRESH_SITE,,true\n");
        assertTrue(program().isShowOnMicroCase());
        assertEquals(track("Bacterial"), program().getReportingTrackId());

        load("FRESH_SITE,,false\n");
        assertFalse(program().isShowOnMicroCase());
        assertEquals(track("Bacterial"), program().getReportingTrackId());
    }

    @Test
    public void aBlankMarkLeavesItAndTheTrackIsStillSet() throws Exception {
        load("FRESH_SITE,,true\n");

        load("FRESH_SITE,TB,\n");

        assertTrue(program().isShowOnMicroCase());
        assertEquals(track("TB"), program().getReportingTrackId());
    }

    @Test
    public void anUnknownTrackIsStillRefused() {
        assertThrows(IllegalArgumentException.class, () -> load("FRESH_SITE,No such track,true\n"));

        assertFalse(program().isShowOnMicroCase());
        assertEquals(track("Bacterial"), program().getReportingTrackId());
    }

    @Test
    public void aMarkOtherThanTrueOrFalseIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> load("FRESH_SITE,Bacterial,yes\n"));

        assertFalse(program().isShowOnMicroCase());
    }
}
