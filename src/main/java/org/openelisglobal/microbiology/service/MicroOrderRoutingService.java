package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface MicroOrderRoutingService {
    List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses, String performedBy);
}
