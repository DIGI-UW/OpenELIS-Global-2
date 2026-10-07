package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

/**
 * Groups an unsaved order using catalog tests, without creating clinical
 * records.
 */
public final class MicroOrderDraftGrouping {
    private MicroOrderDraftGrouping() {
    }

    public record Selection(SampleItem specimen, List<Test> tests) {
    }

    public record Group(MicroCaseRoutingKey key, String firstSampleTypeId, List<Integer> specimenIndexes,
            List<String> testIds) {
    }

    public static List<Group> group(List<Selection> selections) {
        List<DraftCase> cases = new ArrayList<>();
        for (int index = 0; index < selections.size(); index++) {
            Selection selection = selections.get(index);
            List<Test> tests = selection.tests().stream().filter(Objects::nonNull).filter(Test::isOpensMicrobiologyCase)
                    .sorted(Comparator.comparing(test -> !test.isCollectedInSets())).toList();
            for (Test test : tests) {
                MicroCaseRoutingKey key = MicroCaseRoutingKey.forTest(selection.specimen(), test);
                DraftCase owner = findOwner(cases, key, index);
                if (owner == null) {
                    owner = new DraftCase(key, selection.specimen().getTypeOfSampleId());
                    cases.add(owner);
                }
                owner.specimens.add(index);
                owner.tests.add(test.getId());
                if (test.isCollectedInSets()) {
                    owner.setsTests.add(test.getId());
                }
            }
        }
        return cases.stream().map(owner -> new Group(owner.key, owner.sampleTypeId, List.copyOf(owner.specimens),
                List.copyOf(owner.tests))).toList();
    }

    private static DraftCase findOwner(List<DraftCase> cases, MicroCaseRoutingKey key, int specimenIndex) {
        // Match the save path's membership-first, then oldest eligible case rule.
        for (DraftCase candidate : cases) {
            if (candidate.sameOrderAndUnit(key) && candidate.specimens.contains(specimenIndex)) {
                return candidate;
            }
        }
        for (DraftCase candidate : cases) {
            if (candidate.sameOrderAndUnit(key)
                    && (key.collectedInSetsTestId() == null ? Objects.equals(candidate.sampleTypeId, key.sampleTypeId())
                            : candidate.setsTests.contains(key.collectedInSetsTestId()))) {
                return candidate;
            }
        }
        return null;
    }

    private static final class DraftCase {
        private final MicroCaseRoutingKey key;
        private final String sampleTypeId;
        private final Set<Integer> specimens = new LinkedHashSet<>();
        private final Set<String> tests = new LinkedHashSet<>();
        private final Set<String> setsTests = new LinkedHashSet<>();

        private DraftCase(MicroCaseRoutingKey key, String sampleTypeId) {
            this.key = key;
            this.sampleTypeId = sampleTypeId;
        }

        private boolean sameOrderAndUnit(MicroCaseRoutingKey other) {
            return Objects.equals(key.sampleId(), other.sampleId())
                    && Objects.equals(key.testSectionId(), other.testSectionId());
        }
    }
}
