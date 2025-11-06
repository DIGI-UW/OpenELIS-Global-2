package org.openelisglobal.dictionary.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
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
import org.openelisglobal.dictionary.form.DictionaryMenuForm;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
@SuppressWarnings("unused")
public class DictionaryMenuRestController extends BaseMenuController<Dictionary> {

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIDs*" };

    @Autowired
    DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = { "/DictionaryMenu",
            "/SearchDictionaryMenu" }, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<?> showDictionaryMenu(HttpServletRequest request, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        DictionaryMenuForm form = new DictionaryMenuForm();

        String forward = performMenuAction(form, request);
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        } else {
            request.setAttribute("menuDefinition", "DictionaryMenuDefinition");
            addFlashMsgsToRequest(request);
            return ResponseEntity.ok(form);
        }
    }

    @RequestMapping(value = "/dictionary-categories", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public List<DictionaryCategory> fetchDictionaryCategories() {
        return dictionaryCategoryService.getAll();
    }

    @RequestMapping(value = "/DeleteDictionary", method = RequestMethod.POST)
    public ResponseEntity<?> showDeleteDictionary(HttpServletRequest request,
            @RequestParam(value = ID, required = false) String id) {

        String[] IDs = id.split(",");
        List<String> selectedIDs = new ArrayList<>();
        for (int i = 0; i < IDs.length; i++) {
            selectedIDs.add(IDs[i]);
        }

        List<Dictionary> dictionaries = new ArrayList<>();
        for (int i = 0; i < selectedIDs.size(); i++) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(selectedIDs.get(i));
            dictionary.setSysUserId(getSysUserId(request));
            dictionaries.add(dictionary);
        }

        try {
            dictionaryService.deleteAll(dictionaries);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {

            } else {

            }
            return new ResponseEntity<>("Dictionary Menu Failed to deleted successfully",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Dictionary Menu deleted successfully", HttpStatus.OK);
    }

    @Override
    protected List<Dictionary> createMenuList(AdminOptionMenuForm<Dictionary> form, HttpServletRequest request) {
        List<Dictionary> dictionaries;
        int startingRecNo = Integer.parseInt((String) request.getAttribute("startingRecNo"));
        int total;
        if (YES.equals(request.getParameter("search"))) {
            dictionaries = dictionaryService.getPagesOfSearchedDictionaries(startingRecNo,
                    request.getParameter("searchString"));
            total = dictionaryService.getCountSearchedDictionaries(request.getParameter("searchString"));
        } else {
            dictionaries = dictionaryService.getPage(startingRecNo);
            total = dictionaryService.getCount();
        }

        request.setAttribute("menuDefinition", "DictionaryMenuDefinition");
        request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(total));
        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));

        int numOfRecs = 0;
        if (dictionaries.size() > Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))) {
            numOfRecs = Integer
                    .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        } else {
            numOfRecs = dictionaries.size();
        }
        numOfRecs--;
        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
        form.setToRecordCount(String.valueOf(endingRecNo));
        form.setFromRecordCount(String.valueOf(startingRecNo));
        form.setTotalRecordCount(String.valueOf(total));

        request.setAttribute(MENU_SEARCH_BY_TABLE_COLUMN, "dictionary.dictEntry");

        if (YES.equals(request.getParameter("search"))) {
            request.setAttribute(IN_MENU_SELECT_LIST_HEADER_SEARCH, "true");
        }
        return dictionaries;
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false";
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "dictionaryMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/DictionaryMenu";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/DictionaryMenu";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "dictionary.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "dictionary.browse.title";
    }
}
