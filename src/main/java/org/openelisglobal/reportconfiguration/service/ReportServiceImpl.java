package org.openelisglobal.reportconfiguration.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.reportconfiguration.dao.ReportDAO;
import org.openelisglobal.reportconfiguration.form.ReportConfigurationForm;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.openelisglobal.reportconfiguration.valueholder.ReportCategory;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl extends AuditableBaseObjectServiceImpl<Report, String> implements ReportService {

    @Autowired
    protected ReportDAO baseObjectDAO;
    @Autowired
    private MenuService menuService;
    @Autowired
    private SiteInformationService siteInformationService;

    ReportServiceImpl() {
        super(Report.class);
    }

    @Override
    public List<Report> getReports() {
        List<Report> reportList = this.getAll();
        for (Report report : reportList) {
            report.setName(MessageUtil.getMessage(report.getDisplayKey()));
        }
        return reportList;
    }

    @Override
    public boolean updateReport(ReportConfigurationForm form, String currentUserId) {
        try {
            Report report = baseObjectDAO.get(form.getCurrentReport().getId()).get();
            List<Integer> idOrder = Arrays.asList(form.getIdOrder().split(",")).stream().map(Integer::parseInt)
                    .collect(Collectors.toList());

            final String categoryId = form.getCurrentReport().getCategory();
            ReportCategory reportCategory = form.getReportCategoryList().stream()
                    .filter(rc -> rc.getId().equalsIgnoreCase(categoryId)).findFirst().orElse(null);
            if (report != null) {
                String previousCategory = report.getCategory();
                report.setName(form.getCurrentReport().getName());
                report.setCategory(form.getCurrentReport().getCategory());
                report.setIsVisible(form.getCurrentReport().getIsVisible());
                report.setSortOrder(idOrder.indexOf(Integer.valueOf(form.getCurrentReport().getId())));
                baseObjectDAO.update(report);
                // If the report category has changed, update the menus.
                if (categoryId != previousCategory) {
                    // new category
                    Menu menu = menuService.getMenuByElementId(reportCategory.getMenuElementId());
                    Menu reportMenu = menuService.getMenuByElementId(report.getMenuElementId());
                    if (menu != null) {
                        reportMenu.setParent(menu);
                        menuService.save(reportMenu);
                    }
                }
            }
            for (int i = 0; i < idOrder.size(); i++) {
                Report r = baseObjectDAO.get(idOrder.get(i) + "").get();
                if (r != null) {
                    Menu menu = menuService.getMenuByElementId(r.getMenuElementId());
                    menu.setIsActive(r.getIsVisible() ? true : false);
                    menu.setPresentationOrder((i + 1) * 50);
                    menuService.save(menu);
                    r.setSortOrder(i);
                    baseObjectDAO.update(r);
                }
            }
            return true;
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
        }
        return false;
    }

    @Override
    public boolean createNewReport(ReportConfigurationForm form, String currentUserId) {
        try {
            int maxSortOrder = baseObjectDAO.getMaxSortOrder(form.getCurrentReport().getCategory());
            form.getCurrentReport().setSortOrder(maxSortOrder + 1);

            final String categoryId = form.getCurrentReport().getCategory();
            ReportCategory reportCategory = form.getReportCategoryList().stream()
                    .filter(rc -> rc.getId().equalsIgnoreCase(categoryId)).findFirst().orElse(null);

            form.getCurrentReport().setDisplayKey(form.getMenuDisplayKey());
            Report createdReport = baseObjectDAO.update(form.getCurrentReport());

            Menu menu = menuService.getMenuByElementId(reportCategory.getMenuElementId());
            /*
             * Create the menu for the new report
             */
            Menu reportMenu = new Menu();
            reportMenu.setElementId(createdReport.getMenuElementId());
            reportMenu.setParent(menu);
            reportMenu.setPresentationOrder((maxSortOrder + 1) * 50);
            reportMenu.setIsActive(createdReport.getIsVisible());
            reportMenu.setDisplayKey(form.getMenuDisplayKey());
            reportMenu.setActionURL(form.getMenuActionUrl());

            reportMenu.setParent(menu);
            menuService.save(reportMenu);

            SiteInformation reportsPath = siteInformationService.getSiteInformationByName("reportsDirectory");
            if (reportsPath != null) {
                String reportPath = reportsPath.getValue();

                if (form.getReportDataFile() != null && form.getReportTemplateFile() != null) {
                    try {
                        if (form.getReportDataFile().getOriginalFilename().matches("[a-zA-Z1-9_]+\\.jasper") && form
                                .getReportTemplateFile().getOriginalFilename().matches("[a-zA-Z1-9_]+\\.jrxml")) {
                            File dataFile = new File(reportPath, form.getReportDataFile().getOriginalFilename());
                            form.getReportDataFile().transferTo(dataFile);

                            File templateFile = new File(reportPath,
                                    form.getReportTemplateFile().getOriginalFilename());
                            form.getReportTemplateFile().transferTo(templateFile);
                        }
                    } catch (IOException e) {
                        LogEvent.logDebug(e);
                    }
                }
            }
            return true;
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
        }
        return false;
    }

    @Override
    protected ReportDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
