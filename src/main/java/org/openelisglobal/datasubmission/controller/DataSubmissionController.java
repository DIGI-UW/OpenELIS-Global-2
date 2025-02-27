package org.openelisglobal.datasubmission.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.datasubmission.DataIndicatorFactory;
import org.openelisglobal.datasubmission.DataSubmitter;
import org.openelisglobal.datasubmission.form.DataSubmissionForm;
import org.openelisglobal.datasubmission.service.DataIndicatorService;
import org.openelisglobal.datasubmission.service.TypeOfDataIndicatorService;
import org.openelisglobal.datasubmission.validator.DataSubmissionFormValidator;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DataSubmissionController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "dataSubUrl.value", "month", "year",
            "indicators*.sendIndicator", "indicators*.dataValue.value", "indicators*.resources*.columnValues*.value" };

    @Autowired
    DataSubmissionFormValidator formValidator;
    @Autowired
    SiteInformationService siteInformationService;
    @Autowired
    TypeOfDataIndicatorService typeOfDataIndicatorService;
    @Autowired
    DataIndicatorService dataIndicatorService;
    @Autowired
    DataSubmitter dataSubmitter;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/DataSubmission", method = RequestMethod.GET)
    public ModelAndView showDataSubmission(HttpServletRequest request) {
        DataSubmissionForm form = new DataSubmissionForm();

        int month = org.apache.commons.validator.GenericValidator.isBlankOrNull(request.getParameter("month"))
                ? DateUtil.getCurrentMonth() + 1
                : Integer.parseInt(request.getParameter("month"));
        int year = org.apache.commons.validator.GenericValidator.isBlankOrNull(request.getParameter("year"))
                ? DateUtil.getCurrentYear()
                : Integer.parseInt(request.getParameter("year"));

        if (month < 0) {
            month = DateUtil.getCurrentMonth() + 1;
        }
        if (year < 0) {
            year = DateUtil.getCurrentYear();
        }

        List<DataIndicator> indicators = new ArrayList<>();
        List<TypeOfDataIndicator> typeOfIndicatorList = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
        for (TypeOfDataIndicator typeOfIndicator : typeOfIndicatorList) {
            DataIndicator indicator = dataIndicatorService.getIndicatorByTypeYearMonth(typeOfIndicator, year, month);
            if (indicator == null) {
                indicator = DataIndicatorFactory.createBlankDataIndicatorForType(typeOfIndicator);
            }
            indicator.setYear(year);
            indicator.setMonth(month);
            indicators.add(indicator);
        }

        form.setDataSubUrl(siteInformationService.getSiteInformationByName("Data Sub URL"));
        form.setIndicators(indicators);
        form.setMonth(month);
        form.setYear(year);
        form.setSiteId(ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));

        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/DataSubmission", method = RequestMethod.POST)
    public ModelAndView showDataSubmissionSave(HttpServletRequest request,
            @ModelAttribute("form") @Validated(DataSubmissionForm.DataSubmission.class) DataSubmissionForm form,
            BindingResult result, RedirectAttributes redirectAttributes) throws IOException, ParseException {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        int month = form.getMonth();
        int year = form.getYear();
        List<DataIndicator> indicators = form.getIndicators();
        boolean submit = "true".equalsIgnoreCase(request.getParameter("submit"));
        SiteInformation dataSubUrl = form.getDataSubUrl();
        dataSubUrl = (SiteInformation) siteInformationService.getSiteInformationByDomainName("Data Sub URL");
        dataSubUrl.setValue(form.getDataSubUrl().getValue());
        dataSubUrl.setSysUserId(getSysUserId(request));
        siteInformationService.update(dataSubUrl);
        for (DataIndicator indicator : indicators) {
            if (submit && indicator.isSendIndicator()) {
                boolean success = dataSubmitter.sendDataIndicator(indicator);
                indicator.setStatus(DataIndicator.SENT);
                if (success) {
                    indicator.setStatus(DataIndicator.RECEIVED);
                } else {
                    indicator.setStatus(DataIndicator.FAILED);
                    result.reject("errors.IndicatorCommunicationException",
                            new String[] { MessageUtil.getMessage(indicator.getTypeOfIndicator().getNameKey()) },
                            "errors.IndicatorCommunicationException");
                }
            }

            DataIndicator databaseIndicator = dataIndicatorService
                    .getIndicatorByTypeYearMonth(indicator.getTypeOfIndicator(), year, month);
            if (databaseIndicator == null) {
                dataIndicatorService.insert(indicator);
            } else {
                indicator.setId(databaseIndicator.getId());
                dataIndicatorService.update(indicator);
            }
        }

        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "dataSubmissionDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/DataSubmission";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "dataSubmissionDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "datasubmission.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "datasubmission.browse.title";
    }
}
