package org.openelisglobal.microbiology.service;

import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

public interface MicroOrderRoutingService {
    java.util.List<MicroOrderDraftGrouping.Group> previewNewOrder(
            java.util.List<MicroOrderDraftGrouping.Selection> selections);

    void routeOrder(Sample order, String actor);

    void routeOrder(Sample order, String actor, String cancelReason);

    void routeAnalysis(Analysis analysis, String actor);

    void routeCaseTest(SampleItem sample, Test test, String actor);

    void routeCaseTest(Sample order, String sampleTypeId, SampleItem sample, Test test, String actor);
}
