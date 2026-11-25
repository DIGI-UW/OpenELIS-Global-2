package org.openelisglobal.containertype.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.containertype.dao.TestExpectedContainerDAO;
import org.openelisglobal.containertype.form.ContainerProposalForm;
import org.openelisglobal.containertype.form.ContainerProposalView;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.valueholder.TestExpectedContainer;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContainerProposalServiceImpl implements ContainerProposalService {

    private static final String CLINICAL = "CLINICAL";
    private static final String SHARED = "shared";

    @Autowired
    private TestExpectedContainerDAO expectedContainerDAO;
    @Autowired
    private ContainerTypeService containerTypeService;
    @Lazy
    @Autowired
    private TestService testService;
    @Lazy
    @Autowired
    private PanelItemService panelItemService;

    @Override
    @Transactional(readOnly = true)
    public List<ContainerProposalView> propose(ContainerProposalForm form) {
        List<ContainerProposalForm.Sample> samples = form == null || form.samples() == null ? List.of()
                : form.samples();
        Map<Long, ContainerTypeView> active = new LinkedHashMap<>();
        containerTypeService.list(null, CLINICAL, null, false).forEach(type -> active.put(type.id(), type));
        List<ContainerProposalView> views = new ArrayList<>();
        for (ContainerProposalForm.Sample sample : samples) {
            if (sample != null) {
                views.add(proposeFor(sample, active));
            }
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canHold(Long containerTypeId, String sampleTypeId, Long keptContainerTypeId) {
        Optional<ContainerTypeView> container = containerTypeService.get(containerTypeId);
        return container.isPresent() && (container.get().active() || containerTypeId.equals(keptContainerTypeId))
                && yields(container.get(), sampleTypeId);
    }

    private ContainerProposalView proposeFor(ContainerProposalForm.Sample sample, Map<Long, ContainerTypeView> active) {
        String sampleTypeId = StringUtils.trimToEmpty(sample.sampleTypeId());
        List<Ordered> ordered = orderedTests(sample, sampleTypeId, active);
        shareAlternates(ordered);
        Map<String, Group> groups = new LinkedHashMap<>();
        List<String> unproposed = new ArrayList<>();
        for (Ordered test : ordered) {
            if (test.options.isEmpty()) {
                unproposed.add(test.testId);
                continue;
            }
            Option chosen = test.chosen();
            Group group = groups.computeIfAbsent(test.scope + "|" + chosen.container.id(),
                    key -> new Group(chosen.container, !SHARED.equals(test.scope)));
            group.count = Math.max(group.count, chosen.count);
            group.testIds.add(test.testId);
            group.testNames.add(test.name);
            if (test.panelId != null && !group.panelIds.contains(test.panelId)) {
                group.panelIds.add(test.panelId);
            }
        }
        List<ContainerProposalView.Proposed> proposed = groups.values().stream()
                .sorted(Comparator.comparing(group -> group.ownContainer))
                .map(group -> new ContainerProposalView.Proposed(group.container.id(), group.container.name(),
                        group.container.capColourHex(), group.container.nominalVolume(), group.container.volumeUnit(),
                        group.count, List.copyOf(group.testIds), List.copyOf(group.testNames),
                        List.copyOf(group.panelIds), group.ownContainer))
                .toList();
        List<ContainerProposalView.Choice> choices = active.values().stream()
                .filter(container -> yields(container, sampleTypeId))
                .sorted(Comparator.comparing(ContainerTypeView::name, String.CASE_INSENSITIVE_ORDER))
                .map(container -> new ContainerProposalView.Choice(container.id(), container.name(),
                        container.capColourHex(), container.nominalVolume(), container.volumeUnit()))
                .toList();
        return new ContainerProposalView(sample.key(), proposed, unproposed, choices, !proposed.isEmpty());
    }

    private List<Ordered> orderedTests(ContainerProposalForm.Sample sample, String sampleTypeId,
            Map<Long, ContainerTypeView> active) {
        Map<String, Ordered> byTest = new LinkedHashMap<>();
        List<String> tests = sample.testIds() == null ? List.of() : sample.testIds();
        Map<String, String> panelOf = panelsOf(sample.panelIds());
        Optional<Long> sampleTypeDefault = !StringUtils.isNumeric(sampleTypeId) ? Optional.empty()
                : expectedContainerDAO.defaultContainerOf(Integer.valueOf(sampleTypeId));
        for (String orderedTest : tests) {
            String testId = StringUtils.trimToNull(orderedTest);
            if (testId == null || byTest.containsKey(testId) || !StringUtils.isNumeric(testId)) {
                continue;
            }
            Optional<Test> found = testService.getMatch("id", testId);
            if (found.isEmpty()) {
                continue;
            }
            Test test = found.get();
            Integer id = Integer.valueOf(testId);
            String panelId = panelOf.get(testId);
            boolean ownContainer = expectedContainerDAO.needsOwnContainer(id);
            String scope = !ownContainer ? SHARED : panelId != null ? "panel:" + panelId : "test:" + testId;
            List<Option> options = new ArrayList<>();
            List<TestExpectedContainer> expected = expectedContainerDAO.getForTest(id).stream()
                    .sorted(Comparator.comparing(
                            (TestExpectedContainer row) -> !TestExpectedContainer.PREFERRED.equals(row.getRole()))
                            .thenComparingInt(TestExpectedContainer::getDisplayOrder))
                    .toList();
            for (TestExpectedContainer row : expected) {
                ContainerTypeView container = active.get(row.getContainerTypeId());
                if (container != null && row.getUsedAsSampleTypeId() == null && yields(container, sampleTypeId)) {
                    options.add(new Option(container, row.getCount()));
                }
            }
            if (options.isEmpty()) {
                sampleTypeDefault.map(active::get).filter(container -> yields(container, sampleTypeId))
                        .ifPresent(container -> options.add(new Option(container, 1)));
            }
            byTest.put(testId, new Ordered(testId, test.getLocalizedName(), panelId, scope, options));
        }
        return new ArrayList<>(byTest.values());
    }

    private Map<String, String> panelsOf(List<String> panelIds) {
        Map<String, String> panelOf = new LinkedHashMap<>();
        for (String panelId : panelIds == null ? List.<String>of() : panelIds) {
            String id = StringUtils.trimToNull(panelId);
            if (id == null || !StringUtils.isNumeric(id)) {
                continue;
            }
            for (PanelItem item : panelItemService.getPanelItemsForPanel(id)) {
                if (item.getTest() != null) {
                    panelOf.putIfAbsent(item.getTest().getId(), id);
                }
            }
        }
        return panelOf;
    }

    /**
     * A test whose first choice no other test in its scope is drawing, but whose
     * alternate another test is drawing, shares that alternate (FR-B25). Repeated
     * until nothing moves, so the order the tests were picked in decides ties.
     */
    private void shareAlternates(List<Ordered> ordered) {
        boolean moved = true;
        while (moved) {
            moved = false;
            for (Ordered test : ordered) {
                if (test.options.size() < 2 || drawnByAnother(ordered, test, test.chosen().container.id())) {
                    continue;
                }
                for (int index = 0; index < test.options.size(); index++) {
                    Long candidate = test.options.get(index).container.id();
                    if (index != test.choice && drawnByAnother(ordered, test, candidate)) {
                        test.choice = index;
                        moved = true;
                        break;
                    }
                }
            }
        }
    }

    private static boolean drawnByAnother(List<Ordered> ordered, Ordered test, Long containerTypeId) {
        return ordered.stream().anyMatch(other -> other != test && other.scope.equals(test.scope)
                && !other.options.isEmpty() && Objects.equals(other.chosen().container.id(), containerTypeId));
    }

    private static boolean yields(ContainerTypeView container, String sampleTypeId) {
        return container.sampleTypes().stream().anyMatch(type -> type.getId().equals(sampleTypeId));
    }

    private record Option(ContainerTypeView container, int count) {
    }

    private static final class Ordered {
        private final String testId;
        private final String name;
        private final String panelId;
        private final String scope;
        private final List<Option> options;
        private int choice;

        private Ordered(String testId, String name, String panelId, String scope, List<Option> options) {
            this.testId = testId;
            this.name = name;
            this.panelId = panelId;
            this.scope = scope;
            this.options = options;
        }

        private Option chosen() {
            return options.get(choice);
        }
    }

    private static final class Group {
        private final ContainerTypeView container;
        private final List<String> testIds = new ArrayList<>();
        private final List<String> testNames = new ArrayList<>();
        private final List<String> panelIds = new ArrayList<>();
        private final boolean ownContainer;
        private int count;

        private Group(ContainerTypeView container, boolean ownContainer) {
            this.container = container;
            this.ownContainer = ownContainer;
        }
    }
}
