package org.openelisglobal.dictionary.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSFrozenRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.form.DictionaryForm;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.validator.DictionaryFormValidator;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
@SessionAttributes("form")
@SuppressWarnings("unused")
public class DictionaryRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "dirtyFormFields", "id",
            "selectedDictionaryCategoryId", "isActive", "dictEntry", "localAbbreviation" };

    @Autowired
    private DictionaryFormValidator formValidator;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @ModelAttribute("form")
    public DictionaryForm form() {
        return new DictionaryForm();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/Dictionary", method = RequestMethod.GET)
    public DictionaryForm showDictionary(HttpServletRequest request, @ModelAttribute("dictform") BaseForm oldForm)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        DictionaryForm newForm = resetSessionFormToType(oldForm, DictionaryForm.class);
        newForm.setCancelAction("CancelDictionary");

        String id = request.getParameter(ID) == null ? "" : request.getParameter(ID);
        if (!id.matches(ValidationHelper.ID_REGEX)) {
            id = "";
        }

        setDefaultButtonAttributes(request);

        Dictionary dictionary = new Dictionary();
        dictionary.setId(id);

        if ((id != null) && (!"0".equals(id))) {
            // this is an existing dictionary
            dictionary = dictionaryService.get(id);

            if (dictionaryService.hasNext(id)) {
                request.setAttribute(NEXT_DISABLED, "false");
            }

            if (dictionaryService.hasPrevious(id)) {
                request.setAttribute(PREVIOUS_DISABLED, "false");
            }

        } else { // this is a new dictionary
            dictionary.setIsActive(YES);
        }

        if (dictionary.getId() != null && !dictionary.getId().equals("0")) {
            request.setAttribute(ID, dictionary.getId());
            // bugzilla 2062 initialize selectedDictionaryCategoryId
            if (dictionary.getDictionaryCategory() != null) {
                dictionary.setSelectedDictionaryCategoryId(dictionary.getDictionaryCategory().getId());
            }
        }

        // populate form from valueholder
        PropertyUtils.copyProperties(newForm, dictionary);

        List<DictionaryCategory> dictCats = dictionaryCategoryService.getAll();

        newForm.setCategories(dictCats);

        return newForm;
    }

    private void setDefaultButtonAttributes(HttpServletRequest request) {
        request.setAttribute(ALLOW_EDITS_KEY, "true");
        // bugzilla 2062
        request.setAttribute(RECORD_FROZEN_EDIT_DISABLED_KEY, "false");
        request.setAttribute(PREVIOUS_DISABLED, "true");
        request.setAttribute(NEXT_DISABLED, "true");
    }

    @RequestMapping(value = "/NextPreviousDictionary", method = RequestMethod.GET)
    public ResponseEntity<?> showNextPreviousDictionary(HttpServletRequest request) {
        String id = request.getParameter(ID);
        String direction = request.getParameter("direction");

        String nextPrevId = null;
        if (FWD_NEXT.equals(direction)) {
            nextPrevId = dictionaryService.getNext(id).getId();
        } else if (FWD_PREVIOUS.equals(direction)) {
            nextPrevId = dictionaryService.getPrevious(id).getId();
        }
        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(nextPrevId)) {
            Errors errors = new BaseErrors();
            errors.reject("dictionary.nextprev.error");
            saveErrors(errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        String url = "redirect:/Dictionary?ID=" + Encode.forUriComponent(nextPrevId);
        return ResponseEntity.status(HttpStatus.OK).body(url);
    }

    /**
     * @param request
     * @param form
     * @param result
     * @param status
     * @param redirectAttributes
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    @RequestMapping(value = "/Dictionary", method = RequestMethod.POST)
    public ResponseEntity<?> showUpdateDictionary(HttpServletRequest request, @RequestBody @Valid DictionaryForm form,
            BindingResult result, SessionStatus status, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result.getAllErrors());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }

        setDefaultButtonAttributes(request);

        Dictionary dictionary = setupDictionary(form);

        try {
            String id = form.getId();
            if (!(id == null || "0".equals(id))) {
                // UPDATE
                // bugzilla 2062
                boolean isDictionaryFrozenCheckRequired = checkForDictionaryFrozenCheck(form);
                dictionaryService.update(dictionary);
            } else {
                // INSERT
                dictionaryService.insert(dictionary);
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            // 1482
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                result.reject("errors.OptimisticLockException");
            } else if (e.getCause() instanceof LIMSDuplicateRecordException) {
                String messageKey = "dictionary.dictEntryByCategory";
                String msg = MessageUtil.getMessage(messageKey);
                result.reject("errors.DuplicateRecord.activate", new String[] { msg },
                        "errors.DuplicateRecord.activate");
            } else if (e.getCause() instanceof LIMSFrozenRecordException) {
                String messageKey = "dictionary.dictEntry";
                String msg = MessageUtil.getMessage(messageKey);
                result.reject("errors.FrozenRecord", new String[] { msg }, "errors.FrozenRecord");
                // Now disallow further edits RECORD_FROZEN_EDIT_DISABLED_KEY
                // in this case User needs to Exit and come back to refresh form
                // for further updates (this is to restore isDirty() functionality
                // that relies on defaultValues of form
                // --this is needed to determine whether frozen check is required
                request.setAttribute(RECORD_FROZEN_EDIT_DISABLED_KEY, "true");
            } else {
                result.reject("errors.UpdateException");
            }

            saveErrors(result);
            // bugzilla 1485: allow change and try updating again (enable save button)
            // disable previous and next
            request.setAttribute(PREVIOUS_DISABLED, "true");
            request.setAttribute(NEXT_DISABLED, "true");
            return (ResponseEntity<?>) ResponseEntity.badRequest();
        }

        status.setComplete();
        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        DisplayListService.getInstance().refreshLists();
        return ResponseEntity.ok(form);
    }

    private Dictionary setupDictionary(DictionaryForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Dictionary dictionary;
        if (StringUtils.isNotBlank(form.getId()) && !form.getId().equals("0")) {
            dictionary = dictionaryService.get(form.getId());
        } else {
            dictionary = new Dictionary();
        }
        // get sysUserId from login module
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        String sysUserId = String.valueOf(usd.getSystemUserId());
        dictionary.setSysUserId(sysUserId);

        // populate valueholder from form
        PropertyUtils.copyProperties(dictionary, form);

        String selectedCategoryId = form.getSelectedDictionaryCategoryId();
        // bugzilla 2108
        DictionaryCategory dictionaryCategory = dictionaryCategoryService.get(selectedCategoryId);
        dictionary.setDictionaryCategory(dictionaryCategory);
        return dictionary;
    }

    private boolean checkForDictionaryFrozenCheck(DictionaryForm form) {
        boolean isDictionaryFrozenCheckRequired = false;
        // there is an exception to rule of checking whether dictionary record
        // is frozen (can no longer be updated):
        // currenly if only isActive has changed and
        // the current value is 'Y'
        // OR
        // bugzilla 1847: also the local abbreviation can be deleted/updated/inserted at
        // anytime
        String dirtyFormFields = form.getDirtyFormFields();
        String isActiveValue = form.getIsActive();

        String[] dirtyFields = dirtyFormFields
                .split(ConfigurationProperties.getInstance().getPropertyValue("default.idSeparator"), -1);
        List<String> listOfDirtyFields = new ArrayList<>();

        for (int i = 0; i < dirtyFields.length; i++) {
            String dirtyField = dirtyFields[i];
            if (!StringUtil.isNullorNill(dirtyField)) {
                listOfDirtyFields.add(dirtyField);
            }
        }

        List<String> listOfDirtyFieldsNoFrozenCheckRequired = new ArrayList<>();
        listOfDirtyFieldsNoFrozenCheckRequired.add("isActive");
        listOfDirtyFieldsNoFrozenCheckRequired.add("localAbbreviation");

        // bugzilla 1847 : added to exception for frozen check required
        // isActive changed to Y (no frozen check required)
        // localAbbreviation has changed (no frozen check required)
        if (!listOfDirtyFields.isEmpty()) {
            for (int i = 0; i < listOfDirtyFields.size(); i++) {
                String dirtyField = listOfDirtyFields.get(i);
                if (!listOfDirtyFieldsNoFrozenCheckRequired.contains(dirtyField)) {
                    isDictionaryFrozenCheckRequired = true;
                } else {
                    // in case of isActive: need to make sure it changed to YES to be able
                    // to skip isFrozenCheck
                    if (dirtyField.equals("isActive") && !isActiveValue.equals(YES)) {
                        isDictionaryFrozenCheckRequired = true;
                    }
                }
            }
        }
        return isDictionaryFrozenCheckRequired;
    }

    @RequestMapping(value = "/CancelDictionary", method = RequestMethod.GET)
    public ResponseEntity<?> cancelDictionary(HttpServletRequest request, SessionStatus status) {
        status.setComplete();
        return ResponseEntity.status(HttpStatus.OK).body("Cancellation successful");
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "dictionaryDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/DictionaryMenu";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/DictionaryMenu";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "dictionaryDefinition";
        } else if (FWD_CANCEL.equals(forward)) {
            return "redirect:/DictionaryMenu";
        } else {

            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "dictionary.edit.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "dictionary.edit.title";
    }
}
