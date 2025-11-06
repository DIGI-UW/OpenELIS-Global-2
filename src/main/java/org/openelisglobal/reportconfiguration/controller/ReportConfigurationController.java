package org.openelisglobal.reportconfiguration.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reportconfiguration.form.ReportConfigurationForm;
import org.openelisglobal.reportconfiguration.service.ReportService;
import org.openelisglobal.reportconfiguration.valueholder.ReportCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportConfigurationController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "currentReport.name", "currentReport.category",
            "currentReport.menuElementId", "menuDisplayKey", "menuActionUrl", "reportDataFile", "reportTemplateFile" };

    @Autowired
    private ReportService reportService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/ReportConfiguration", method = RequestMethod.GET)
    public ModelAndView showReports(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ReportConfigurationForm form = new ReportConfigurationForm();
        form.setReportCategoryList(createReportCategoryList());
        form.setReportList(reportService.getReports());
        return findForward(FWD_SUCCESS, form);
    }

    @RequestMapping(value = "/ReportConfiguration", method = RequestMethod.POST)
    public ModelAndView editReports(HttpServletRequest request, @ModelAttribute("form") ReportConfigurationForm form,
            BindingResult result, RedirectAttributes redirectAttributes)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String currentUserId = getSysUserId(request);
        form.setReportCategoryList(createReportCategoryList());
        boolean updated = false;
        if (!"".equalsIgnoreCase(form.getCurrentReport().getId())) {
            updated = reportService.updateReport(form, currentUserId);
        } else {
            if (form.getReportDataFile() == null || form.getReportTemplateFile() == null) {
                return findForward(FWD_VALIDATION_ERROR, form);
            }
            updated = reportService.createNewReport(form, currentUserId);
        }
        if (updated) {
            return findForward(FWD_SUCCESS_INSERT, form);
        } else {
            return findForward(FWD_FAIL_INSERT, form);
        }
    }

    private List<ReportCategory> createReportCategoryList() {
        List<ReportCategory> reportCategoryList = new ArrayList<>();
        // N.B. If the order is to be changed just change the order but keep the
        // id:value pairing the same
        reportCategoryList.add(new ReportCategory("1", MessageUtil.getMessage("label.select.report.category.routine"),
                "menu_reports_routine", "", ""));
        reportCategoryList.add(new ReportCategory("2", MessageUtil.getMessage("label.select.report.category.study"),
                "menu_reports_study", "", ""));
        reportCategoryList
                .add(new ReportCategory("3", MessageUtil.getMessage("label.select.report.category.patientStatus"),
                        "menu_reports_status_patient", "", "1"));
        reportCategoryList.add(new ReportCategory("4", MessageUtil.getMessage("label.select.report.category.aggregate"),
                "menu_reports_aggregate", "", "1"));
        reportCategoryList.add(new ReportCategory("5",
                MessageUtil.getMessage("label.select.report.category.management"), "menu_reports_management", "", "1"));
        reportCategoryList.add(new ReportCategory("6", MessageUtil.getMessage("label.select.report.category.activity"),
                "menu_reports_activity", "", "5"));
        reportCategoryList.add(
                new ReportCategory("7", MessageUtil.getMessage("label.select.report.category.management.nonConformity"),
                        "menu_reports_nonconformity", "", "5"));
        reportCategoryList
                .add(new ReportCategory("8", MessageUtil.getMessage("label.select.report.category.study.patientStatus"),
                        "menu_reports_patients", "", "2"));
        reportCategoryList.add(new ReportCategory("9", MessageUtil.getMessage("label.select.report.category.study.arv"),
                "menu_reports_arv", "", "7"));
        reportCategoryList.add(new ReportCategory("10",
                MessageUtil.getMessage("label.select.report.category.study.eid"), "menu_reports_eid", "", "7"));
        reportCategoryList.add(new ReportCategory("11", MessageUtil.getMessage("label.select.report.category.study.vl"),
                "menu_reports_vl", "", "7"));
        reportCategoryList.add(
                new ReportCategory("12", MessageUtil.getMessage("label.select.report.category.study.indeterminate"),
                        "menu_reports_indeterminate", "", "7"));
        reportCategoryList
                .add(new ReportCategory("13", MessageUtil.getMessage("label.select.report.category.study.indicator"),
                        "menu_reports_indicator", "", "2"));
        reportCategoryList.add(
                new ReportCategory("14", MessageUtil.getMessage("label.select.report.category.study.nonConformity"),
                        "menu_reports_nonconformity.study", "", "2"));
        reportCategoryList.add(new ReportCategory("15",
                MessageUtil.getMessage("label.select.report.category.exportByDate"), "menu_reports_export", "", "2"));

        return reportCategoryList;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "ReportManagementDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ReportConfiguration";
        } else if (FWD_FAIL.equals(forward)) {
            return "ReportManagementDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageSubtitleKey() {
        return "banner.menu.reportManagements";
    }

    @Override
    protected String getPageTitleKey() {
        return "banner.menu.reportManagement";
    }
}
