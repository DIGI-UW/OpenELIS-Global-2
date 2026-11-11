package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pathology, immunohistochemistry, cytology and programme cases are listed,
 * counted and opened only for the users who may read their order.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ProgramSampleLabUnitScopeTest {

    private static final String SCOPED = "7";
    private static final String WHOLE_LAB = "1";

    @Mock
    private SampleLabUnitScopeService sampleLabUnitScopeService;
    @InjectMocks
    private ProgramSampleLabUnitScope scope;

    private ProgramSample own;
    private ProgramSample other;

    @Before
    public void twoCases() {
        own = programSample(1, "100");
        other = programSample(2, "200");
        when(sampleLabUnitScopeService.restrictedSectionIds(SCOPED)).thenReturn(Set.of("36"));
        when(sampleLabUnitScopeService.restrictedSectionIds(WHOLE_LAB)).thenReturn(Set.of());
        when(sampleLabUnitScopeService.isVisible(SCOPED, own.getSample())).thenReturn(true);
        when(sampleLabUnitScopeService.isVisible(SCOPED, other.getSample())).thenReturn(false);
    }

    @Test
    public void aScopedUserIsListedAndCountedOnlyTheirOwnUnitsCases() {
        assertTrue(scope.isRestricted(SCOPED));
        assertEquals(List.of(own), scope.visible(SCOPED, List.of(own, other)));
        assertEquals(1, scope.count(SCOPED, () -> List.of(own, other), c -> true));
        assertEquals(0, scope.count(SCOPED, () -> List.of(own, other), c -> c.getId() == 2));
    }

    @Test
    public void aUserOfTheWholeLabKeepsEveryCase() {
        List<ProgramSample> cases = List.of(own, other);
        assertFalse(scope.isRestricted(WHOLE_LAB));
        assertSame(cases, scope.visible(WHOLE_LAB, cases));
    }

    @Test
    public void aCaseOutsideTheUsersUnitsIsNotFound() {
        scope.requireVisible(SCOPED, own);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> scope.requireVisible(SCOPED, other));
        assertEquals(HttpStatus.NOT_FOUND, refused.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> scope.requireVisible(SCOPED, null));
    }

    private static ProgramSample programSample(int id, String sampleId) {
        Sample sample = new Sample();
        sample.setId(sampleId);
        ProgramSample programSample = new ProgramSample();
        programSample.setId(id);
        programSample.setSample(sample);
        return programSample;
    }
}
