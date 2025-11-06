package org.openelisglobal.testconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panel.valueholder.PanelSortOrderComparator;
import org.openelisglobal.testconfiguration.action.PanelTestConfigurationUtil;
import org.openelisglobal.testconfiguration.action.SampleTypePanel;
import org.openelisglobal.testconfiguration.form.PanelOrderForm;
import org.openelisglobal.testconfiguration.validator.PanelOrderFormValidator;
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
public class PanelOrderController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "jsonChangeList" };

    @Autowired
    PanelOrderFormValidator formValidator;
    @Autowired
    PanelService panelService;
    @Autowired
    PanelTestConfigurationUtil panelTestConfigurationUtil;

    @RequestMapping(value = "/PanelOrder", method = RequestMethod.GET)
    public ModelAndView showPanelOrder(HttpServletRequest request) {
        PanelOrderForm form = new PanelOrderForm();

        setupDisplayItems(form);

        return findForward(FWD_SUCCESS, form);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    protected void setupDisplayItems(PanelOrderForm form) {
        form.setPanelList(DisplayListService.getInstance().getList(DisplayListService.ListType.PANELS));
        form.setExistingSampleTypeList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE));

        HashMap<String, List<Panel>> existingSampleTypePanelMap = panelTestConfigurationUtil
                .createTypeOfSamplePanelMap(true);
        HashMap<String, List<Panel>> inactiveSampleTypePanelMap = panelTestConfigurationUtil
                .createTypeOfSamplePanelMap(false);
        List<SampleTypePanel> sampleTypePanelsExists = new ArrayList<>();
        List<SampleTypePanel> sampleTypePanelsInactive = new ArrayList<>();

        for (IdValuePair typeOfSample : DisplayListService.getInstance()
                .getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE)) {
            SampleTypePanel sampleTypePanel = new SampleTypePanel(typeOfSample.getValue());
            sampleTypePanel.setPanels(existingSampleTypePanelMap.get(typeOfSample.getValue()));
            if (sampleTypePanel.getPanels() != null && sampleTypePanel.getPanels().size() > 0) {
                Collections.sort(sampleTypePanel.getPanels(), PanelSortOrderComparator.SORT_ORDER_COMPARATOR);
            }

            sampleTypePanelsExists.add(sampleTypePanel);
            SampleTypePanel sampleTypePanelInactive = new SampleTypePanel(typeOfSample.getValue());
            sampleTypePanelInactive.setPanels(inactiveSampleTypePanelMap.get(typeOfSample.getValue()));
            if (sampleTypePanelInactive.getPanels() != null && sampleTypePanelInactive.getPanels().size() > 0) {
                Collections.sort(sampleTypePanelInactive.getPanels(), PanelSortOrderComparator.SORT_ORDER_COMPARATOR);
            }

            sampleTypePanelsInactive.add(sampleTypePanelInactive);
        }

        form.setExistingPanelList(sampleTypePanelsExists);
        form.setInactivePanelList(sampleTypePanelsInactive);
    }

    @RequestMapping(value = "/PanelOrder", method = RequestMethod.POST)
    public ModelAndView postPanelOrder(HttpServletRequest request, @ModelAttribute("form") @Valid PanelOrderForm form,
            BindingResult result) throws ParseException {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }

        String changeList = form.getJsonChangeList();

        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(changeList);
        List<ActivateSet> orderSet = getActivateSetForActions("panels", obj, parser);
        List<Panel> panels = new ArrayList<>();

        String currentUserId = getSysUserId(request);
        for (ActivateSet sets : orderSet) {
            Panel panel = panelService.getPanelById(sets.id);
            panel.setSortOrderInt(sets.sortOrder);
            panel.setSysUserId(currentUserId);
            panels.add(panel);
        }

        try {
            panelService.updateAll(panels);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS_INACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS_ACTIVE);

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private class ActivateSet {
        public String id;
        public Integer sortOrder;
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
            return "panelOrderDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/PanelOrder";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "panelOrderDefinition";
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
