package org.openelisglobal.dashboard.management.antibiogram;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.antibiogram.AntibiogramResponse.Antibiotic;
import org.openelisglobal.dashboard.management.antibiogram.AntibiogramResponse.Cell;
import org.openelisglobal.dashboard.management.antibiogram.AntibiogramResponse.Organism;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService.AstResult;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService.FirstIsolate;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AntibiogramServiceImpl implements AntibiogramService {

    static final int MIN_TESTED = 10;
    static final int TOP_ORGANISMS = 5;

    @Autowired
    private MicroWhonetDatasetService microWhonetDatasetService;
    @Autowired
    private MicroOrganismDAO microOrganismDAO;
    @Autowired
    private MicroAntibioticDAO microAntibioticDAO;

    @Override
    @Transactional(readOnly = true)
    public AntibiogramResponse getAntibiogram(ScopeRange scope) {
        Map<String, List<FirstIsolate>> isolatesByOrganism = microWhonetDatasetService
                .selectFirstIsolates(scope.from(), scope.to()).stream().filter(isolate -> isolate.organismId() != null)
                .collect(groupingBy(FirstIsolate::organismId));
        Map<String, String> organismNames = isolatesByOrganism.isEmpty() ? Map.of()
                : microOrganismDAO.getByIds(List.copyOf(isolatesByOrganism.keySet())).stream()
                        .collect(toMap(MicroOrganism::getId, MicroOrganism::getDisplayName));
        List<Organism> organisms = isolatesByOrganism.entrySet().stream()
                .map(e -> new Organism(e.getKey(), organismNames.getOrDefault(e.getKey(), e.getKey()),
                        e.getValue().size()))
                .sorted(Comparator.comparingInt(Organism::isolates).reversed().thenComparing(Organism::name))
                .limit(TOP_ORGANISMS).toList();

        List<Cell> cells = new ArrayList<>();
        for (Organism organism : organisms) {
            Map<String, Set<String>> tested = new HashMap<>();
            Map<String, Set<String>> resistant = new HashMap<>();
            for (FirstIsolate isolate : isolatesByOrganism.get(organism.id())) {
                for (AstResult result : isolate.results()) {
                    tested.computeIfAbsent(result.antibioticId(), id -> new HashSet<>()).add(isolate.isolateId());
                    if ("R".equals(result.interpretation())) {
                        resistant.computeIfAbsent(result.antibioticId(), id -> new HashSet<>())
                                .add(isolate.isolateId());
                    }
                }
            }
            tested.forEach((antibioticId, isolateIds) -> {
                int n = isolateIds.size();
                int r = resistant.getOrDefault(antibioticId, Set.of()).size();
                cells.add(new Cell(organism.id(), antibioticId, n,
                        n < MIN_TESTED ? null : Math.round(1000.0 * r / n) / 10.0));
            });
        }
        List<Antibiotic> antibiotics = cells.stream().map(Cell::antibioticId).distinct().map(
                id -> new Antibiotic(id, microAntibioticDAO.get(id).map(MicroAntibiotic::getDisplayName).orElse(id)))
                .sorted(Comparator.comparing(Antibiotic::name)).toList();
        return new AntibiogramResponse(organisms, antibiotics, cells);
    }
}
