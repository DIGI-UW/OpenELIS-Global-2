package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.form.SampleSearchForm;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class SampleRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/sample-rest-controller.xml");
    }

    // --- GET /rest/sample/all-by-accession/{accessionNumber} ---

    @Test
    public void getSampleByAccessionNumber_shouldReturn200WithAnalyses_whenSampleExists() throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/sample/all-by-accession/123456")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        String content = mvcResult.getResponse().getContentAsString();
        List<SampleSearchForm> forms = Arrays.asList(super.mapFromJson(content, SampleSearchForm[].class));

        assertEquals(1, forms.size());
        SampleSearchForm firstForm = forms.get(0);
        assertEquals(Integer.valueOf(1), firstForm.getId());
        assertEquals(123456, Integer.parseInt(firstForm.getAccessionNumber()));
        assertEquals(Integer.valueOf(1), firstForm.getAnalysisId());
        assertEquals("HIV Test", firstForm.getReferralTest());
        assertEquals("Blood", firstForm.getSampleType());
    }

    @Test
    public void getSampleByAccessionNumber_shouldReturn200WithEmptyList_whenNoNotStartedAnalyses() throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/sample/all-by-accession/999999")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        String content = mvcResult.getResponse().getContentAsString();
        List<SampleSearchForm> forms = Arrays.asList(super.mapFromJson(content, SampleSearchForm[].class));

        assertTrue(forms.isEmpty());
    }

    @Test
    public void getSampleByAccessionNumber_shouldReturn200WithMultipleAnalyses_whenMultipleNotStarted()
            throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/sample/all-by-accession/654321")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        String content = mvcResult.getResponse().getContentAsString();
        List<SampleSearchForm> forms = Arrays.asList(super.mapFromJson(content, SampleSearchForm[].class));

        assertEquals(2, forms.size());
        assertEquals(654321, Integer.parseInt(forms.get(0).getAccessionNumber()));
        assertEquals(654321, Integer.parseInt(forms.get(1).getAccessionNumber()));
    }

    @Test
    public void getSampleByAccessionNumber_shouldReturn404_whenSampleNotFound() throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/sample/all-by-accession/NONEXISTENT")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(404, mvcResult.getResponse().getStatus());
    }

}
