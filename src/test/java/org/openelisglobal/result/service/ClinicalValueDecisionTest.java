package org.openelisglobal.result.service;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.util.ValidationSignals;

public class ClinicalValueDecisionTest {
    @Test
    public void reportingSettingsDoNotChangeCriticalAcknowledgementOrReleaseClassification() {
        ResultLimit limit = new ResultLimit();
        limit.setId("1");
        limit.setLowValid(0);
        limit.setHighValid(100);
        limit.setLowNormal(2);
        limit.setHighNormal(10);
        limit.setLowCritical(2);
        limit.setHighCritical(10);
        ResultEntryAcknowledgementServiceImpl acknowledgements = new ResultEntryAcknowledgementServiceImpl();

        for (int places : new int[] { -1, 0, 1, 2 }) {
            for (String value : new String[] { "1.96", "2.0", "2.04", "9.96", "10.0", "10.04" }) {
                Result result = new Result();
                result.setResultType("N");
                result.setValue(value);
                if (places < 0)
                    result.clearReportingDecimalPlaces();
                else
                    result.setReportingDecimalPlaces(places);
                boolean critical = value.equals("1.96") || value.equals("10.04");

                assertEquals(value + " at " + places, critical, ValidationSignals.isCritical(limit, result));
                assertEquals(!critical, ValidationSignals.isNormalResult(limit, result));
                assertEquals(critical ? "CRITICAL" : "NORMAL",
                        ValidationSignals.resultFlag(limit, "N", result.getValue()));
                assertEquals(critical ? 1 : 0, acknowledgements.alertsFor(limit, "N", value, null).size());
                assertEquals("unchanged value needs no new acknowledgement", 0,
                        acknowledgements.alertsFor(limit, "N", value, value).size());
                assertEquals(value, result.getValue());
            }
        }
    }
}
