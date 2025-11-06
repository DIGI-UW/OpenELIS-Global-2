package org.openelisglobal.workplan.controller;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.workplan.form.WorkplanForm;
import org.openelisglobal.workplan.form.WorkplanForm.PrintWorkplan;
import org.openelisglobal.workplan.reports.IWorkplanReport;
import org.openelisglobal.workplan.reports.TestSectionWorkplanReport;
import org.openelisglobal.workplan.reports.TestWorkplanReport;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class PrintWorkplanReportController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedSearchID", "type", "testTypeID",
            "testSectionId", "testName", "workplanTests*.accessionNumber", "workplanTests*.patientInfo",
            "workplanTests*.receivedDate", "workplanTests*.testName", "workplanTests*.notIncludedInWorkplan",
            "resultList" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    private String reportPath = null;

    @RequestMapping(value = "/PrintWorkplanReport", method = RequestMethod.POST)
    public void showPrintWorkplanReport(HttpServletRequest request, HttpServletResponse response,
            @ModelAttribute("form") @Validated(PrintWorkplan.class) WorkplanForm form, BindingResult result) {
        if (result.hasErrors()) {
            writeErrorsToResponse(result, response);
            return;
        }

        request.getSession().setAttribute(SAVE_DISABLED, "true");

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

        workplanReport.setReportPath(getReportPath());

        // set jasper report parameters
        HashMap<String, Object> parameterMap = workplanReport.getParameters();

        // prepare report
        List<?> workplanRows = workplanReport.prepareRows(form);

        // set Jasper report file name
        String reportFileName = workplanReport.getFileName();

        try {

            byte[] bytes = null;

            JRDataSource dataSource = createReportDataSource(workplanRows);
            bytes = JasperRunManager.runReportToPdf(getReportPath() + reportFileName + ".jasper", parameterMap,
                    dataSource);

            ServletOutputStream servletOutputStream = response.getOutputStream();
            response.setContentType("application/pdf");
            response.setContentLength(bytes.length);
            String downloadFilename = "WorkplanReport";
            response.setHeader("Content-Disposition", "filename=\"" + downloadFilename + ".pdf\"");

            servletOutputStream.write(bytes, 0, bytes.length);
            servletOutputStream.flush();
            servletOutputStream.close();

        } catch (JRException | IOException e) {
            LogEvent.logError(e);
            result.reject("error.jasper", "error.jasper");
        }
        if (result.hasErrors()) {
            saveErrors(result);
        }
    }

    private JRDataSource createReportDataSource(List<?> includedTests) {
        JRBeanCollectionDataSource dataSource;
        dataSource = new JRBeanCollectionDataSource(includedTests);

        return dataSource;
    }

    private String getTestTypeName(String id) {
        return TestServiceImpl.getUserLocalizedTestName(id);
    }

    public IWorkplanReport getWorkplanReport(String testType, String name) {

        IWorkplanReport workplan;

        if ("test".equals(testType)) {
            workplan = new TestWorkplanReport(name);
            // } else if ("Serology".equals(testType)) {
            // workplan = new ElisaWorkplanReport(name);
        } else {
            workplan = new TestSectionWorkplanReport(name);
        }

        return workplan;
    }

    private String getReportPathValue() {
        if (reportPath == null) {
            ClassLoader classLoader = getClass().getClassLoader();
            reportPath = classLoader.getResource("reports").getPath();
            try {
                reportPath = URLDecoder.decode(reportPath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException(e);
            }
        }
        return reportPath;
    }

    private String getReportPath() {
        String reportPath = getReportPathValue();
        if (reportPath.endsWith(File.separator)) {
            return reportPath;
        } else {
            return reportPath + File.separator;
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "homePageDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageSubtitleKey() {
        return "workplan.title";
    }

    @Override
    protected String getPageTitleKey() {
        return "workplan.title";
    }
}
