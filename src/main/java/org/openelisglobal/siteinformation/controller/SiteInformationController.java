package org.openelisglobal.siteinformation.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.PhoneNumberService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.URLUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.siteinformation.form.SiteInformationForm;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.validator.SiteInformationFormValidator;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@SessionAttributes("form")
public class SiteInformationController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "paramName", "value", "localization.localeValues*" };

    @Autowired
    SiteInformationFormValidator formValidator;
    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private SiteInformationDomainService siteInformationDomainService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private LocaleResolver localeResolver;

    @ModelAttribute("form")
    public SiteInformationForm form(HttpServletRequest request) {
        SiteInformationForm form = new SiteInformationForm();
        setupFormForRequest(form, request);
        return form;
    }

    private static final String ACCESSION_NUMBER_PREFIX = "Accession number prefix";
    private SiteInformationDomain SITE_IDENTITY_DOMAIN;
    private SiteInformationDomain RESULT_CONFIG_DOMAIN;

    @PostConstruct
    private void initialize() {
        SITE_IDENTITY_DOMAIN = siteInformationDomainService.getByName("siteIdentity");
        RESULT_CONFIG_DOMAIN = siteInformationDomainService.getByName("resultConfiguration");
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = { "/NonConformityConfiguration", "/WorkplanConfiguration", "/PrintedReportsConfiguration",
            "/SampleEntryConfig", "/ResultConfiguration", "/MenuStatementConfig", "/PatientConfiguration",
            "/ValidationConfiguration", "/SiteInformation", "/NextPreviousNonConformityConfiguration",
            "/NextPreviousWorkplanConfiguration", "/NextPreviousPrintedReportsConfiguration",
            "/NextPreviousSampleEntryConfig", "/NextPreviousResultConfiguration", "/NextPreviousMenuStatementConfig",
            "/NextPreviousPatientConfiguration", "/NextPreviousSiteInformation",
            "/NextPreviousValidationConfiguration" }, method = RequestMethod.GET)
    // TODO decide if still needing NextPrevious (functionality is not implemented)
    public ModelAndView showSiteInformation(HttpServletRequest request,
            @ModelAttribute("form") SiteInformationForm oldForm)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // protect form from injection arbitrary values on the get step (since csrf is
        // not checked at this stage)
        SiteInformationForm newForm = resetSessionFormToType(oldForm, SiteInformationForm.class);
        setupFormForRequest(newForm, request);

        String id = request.getParameter(ID);

        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, TRUE);
        request.setAttribute(NEXT_DISABLED, TRUE);

        boolean isNew = id == null || "0".equals(id);

        if (!isNew) {
            SiteInformation siteInformation = siteInformationService.get(id);

            request.setAttribute(ID, siteInformation.getId());

            newForm.setParamName(siteInformation.getName());
            newForm.setDescription(getInstruction(siteInformation));
            newForm.setValue(siteInformation.getValue());
            setLocalizationValues(newForm, siteInformation);
            newForm.setEncrypted(siteInformation.isEncrypted());
            newForm.setValueType(siteInformation.getValueType());
            newForm.setEditable(isEditable(siteInformation));
            newForm.setTag(siteInformation.getTag());

            if ("dictionary".equals(siteInformation.getValueType())) {
                List<String> dictionaryValues = new ArrayList<>();

                List<Dictionary> dictionaries = dictionaryService
                        .getDictionaryEntriesByCategoryId(siteInformation.getDictionaryCategoryId());

                for (Dictionary dictionary : dictionaries) {
                    dictionaryValues.add(dictionary.getDictEntry());
                }

                newForm.setDictionaryValues(dictionaryValues);
            }
        }

        String domainName = newForm.getSiteInfoDomainName();
        if ("SiteInformation".equals(domainName)) {
            if (isNew) {
                request.setAttribute("key", "siteInformation.add.title");
            } else {
                request.setAttribute("key", "siteInformation.edit.title");
            }
        } else if ("ResultConfiguration".equals(domainName)) {
            if (isNew) {
                request.setAttribute("key", "resultConfiguration.add.title");
            } else {
                request.setAttribute("key", "resultConfiguration.edit.title");
            }
        }

        return findForward(FWD_SUCCESS, newForm);
    }

    private void setupFormForRequest(SiteInformationForm form, HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.contains("NonConformityConfiguration")) {
            form.setSiteInfoDomainName("non_conformityConfiguration");
            form.setFormName("NonConformityConfigurationForm");
            form.setFormAction("NonConformityConfiguration");

        } else if (path.contains("WorkplanConfiguration")) {
            form.setSiteInfoDomainName("WorkplanConfiguration");
            form.setFormName("WorkplanConfigurationForm");
            form.setFormAction("WorkplanConfiguration");

        } else if (path.contains("PrintedReportsConfiguration")) {
            form.setSiteInfoDomainName("PrintedReportsConfiguration");
            form.setFormName("PrintedReportsConfigurationForm");
            form.setFormAction("PrintedReportsConfiguration");

        } else if (path.contains("SampleEntryConfig")) {
            form.setSiteInfoDomainName("sampleEntryConfig");
            form.setFormName("sampleEntryConfigForm");
            form.setFormAction("SampleEntryConfig");

        } else if (path.contains("ResultConfiguration")) {
            form.setSiteInfoDomainName("ResultConfiguration");
            form.setFormName("resultConfigurationForm");
            form.setFormAction("ResultConfiguration");

        } else if (path.contains("MenuStatementConfig")) {
            form.setSiteInfoDomainName("MenuStatementConfig");
            form.setFormName("MenuStatementConfigForm");
            form.setFormAction("MenuStatementConfig");

        } else if (path.contains("PatientConfiguration")) {
            form.setSiteInfoDomainName("PaitientConfiguration");
            form.setFormName("PatientConfigurationForm");
            form.setFormAction("PatientConfiguration");

        } else if (path.contains("ValidationConfiguration")) {
            form.setSiteInfoDomainName("validationConfig");
            form.setFormName("ValidationConfigurationForm");
            form.setFormAction("ValidationConfiguration");

        } else {
            form.setSiteInfoDomainName("SiteInformation");
            form.setFormName("SiteInformationForm");
            form.setFormAction("SiteInformation");
        }

        form.setCancelAction("Cancel" + form.getFormAction());
    }

    private String getInstruction(SiteInformation siteInformation) {
        String instruction = MessageUtil.getMessage(siteInformation.getInstructionKey());
        if (GenericValidator.isBlankOrNull(instruction)) {
            instruction = MessageUtil.getMessage(siteInformation.getDescriptionKey());
        }

        return GenericValidator.isBlankOrNull(instruction) ? siteInformation.getDescription() : instruction;
    }

    private void setLocalizationValues(SiteInformationForm form, SiteInformation siteInformation) {
        if ("localization".equals(siteInformation.getTag())) {
            form.setLocalization(localizationService.get(siteInformation.getValue()));
        }
    }

    private Boolean isEditable(SiteInformation siteInformation) {
        if (ACCESSION_NUMBER_PREFIX.endsWith(siteInformation.getName())) {
            return sampleService.getCount() == 0;
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = { "/NonConformityConfiguration", "/WorkplanConfiguration", "/PrintedReportsConfiguration",
            "/SampleEntryConfig", "/ResultConfiguration", "/MenuStatementConfig", "/PatientConfiguration",
            "/ValidationConfiguration", "/SiteInformation" }, method = RequestMethod.POST)
    public ModelAndView showUpdateSiteInformation(HttpServletRequest request, HttpServletResponse response,
            @ModelAttribute("form") @Valid SiteInformationForm form, BindingResult result, SessionStatus status,
            RedirectAttributes redirectAttributes) {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }
        String forward;

        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "false");
        request.setAttribute(NEXT_DISABLED, "false");

        String id = request.getParameter(ID);
        boolean isNew = id == null || id.equals("0");

        // N.B. The reason for this branch is that localization does not actually update
        // site information, it updates the
        // localization table
        if ("localization".equals(form.getTag())) {
            String localizationId = form.getValue();
            forward = validateAndUpdateLocalization(request, localizationId, form.getLocalization());
        } else {
            forward = validateAndUpdateSiteInformation(request, response, form, isNew);
        }
        // makes the changes take effect immediately
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        DisplayListService.getInstance().refreshLists();
        if (FWD_SUCCESS_INSERT.equals(forward)) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            // signal to remove form from session
            status.setComplete();
        }
        return findForward(forward, form);
    }

    private String validateAndUpdateLocalization(HttpServletRequest request, String localizationId,
            Localization newLocalization) {
        Localization localization = localizationService.get(localizationId);
        localization.setSysUserId(getSysUserId(request));
        String forward = FWD_SUCCESS_INSERT;
        if (localizationService.languageChanged(localization, newLocalization)) {
            Errors errors;
            for (Locale locale : newLocalization.getAllActiveLocales()) {
                localization.setLocalizedValue(locale, newLocalization.getLocalizedValue(locale));
            }
            try {
                localizationService.update(localization);
            } catch (LIMSRuntimeException e) {
                errors = new BaseErrors();
                errors.reject("errors.UpdateException");
                saveErrors(errors);
                forward = FWD_FAIL_INSERT;
            }
        }
        return forward;
    }

    public String validateAndUpdateSiteInformation(HttpServletRequest request, HttpServletResponse response,
            SiteInformationForm form, boolean newSiteInformation) {

        String name = form.getParamName();
        String value = form.getValue();
        Errors errors = new BaseErrors();

        if (!isValid(request, name, value, errors)) {
            return FWD_FAIL_INSERT;
        }

        String forward = FWD_SUCCESS_INSERT;
        SiteInformation siteInformation = new SiteInformation();

        if (newSiteInformation) {
            siteInformation.setName(name);
            siteInformation.setDescription(form.getDescription());
            siteInformation.setValueType("text");
            siteInformation.setEncrypted(form.isEncrypted());
            siteInformation.setDomain(SITE_IDENTITY_DOMAIN);
        } else {
            siteInformation = siteInformationService.get(request.getParameter(ID));
        }

        siteInformation.setValue(value);
        siteInformation.setSysUserId(getSysUserId(request));

        String domainName = form.getSiteInfoDomainName();

        if ("SiteInformation".equals(domainName)) {
            siteInformation.setDomain(SITE_IDENTITY_DOMAIN);
        } else if ("ResultConfiguration".equals(domainName)) {
            siteInformation.setDomain(RESULT_CONFIG_DOMAIN);
        }
        try {
            siteInformationService.persistData(siteInformation, newSiteInformation);
            if (siteInformation.getName().equals(Property.DEFAULT_LANG_LOCALE.getDBName())) {
                localeResolver.setLocale(request, response, Locale.forLanguageTag(siteInformation.getValue()));
            }
        } catch (LIMSRuntimeException e) {
            String errorMsg;
            if (e.getCause() instanceof StaleObjectStateException) {

                errorMsg = "errors.OptimisticLockException";

            } else {
                errorMsg = "errors.UpdateException";
            }

            errors.reject(errorMsg);
            saveErrors(errors);

            // disable previous and next
            request.setAttribute(PREVIOUS_DISABLED, TRUE);
            request.setAttribute(NEXT_DISABLED, TRUE);
            forward = FWD_FAIL_INSERT;
        }
        return forward;
    }

    private boolean isValid(HttpServletRequest request, String name, String value, Errors errors) {
        if (GenericValidator.isBlankOrNull(name)) {
            errors.reject("error.SiteInformation.name.required");
            saveErrors(errors);

            return false;
        }

        if ("phone format".equals(name) && !PhoneNumberService.validatePhoneFormat(value)) {
            errors.reject("error.SiteInformation.phone.format");
            saveErrors(errors);

            return false;
        }

        return true;
    }

    /*
     * @RequestMapping(value = { "/UpdateNextPreviousNonConformityConfiguration",
     * "/UpdateNextPreviousWorkplanConfiguration",
     * "/UpdateNextPreviousPrintedReportsConfiguration",
     * "/UpdateNextPreviousSampleEntryConfig",
     * "/UpdateNextPreviousResultConfiguration",
     * "/UpdateNextPreviousMenuStatementConfig",
     * "/UpdateNextPreviousPatientConfiguration",
     * "/UpdateNextPreviousSiteInformation" }, method = RequestMethod.POST) public
     * ModelAndView showNextPreviousSiteInformation(HttpServletRequest request,
     *
     * @ModelAttribute("form") SiteInformationForm form, BindingResult result,
     * RedirectAttributes redirectAttributes) { String forward = FWD_SUCCESS_INSERT;
     * request.setAttribute(ALLOW_EDITS_KEY, TRUE);
     * request.setAttribute(PREVIOUS_DISABLED, FALSE);
     * request.setAttribute(NEXT_DISABLED, FALSE);
     *
     * String id = request.getParameter(ID);
     *
     * String start = request.getParameter("startingRecNo"); String direction =
     * request.getParameter("direction");
     *
     * if (id != null && !id.equals("0")) { String updateResponse =
     * validateAndUpdateSiteInformation(request, form, false);
     *
     * if (updateResponse.equals(FWD_FAIL_INSERT)) { return
     * getForward(findForward(FWD_FAIL_INSERT, form), id, start, null); } }
     *
     * SiteInformation siteInformation = new SiteInformation();
     * siteInformation.setId(id);
     *
     * try {
     *
     * SiteInformationDAO SiteInformationDAO = new SiteInformationDAOImpl();
     *
     * SiteInformationDAO.getData(siteInformation);
     *
     * if (FWD_NEXT.equals(direction)) {
     *
     * List<SiteInformation> SiteInformations = SiteInformationDAO
     * .getNextSiteInformationRecord(siteInformation.getId());
     *
     * if (SiteInformations != null && SiteInformations.size() > 0) {
     * siteInformation = SiteInformations.get(0);
     * SiteInformationDAO.getData(siteInformation); if (SiteInformations.size() < 2)
     * { // disable next button request.setAttribute(NEXT_DISABLED, TRUE); } id =
     * siteInformation.getId(); } else { // just disable next button
     * request.setAttribute(NEXT_DISABLED, TRUE); } }
     *
     * if (FWD_PREVIOUS.equals(direction)) {
     *
     * List<SiteInformation> SiteInformations = SiteInformationDAO
     * .getPreviousSiteInformationRecord(siteInformation.getId());
     *
     * if (SiteInformations != null && SiteInformations.size() > 0) {
     * siteInformation = SiteInformations.get(0);
     * SiteInformationDAO.getData(siteInformation); if (SiteInformations.size() < 2)
     * { // disable previous button request.setAttribute(PREVIOUS_DISABLED, TRUE); }
     * id = siteInformation.getId(); } else { // just disable next button
     * request.setAttribute(PREVIOUS_DISABLED, TRUE); } }
     *
     * } catch (LIMSRuntimeException e) { request.setAttribute(ALLOW_EDITS_KEY,
     * FALSE); // disable previous and next request.setAttribute(PREVIOUS_DISABLED,
     * TRUE); request.setAttribute(NEXT_DISABLED, TRUE); forward = FWD_FAIL_INSERT;
     * } if (forward.equals(FWD_FAIL_INSERT)) { return findForward(forward, form); }
     * redirectAttributes.addFlashAttribute(FWD_SUCCESS, true); return new
     * ModelAndView(findForward(forward));
     *
     * }
     */

    @RequestMapping(value = { "/CancelNonConformityConfiguration", "/CancelWorkplanConfiguration",
            "/CancelPrintedReportsConfiguration", "/CancelSampleEntryConfig", "/CancelResultConfiguration",
            "/CancelMenuStatementConfig", "/CancelPatientConfiguration", "/CancelValidationConfiguration",
            "/CancelSiteInformation" }, method = RequestMethod.GET)
    public ModelAndView cancelSiteInformation(HttpServletRequest request, SessionStatus status) {
        status.setComplete();
        return findForward(FWD_CANCEL, new SiteInformationForm());
    }

    @Override
    protected String findLocalForward(String forward) {
        String pathNoSuffix = URLUtil.getReourcePathFromRequest(request);
        if (FWD_SUCCESS.equals(forward)) {
            return "siteInformationDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            String url = pathNoSuffix + "Menu";
            return "redirect:" + url;
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            String url = pathNoSuffix;
            return "redirect:" + url;
        } else if (FWD_CANCEL.equals(forward)) {
            String prefix = "Cancel";
            String url = pathNoSuffix.substring(pathNoSuffix.indexOf(prefix) + prefix.length()) + "Menu";
            return "redirect:" + url;
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return (String) request.getAttribute("key");
    }

    @Override
    protected String getPageSubtitleKey() {
        return (String) request.getAttribute("key");
    }
}
