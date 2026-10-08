package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

public class RetroCINonConformityByLabnoTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/non-conformity-checklist-report.xml");
    }

    @Test
    public void ticksTheReasonRecordedForTheOrdersSample() throws Exception {
        ReportForm form = new ReportForm();
        form.setAccessionDirect("DEV0124000000000931");
        form.setHighAccessionDirect("DEV0124000000000931");
        RetroCINonConformityByLabno report = new RetroCINonConformityByLabno();
        report.initializeReport(form);

        String text = PdfText.of(report.runReport());

        List<String> lines = Arrays.stream(text.split("\\R")).map(String::trim).toList();
        assertTrue(text, text.contains("Sujetno: SUBJ-0042"));
        assertTrue(text, text.contains("Labno: DEV0124000000000931"));
        assertTrue(text, text.contains("Date Naiss.: 12/03/1992"));
        assertTrue(text, lines.contains("Echantillon Coagulé X"));
    }

    @Test
    public void anOrderCollectedWithoutADateLeavesItBlank() throws Exception {
        ReportForm form = new ReportForm();
        form.setAccessionDirect("DEV0124000000000932");
        form.setHighAccessionDirect("DEV0124000000000932");
        RetroCINonConformityByLabno report = new RetroCINonConformityByLabno();
        report.initializeReport(form);

        String text = PdfText.of(report.runReport());

        assertTrue(text, text.contains("Labno: DEV0124000000000932"));
        assertFalse(text, text.contains("null"));
    }
}
