package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;

import java.sql.Date;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.reports.action.implementation.Report.DateRange;

public class CSVSampleRejectionColumnBuilder extends CSVColumnBuilder {
    protected DateRange dateRange;

    public CSVSampleRejectionColumnBuilder(DateRange dateRange) {
        super(StatusService.AnalysisStatus.SampleRejected);
        this.dateRange = dateRange;
        defineAllReportColumns();
    }

    @Override
    public void makeSQL() {
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append("SELECT s.accession_number AS lab_no ,s.received_date AS request_date , o.name as"
                + " site_name, ts.description AS sample_type , t.name AS test_name , d.dict_entry AS"
                + " rejection_reason ,si.lastupdated AS rejection_date FROM sample s INNER JOIN"
                + " sample_item si ON s.id = si.samp_id INNER JOIN type_of_sample ts ON si.typeosamp_id"
                + " = ts.id INNER JOIN analysis a ON si.id = a.sampitem_id INNER JOIN test t ON"
                + " a.test_id = t.id  LEFT JOIN dictionary d ON si.reject_reason_id = d.id  INNER JOIN"
                + " sample_requester sr ON s.id = sr.sample_id  INNER JOIN organization o ON"
                + " sr.requester_id = o.id  WHERE si.rejected = true AND sr.requester_type_id = (SELECT"
                + " rt.id FROM requester_type rt WHERE rt.requester_type = 'organization')  AND"
                + " s.entered_date BETWEEN '" + lowDate + "' AND '" + highDate + "' ORDER BY s.accession_number");
    }

    protected void defineAllReportColumns() {
        add("lab_no", "Lab No ", NONE);
        add("request_date", "Request Date", DATE_TIME);
        add("site_name", "Site Name", NONE);
        add("sample_type", "Sample", NONE);
        add("test_name", "Test", NONE);
        add("rejection_reason", "Rejection Reason", NONE);
        add("rejection_date", "Rejection Date", DATE_TIME);
    }
}
