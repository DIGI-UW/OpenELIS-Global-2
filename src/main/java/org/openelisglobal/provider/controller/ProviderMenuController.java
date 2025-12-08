package org.openelisglobal.provider.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.provider.form.ProviderMenuForm;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProviderMenuController extends BaseMenuController<Provider> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIds*", "searchString" };

    @Autowired
    private ProviderService providerService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = { "/ProviderMenu", "/SearchProviderMenu" }, method = RequestMethod.GET)
    public ModelAndView showProviderMenu(HttpServletRequest request, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String forward;
        ProviderMenuForm form = new ProviderMenuForm();

        request.setAttribute(ALLOW_EDITS_KEY, "false");
        forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            return findForward(forward, form);
        } else {
            request.setAttribute("menuDefinition", "ProviderMenuDefinition");
            addFlashMsgsToRequest(request);
            return findForward(forward, form);
        }
    }

    @Override
    protected List<Provider> createMenuList(AdminOptionMenuForm<Provider> form, HttpServletRequest request) {

        List<Provider> providers = new ArrayList<>();

        int startingRecNo = this.getCurrentStartingRecNo(request);

        if (YES.equals(request.getParameter("search"))) {
            providers = providerService.getPagesOfSearchedProviders(startingRecNo,
                    request.getParameter("searchString"));
        } else {
            providers = providerService.getPagesOfSearchedProviders(startingRecNo, "");
        }

        request.setAttribute("menuDefinition", "ProviderMenuDefinition");

        // bugzilla 1411 set pagination variables
        // bugzilla 2372 set pagination variables for searched results
        if (YES.equals(request.getParameter("search"))) {
            request.setAttribute(MENU_TOTAL_RECORDS, String
                    .valueOf(providerService.getTotalSearchedProviderCount(request.getParameter("searchString"))));
            request.setAttribute(SEARCHED_STRING, request.getParameter("searchString"));
        } else {
            request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(providerService.getCount()));
        }

        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));
        int numOfRecs = 0;
        if (providers != null) {
            if (providers.size() > Integer
                    .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))) {
                numOfRecs = Integer
                        .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
            } else {
                numOfRecs = providers.size();
            }
            numOfRecs--;
        }
        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
        // end bugzilla 1411

        // bugzilla 2372
        request.setAttribute(MENU_SEARCH_BY_TABLE_COLUMN, "provider.providerName");
        // bugzilla 2372 set up a seraching mode so the next and previous action will
        // know
        // what to do

        if (YES.equals(request.getParameter("search"))) {

            request.setAttribute(IN_MENU_SELECT_LIST_HEADER_SEARCH, "true");
        }

        return providers;
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false";
    }

    @Override
    protected String getAddDisabled() {
        return "true";
    }

    @Override
    protected String getEditDisabled() {
        return "true";
    }

    @Override
    protected int getPageSize() {
        return Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
    }

    // gnr: Deactivate not Delete
    @RequestMapping(value = "/DeleteProvider", method = RequestMethod.POST)
    public ModelAndView showDeleteProvider(HttpServletRequest request,
            @RequestParam(value = ID, required = false) @Pattern(regexp = "[a-zA-Z0-9 -]*") String id,
            @ModelAttribute("form") @Valid ProviderMenuForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            findForward(FWD_FAIL_DELETE, form);
        }

        String[] IDs = id.split(",");
        List<String> selectedIDs = new ArrayList<>();
        for (int i = 0; i < IDs.length; i++) {
            selectedIDs.add(IDs[i]);
        }
        List<Provider> providers = new ArrayList<>();
        for (int i = 0; i < selectedIDs.size(); i++) {
            Provider provider = new Provider();
            provider.setId(selectedIDs.get(i));
            provider.setSysUserId(getSysUserId(request));
            providers.add(provider);
        }

        try {
            providerService.deactivateProviders(providers);
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);

            String errorMsg;
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                errorMsg = "errors.OptimisticLockException";
            } else {
                errorMsg = "errors.DeleteException";
            }
            result.reject(errorMsg);
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            return findForward(FWD_FAIL_DELETE, form);
        }
        redirectAttributes.addAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_DELETE, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "providerMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/ProviderMenu";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/ProviderMenu";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "provider.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "provider.browse.title";
    }
}
