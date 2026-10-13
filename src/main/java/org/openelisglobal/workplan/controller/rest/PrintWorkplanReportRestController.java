package org.openelisglobal.workplan.controller.rest;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.openelisglobal.workplan.form.WorkplanForm.PrintWorkplan;
import org.openelisglobal.workplan.reports.IWorkplanReport;
import org.openelisglobal.workplan.reports.TestSectionWorkplanReport;
import org.openelisglobal.workplan.reports.TestWorkplanReport;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("PrintWorkplanReportRestController")
public class PrintWorkplanReportRestController extends BaseRestController {

    @PostMapping(value = "/rest/PrintWorkplanReport")
    public void showRestPrintWorkplanReport(HttpServletRequest request, HttpServletResponse response,
            @RequestBody @Validated(PrintWorkplan.class) WorkplanForm form, BindingResult result) {

        String workplanType = form.getType();
        String workplanName;

        if (workplanType.equals("test")) {
            String testID = form.getTestTypeID();
            workplanName = getTestTypeName(testID);
        } else {
            workplanType = Character.toUpperCase(workplanType.charAt(0)) + workplanType.substring(1);
            workplanName = form.getTestName();
        }

        // get workplan report based on testName
        IWorkplanReport workplanReport = getWorkplanReport(workplanType, workplanName);

        List<TestResultItem> workplanRows = workplanReport.prepareRows(form);

        try {
            byte[] bytes = workplanReport.renderPdf(workplanRows);

            ServletOutputStream servletOutputStream = response.getOutputStream();
            response.setContentType("application/pdf");
            response.setContentLength(bytes.length);
            String downloadFilename = "WorkplanReport";
            response.setHeader("Content-Disposition", "filename=\"" + downloadFilename + ".pdf\"");

            servletOutputStream.write(bytes, 0, bytes.length);
            servletOutputStream.flush();
            servletOutputStream.close();

        } catch (IOException e) {
            LogEvent.logError(e);
            result.reject("error.jasper", "error.jasper");
        }
    }

    private String getTestTypeName(String id) {
        return TestServiceImpl.getUserLocalizedTestName(id);
    }

    public IWorkplanReport getWorkplanReport(String testType, String name) {

        IWorkplanReport workplan;

        if ("test".equals(testType)) {
            workplan = new TestWorkplanReport(name);
        } else {
            workplan = new TestSectionWorkplanReport(name);
        }

        return workplan;
    }

}
