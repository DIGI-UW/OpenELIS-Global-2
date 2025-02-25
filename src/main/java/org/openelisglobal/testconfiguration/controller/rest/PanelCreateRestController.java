package org.openelisglobal.testconfiguration.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.testconfiguration.action.PanelTestConfigurationUtil;
import org.openelisglobal.testconfiguration.action.SampleTypePanel;
import org.openelisglobal.testconfiguration.form.PanelCreateForm;
import org.openelisglobal.testconfiguration.service.PanelCreateService;
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
public class PanelCreateRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "panelEnglishName", "panelFrenchName",
            "sampleTypeId", };

    @Autowired
    private PanelService panelService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PanelTestConfigurationUtil panelTestConfigurationUtil;
    @Autowired
    private PanelCreateService panelCreateService;

    public static final String NAME_SEPARATOR = "$";

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/PanelCreate")
    public PanelCreateForm showPanelCreate(HttpServletRequest request) {
        PanelCreateForm form = new PanelCreateForm();

        setupDisplayItems(form);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(PanelCreateForm form) {
        HashMap<String, List<Panel>> existingSampleTypePanelMap = panelTestConfigurationUtil
                .createTypeOfSamplePanelMap(true);
        HashMap<String, List<Panel>> inactiveSampleTypePanelMap = panelTestConfigurationUtil
                .createTypeOfSamplePanelMap(false);
        form.setExistingSampleTypeList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE));

        List<Panel> panels = panelService.getAllPanels();
        form.setExistingEnglishNames(getExistingTestNames(panels, Locale.ENGLISH));
        form.setExistingFrenchNames(getExistingTestNames(panels, Locale.FRENCH));

        List<SampleTypePanel> sampleTypePanelsExists = new ArrayList<>();
        List<SampleTypePanel> sampleTypePanelsInactive = new ArrayList<>();

        for (IdValuePair typeOfSample : DisplayListService.getInstance()
                .getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE)) {
            SampleTypePanel sampleTypePanel = new SampleTypePanel(typeOfSample.getValue());
            sampleTypePanel.setPanels(existingSampleTypePanelMap.get(typeOfSample.getValue()));
            sampleTypePanelsExists.add(sampleTypePanel);
            SampleTypePanel sampleTypePanelInactive = new SampleTypePanel(typeOfSample.getValue());
            sampleTypePanelInactive.setPanels(inactiveSampleTypePanelMap.get(typeOfSample.getValue()));
            sampleTypePanelsInactive.add(sampleTypePanelInactive);
        }

        form.setExistingPanelList(sampleTypePanelsExists);
        form.setInactivePanelList(sampleTypePanelsInactive);
    }

    private String getExistingTestNames(List<Panel> panels, Locale locale) {
        StringBuilder builder = new StringBuilder(NAME_SEPARATOR);

        for (Panel panel : panels) {
            builder.append(panel.getLocalization().getLocalizedValue(locale));
            builder.append(NAME_SEPARATOR);
        }

        return builder.toString();
    }

    @PostMapping(value = "/PanelCreate")
    public PanelCreateForm postPanelCreate(HttpServletRequest request, @RequestBody @Valid PanelCreateForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String identifyingName = form.getPanelEnglishName();
        String sampleTypeId = form.getSampleTypeId();
        String systemUserId = getSysUserId(request);

        Localization localization = createLocalization(form.getPanelFrenchName(), identifyingName, systemUserId);

        Panel panel = createPanel(identifyingName, systemUserId);
        SystemModule workplanModule = createSystemModule("Workplan", identifyingName, systemUserId);
        SystemModule resultModule = createSystemModule("LogbookResults", identifyingName, systemUserId);
        SystemModule validationModule = createSystemModule("ResultValidation", identifyingName, systemUserId);

        Role resultsEntryRole = roleService.getRoleByName(Constants.ROLE_RESULTS);
        Role validationRole = roleService.getRoleByName(Constants.ROLE_VALIDATION);

        RoleModule workplanResultModule = createRoleModule(systemUserId, workplanModule, resultsEntryRole);
        RoleModule resultResultModule = createRoleModule(systemUserId, resultModule, resultsEntryRole);
        RoleModule validationValidationModule = createRoleModule(systemUserId, validationModule, validationRole);

        try {
            panelCreateService.insert(localization, panel, workplanModule, resultModule, validationModule,
                    workplanResultModule, resultResultModule, validationValidationModule, sampleTypeId, systemUserId);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS_INACTIVE);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private Localization createLocalization(String french, String english, String currentUserId) {
        Localization localization = new Localization();
        localization.setEnglish(english);
        localization.setFrench(french);
        localization.setDescription("panel name");
        localization.setSysUserId(currentUserId);
        return localization;
    }

    private RoleModule createRoleModule(String userId, SystemModule workplanModule, Role role) {
        RoleModule roleModule = new RoleModule();
        roleModule.setRole(role);
        roleModule.setSystemModule(workplanModule);
        roleModule.setSysUserId(userId);
        roleModule.setHasAdd("Y");
        roleModule.setHasDelete("Y");
        roleModule.setHasSelect("Y");
        roleModule.setHasUpdate("Y");
        return roleModule;
    }

    private Panel createPanel(String identifyingName, String userId) {
        Panel panel = new Panel();
        panel.setDescription(identifyingName);
        panel.setPanelName(identifyingName);
        panel.setIsActive("N");
        panel.setSortOrderInt(Integer.MAX_VALUE);
        panel.setSysUserId(userId);
        return panel;
    }

    private SystemModule createSystemModule(String menuItem, String identifyingName, String userId) {
        SystemModule module = new SystemModule();
        module.setSystemModuleName(menuItem + ":" + identifyingName);
        module.setDescription(menuItem + "=>panel=>" + identifyingName);
        module.setSysUserId(userId);
        module.setHasAddFlag("Y");
        module.setHasDeleteFlag("Y");
        module.setHasSelectFlag("Y");
        module.setHasUpdateFlag("Y");
        return module;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "panelCreateDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/PanelCreate";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "panelCreateDefinition";
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
