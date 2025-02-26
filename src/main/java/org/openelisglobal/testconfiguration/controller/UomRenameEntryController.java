package org.openelisglobal.testconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.hibernate.HibernateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.testconfiguration.form.UomRenameEntryForm;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UomRenameEntryController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "uomId", "nameEnglish" };

    @Autowired
    UnitOfMeasureService unitOfMeasureService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/UomRenameEntry", method = RequestMethod.GET)
    public ModelAndView showUomRenameEntry(HttpServletRequest request) {
        UomRenameEntryForm form = new UomRenameEntryForm();
        form.setUomList(DisplayListService.getInstance().getList(DisplayListService.ListType.UNIT_OF_MEASURE));

        return findForward(FWD_SUCCESS, form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "uomRenameDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/UomRenameEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "uomRenameDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @RequestMapping(value = "/UomRenameEntry", method = RequestMethod.POST)
    public ModelAndView updateUomRenameEntry(HttpServletRequest request,
            @ModelAttribute("form") @Valid UomRenameEntryForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            form.setUomList(DisplayListService.getInstance().getList(DisplayListService.ListType.UNIT_OF_MEASURE));
            return findForward(FWD_FAIL_INSERT, form);
        }
        String uomId = form.getUomId();
        String nameEnglish = form.getNameEnglish();
        String userId = getSysUserId(request);

        updateUomNames(uomId, nameEnglish, userId);

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private void updateUomNames(String uomId, String nameEnglish, String userId) {
        UnitOfMeasure unitOfMeasure = unitOfMeasureService.getUnitOfMeasureById(uomId);

        if (unitOfMeasure != null) {

            // not using localization for UOM

            // Localization name = unitOfMeasure.getLocalization();
            //
            // name.setEnglish( nameEnglish.trim() );
            // name.setFrench( nameFrench.trim() );
            // name.setSysUserId( userId );

            unitOfMeasure.setUnitOfMeasureName(nameEnglish.trim());
            unitOfMeasure.setSysUserId(userId);

            try {
                unitOfMeasureService.update(unitOfMeasure);
            } catch (HibernateException e) {
                LogEvent.logDebug(e);
            }
        }

        // Refresh Uom names
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.UNIT_OF_MEASURE);
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
