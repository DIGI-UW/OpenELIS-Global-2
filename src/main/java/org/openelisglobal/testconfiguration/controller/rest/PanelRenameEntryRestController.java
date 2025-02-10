package org.openelisglobal.testconfiguration.controller.rest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.testconfiguration.form.PanelRenameEntryForm;
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
public class PanelRenameEntryRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "panelId", "nameEnglish", "nameFrench" };

    @Autowired
    PanelService panelService;
    @Autowired
    LocalizationService localizationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/PanelRenameEntry")
    public PanelRenameEntryForm showPanelRenameEntry(HttpServletRequest request) {
        PanelRenameEntryForm form = new PanelRenameEntryForm();
        form.setPanelList(DisplayListService.getInstance().getList(DisplayListService.ListType.PANELS));

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "panelRenameDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/PanelRenameEntry";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "panelRenameDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @PostMapping(value = "/PanelRenameEntry")
    public PanelRenameEntryForm updatePanelRenameEntry(HttpServletRequest request,
            @RequestBody @Valid PanelRenameEntryForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            form.setPanelList(DisplayListService.getInstance().getList(DisplayListService.ListType.PANELS));
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String panelId = form.getPanelId();
        String nameEnglish = form.getNameEnglish();
        String nameFrench = form.getNameFrench();
        String userId = getSysUserId(request);

        updatePanelNames(panelId, nameEnglish, nameFrench, userId);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private void updatePanelNames(String panelId, String nameEnglish, String nameFrench, String userId) {
        Panel panel = panelService.getPanelById(panelId);

        if (panel != null) {

            Localization name = panel.getLocalization();
            name.setEnglish(nameEnglish.trim());
            name.setFrench(nameFrench.trim());
            name.setSysUserId(userId);

            try {
                localizationService.update(name);
            } catch (LIMSRuntimeException e) {
                LogEvent.logDebug(e);
            }
        }
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.PANELS);
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
