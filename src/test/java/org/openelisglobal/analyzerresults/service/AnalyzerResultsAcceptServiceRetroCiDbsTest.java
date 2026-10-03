package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.test.util.ReflectionTestUtils;

public class AnalyzerResultsAcceptServiceRetroCiDbsTest {

    private TypeOfSampleService typeOfSampleService;
    private SiteInformationService siteInformationService;
    private TypeOfSampleTestService typeOfSampleTestService;
    private SampleService sampleService;
    private AnalyzerResultsAcceptServiceImpl service;

    @Before
    public void setUp() {
        typeOfSampleService = mock(TypeOfSampleService.class);
        siteInformationService = mock(SiteInformationService.class);
        typeOfSampleTestService = mock(TypeOfSampleTestService.class);
        sampleService = mock(SampleService.class);

        service = new AnalyzerResultsAcceptServiceImpl(typeOfSampleService, siteInformationService);
        ReflectionTestUtils.setField(service, "typeOfSampleTestService", typeOfSampleTestService);
        ReflectionTestUtils.setField(service, "sampleService", sampleService);
        service.setRetroCiForTesting(true);
    }

    private SiteInformation createSiteInfo(String value) {
        SiteInformation siteInfo = new SiteInformation();
        siteInfo.setName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG);
        siteInfo.setValue(value);
        return siteInfo;
    }

    private TypeOfSample createSampleType(String id, String description, String localAbbrev) {
        TypeOfSample sampleType = new TypeOfSample();
        sampleType.setId(id);
        sampleType.setDescription(description);
        sampleType.setLocalAbbreviation(localAbbrev);
        sampleType.setDomain(Domain.CLINICAL.name());
        return sampleType;
    }

    @Test
    public void settingPresent_resolvesByLocalAbbreviation() {
        SiteInformation siteInfo = createSiteInfo("DBS");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);

        TypeOfSample dbsType = createSampleType("26", "DBS", "DBS");
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("DBS", Domain.CLINICAL.name()))
                .thenReturn(dbsType);

        assertEquals("26", service.resolveDbsSampleTypeId());
    }

    @Test
    public void settingPresent_resolvesById() {
        SiteInformation siteInfo = createSiteInfo("26");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);

        TypeOfSample dbsType = createSampleType("26", "Dried Blood Spot", "DBS");
        when(typeOfSampleService.getTypeOfSampleById("26")).thenReturn(dbsType);

        assertEquals("26", service.resolveDbsSampleTypeId());
    }

    @Test
    public void settingPresent_resolvesByDescriptionFallback() {
        SiteInformation siteInfo = createSiteInfo("Dried blood spot card");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);

        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("Dried blood spot card", Domain.CLINICAL.name()))
                .thenReturn(null);

        TypeOfSample dbsType = createSampleType("26", "Dried blood spot card", "DBS-CARD");
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(
                argThat(tos -> tos != null && "Dried blood spot card".equals(tos.getDescription())), eq(false)))
                .thenReturn(dbsType);

        assertEquals("26", service.resolveDbsSampleTypeId());
    }

    @Test
    public void settingMissing_returnsNull() {
        when(siteInformationService.getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(null);
        when(siteInformationService.getSiteInformationByName("retrociDbsSampleType")).thenReturn(null);

        assertNull(service.resolveDbsSampleTypeId());
        verify(typeOfSampleService, never()).getTypeOfSampleById(any());
        verify(typeOfSampleService, never()).getTypeOfSampleByLocalAbbrevAndDomain(any(), any());
    }

    @Test
    public void settingBlank_returnsNull() {
        SiteInformation emptySetting = createSiteInfo("   ");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(emptySetting);

        assertNull(service.resolveDbsSampleTypeId());
        verify(typeOfSampleService, never()).getTypeOfSampleById(any());
        verify(typeOfSampleService, never()).getTypeOfSampleByLocalAbbrevAndDomain(any(), any());
    }

    @Test
    public void settingPointsToNonExistentType_returnsNull() {
        SiteInformation siteInfo = createSiteInfo("NON_EXISTENT");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);

        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("NON_EXISTENT", Domain.CLINICAL.name()))
                .thenReturn(null);
        when(typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(any(), eq(false))).thenReturn(null);

        assertNull(service.resolveDbsSampleTypeId());
    }

    @Test
    public void nonRetroCiMode_returnsNullEvenIfSettingPresent() {
        service.setRetroCiForTesting(false);
        SiteInformation siteInfo = createSiteInfo("DBS");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);

        assertNull(service.resolveDbsSampleTypeId());
        verify(siteInformationService, never()).getSiteInformationByName(any());
    }

    @Test
    public void dynamicUpdate_reflectsChangeWithoutRestart() {
        SiteInformation siteInfo = createSiteInfo("DBS");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);

        TypeOfSample dbsType = createSampleType("26", "DBS", "DBS");
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("DBS", Domain.CLINICAL.name()))
                .thenReturn(dbsType);

        assertEquals("26", service.resolveDbsSampleTypeId());

        // Update the site setting dynamically
        siteInfo.setValue("NEW_DBS");
        TypeOfSample newType = createSampleType("30", "New DBS", "NEW_DBS");
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("NEW_DBS", Domain.CLINICAL.name()))
                .thenReturn(newType);

        assertEquals("30", service.resolveDbsSampleTypeId());
    }

    @Test
    public void needsSpecimenChoice_withConfiguredDbs_defaultsLdbsAccession() {
        SiteInformation siteInfo = createSiteInfo("DBS");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("DBS", Domain.CLINICAL.name()))
                .thenReturn(createSampleType("26", "DBS", "DBS"));

        TypeOfSampleTest cand1 = new TypeOfSampleTest();
        cand1.setTypeOfSampleId("25");
        TypeOfSampleTest cand2 = new TypeOfSampleTest();
        cand2.setTypeOfSampleId("26");
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("test-101")).thenReturn(List.of(cand1, cand2));

        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("test-101");
        item.setAccessionNumber("LDBS-2026-00123");
        item.setIsControl(false);

        // Candidates include 26 (configured DBS), accession starts with LDBS => returns
        // false (no choice needed)
        assertFalse(service.needsSpecimenChoice(item));
    }

    @Test
    public void needsSpecimenChoice_whenSettingMissing_doesNotDefaultLdbsAccession() {
        when(siteInformationService.getSiteInformationByName(any())).thenReturn(null);

        TypeOfSampleTest cand1 = new TypeOfSampleTest();
        cand1.setTypeOfSampleId("25");
        TypeOfSampleTest cand2 = new TypeOfSampleTest();
        cand2.setTypeOfSampleId("26");
        when(typeOfSampleTestService.getTypeOfSampleTestsForTest("test-101"))
                .thenReturn(List.of(cand1, cand2));

        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("test-101");
        item.setAccessionNumber("LDBS-2026-00123");
        item.setIsControl(false);

        // DBS is not configured => normal resolution applies => requires user choice
        assertTrue(service.needsSpecimenChoice(item));
    }

    @Test
    public void getTypeOfSampleId_withConfiguredDbs_returnsDbsSampleTypeId() {
        SiteInformation siteInfo = createSiteInfo("DBS");
        when(siteInformationService
                .getSiteInformationByName(AnalyzerResultsAcceptServiceImpl.RETROCI_DBS_SAMPLE_TYPE_CONFIG))
                .thenReturn(siteInfo);
        when(typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("DBS", Domain.CLINICAL.name()))
                .thenReturn(createSampleType("26", "DBS", "DBS"));

        String resolved = service.getTypeOfSampleId(List.of("25", "26", "27"), "LDBS-2026-00123");
        assertEquals("26", resolved);
    }

    @Test
    public void getTypeOfSampleId_whenSettingMissing_fallsBackToFirstCandidate() {
        when(siteInformationService.getSiteInformationByName(any())).thenReturn(null);

        String resolved = service.getTypeOfSampleId(List.of("25", "26", "27"), "LDBS-2026-00123");
        assertEquals("25", resolved);
    }
}
