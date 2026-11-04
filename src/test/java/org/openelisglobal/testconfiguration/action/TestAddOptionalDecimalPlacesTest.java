package org.openelisglobal.testconfiguration.action;

import static org.junit.Assert.assertEquals;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.test.util.ReflectionTestUtils;

public class TestAddOptionalDecimalPlacesTest {
    @Test
    public void numericCreationAcceptsMissingNullBlankAndExplicitPlaces() {
        Object previousId = ReflectionTestUtils.getField(ResultType.NUMERIC, "id");
        try {
            ReflectionTestUtils.setField(ResultType.NUMERIC, "id", "test-numeric");
            JSONObject body = new JSONObject();
            body.put("resultType", "test-numeric");
            for (String field : new String[] { "lowValid", "highValid", "lowReportingRange", "highReportingRange",
                    "lowCritical", "highCritical" })
                body.put(field, "0");
            for (String field : new String[] { "panels", "sampleTypes", "resultLimits" })
                body.put(field, new JSONArray());
            TestAddControllerUtills util = new TestAddControllerUtills();
            assertEquals(null, util.extractTestAddParms(body, new JSONParser()).reportingDecimalPlaces);
            for (Object places : new Object[] { null, "", 0, 2 }) {
                body.put("reportingDecimalPlaces", places);
                assertEquals(java.util.Objects.toString(places, null),
                        util.extractTestAddParms(body, new JSONParser()).reportingDecimalPlaces);
            }
        } finally {
            ReflectionTestUtils.setField(ResultType.NUMERIC, "id", previousId);
        }
    }
}
