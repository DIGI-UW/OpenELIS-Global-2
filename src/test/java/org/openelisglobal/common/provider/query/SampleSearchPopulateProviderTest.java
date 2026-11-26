package org.openelisglobal.common.provider.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.servlet.validation.AjaxServlet;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;

@RunWith(MockitoJUnitRunner.class)
public class SampleSearchPopulateProviderTest {

    @Mock
    private SampleService sampleService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private IStatusService statusService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private SampleOrganizationService sampleOrganizationService;
    @Mock
    private AjaxServlet ajaxServlet;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private SampleSearchPopulateProvider provider;

    @Before
    public void setUp() {
        provider = new SampleSearchPopulateProvider(sampleService, analysisService, statusService, sampleHumanService,
                sampleOrganizationService);
        provider.setServlet(ajaxServlet);
    }

    @Test
    public void accessionNoAndLoinc_returnsEmptyWhenSampleDoesNotContainLoinc() throws Exception {
        Sample sample = buildSample("10", "ACC-100");
        StatusSet statusSet = new StatusSet();
        statusSet.setPatientId("20");

        when(request.getParameter("accessionNo")).thenReturn("ACC-100");
        when(request.getParameter("loinc")).thenReturn("2345-7");
        when(sampleService.getSampleByAccessionNumber("ACC-100")).thenReturn(sample);
        when(sampleService.sampleContainsTestWithLoinc("10", "2345-7")).thenReturn(false);
        when(statusService.getStatusSetForAccessionNumber("ACC-100")).thenReturn(statusSet);

        provider.processRequest(request, response);

        verify(sampleService).sampleContainsTestWithLoinc("10", "2345-7");
        verify(sampleService, never()).sampleContainsTest(any(), any());

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(ajaxServlet).sendData(xmlCaptor.capture(), eq("valid"), eq(request), eq(response));
        assertEquals("empty", xmlCaptor.getValue());
    }

    @Test
    public void accessionNoAndLoinc_returnsSampleXmlWhenSampleContainsLoinc() throws Exception {
        Sample sample = buildSample("10", "ACC-100");
        StatusSet statusSet = new StatusSet();
        statusSet.setPatientId("20");

        when(request.getParameter("accessionNo")).thenReturn("ACC-100");
        when(request.getParameter("loinc")).thenReturn("2345-7");
        when(sampleService.getSampleByAccessionNumber("ACC-100")).thenReturn(sample);
        when(sampleService.sampleContainsTestWithLoinc("10", "2345-7")).thenReturn(true);
        when(statusService.getStatusSetForAccessionNumber("ACC-100")).thenReturn(statusSet);
        when(sampleOrganizationService.getDataBySample(sample)).thenReturn(new SampleOrganization());

        provider.processRequest(request, response);

        verify(sampleService).sampleContainsTestWithLoinc("10", "2345-7");

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(ajaxServlet).sendData(xmlCaptor.capture(), eq("valid"), eq(request), eq(response));
        assertTrue(xmlCaptor.getValue().contains("<samplePK>10</samplePK>"));
        assertTrue(xmlCaptor.getValue().contains("<labNo>ACC-100</labNo>"));
    }

    @Test
    public void accessionNoAndTestId_returnsEmptyWhenSampleDoesNotContainTest() throws Exception {
        Sample sample = buildSample("10", "ACC-100");
        StatusSet statusSet = new StatusSet();
        statusSet.setPatientId("20");

        when(request.getParameter("accessionNo")).thenReturn("ACC-100");
        when(request.getParameter("testId")).thenReturn("99");
        when(sampleService.getSampleByAccessionNumber("ACC-100")).thenReturn(sample);
        when(sampleService.sampleContainsTest("10", "99")).thenReturn(false);
        when(statusService.getStatusSetForAccessionNumber("ACC-100")).thenReturn(statusSet);

        provider.processRequest(request, response);

        verify(sampleService).sampleContainsTest("10", "99");
        verify(sampleService, never()).sampleContainsTestWithLoinc(any(), any());

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(ajaxServlet).sendData(xmlCaptor.capture(), eq("valid"), eq(request), eq(response));
        assertEquals("empty", xmlCaptor.getValue());
    }

    @Test
    public void accessionNoAndLoinc_returnsEmptyWhenSampleNotFound() throws Exception {
        StatusSet statusSet = new StatusSet();
        statusSet.setPatientId(null);

        when(request.getParameter("accessionNo")).thenReturn("MISSING-1");
        when(request.getParameter("loinc")).thenReturn("2345-7");
        when(sampleService.getSampleByAccessionNumber("MISSING-1")).thenReturn(null);
        when(statusService.getStatusSetForAccessionNumber("MISSING-1")).thenReturn(statusSet);

        provider.processRequest(request, response);

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(ajaxServlet).sendData(xmlCaptor.capture(), eq("valid"), eq(request), eq(response));
        assertEquals("empty", xmlCaptor.getValue());
    }

    private Sample buildSample(String id, String accessionNumber) {
        Sample sample = new Sample();
        sample.setId(id);
        sample.setAccessionNumber(accessionNumber);
        return sample;
    }
}
