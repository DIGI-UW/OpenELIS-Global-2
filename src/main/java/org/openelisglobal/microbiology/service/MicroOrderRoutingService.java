package org.openelisglobal.microbiology.service;

import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

public interface MicroOrderRoutingService {
    void routeOrder(Sample order, String actor);
    void routeAnalysis(Analysis analysis, String actor);
    void routeCaseTest(SampleItem sample, Test test, String actor);
}
