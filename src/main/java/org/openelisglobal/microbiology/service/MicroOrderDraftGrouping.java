package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

/**
 * Evaluates the saving rule over unsaved samples without changing stored work.
 */
public final class MicroOrderDraftGrouping {
    private MicroOrderDraftGrouping() {
    }

    public record Selection(SampleItem specimen, List<Test> tests) {
    }

    public record Group(MicroCaseRoutingKey key, String firstSampleTypeId, List<Integer> specimenIndexes,
            List<String> testIds) {
    }

    private record Work(int index, SampleItem sample, Test test) {
    }

    public static List<Group> group(List<Selection> selections) {
        List<Work> work = new ArrayList<>();
        for (int i = 0; i < selections.size(); i++) {
            Selection selection = selections.get(i);
            if (selection.specimen() == null || selection.specimen().getId() != null
                    || (selection.specimen().getSample() != null && selection.specimen().getSample().getId() != null))
                throw new IllegalArgumentException("Preview requires unsaved specimens");
            for (Test test : selection.tests()) {
                if (test.isOpensMicrobiologyCase())
                    work.add(new Work(i, selection.specimen(), test));
            }
        }
        work.sort(Comparator.comparing(w -> !w.test.isCollectedInSets()));
        List<MicroCaseRoutingRule.Candidate> candidates = new ArrayList<>();
        Map<String, MicroCaseRoutingKey> keys = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> tests = new LinkedHashMap<>();
        for (Work selected : work) {
            MicroCaseRoutingKey key = MicroCaseRoutingKey.forTest(selected.sample, selected.test);
            String sampleKey = Integer.toString(selected.index);
            var chosen = MicroCaseRoutingRule.choose(candidates, key.testSectionId(),
                    selected.sample.getTypeOfSampleId(), key.collectedInSetsTestId(), sampleKey);
            if (chosen == null) {
                chosen = new MicroCaseRoutingRule.Candidate(Integer.toString(candidates.size()), key.testSectionId(),
                        selected.sample.getTypeOfSampleId());
                candidates.add(chosen);
                keys.put(chosen.caseId, key);
                tests.put(chosen.caseId, new LinkedHashSet<>());
            }
            chosen.samples.add(sampleKey);
            tests.get(chosen.caseId).add(selected.test.getId());
            if (selected.test.isCollectedInSets())
                chosen.setsTests.add(selected.test.getId());
        }
        return candidates.stream().map(c -> new Group(keys.get(c.caseId), c.sampleTypeId,
                c.samples.stream().map(Integer::valueOf).toList(), List.copyOf(tests.get(c.caseId)))).toList();
    }
}
