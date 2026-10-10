package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.form.MicroCaseSearchForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface MicroCaseSearchDAO {
    List<Object[]> search(MicroCaseSearchForm query, Set<String> labUnitIds);

    long count(MicroCaseSearchForm query, Set<String> labUnitIds);

    Object[] getSummary(String caseId);

    List<SampleItem> getSamples(String caseId);

    List<IdValuePair> getEligibleLabUnits();
}
