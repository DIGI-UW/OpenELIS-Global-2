package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.form.MicroCaseSearchForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public interface MicroCaseSearchDAO {
    List<Object[]> search(MicroCaseSearchForm query, Set<String> labUnitIds);

    List<Object[]> benchPage(org.openelisglobal.microbiology.form.MicroBenchQueryForm query, Set<String> units,
            List<String> requiredFields, List<String> questionnaireAttentionIds, double now);

    long benchCount(org.openelisglobal.microbiology.form.MicroBenchQueryForm query, Set<String> units,
            List<String> requiredFields, List<String> questionnaireAttentionIds, double now);

    List<String> questionnaireCases(MicroCaseSearchForm query, Set<String> units);

    long count(MicroCaseSearchForm query, Set<String> labUnitIds);

    Object[] getSummary(String caseId);

    List<SampleItem> getSamples(String caseId);

    List<IdValuePair> getEligibleLabUnits();
}
