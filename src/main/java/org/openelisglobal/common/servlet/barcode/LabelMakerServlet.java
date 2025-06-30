package org.openelisglobal.common.servlet.barcode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

/**
 * Servlet for pages to send bar code label requests. Returns as pdf unless...
 * User unauthenticated - gives error response Invalid request parameters -
 * gives error response Maximum printing has been reached (pdf length 0) -
 * returns override page
 *
 * @author Caleb
 */
public class LabelMakerServlet extends HttpServlet implements IActionConstants {

    private static final long serialVersionUID = 4756240897909804141L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean unauthorized = false;

        // check for module authentication
        UserModuleService userModuleService = SpringContext.getBean(UserModuleService.class);
        unauthorized |= userModuleService.isSessionExpired(request);

        if (unauthorized) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().println(MessageUtil.getMessage("message.error.unauthorized"));
            return;
        }
        if ("block".equals(request.getParameter("labelType")) || "slide".equals(request.getParameter("labelType"))) {
            printPathologyBarcodeLabel(request, response);
        } else if ("true".equalsIgnoreCase(request.getParameter("prePrinting"))) {
            // writes to response
            try {
                prePrintLabels(request, response);
            } catch (NumberFormatException | LIMSInvalidConfigurationException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "doGet",
                        "invalid configuration, could not generate a pre-printed accession number");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/html; charset=utf-8");
                response.getWriter().println(MessageUtil.getMessage("error.accession.no.error"));
                return;
            }
        } else {
            // writes to response
            printExistingOrder(request, response);
        }
    }

    private void printPathologyBarcodeLabel(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // create requested labels as pdf stream
        // In printPathologyBarcodeLabel method:
        BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        labelMaker.setSysUserId(String.valueOf(usd.getSystemUserId()));

        labelMaker.generateGenericBarcodeLabel(request.getParameter("code"), request.getParameter("labelType"));
        ByteArrayOutputStream labelAsOutputStream = labelMaker.createLabelsAsStream();

        response.setContentType("application/pdf");
        response.addHeader("Content-Disposition", "inline; filename=" + "barcode.pdf");
        response.setContentLength(labelAsOutputStream.size());
        labelAsOutputStream.writeTo(response.getOutputStream());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private void prePrintLabels(HttpServletRequest request, HttpServletResponse response)
            throws IOException, NumberFormatException, LIMSInvalidConfigurationException {
        // get tests for request

        String testIds = request.getParameter("testIds");
        String[] testIdsArray = testIds.split(",");
        List<Test> tests = new ArrayList<>();
        for (String testId : testIdsArray) {
            tests.add(SpringContext.getBean(TestService.class).getActiveTestById(Integer.parseInt(testId)));
        }

        // create requested labels as pdf stream
        BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        labelMaker.setSysUserId(String.valueOf(usd.getSystemUserId()));

        String startingAt = request.getParameter("startingAt");
        if (GenericValidator.isBlankOrNull(startingAt) || startingAt.trim().equals("null")
                || startingAt.trim().equals("undefined")) {
            startingAt = "";
        }
        labelMaker.generatePrePrintLabels(Integer.parseInt(request.getParameter("numSetsOfLabels")),
                Integer.parseInt(request.getParameter("numOrderLabelsPerSet")),
                Integer.parseInt(request.getParameter("numSpecimenLabelsPerSet")), request.getParameter("facilityName"),
                tests, startingAt);
        ByteArrayOutputStream labelAsOutputStream = labelMaker.createLabelsAsStream();

        // if empty stream, assume at max printing
        response.setContentType("application/pdf");
        response.addHeader("Content-Disposition", "inline; filename=" + "sample.pdf");
        response.setContentLength(labelAsOutputStream.size());
        labelAsOutputStream.writeTo(response.getOutputStream());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private void printExistingOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // get parameters
        String labNo = request.getParameter("labNo");
        String programCode = request.getParameter("programCode");
        String type = request.getParameter("type");
        String quantity = request.getParameter("quantity");
        String override = request.getParameter("override");
        if (StringUtils.isEmpty(labNo)) { // get last used accession number if none provided
            labNo = (String) request.getSession().getAttribute("lastAccessionNumber");
            labNo = StringUtil.replaceNullWithEmptyString(labNo);
        }
        // set to default values if none provided
        if (StringUtils.isEmpty(type)) {
            type = "default";
        }
        if (StringUtils.isEmpty(quantity)) {
            quantity = "1";
        }
        if (StringUtils.isEmpty(override)) {
            override = "false";
        }
        // correct incorrect formatting of specimen number
        if (labNo.contains("-") && !labNo.contains(".")) {
            labNo = labNo.replace('-', '.');
        }

        // validate the given parameters
        Errors errors = validate(labNo, programCode, type, quantity, override);
        if (errors.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().println("One or more fields are invalid");
            response.getWriter().println("<ul>");

            for (ObjectError error : errors.getAllErrors()) {
                response.getWriter().println("<li>" + MessageUtil.getMessage(error.getCode()) + "</li>");
            }
            response.getWriter().println("</ul>");
            return;
        }

        // create requested labels as pdf stream
        BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        labelMaker.setOverride(override);
        labelMaker.setSysUserId(String.valueOf(usd.getSystemUserId()));
        labelMaker.generateLabels(labNo, type, quantity, override);
        ByteArrayOutputStream labelAsOutputStream = labelMaker.createLabelsAsStreamWithMaximumPrints();

        // if empty stream, assume at max printing
        if (labelAsOutputStream.size() == 0) {
            String path = request.getContextPath();
            String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + path + "/";
            response.setContentType("text/html; charset=utf-8");
            response.getWriter()
                    .println("<script type=\"text/javascript\">" + "function override() {\n"
                            + "    var url = new URL(window.location.href);\n"
                            + "    url.searchParams.set('override', 'true');\n"
                            + "    window.location.href = url.toString();\n" + "}" + "</script>");
            response.getWriter().println(MessageUtil.getMessage("barcode.message.maxreached"));
            response.getWriter().println("</br>");
            response.getWriter()
                    .println("<input type='button' id='overrideButton' value='Override' onclick='override();'>");
            // else return the pdf
        } else {
            response.setContentType("application/pdf");
            response.addHeader("Content-Disposition", "inline; filename=" + "sample.pdf");
            response.setContentLength(labelAsOutputStream.size());
            labelAsOutputStream.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
            response.getOutputStream().close();
            return;
        }
    }

    /**
     * Validate the given parameters
     *
     * @param labNo       Make sure it is properly formatted
     * @param programCode Optional variable to tell what accessionNumberUtil to get
     * @param patientId   Ensure is int
     * @param type        Ensure is default, specimen, order, or blank
     * @param quantity    Ensure is int
     * @param override    Ensure is bool
     * @return any errors that were generated along the way
     */
    private Errors validate(String labNo, String programCode, String type, String quantity, String override) {
        Errors errors = new BaseErrors();
        // Validate quantity
        if (!org.apache.commons.validator.GenericValidator.isInt(quantity)) {
            errors.reject("barcode.label.error.quantity.invalid", "barcode.label.error.quantity.invalid");
        }
        // Validate type
        if (!"default".equals(type) && !"order".equals(type) && !"specimen".equals(type) && !"blank".equals(type)) {
            errors.reject("barcode.label.error.type.invalid", "barcode.label.error.type.invalid");
        }
        // Validate "labNo" (either labNo, labNo.itemNo)
        boolean validateAccessionNumber = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ACCESSION_NUMBER_VALIDATE, "true");
        if (validateAccessionNumber) {
            IAccessionNumberValidator accessionNumberValidator = AccessionNumberUtil
                    .getGeneralAccessionNumberValidator();
            String accessionNumber;
            // String sampleItemNumber;
            if (labNo.indexOf(".") > 0) {
                accessionNumber = labNo.substring(0, labNo.indexOf("."));
                // sampleItemNumber = labNo.substring(labNo.indexOf(".") + 1);
            } else {
                accessionNumber = labNo;
                // sampleItemNumber = "0";
            }
            if (!(IAccessionNumberValidator.ValidationResults.SUCCESS == accessionNumberValidator
                    .validFormat(accessionNumber, false))) {
                errors.reject("barcode.label.error.accession.invalid", "barcode.label.error.accession.invalid");
            }
        } else if (AccessionNumberUtil.containsBlackListCharacters(labNo)) {
            errors.reject("barcode.label.error.accession.invalid", "barcode.label.error.accession.invalid");
        }
        SampleService sampleService = SpringContext.getBean(SampleService.class);
        if (sampleService.getSampleByAccessionNumber(labNo) == null) {
            errors.reject("barcode.label.error.accession.nosample", "barcode.label.error.accession.nosample");
        }
        // validate override
        if (!GenericValidator.isBool(override)
                && !org.apache.commons.validator.GenericValidator.isBlankOrNull(override)) {
            errors.reject("barcode.label.error.override.invalid", "barcode.label.error.override.invalid");
        }

        return errors;
    }
}
