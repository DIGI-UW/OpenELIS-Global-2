package org.openelisglobal.menu.service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.formfields.AdminFormFields;
import org.openelisglobal.common.formfields.AdminFormFields.Field;
import org.openelisglobal.common.util.ConfigurationListener;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.menu.valueholder.AdminMenuItem;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn({ "springContext", "defaultConfigurationProperties" })
public class AdminMenuItemServiceImpl implements AdminMenuItemService, ConfigurationListener {

    List<AdminMenuItem> adminMenuItems;

    @PostConstruct
    public synchronized void createActiveList() {
        adminMenuItems = new ArrayList<>();
        String permissionBase = ConfigurationProperties.getInstance().getPropertyValue("permissions.agent");
        AdminFormFields adminFields = AdminFormFields.getInstance();
        AdminMenuItem curItem;

        if ("true".equals(ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(ConfigurationProperties.Property.TrainingInstallation))) {
            curItem = new AdminMenuItem();
            curItem.setPath("/DatabaseCleaningRequest");
            curItem.setMessageKey("database.clean");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.AnalyzerTestNameMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/AnalyzerTestNameMenu");
            curItem.setMessageKey("analyzerTestName.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.DictionaryMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/DictionaryMenu");
            curItem.setMessageKey("dictionary.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.OrganizationMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/OrganizationMenu");
            curItem.setMessageKey("organization.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.PatientTypeMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/PatientTypeMenu");
            curItem.setMessageKey("patienttype.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.SiteInformationMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/SiteInformationMenu");
            curItem.setMessageKey("siteInformation.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.SampleEntryMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/SampleEntryConfigMenu");
            curItem.setMessageKey("sample.entry.browse.title");
            adminMenuItems.add(curItem);
        }

        curItem = new AdminMenuItem();
        curItem.setPath("/TestManagementConfigMenu");
        curItem.setMessageKey("configuration.test.management");
        adminMenuItems.add(curItem);

        curItem = new AdminMenuItem();
        curItem.setPath("/BatchTestReassignment");
        curItem.setMessageKey("configuration.batch.test.reassignment");
        adminMenuItems.add(curItem);

        curItem = new AdminMenuItem();
        curItem.setPath("/MenuStatementConfigMenu");
        curItem.setMessageKey("MenuStatementConfig.browse.title");
        adminMenuItems.add(curItem);

        if (adminFields.useField(Field.PATIENT_ENTRY_CONFIGURATION)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/PatientConfigurationMenu");
            curItem.setMessageKey("patientEntryConfiguration.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.ResultInformationMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/ResultConfigurationMenu");
            curItem.setMessageKey("resultConfiguration.browse.title");
            adminMenuItems.add(curItem);
        }

        // gnr: pivotal 176523804, 176372736
        if (false) {
            if (adminFields.useField(Field.NON_CONFORMITY_CONFIGURATION)) {
                curItem = new AdminMenuItem();
                curItem.setPath("/NonConformityConfigurationMenu");
                curItem.setMessageKey("nonConformityConfiguration.browse.title");
                adminMenuItems.add(curItem);
            }
            curItem = new AdminMenuItem();
            curItem.setPath("/ReportConfiguration");
            curItem.setMessageKey("banner.menu.reportManagement");
            adminMenuItems.add(curItem);
        }

        if (adminFields.useField(Field.PRINTED_REPORTS_CONFIGURATION)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/PrintedReportsConfigurationMenu");
            curItem.setMessageKey("printedReportsConfiguration.browse.title");
            adminMenuItems.add(curItem);
        }

        if (adminFields.useField(Field.WORKPLAN_CONFIGURATION)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/WorkplanConfigurationMenu");
            curItem.setMessageKey("workplanConfiguration.browse.title");
            adminMenuItems.add(curItem);
        }

        if (adminFields.useField(Field.TypeOfSamplePanelMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/TypeOfSamplePanelMenu");
            curItem.setMessageKey("typeofsample.panel");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.TypeOfSampleTestMenu)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/TypeOfSampleTestMenu");
            curItem.setMessageKey("typeofsample.test");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.TestUsageAggregatation)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/TestUsageConfiguration");
            curItem.setMessageKey("testusageconfiguration.browse.title");
            adminMenuItems.add(curItem);
        }
        if (adminFields.useField(Field.RESULT_REPORTING_CONFIGURATION)) {
            curItem = new AdminMenuItem();
            curItem.setPath("/ResultReportingConfiguration");
            curItem.setMessageKey("resultreporting.browse.title");
            adminMenuItems.add(curItem);
        }
        curItem = new AdminMenuItem();
        curItem.setPath("/BarcodeConfiguration");
        curItem.setMessageKey("barcodeconfiguration.browse.title");
        adminMenuItems.add(curItem);

        curItem = new AdminMenuItem();
        curItem.setPath("/ValidationConfigurationMenu");
        curItem.setMessageKey("validationconfiguration.browse.title");
        adminMenuItems.add(curItem);

        curItem = new AdminMenuItem();
        curItem.setPath("/ExternalConnectionsMenu");
        curItem.setMessageKey("externalConnections.browse.title");
        adminMenuItems.add(curItem);

        curItem = new AdminMenuItem();
        curItem.setPath("/ListPlugins");
        curItem.setMessageKey("plugin.menu.list.plugins");
        adminMenuItems.add(curItem);

        if (permissionBase.equalsIgnoreCase("ROLE")) {
            curItem = new AdminMenuItem();
            curItem.setPath("/UnifiedSystemUserMenu");
            curItem.setMessageKey("unifiedSystemUser.browser.title");
            adminMenuItems.add(curItem);
        }

        curItem = new AdminMenuItem();
        curItem.setPath("/TestNotificationConfigMenu");
        curItem.setMessageKey("testnotificationconfig.browse.title");
        adminMenuItems.add(curItem);

        curItem = new AdminMenuItem();
        curItem.setPath("/ProviderMenu");
        curItem.setMessageKey("provider.browse.title");
        adminMenuItems.add(curItem);
    }

    @Override
    public List<AdminMenuItem> getActiveItemsSorted() {
        List<AdminMenuItem> sortedMenuItems = new ArrayList<>(adminMenuItems);
        sortedMenuItems.sort(new Comparator<AdminMenuItem>() {

            @Override
            public int compare(AdminMenuItem arg0, AdminMenuItem arg1) {
                return MessageUtil.getMessage(arg0.getMessageKey(), Locale.ENGLISH)
                        .compareTo(MessageUtil.getMessage(arg1.getMessageKey(), Locale.ENGLISH));
            }
        });
        return sortedMenuItems;
    }

    @Override
    public void refreshConfiguration() {
        createActiveList();
    }
}
