package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class SampleEntryTestsForTypeProviderRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/sample-entry-default-uoms.xml");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSampleTypeDefaultUoms_shouldReturnOnlySampleTypesWithDefaultUom() throws Exception {
        MvcResult mvcResult = super.mockMvc.perform(get("/rest/sample-type-default-uoms")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        Map<String, String> uomMap = super.mapFromJson(mvcResult.getResponse().getContentAsString(), Map.class);

        assertNotNull(uomMap);
        assertEquals("100", uomMap.get("200"));
        assertFalse(uomMap.containsKey("201"));
    }
}
