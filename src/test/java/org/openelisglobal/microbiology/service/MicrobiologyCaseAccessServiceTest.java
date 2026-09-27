package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.UserService;

public class MicrobiologyCaseAccessServiceTest {

    private MicroCaseService caseService;
    private SampleItemService sampleItemService;
    private AnalysisService analysisService;
    private UserService userService;
    private MicrobiologyCaseAccessServiceImpl accessService;

    @Before
    public void setUp() {
        caseService = org.mockito.Mockito.mock(MicroCaseService.class);
        sampleItemService = org.mockito.Mockito.mock(SampleItemService.class);
        analysisService = org.mockito.Mockito.mock(AnalysisService.class);
        userService = org.mockito.Mockito.mock(UserService.class);
        accessService = new MicrobiologyCaseAccessServiceImpl(caseService, sampleItemService, analysisService,
                userService);
    }

    @Test
    public void administratorCanAccessCaseWithoutLabUnitFiltering() {
        assertTrue(accessService.canAccessCase("case-1", "7", true));

        verify(caseService, never()).getCase("case-1");
    }

    @Test
    public void resultsUserCanAccessCaseWhenAnySampleAnalysisIsInTheirLabUnit() {
        MicroCase microCase = new MicroCase();
        microCase.setSampleItemId("sample-item-1");
        SampleItem sampleItem = new SampleItem();
        Analysis analysis = new Analysis();
        when(caseService.getCase("case-1")).thenReturn(microCase);
        when(sampleItemService.get("sample-item-1")).thenReturn(sampleItem);
        when(analysisService.getAnalysesBySampleItem(sampleItem)).thenReturn(Collections.singletonList(analysis));
        when(userService.filterAnalysesByLabUnitRoles("7", Collections.singletonList(analysis), Constants.ROLE_RESULTS))
                .thenReturn(Collections.singletonList(analysis));

        assertTrue(accessService.canAccessCase("case-1", "7", false));
    }

    @Test
    public void userWithoutResultsOrValidationLabUnitAccessCannotAccessCase() {
        MicroCase microCase = new MicroCase();
        microCase.setSampleItemId("sample-item-1");
        SampleItem sampleItem = new SampleItem();
        Analysis analysis = new Analysis();
        when(caseService.getCase("case-1")).thenReturn(microCase);
        when(sampleItemService.get("sample-item-1")).thenReturn(sampleItem);
        when(analysisService.getAnalysesBySampleItem(sampleItem)).thenReturn(Collections.singletonList(analysis));
        when(userService.filterAnalysesByLabUnitRoles("7", Collections.singletonList(analysis), Constants.ROLE_RESULTS))
                .thenReturn(Collections.emptyList());
        when(userService.filterAnalysesByLabUnitRoles("7", Collections.singletonList(analysis),
                Constants.ROLE_VALIDATION)).thenReturn(Collections.emptyList());

        assertFalse(accessService.canAccessCase("case-1", "7", false));
    }

    @Test
    public void missingCaseCannotBeAccessed() {
        when(caseService.getCase("missing")).thenReturn(null);

        assertFalse(accessService.canAccessCase("missing", "7", false));
    }
}
