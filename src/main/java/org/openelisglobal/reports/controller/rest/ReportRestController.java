package org.openelisglobal.reports.controller.rest;

import com.itextpdf.text.DocumentException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import net.sf.jasperreports.engine.JRException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.reports.action.implementation.IReportCreator;
import org.openelisglobal.reports.action.implementation.ReportImplementationFactory;
import org.openelisglobal.reports.form.ReportForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class ReportRestController extends BaseRestController {

    @Autowired
    private ServletContext context;

    private static String reportPath = null;

    private static String imagesPath = null;

    @RequestMapping(value = "ReportPrint", method = RequestMethod.POST)
    @ResponseBody
    public void showReportPrint(@RequestBody ReportForm form, HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        LogEvent.logTrace("ReportController", "Log GET ", form.getReport());
        IReportCreator reportCreator = ReportImplementationFactory.getReportCreator(form.getReport());

        if (reportCreator != null) {
            reportCreator.setSystemUserId(getSysUserId(request));
            reportCreator.setRequestedReport(form.getReport());
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
}
