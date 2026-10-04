package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

public class ReferredOutReportTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referred-out-report.xml");
    }

    @Test
    public void listsEachReferredTestUnderItsLocalTest() throws Exception {
        List<String> lines = render();

        assertLine(lines, "External Referrals Report: Reference Lab");
        assertLine(lines, "Referrals Out 01/01/2024 - 12/12/2024");
        assertLine(lines, "DEV0124000000000921 Viral Load 1200 copies/mL Confirmation");
        assertLine(lines, "Reception: 03/06/2024 HIV-1 Genotype Wild type copies/mL 05/06/2024 12/06/2024");
        assertLine(lines, "Test: 04/06/2024");
        assertLine(lines, "HIV-1 RNA 1150 copies/mL 05/06/2024 10/06/2024");
        assertLine(lines, "DEV0124000000000922 CD4 Equipment down");
        assertEquals("the dates print once per local test: " + lines, 2,
                lines.stream().filter(line -> line.startsWith("Reception:")).count());
    }

    private List<String> render() throws Exception {
        ReferredOutReport report = new ReferredOutReport() {
            @Override
            protected void createReportItems() {
                reportItems.add(referral("DEV0124000000000921", "Viral Load", "1200", "copies/mL", "Confirmation",
                        "HIV-1 RNA", "1150", "05/06/2024", "10/06/2024"));
                reportItems.add(referral("DEV0124000000000921", "Viral Load", "1200", "copies/mL", "Confirmation",
                        "HIV-1 Genotype", "Wild type", "05/06/2024", "12/06/2024"));
                reportItems.add(referral("DEV0124000000000922", "CD4", "", "cells/uL", "Equipment down", "CD4 count",
                        "640", "06/06/2024", "08/06/2024"));
            }
        };
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2024");
        form.setUpperDateRange("12/12/2024");
        form.setLocationCode("921");
        report.initializeReport(form);
        report.setReportPath(getClass().getClassLoader().getResource("reports/").getPath());
        return Arrays.asList(PdfText.of(report.runReport()).split("\n"));
    }

    private static ClinicalPatientData referral(String accession, String test, String result, String uom, String reason,
            String referralTest, String referralResult, String sent, String reported) {
        ClinicalPatientData data = new ClinicalPatientData();
        data.setAccessionNumber(accession);
        data.setTestName(test);
        data.setResult(result);
        data.setUom(uom);
        data.setReferralReason(reason);
        data.setReferralTestName(referralTest);
        data.setReferralResult(referralResult);
        data.setReferralSentDate(sent);
        data.setReferralResultReportDate(reported);
        data.setReceivedDate("03/06/2024");
        data.setTestDate("04/06/2024");
        return data;
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
