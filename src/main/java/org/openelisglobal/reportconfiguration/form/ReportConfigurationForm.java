package org.openelisglobal.reportconfiguration.form;

import java.io.Serializable;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.openelisglobal.reportconfiguration.valueholder.ReportCategory;
import org.springframework.web.multipart.MultipartFile;

public class ReportConfigurationForm extends BaseForm implements Serializable {

    private static final long serialVersionUID = 74458L;

    private List<Report> reportList;

    private List<ReportCategory> reportCategoryList;

    private Report currentReport;

    private String idOrder;

    private MultipartFile reportTemplateFile;

    /*
     *
     */
    private MultipartFile reportDataFile;

    private String menuDisplayKey;
    private String menuActionUrl;

    public ReportConfigurationForm() {
        this.currentReport = new Report();
    }

    public List<Report> getReportList() {
        return reportList;
    }

    public void setReportList(List<Report> reportList) {
        this.reportList = reportList;
    }

    public Report getCurrentReport() {
        return currentReport;
    }

    public void setCurrentReport(Report currentReport) {
        this.currentReport = currentReport;
    }

    public String getIdOrder() {
        return idOrder;
    }

    public void setIdOrder(String idOrder) {
        this.idOrder = idOrder;
    }

    public List<ReportCategory> getReportCategoryList() {
        return reportCategoryList;
    }

    public void setReportCategoryList(List<ReportCategory> reportCategoryList) {
        this.reportCategoryList = reportCategoryList;
    }

    public MultipartFile getReportTemplateFile() {
        return reportTemplateFile;
    }

    public void setReportTemplateFile(MultipartFile reportTemplateFile) {
        this.reportTemplateFile = reportTemplateFile;
    }

    public MultipartFile getReportDataFile() {
        return reportDataFile;
    }

    public void setReportDataFile(MultipartFile reportDataFile) {
        this.reportDataFile = reportDataFile;
    }

    public String getMenuDisplayKey() {
        return menuDisplayKey;
    }

    public void setMenuDisplayKey(String menuDisplayKey) {
        this.menuDisplayKey = menuDisplayKey;
    }

    public String getMenuActionUrl() {
        return menuActionUrl;
    }

    public void setMenuActionUrl(String menuActionUrl) {
        this.menuActionUrl = menuActionUrl;
    }
}
