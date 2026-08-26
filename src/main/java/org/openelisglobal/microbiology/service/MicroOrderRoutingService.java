package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface MicroOrderRoutingService {
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
