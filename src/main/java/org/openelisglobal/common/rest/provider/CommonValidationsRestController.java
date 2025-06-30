package org.openelisglobal.common.rest.provider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.ProgramAccessionValidator;
import org.openelisglobal.common.services.PhoneNumberService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.util.CI.ProjectForm;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class CommonValidationsRestController {

    private ResponseObject responseObject;

    @Autowired
    protected ProjectService projectService;

    protected SearchResultsService searchResultsService = SpringContext.getBean(SearchResultsService.class);

    public CommonValidationsRestController() {
        this.responseObject = new ResponseObject();
    }

    @GetMapping(value = "SampleEntryAccessionNumberValidation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject getAccessionNumberValidation(HttpServletRequest request) {
        String accessionNumber = request.getParameter("accessionNumber");
        String field = request.getParameter("field");
        String recordType = request.getParameter("recordType");
        String isRequired = request.getParameter("isRequired");
        String projectFormName = request.getParameter("projectFormName");
        boolean altAccession = "true".equalsIgnoreCase(request.getParameter("altAccession"));
        boolean parseForProjectFormName = "true".equalsIgnoreCase(request.getParameter("parseForProjectFormName"));
        boolean ignoreYear = "true".equals(request.getParameter("ignoreYear"));
        boolean ignoreUsage = "true".equals(request.getParameter("ignoreUsage"));

        IAccessionNumberValidator.ValidationResults result;

        if (parseForProjectFormName) {
            projectFormName = ProgramAccessionValidator.findStudyFormName(accessionNumber);
        }
        boolean projectFormNameUsed = ProjectForm.findProjectFormByFormId(projectFormName) != null;

        if (ignoreYear || ignoreUsage) {
            result = projectFormNameUsed ? new ProgramAccessionValidator().validFormat(accessionNumber, !ignoreYear)
                    : AccessionNumberUtil.getGeneralAccessionNumberValidator().validFormat(accessionNumber,
                            !ignoreYear);
            if (result == IAccessionNumberValidator.ValidationResults.SUCCESS && !ignoreUsage) {
                result = AccessionNumberUtil.isUsed(accessionNumber)
                        ? IAccessionNumberValidator.ValidationResults.SAMPLE_FOUND
                        : IAccessionNumberValidator.ValidationResults.SAMPLE_NOT_FOUND;
            }
        } else {
            // year matters and number must not be used
            result = projectFormNameUsed
                    ? new ProgramAccessionValidator().checkAccessionNumberValidity(accessionNumber, recordType,
                            isRequired, projectFormName)
                    : AccessionNumberUtil.getGeneralAccessionNumberValidator()
                            .checkAccessionNumberValidity(accessionNumber, recordType, isRequired, projectFormName);
        }

        // if( !Boolean.valueOf(ConfigurationProperties.getInstance()
        // .getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE))) {
        // result = ValidationResults.SUCCESS;
        // }

        switch (result) {
        case SUCCESS:
            responseObject.setStatus(true);
            responseObject.setBody("Valid accession number");
            break;
        case SAMPLE_FOUND:
        case SAMPLE_NOT_FOUND:
            responseObject.setStatus(false);
            responseObject.setBody(result.name());
            break;
        default:
            String message;
            if (projectFormNameUsed) {
                message = !ignoreUsage ? new ProgramAccessionValidator().getInvalidMessage(result)
                        : new ProgramAccessionValidator().getInvalidFormatMessage(result);
            } else {
                message = !ignoreUsage ? altAccession //
                        ? AccessionNumberUtil.getAltAccessionNumberValidator().getInvalidMessage(result)
                        : AccessionNumberUtil.getMainAccessionNumberValidator().getInvalidMessage(result)
                        : altAccession
                                ? AccessionNumberUtil.getAltAccessionNumberValidator().getInvalidFormatMessage(result)
                                : AccessionNumberUtil.getMainAccessionNumberValidator().getInvalidFormatMessage(result);
            }
            responseObject.setStatus(false);
            responseObject.setBody(message);
        }

        return responseObject;
    }

    @GetMapping(value = "SampleEntryGenerateScanProvider", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject accessionNumberGenerator(@RequestParam(required = false) String programCode,
            @RequestParam(defaultValue = "false") Boolean noIncrement,
            @RequestParam(required = false) AccessionFormat format) {

        String nextNumber = null;
        String error = null;
        try {
            if (GenericValidator.isBlankOrNull(programCode)) {
                if (format == null) {
                    nextNumber = AccessionNumberUtil.getMainAccessionNumberGenerator()
                            .getNextAvailableAccessionNumber("", !noIncrement);
                } else {
                    nextNumber = AccessionNumberUtil.getAccessionNumberGenerator(format)
                            .getNextAvailableAccessionNumber("", !noIncrement);
                }
            } else {
                // check program code validity
                List<Project> programCodes = projectService.getAllProjects();
                boolean found = false;
                for (Project code : programCodes) {
                    if (programCode.equals(code.getProgramCode())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    nextNumber = AccessionNumberUtil.getProgramAccessionNumberGenerator()
                            .getNextAvailableAccessionNumber(programCode, !noIncrement);
                    if (GenericValidator.isBlankOrNull(nextNumber)) {
                        error = MessageUtil.getMessage("error.accession.no.next");
                    }
                } else {
                    error = MessageUtil.getMessage("errors.invalid", "program.code");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            error = MessageUtil.getMessage("error.accession.no.error");
            LogEvent.logError(this.getClass().getSimpleName(), "processRequest", e.toString());
        }

        boolean success = !GenericValidator.isBlankOrNull(nextNumber);
        if (success) {
            responseObject.setStatus(true);
        }
        String result = GenericValidator.isBlankOrNull(error) ? nextNumber : error;
        responseObject.setBody(result);
        return responseObject;
    }

    @GetMapping(value = "PhoneNumberValidationProvider", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject getPhoneNumberValidation(HttpServletRequest request) {

        String field = request.getParameter("fieldId");
        String phoneNumber = request.getParameter("value");

        PhoneNumberService numberService = new PhoneNumberService();
        boolean valid = numberService.validatePhoneNumber(phoneNumber);

        if (!valid) {
            responseObject.setStatus(false);
            String result = MessageUtil.getMessage("phone.number.format.error", PhoneNumberService.getPhoneFormat());
            responseObject.setBody(result);
        } else {
            responseObject.setStatus(true);
            responseObject.setBody("Valid phone number");
        }
        return responseObject;
    }

    @GetMapping(value = "subjectNumberValidationProvider", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject validateSubjectNumberAndNationalId(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fieldId = request.getParameter("fieldId");
        String number = request.getParameter("subjectNumber");
        String numberType = request.getParameter("numberType");
        String STNumber = numberType.equals("STnumber") ? number : null;
        String subjectNumber = numberType.equals("subjectNumber") ? number : null;
        String nationalId = numberType.equals("nationalId") ? number : null;

        responseObject.setStatus(false);
        String queryResponse = "";
        // We just care about duplicates but blank values do not count as duplicates
        if (!(GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalId))) {
            List<PatientSearchResults> results = searchResultsService.getSearchResultsExact(null, null, STNumber,
                    subjectNumber, nationalId, null, null, null, null, null);

            boolean allowDuplicateSubjectNumber = ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_SUBJECT_NUMBERS, "true");
            boolean allowDuplicateNationalId = ConfigurationProperties.getInstance()
                    .isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_NATIONAL_IDS, "true");
            if (!results.isEmpty() && !GenericValidator.isBlankOrNull(subjectNumber)) {
                queryResponse = (allowDuplicateSubjectNumber ? "warning#" + MessageUtil.getMessage("alert.warning")
                        : "fail#" + MessageUtil.getMessage("alert.error")) + ": "
                        + MessageUtil.getMessage("error.duplicate.subjectNumber.warning");
                responseObject.setBody(queryResponse);

            } else if (!results.isEmpty() && !GenericValidator.isBlankOrNull(nationalId)) {
                queryResponse = (allowDuplicateNationalId ? "warning#" + MessageUtil.getMessage("alert.warning")
                        : "fail#" + MessageUtil.getMessage("alert.error")) + ": "
                        + MessageUtil.getMessage("error.duplicate.subjectNumber.warning");
                responseObject.setBody(queryResponse);
            } else if (!results.isEmpty()) {
                queryResponse = "fail#" + MessageUtil.getMessage("alert.error") + ": "
                        + MessageUtil.getMessage("error.duplicate.subjectNumber.warning");
                responseObject.setBody(queryResponse);
            } else {
                responseObject.setStatus(true);
                responseObject.setBody("Valid");
            }
        }
        return responseObject;
    }

    public static class ResponseObject {

        private boolean status = false;

        private String body;

        public ResponseObject() {
        }

        public boolean isStatus() {
            return status;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
