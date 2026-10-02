package org.openelisglobal.reports.service;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Gated per method rather than at class level, deliberately. Mockito copies a
 * CLASS-level annotation onto the generated mock, so the mock carries
 * {@code @PreAuthorize} twice — once from the interface, once from the copy —
 * and Spring Security's {@code findUniqueAnnotation} throws
 * AnnotationConfigurationException. AppTestConfig publishes
 * {@code mock(WHONetReportService.class)} as a shared test bean, so that single
 * conflict aborted every context that loaded it. Per-method annotations are not
 * copied and avoid the problem entirely; the privilege is identical on all five
 * methods, so this is a packaging change, not a policy change.
 */
public interface WHONetReportService {

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    List<SampleItem> getAntimicrobialEntries(Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    List<WHONetRow> getWHONetRows(Date lowDate, Date highDate);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    MicroWhonetPreviewForm previewMicrobiologyExport(MicroWhonetExportQueryForm query);

    // Also PRIV_MICRO_VIEW: the WHONET export page is routed to Results, and
    // every other read on it is a microbiology read on micro:view; only this
    // filter-option lookup sat on report:run, so Results reached a page that
    // 403'd its filters.
    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_MICRO_VIEW')")
    MicroWhonetFilterOptionsForm getMicrobiologyExportFilterOptions(MicroWhonetExportQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    MicroWhonetExportResult generateMicrobiologyExport(MicroWhonetExportQueryForm query, String authenticatedUserId);

}
