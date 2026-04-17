package org.openelisglobal.reports.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface WHONetReportService {

    List<SampleItem> getAntimicrobialEntries(Date lowDate, Date highDate);

    List<WHONetRow> getWHONetRows(Date lowDate, Date highDate);

}
