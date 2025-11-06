package org.openelisglobal.common.services;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.services.ReportTrackingService.ReportType;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.sample.valueholder.Sample;

public interface IReportTrackingService {

    public void addReports(List<String> refIds, ReportType type, String name, String currentSystemUserId);

    public List<DocumentTrack> getReportsForSample(Sample sample, ReportType type);

    public List<DocumentTrack> getReportsForSampleAndReportName(Sample sample, ReportType type, String name);

    public DocumentTrack getLastReportForSample(Sample sample, ReportType type);

    public DocumentTrack getLastNamedReportForSample(Sample sample, ReportType type, String name);

    public Timestamp getTimeOfLastReport(Sample sample, ReportType type);

    public Timestamp getTimeOfLastNamedReport(Sample sample, ReportType type, String name);

    public DocumentTrack getDocumentForId(String id);
}
