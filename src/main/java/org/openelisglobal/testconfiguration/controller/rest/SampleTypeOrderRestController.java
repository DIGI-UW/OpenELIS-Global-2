package org.openelisglobal.testconfiguration.controller.rest;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.hibernate.HibernateException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.testconfiguration.form.SampleTypeOrderForm;
import org.openelisglobal.testconfiguration.validator.SampleTypeOrderFormValidator;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
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
public class SampleTypeOrderRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "jsonChangeList" };

    @Autowired
    SampleTypeOrderFormValidator formValidator;
    @Autowired
    TypeOfSampleService typeOfSampleService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/SampleTypeOrder")
    public SampleTypeOrderForm showSampleTypeOrder(HttpServletRequest request) {
        SampleTypeOrderForm form = new SampleTypeOrderForm();

        setupDisplayItems(form);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(SampleTypeOrderForm form) {
        form.setSampleTypeList(DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE));
    }

    private class ActivateSet {
        public String id;
        public Integer sortOrder;
    }

    @PostMapping(value = "/SampleTypeOrder")
    public SampleTypeOrderForm postSampleTypeOrder(HttpServletRequest request,
            @RequestBody @Valid SampleTypeOrderForm form, BindingResult result) throws ParseException {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String changeList = form.getJsonChangeList();

        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(changeList);
        List<ActivateSet> orderSet = getActivateSetForActions("sampleTypes", obj, parser);
        List<TypeOfSample> typeOfSamples = new ArrayList<>();

        String currentUserId = getSysUserId(request);
        for (ActivateSet sets : orderSet) {
            TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(sets.id);
            typeOfSample.setSortOrder(sets.sortOrder);
            typeOfSample.setSysUserId(currentUserId);
            typeOfSamples.add(typeOfSample);
        }

        try {
            typeOfSampleService.updateAll(typeOfSamples);
        } catch (HibernateException e) {
            LogEvent.logDebug(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private List<ActivateSet> getActivateSetForActions(String key, JSONObject root, JSONParser parser) {
        List<ActivateSet> list = new ArrayList<>();

        String action = (String) root.get(key);

        try {
            JSONArray actionArray = (JSONArray) parser.parse(action);

            for (int i = 0; i < actionArray.size(); i++) {
                ActivateSet set = new ActivateSet();
                set.id = String.valueOf(((JSONObject) actionArray.get(i)).get("id"));
                Long longSort = (Long) ((JSONObject) actionArray.get(i)).get("sortOrder");
                set.sortOrder = longSort.intValue();
                list.add(set);
            }
        } catch (ParseException e) {
            LogEvent.logDebug(e);
        }

        return list;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTypeOrderDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SampleTypeOrder";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "sampleTypeOrderDefinition";
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
