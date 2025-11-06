package org.openelisglobal.provider.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.provider.form.ProviderMenuForm;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class ProviderMenuRestController extends BaseMenuController<Provider> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIds*", "searchString" };
    private static final String FWD_SUCCESS = "success";
    private static final String FWD_FAIL = "fail";
    private static final String FWD_SUCCESS_INSERT = "success_insert";
    private static final String FWD_FAIL_INSERT = "fail_insert";
    private static final String MENU_FROM_RECORD = "menu_from_record";
    private static final String MENU_TO_RECORD = "menu_to_record";
    private static final String MENU_TOTAL_RECORDS = "menu_total_records";
    private static final String MENU_SEARCH_BY_TABLE_COLUMN = "menu_search_by_table_column";
    private static final String IN_MENU_SELECT_LIST_HEADER_SEARCH = "in_menu_select_list_header_search";
    private static final String SEARCHED_STRING = "searched_string";
    private static final String ERRORS = "error_during_action";
    private static final String VALIDATION = "validation_errors_occurred";

    @Autowired
    private ProviderService providerService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = { "/ProviderMenu", "/SearchProviderMenu" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProviderMenuForm> showProviderMenu(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ProviderMenuForm form = new ProviderMenuForm();

        request.setAttribute(ALLOW_EDITS_KEY, "false");
        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            // Errors errors = new BaseErrors();
            // errors.reject("error.generic");
            // redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            // return findForward(forward, form);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } else {
            // request.setAttribute("menuDefinition", "ProviderMenuDefinition");
            addFlashMsgsToRequest(request);
            // return findForward(forward, form);
            List<Provider> providers = createMenuList(form, request);
            // return ResponseEntity.ok(providers);
            form.setProviders(providers);
            return ResponseEntity.ok(form);
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
        form.setToRecordCount(String.valueOf(endingRecNo));
        form.setFromRecordCount(String.valueOf(startingRecNo));
        form.setTotalRecordCount(String.valueOf(String.valueOf(providerService.getCount())));

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
    @PostMapping(value = "/DeleteProvider", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> showDeleteProvider(HttpServletRequest request,
            @RequestParam(value = ID, required = false) @Pattern(regexp = "[a-zA-Z0-9, -]*") String id,
            @Valid @ModelAttribute("form") ProviderMenuForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (result.hasErrors()) {
            // redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            // findForward(FWD_FAIL_DELETE, form);
            return ResponseEntity.badRequest().body(VALIDATION);
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
            return ResponseEntity.ok("Providers successfully deactivated.");
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while deleting providers.");

            // String errorMsg;
            // if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
            // errorMsg = "errors.OptimisticLockException";
            // } else {
            // errorMsg = "errors.DeleteException";
            // }
            // result.reject(errorMsg);
            // redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
            // return findForward(FWD_FAIL_DELETE, form);

        }
        // redirectAttributes.addAttribute(FWD_SUCCESS, true);
        // return findForward(FWD_SUCCESS_DELETE, form);
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
