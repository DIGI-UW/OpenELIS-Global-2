package org.openelisglobal.analyzerresults.action.beanitems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.openelisglobal.result.form.AnalyzerResultsForm;

public class AnalyzerResultItemJsonContractTest {

    @Test
    public void displayOnlyDictionaryOptionsAreIgnoredWhenResultsAreSubmitted() throws Exception {
        String json = "{\"resultList\":[{\"id\":\"1005\",\"result\":\"1379\","
                + "\"dictionaryResultList\":[{\"id\":\"1379\",\"displayValue\":\"NOT DETECTED\"}]}]}";

        AnalyzerResultsForm form = new ObjectMapper().readValue(json, AnalyzerResultsForm.class);

        assertEquals("1379", form.getResultList().get(0).getResult());
        assertNull(form.getResultList().get(0).getDictionaryResultList());
    }
}
