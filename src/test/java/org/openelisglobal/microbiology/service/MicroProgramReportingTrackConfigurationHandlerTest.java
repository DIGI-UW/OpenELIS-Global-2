package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;

public class MicroProgramReportingTrackConfigurationHandlerTest {
    @Test
    public void configuredTrackUpdatesOnlyMatchingProgramsAndRepeatedImportsAreIdempotent() throws Exception {
        var programs = mock(ProgramService.class);
        var dictionary = mock(DictionaryService.class);
        var configured = new Program();
        configured.setCode("LOCAL_WATER");
        var other = new Program();
        other.setCode("CLINICAL");
        when(programs.getAll()).thenReturn(List.of(configured, other));
        var track = new org.openelisglobal.dictionary.valueholder.Dictionary();
        track.setId("17");
        track.setIsActive("Y");
        when(dictionary.getDictionaryEntryByNameAndCategoryName("Water surveillance", "Reporting track"))
                .thenReturn(track);
        var service = new MicroProgramReportingTrackConfigurationHandler(programs, dictionary);
        for (int i = 0; i < 2; i++)
            service.processConfiguration(csv(), "local.csv");
        assertEquals("17", configured.getReportingTrackId());
        assertNull(other.getReportingTrackId());
        verify(programs, times(1)).update(configured);
        verify(programs, never()).update(other);
    }

    @Test
    public void unresolvedTrackStopsTheImportWithoutCreatingConcepts() {
        var programs = mock(ProgramService.class);
        var dictionary = mock(DictionaryService.class);
        var service = new MicroProgramReportingTrackConfigurationHandler(programs, dictionary);
        assertThrows(IllegalArgumentException.class, () -> service.processConfiguration(csv(), "local.csv"));
        verify(programs, never()).update(any());
        verify(dictionary, never()).insert(any());
    }

    private ByteArrayInputStream csv() {
        return new ByteArrayInputStream(
                "programCode,reportingTrack\nLOCAL_WATER,Water surveillance\n".getBytes(StandardCharsets.UTF_8));
    }
}
