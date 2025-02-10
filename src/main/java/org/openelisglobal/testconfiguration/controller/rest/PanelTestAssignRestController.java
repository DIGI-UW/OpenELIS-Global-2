package org.openelisglobal.testconfiguration.controller.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestComparator;
import org.openelisglobal.testconfiguration.action.PanelTests;
import org.openelisglobal.testconfiguration.form.PanelTestAssignForm;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
public class PanelTestAssignRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "panelId", "deactivatePanelId", "currentTests*",
            "availableTests*" };

    @Autowired
    private PanelService panelService;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private TestService testService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/PanelTestAssign")
    public PanelTestAssignForm showPanelTestAssign(@Valid @RequestBody PanelTestAssignForm oldForm,
            BindingResult result, HttpServletRequest request) {
        PanelTestAssignForm form = new PanelTestAssignForm();

        if (!result.hasFieldErrors("panelId")) {
            String panelId = oldForm.getPanelId();
            if (panelId == null) {
                panelId = "";
            }
            form.setPanelId(panelId);
        }

        setupDisplayItems(form);

        addFlashMsgsToRequest(request);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(PanelTestAssignForm form) {
        List<IdValuePair> panels = DisplayListService.getInstance().getList(DisplayListService.ListType.PANELS);

        form.setPanelList(panels);

        if (!GenericValidator.isBlankOrNull(form.getPanelId())) {
            Panel panel = panelService.getPanelById(form.getPanelId());
            IdValuePair panelPair = new IdValuePair(panel.getId(), panel.getLocalizedName());

            List<IdValuePair> tests = new ArrayList<>();

            List<Test> testList = getAllTestsByPanelId(panel.getId(), false);

            PanelTests panelTests = new PanelTests(panelPair);
            HashSet<String> testIdSet = new HashSet<>();

            for (Test test : testList) {
                if (test.isActive()) {
                    tests.add(new IdValuePair(test.getId(), TestServiceImpl.getUserLocalizedTestName(test)));
                    testIdSet.add(test.getId());
                }
            }
            panelTests.setTests(tests, testIdSet);

            form.setSelectedPanel(panelTests);
        }
    }

    public List<Test> getAllTestsByPanelId(String panelId, boolean alphabetical) {
        List<Test> testList = new ArrayList<>();

        List<PanelItem> testLinks = panelItemService.getPanelItemsForPanel(panelId);

        for (PanelItem link : testLinks) {
            testList.add(link.getTest());
        }

        if (alphabetical) {
            Collections.sort(testList, TestComparator.NAME_COMPARATOR);
        }
        return testList;
    }

    @PostMapping(value = "/PanelTestAssign")
    public PanelTestAssignForm postPanelTestAssign(HttpServletRequest request,
            @RequestBody @Valid PanelTestAssignForm form, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String panelId = form.getPanelId();
        String currentUser = getSysUserId(request);
        boolean updatePanel = false;

        Panel panel = panelService.getPanelById(panelId);

        if (!GenericValidator.isBlankOrNull(panelId)) {
            List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);

            List<String> newTestIds = form.getCurrentTests();
            List<Test> newTests = new ArrayList<>();
            for (String testId : newTestIds) {
                newTests.add(testService.get(testId));
            }

            try {
                panelItemService.updatePanelItems(panelItems, panel, updatePanel, currentUser, newTests);
            } catch (LIMSRuntimeException e) {
                LogEvent.logDebug(e);
            }
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS_INACTIVE);

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "panelAssignDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            String url = "/PanelTestAssign?panelId=" + Encode.forUriComponent(request.getParameter("panelId"));
            return "redirect:" + url;
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "panelAssignDefinition";
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
