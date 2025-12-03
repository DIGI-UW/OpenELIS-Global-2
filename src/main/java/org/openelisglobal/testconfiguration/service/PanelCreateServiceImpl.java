package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.event.PanelCreatedOrUpdatedEvent;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelCreateServiceImpl implements PanelCreateService {

    @Autowired
    private PanelService panelService;
    @Autowired
    private RoleModuleService roleModuleService;
    @Autowired
    private SystemModuleService systemModuleService;
    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;
    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void insert(Localization localization, Panel panel, SystemModule workplanModule, SystemModule resultModule,
            SystemModule validationModule, RoleModule workplanResultModule, RoleModule resultResultModule,
            RoleModule validationValidationModule, String sampleTypeId, String systemUserId) {
        localizationService.insert(localization);
        panel.setLocalization(localization);
        panelService.insert(panel);

        // Publish event so integrations (e.g. Odoo) can react to new panels
        eventPublisher.publishEvent(new PanelCreatedOrUpdatedEvent(this, panel));

        TypeOfSamplePanel typeOfSamplePanel = createTypeOfSamplePanel(sampleTypeId, panel, systemUserId);
        typeOfSamplePanelService.insert(typeOfSamplePanel);

        systemModuleService.insert(workplanModule);
        systemModuleService.insert(resultModule);
        systemModuleService.insert(validationModule);

        roleModuleService.insert(workplanResultModule);
        roleModuleService.insert(resultResultModule);
        roleModuleService.insert(validationValidationModule);
    }

    private TypeOfSamplePanel createTypeOfSamplePanel(String sampleTypeId, Panel panel, String userId) {
        TypeOfSamplePanel sampleTypePanel = new TypeOfSamplePanel();
        sampleTypePanel.setPanelId(panel.getId());
        sampleTypePanel.setTypeOfSampleId(sampleTypeId);
        sampleTypePanel.setSysUserId(userId);
        return sampleTypePanel;
    }
}
