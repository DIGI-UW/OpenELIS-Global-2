package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

public interface MicroOrderRoutingService {
    /**
     * The single microbiology eligibility decision for an order: a configured
     * culture-workflow test, or an explicitly selected microbiology program as the
     * documented fallback. Callers must not re-derive this from test names, program
     * codes, or a submitted order-detail payload.
     */
    boolean isMicrobiologyOrder(List<Test> tests, boolean microbiologyProgramSelected);

    List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses, String performedBy);

    /**
     * Routes as {@link #routeAnalysesForSampleItem(SampleItem, List, String)} and,
     * when {@code orderDetail} is non-null, persists it against every case routed
     * for this order/sample save.
     */
    List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses, String performedBy,
            MicroCaseOrderDetailRequestForm orderDetail);

    List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses, String performedBy,
            MicroCaseOrderDetailRequestForm orderDetail, boolean microbiologyProgramSelected);
}
