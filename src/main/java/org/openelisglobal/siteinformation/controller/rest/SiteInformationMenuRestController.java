package org.openelisglobal.siteinformation.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.siteinformation.form.SiteInformationMenuForm;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.validator.SiteInformationMenuFormValidator;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
public class SiteInformationMenuRestController extends BaseMenuController<SiteInformation> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIds*" };

    @Autowired
    private SiteInformationMenuFormValidator formValidator;
    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private LocalizationService localizationService;

    private static final String TITLE_KEY = "titleKey";

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = { "/NonConformityConfigurationMenu", "/WorkplanConfigurationMenu",
            "/PrintedReportsConfigurationMenu", "/SampleEntryConfigMenu", "/ResultConfigurationMenu",
            "/MenuStatementConfigMenu", "/PatientConfigurationMenu", "/ValidationConfigurationMenu",
            "/SiteInformationMenu" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showSiteInformationMenu(HttpServletRequest request, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        SiteInformationMenuForm form = new SiteInformationMenuForm();
        setupFormForRequest(form, request);

        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        } else {
            addFlashMsgsToRequest(request);
            return ResponseEntity.ok(form);
        }
    }

    private void setupFormForRequest(SiteInformationMenuForm form, HttpServletRequest request) {
        String path = request.getServletPath();

        if (path.contains("NonConformityConfiguration")) {
            form.setSiteInfoDomainName("non_conformityConfiguration");
            form.setFormName("NonConformityConfigurationMenuForm");
        } else if (path.contains("WorkplanConfiguration")) {
            form.setSiteInfoDomainName("WorkplanConfiguration");
            form.setFormName("WorkplanConfigurationMenuForm");
        } else if (path.contains("PrintedReportsConfiguration")) {
            form.setSiteInfoDomainName("PrintedReportsConfiguration");
            form.setFormName("PrintedReportsConfigurationMenuForm");
        } else if (path.contains("SampleEntryConfig")) {
            form.setSiteInfoDomainName("sampleEntryConfig");
            form.setFormName("sampleEntryConfigMenuForm");
        } else if (path.contains("ResultConfiguration")) {
            form.setSiteInfoDomainName("ResultConfiguration");
            form.setFormName("resultConfigurationMenuForm");
        } else if (path.contains("MenuStatementConfig")) {
            form.setSiteInfoDomainName("MenuStatementConfig");
            form.setFormName("MenuStatementConfigMenuForm");
        } else if (path.contains("PatientConfiguration")) {
            form.setSiteInfoDomainName("PatientConfiguration");
            form.setFormName("PatientConfigurationMenuForm");
        } else if (path.contains("ValidationConfiguration")) {
            form.setSiteInfoDomainName("validationConfig");
            form.setFormName("ValidationConfigurationMenuForm");
        } else {
            form.setSiteInfoDomainName("SiteInformation");
            form.setFormName("siteInformationMenuForm");
        }

        form.setFormMethod(RequestMethod.POST);
    }

    @Override
    protected List<SiteInformation> createMenuList(AdminOptionMenuForm<SiteInformation> form,
            HttpServletRequest request) {
        List<SiteInformation> configurationList;

        if (!(form instanceof SiteInformationMenuForm)) {
            throw new ClassCastException();
        }

        SiteInformationMenuForm siteInformationMenuForm = (SiteInformationMenuForm) form;

        String domainName = siteInformationMenuForm.getSiteInfoDomainName();
        String dbDomainName = null;
        if ("SiteInformation".equals(domainName)) {
            dbDomainName = "siteIdentity";
            request.setAttribute(TITLE_KEY, "siteInformation.browse.title");
        } else if ("ResultConfiguration".equals(domainName)) {
            dbDomainName = "resultConfiguration";
            request.setAttribute(TITLE_KEY, "resultConfiguration.browse.title");
        } else if ("sampleEntryConfig".equals(domainName)) {
            dbDomainName = "sampleEntryConfig";
            request.setAttribute(TITLE_KEY, "sample.entry.browse.title");
        } else if ("PrintedReportsConfiguration".equals(domainName)) {
            dbDomainName = "printedReportsConfig";
            request.setAttribute(TITLE_KEY, "printedReportsConfiguration.browse.title");
        } else if ("WorkplanConfiguration".equals(domainName)) {
            dbDomainName = "workplanConfig";
            request.setAttribute(TITLE_KEY, "workplanConfiguration.browse.title");
        } else if ("non_conformityConfiguration".equals(domainName)) {
            dbDomainName = "non_conformityConfig";
            request.setAttribute(TITLE_KEY, "nonConformityConfiguration.browse.title");
        } else if ("PatientConfiguration".equals(domainName)) {
            dbDomainName = "patientEntryConfig";
            request.setAttribute(TITLE_KEY, "patientEntryConfiguration.browse.title");
        } else if ("MenuStatementConfig".equals(domainName)) {
            dbDomainName = "MenuStatementConfig";
            request.setAttribute(TITLE_KEY, "MenuStatementConfig.browse.title");
        } else if ("validationConfig".equals(domainName)) {
            dbDomainName = "validationConfig";
            request.setAttribute(TITLE_KEY, "validationConfig.browse.title");
        }

        int startingRecNo = Integer.parseInt((String) request.getAttribute("startingRecNo"));

        request.setAttribute("menuDefinition", "SiteInformationMenuDefinition");

        configurationList = siteInformationService.getPageOfSiteInformationByDomainName(startingRecNo, dbDomainName);
        for (SiteInformation siteInformation : configurationList) {
            if ("localization".equals(siteInformation.getTag())) {
                siteInformation.setLocalization(localizationService.get(siteInformation.getValue()));
            }
        }

        hideEncryptedFields(configurationList);

        setDisplayPageBounds(request, configurationList == null ? 0 : configurationList.size(), startingRecNo,
                siteInformationService.getCountForDomainName(dbDomainName));

        return configurationList;
    }

    private void hideEncryptedFields(List<SiteInformation> siteInformationList) {
        for (SiteInformation siteInformation : siteInformationList) {
            if (siteInformation.isEncrypted() && !GenericValidator.isBlankOrNull(siteInformation.getValue())) {
                siteInformation.setValue(siteInformation.getValue().replaceAll(".", "*"));
            }
        }
    }

    @Override
    protected String getDeactivateDisabled() {
        return "true";
    }

    @Override
    protected String getAddDisabled() {
        return "true";
    }

    @GetMapping(value = { "/DeleteMenuStatementConfig", "/DeleteWorkplanConfiguration", "/DeletePatientConfiguration",
            "/DeleteNonConformityConfiguration", "/DeleteResultConfiguration", "/DeletePrintedReportsConfiguration",
            "/DeleteSiteInformation" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> showDeleteSiteInformation(HttpServletRequest request,
            @RequestBody @Valid SiteInformationMenuForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        List<String> selectedIDs = form.getSelectedIDs();
        try {
            siteInformationService.deleteAll(selectedIDs, getSysUserId(request));
            // for (String siteInformationId : selectedIDs) {
            // siteInformationDAO.deleteData(siteInformationId, getSysUserId(request));
            // siteInformationService.delete(siteInformationId, getSysUserId(request));
            // }
        } catch (LIMSRuntimeException e) {
            String errorMsg;
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                errorMsg = "errors.OptimisticLockException";
            } else {
                errorMsg = "errors.DeleteException";
            }
            result.reject(errorMsg);
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.getAllErrors());
        }

        ConfigurationProperties.loadDBValuesIntoConfiguration();

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return ResponseEntity.ok().body("Delete successful");
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "siteInfoMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return (String) request.getAttribute(TITLE_KEY);
    }

    @Override
    protected String getPageSubtitleKey() {
        return (String) request.getAttribute(TITLE_KEY);
    }
}
