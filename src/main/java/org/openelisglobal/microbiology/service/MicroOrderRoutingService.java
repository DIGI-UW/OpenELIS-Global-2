package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

public interface MicroOrderRoutingService {
    /**
     * Program and surveillance flags do not decide whether an order opens a case.
     */
    boolean isMicrobiologyOrder(List<Test> tests);

    /**
     * New-order grouping only; existing-order preview must retain saved ownership.
     */
    List<MicroOrderDraftGrouping.Group> previewNewOrder(List<MicroOrderDraftGrouping.Selection> selections);

    List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses, String performedBy);
}
