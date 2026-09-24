package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReportImplementationFactoryTest {

    @Test
    public void testLegacyWhonetExportIsUnavailable() {
        // OGC-782 M4: Prove the legacy WHONET reports path cannot be resolved
        assertNull("The legacy WHONET export route should not return a creator",
                ReportImplementationFactory.getReportCreator("ExportWHONETReportByDate"));

        assertNull("The legacy WHONET export route should not return a parameter setter",
                ReportImplementationFactory.getParameterSetter("ExportWHONETReportByDate"));
    }
}