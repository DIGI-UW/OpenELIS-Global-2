package org.openelisglobal.reports.controller;

import com.itextpdf.text.DocumentException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.sf.jasperreports.engine.JRException;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IReportTrackingService;
import org.openelisglobal.common.services.ReportTrackingService.ReportType;
import org.openelisglobal.reports.action.implementation.IReportCreator;
import org.openelisglobal.reports.action.implementation.IReportParameterSetter;
import org.openelisglobal.reports.action.implementation.ReportImplementationFactory;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SessionAttributes("form")
public class ReportController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "report", "reportType", "type", "accessionDirect",
            "highAccessionDirect", "patientNumberDirect", "patientUpperNumberDirect", "lowerDateRange",
            "upperDateRange", "locationCode", "projectCode", "datePeriod", "lowerMonth", "lowerYear", "upperMonth",
            "upperYear", "selectList.selection", "experimentId", "reportName", "selPatient", "analysisIds",
            "referringSiteId", "referringSiteDepartmentId", "onlyResults", "dateType", "labSections", "priority",
            "receptionTime", "vlStudyType" };

    @Autowired
    private ServletContext context;

    private String reportPath = null;
    private String imagesPath = null;

    @ModelAttribute("form")
    public BaseForm form() {
        return new ReportForm();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/Report", method = RequestMethod.GET)
    public ModelAndView showReport(HttpServletRequest request, @ModelAttribute("form") BaseForm oldForm)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ReportForm newForm = resetSessionFormToType(oldForm, ReportForm.class);
        newForm.setFormMethod(RequestMethod.GET);

        newForm.setType(request.getParameter("type"));
        newForm.setReport(request.getParameter("report"));
        IReportParameterSetter setter = ReportImplementationFactory.getParameterSetter(request.getParameter("report"));

        if (setter != null) {
            setter.setRequestParameters(newForm);
        }

        return findForward(FWD_SUCCESS, newForm);
    }

    @RequestMapping(value = "/ReportPrint", method = RequestMethod.GET)
    public ModelAndView showReportPrint(HttpServletRequest request, HttpServletResponse response,
            @ModelAttribute("ReportPrintForm") @Valid ReportForm form, BindingResult result, SessionStatus status)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL, form);
        }

        LogEvent.logTrace("ReportController", "Log GET ", request.getParameter("report"));
        printReport(request, response, form);

        // signal to remove from from session
        status.setComplete();
        return null;
    }

    private void printReport(HttpServletRequest request, HttpServletResponse response, ReportForm form) {
        IReportCreator reportCreator = ReportImplementationFactory.getReportCreator(request.getParameter("report"));

        if (reportCreator != null) {
            reportCreator.setSystemUserId(getSysUserId(request));
            reportCreator.setRequestedReport(request.getParameter("report"));
            reportCreator.initializeReport(form);
            reportCreator.setReportPath(getReportPath());

            HashMap<String, String> parameterMap = (HashMap<String, String>) reportCreator.getReportParameters();
            parameterMap.put("SUBREPORT_DIR", getReportPath());
            parameterMap.put("imagesPath", getImagesPath());

            try {
                response.setContentType(reportCreator.getContentType());
                String responseHeaderName = reportCreator.getResponseHeaderName();
                String responseHeaderContent = reportCreator.getResponseHeaderContent();
                if (!GenericValidator.isBlankOrNull(responseHeaderName)
                        && !GenericValidator.isBlankOrNull(responseHeaderContent)) {
                    response.setHeader(responseHeaderName, responseHeaderContent);
                }

                byte[] bytes = reportCreator.runReport();

                response.setContentLength(bytes.length);

                ServletOutputStream servletOutputStream = response.getOutputStream();

                servletOutputStream.write(bytes, 0, bytes.length);
                servletOutputStream.flush();
                servletOutputStream.close();
            } catch (IOException | SQLException | JRException | DocumentException | ParseException e) {
                LogEvent.logError(e);
            }
        }

        if ("patient".equals(request.getParameter("type"))) {
            trackReports(reportCreator, request.getParameter("report"), ReportType.PATIENT);
        }
    }

    private void trackReports(IReportCreator reportCreator, String reportName, ReportType type) {
        List<String> refIds = reportCreator.getReportedOrders() != null ? reportCreator.getReportedOrders()
                : new ArrayList<>();
        SpringContext.getBean(IReportTrackingService.class).addReports(refIds, type, reportName, getSysUserId(request));
    }

    private String getReportPath() {
        String reportPath = getReportPathValue();
        if (reportPath.endsWith(File.separator)) {
            return reportPath;
        } else {
            return reportPath + File.separator;
        }
    }

    private String getReportPathValue() {

        if (reportPath == null) {
            // TODO csl this was added by external developer but it breaks the other reports
            // SiteInformation reportsPath =
            // siteInformationService.getSiteInformationByName("reportsDirectory");
            // if (reportsPath != null) {
            // reportPath = reportsPath.getValue();
            // return reportPath;
            // }
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

    public String getImagesPath() {
        if (imagesPath == null) {
            imagesPath = context.getRealPath("") + "static" + File.separator + "images" + File.separator;
            try {
                imagesPath = URLDecoder.decode(imagesPath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException(e);
            }
        }
        return imagesPath;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "commonReportDefiniton";
        } else if (FWD_FAIL.equals(forward)) {
            return "commonReportDefiniton";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageSubtitleKey() {
        return "reports.add.params";
    }

    @Override
    protected String getPageTitleKey() {
        return "reports.add.params";
    }
}
