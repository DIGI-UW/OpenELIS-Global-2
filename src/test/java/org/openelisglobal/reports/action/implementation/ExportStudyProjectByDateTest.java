package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.rest.DisplayListController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;

/**
 * The general study export by date (CIStudyExport) offers only the studies it
 * has columns for. A project outside that list (Testing, Quality Control) used
 * to reach the column builder and come back as "Internal error while generating
 * report"; it is now refused with a message that says why.
 */
public class ExportStudyProjectByDateTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/study-export-projects.xml");
    }

    @Test
    public void offersTheStudiesItCanExportIncludingRoutineHivTesting() {
        List<String> names = new ExportStudyProjectByDate().getProjectList().stream().map(Project::getProjectName)
                .collect(Collectors.toList());

        assertTrue(names.contains("Routine HIV Testing"));
        assertTrue(names.contains("Viral Load Results"));
        assertFalse(names.contains("Testing"));
    }

    @Test
    public void studyProjectsEndpointListsTheSameStudies() {
        List<String> ids = new DisplayListController().getStudyExportProjects().stream().map(IdValuePair::getId)
                .collect(Collectors.toList());

        assertTrue(ids.contains("9026"));
        assertTrue(ids.contains("9028"));
        assertFalse(ids.contains("9001"));
    }

    @Test
    public void aProjectWithoutAStudyExportIsRefusedWithItsOwnMessage() {
        ExportStudyProjectByDate report = new ExportStudyProjectByDate();
        report.setSystemUserId("990901");
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2026");
        form.setUpperDateRange("01/12/2026");
        form.setProjectCode("9001");

        report.initializeReport(form);

        assertTrue(report.errorFound);
        assertEquals(1, report.errorMsgs.size());
        assertEquals(MessageUtil.getMessage("report.error.message.project.notExportable"),
                report.errorMsgs.get(0).getMsgLine1());
    }

    @Test
    public void aProjectWithoutADisplayKeyLeavesTheExportsWorking() {
        CSVColumnBuilder.defineAllProjectTags();

        assertEquals("RTN", CSVColumnBuilder.translateProjectId("9026"));
        assertEquals("", CSVColumnBuilder.translateProjectId("9099"));
    }
}
