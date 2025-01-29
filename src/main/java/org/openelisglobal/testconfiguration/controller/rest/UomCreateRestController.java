package org.openelisglobal.testconfiguration.controller.rest;

import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.testconfiguration.form.UomCreateForm;
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
public class UomCreateRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "uomEnglishName" };

    public static final String NAME_SEPARATOR = "$";

    @Autowired
    UnitOfMeasureService unitOfMeasureService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/UomCreate")
    public UomCreateForm showUomCreate(HttpServletRequest request) {
        UomCreateForm form = new UomCreateForm();

        setupDisplayItems(form);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(UomCreateForm form) {
        form.setExistingUomList(DisplayListService.getInstance().getList(DisplayListService.ListType.UNIT_OF_MEASURE));
        form.setInactiveUomList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.UNIT_OF_MEASURE_INACTIVE));
        List<UnitOfMeasure> uoms = unitOfMeasureService.getAll();
        form.setExistingEnglishNames(getExistingUomNames(uoms, Locale.ENGLISH));
        form.setExistingFrenchNames(getExistingUomNames(uoms, Locale.FRENCH));
    }

    private String getExistingUomNames(List<UnitOfMeasure> uoms, Locale locale) {
        StringBuilder builder = new StringBuilder(NAME_SEPARATOR);

        for (UnitOfMeasure uom : uoms) {
            builder.append(uom.getLocalization().getLocalizedValue(locale));
            builder.append(NAME_SEPARATOR);
        }

        return builder.toString();
    }

    @PostMapping(value = "/UomCreate")
    public UomCreateForm postUomCreate(HttpServletRequest request, @RequestBody @Valid UomCreateForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String identifyingName = form.getUomEnglishName();
        String userId = getSysUserId(request);

        // Localization localization =
        // createLocalization(dynaform.getUomFrenchName(), identifyingName,
        // userId);

        UnitOfMeasure unitOfMeasure = createUnitOfMeasure(identifyingName, userId);

        try {
            unitOfMeasureService.insert(unitOfMeasure);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.UNIT_OF_MEASURE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.UNIT_OF_MEASURE_INACTIVE);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private UnitOfMeasure createUnitOfMeasure(String identifyingName, String userId) {
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setDescription(identifyingName);
        unitOfMeasure.setUnitOfMeasureName(identifyingName);
        return unitOfMeasure;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "uomCreateDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/UomCreate";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "uomCreateDefinition";
        } else {
            return "PageNotFound";
        }
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
