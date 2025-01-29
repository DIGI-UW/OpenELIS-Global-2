package org.openelisglobal.testconfiguration.controller.rest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.hibernate.HibernateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.testconfiguration.form.UomRenameEntryForm;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class UomRenameEntryRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "uomId", "nameEnglish" };

    @Autowired
    UnitOfMeasureService unitOfMeasureService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/UomRenameEntry")
    public UomRenameEntryForm showUomRenameEntry(HttpServletRequest request) {
        UomRenameEntryForm form = new UomRenameEntryForm();
        form.setUomList(DisplayListService.getInstance().getList(DisplayListService.ListType.UNIT_OF_MEASURE));

        // return findForward(FWD_SUCCESS, form);
        return form;
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

    @PostMapping(value = "/UomRenameEntry")
    public UomRenameEntryForm updateUomRenameEntry(HttpServletRequest request,
            @RequestBody @Valid UomRenameEntryForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            form.setUomList(DisplayListService.getInstance().getList(DisplayListService.ListType.UNIT_OF_MEASURE));
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }
        String uomId = form.getUomId();
        String nameEnglish = form.getNameEnglish();
        String userId = getSysUserId(request);

        updateUomNames(uomId, nameEnglish, userId);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
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
