package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel lifecycle + sealed-target read rule (OGC-609). SCORED/CLOSED moves
 * belong to the scoring flows, not here.
 */
@Service
@Transactional
public class EQAPanelServiceImpl extends BaseObjectServiceImpl<EQAPanel, Long> implements EQAPanelService {

    /** FR-V2.1-11's forward edges for the moves this service owns. */
    private static final Map<EQAPanelStatus, Set<EQAPanelStatus>> EDGES = new EnumMap<>(EQAPanelStatus.class);

    /** Targets stay hidden until one of these states (FR-V2.1-16). */
    private static final Set<EQAPanelStatus> TARGETS_REVEALED = EnumSet.of(EQAPanelStatus.UNBLINDED,
            EQAPanelStatus.SCORED, EQAPanelStatus.CLOSED);

    static {
        EDGES.put(EQAPanelStatus.PREPARING, EnumSet.of(EQAPanelStatus.SEALED));
        EDGES.put(EQAPanelStatus.SEALED, EnumSet.of(EQAPanelStatus.DISTRIBUTED));
        EDGES.put(EQAPanelStatus.DISTRIBUTED, EnumSet.of(EQAPanelStatus.UNBLINDED));
    }

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Autowired
    private AnalyteService analyteService;

    @Autowired
    private TestAnalyteService testAnalyteService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    public EQAPanelServiceImpl() {
        super(EQAPanel.class);
    }

    @Override
    protected EQAPanelDAO getBaseObjectDAO() {
        return eqaPanelDAO;
    }

    @Override
    public EQAPanel create(EQAPanel panel, List<EQAPanelSample> samples, String sysUserId) {
        if (panel.getScheme() == null) {
            throw new IllegalArgumentException("A panel needs a scheme");
        }
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("A panel needs at least one sample");
        }

        panel.setStatus(EQAPanelStatus.PREPARING);
        panel.setPreparedBy(GenericValidator.isBlankOrNull(sysUserId) ? null : Long.valueOf(sysUserId));
        panel.setPreparedAt(new Timestamp(System.currentTimeMillis()));
        panel.setSysUserId(sysUserId);
        panel.setId(eqaPanelDAO.insert(panel));

        // Only an in-house panel gets blind codes. They exist so a participant
        // analyst cannot recognise the material, and at distribution each one
        // becomes a lab order's accession number (V2.4) — neither applies to a
        // provider panel, which is shipped physically and identified to the
        // receiving lab by its sample code. Stamping "IH-" codes on provider
        // material would be a claim about how it was blinded that is not true.
        boolean blinded = panel.getScheme().getSchemeType() == EQASchemeType.IN_HOUSE;

        int position = 1;
        for (EQAPanelSample sample : samples) {
            sample.setPanel(panel);
            sample.setSysUserId(sysUserId);
            if (GenericValidator.isBlankOrNull(sample.getSampleCode())) {
                sample.setSampleCode(String.format("S%02d", position));
            }
            // The blind code becomes an accession number at distribution, so it has
            // to be unique lab-wide: the panel id is the only identifier in hand
            // that already is.
            if (blinded && GenericValidator.isBlankOrNull(sample.getBlindCode())) {
                sample.setBlindCode(String.format("IH-%d-%02d", panel.getId(), position));
            }
            sample.setId(eqaPanelSampleDAO.insert(sample));
            position++;
        }
        return panel;
    }

    @Override
    public EQAPanel seal(Long panelId, String sysUserId) {
        EQAPanel panel = get(panelId);
        requireEdge(panel, EQAPanelStatus.SEALED);

        List<EQAPanelSample> samples = samplesOf(panelId);
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("Cannot seal a panel with no samples");
        }
        // EncryptionConverter passes blank values through unencrypted, so the seal is
        // the last gate that can keep plaintext-blank targets out of the column.
        if (samples.stream().anyMatch(s -> GenericValidator.isBlankOrNull(s.getTargetValue()))) {
            throw new IllegalArgumentException("Cannot seal a panel while a sample has no target value");
        }
        if (panel.getScheme() != null && panel.getScheme().getSchemeType() == EQASchemeType.IN_HOUSE
                && panel.getUnblindDate() == null) {
            throw new IllegalArgumentException("An in-house panel requires an unblind date before sealing");
        }

        return move(panel, EQAPanelStatus.SEALED, sysUserId);
    }

    @Override
    public EQAPanel distribute(Long panelId, String sysUserId) {
        EQAPanel panel = get(panelId);
        requireEdge(panel, EQAPanelStatus.DISTRIBUTED);
        return move(panel, EQAPanelStatus.DISTRIBUTED, sysUserId);
    }

    @Override
    public EQAPanel unblind(Long panelId, String sysUserId) {
        EQAPanel panel = get(panelId);
        requireEdge(panel, EQAPanelStatus.UNBLINDED);
        return move(panel, EQAPanelStatus.UNBLINDED, sysUserId);
    }

    @Override
    public EQAPanel unblindForUpdate(Long panelId, String sysUserId, EQAUnblindMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("An unblind must record whether it was scheduled or manual");
        }
        EQAPanel panel = eqaPanelDAO.getForUpdate(panelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown panel " + panelId));
        // The loser of a race re-reads the committed status here and fails the
        // edge check, rather than both callers passing it against the same
        // pre-lock read and scoring twice.
        requireEdge(panel, EQAPanelStatus.UNBLINDED);
        panel.setUnblindMethod(method);
        panel.setUnblindedBy(GenericValidator.isBlankOrNull(sysUserId) ? null : Long.valueOf(sysUserId));
        panel.setUnblindedAt(new Timestamp(System.currentTimeMillis()));
        return move(panel, EQAPanelStatus.UNBLINDED, sysUserId);
    }

    private void requireEdge(EQAPanel panel, EQAPanelStatus target) {
        Set<EQAPanelStatus> legal = EDGES.getOrDefault(panel.getStatus(), EnumSet.noneOf(EQAPanelStatus.class));
        if (!legal.contains(target)) {
            throw new IllegalStateException("Cannot move a panel from " + panel.getStatus() + " to " + target);
        }
    }

    private EQAPanel move(EQAPanel panel, EQAPanelStatus target, String sysUserId) {
        panel.setStatus(target);
        panel.setSysUserId(sysUserId);
        return eqaPanelDAO.update(panel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPanelDtos(Long cycleId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAPanel panel : eqaPanelDAO.getAllMatching("cycle.id", cycleId)) {
            rows.add(toPanelDto(panel));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPanelDtosByScheme(Long schemeId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAPanel panel : eqaPanelDAO.getAllMatching("scheme.id", schemeId)) {
            rows.add(toPanelDto(panel));
        }
        return rows;
    }

    /**
     * {@link TestService} is resolved at call time, never injected: injecting it
     * into a service drags TestServiceImpl into early creation, whose static
     * initializer reaches TypeOfSampleServiceImpl, which injects TestService back —
     * an unresolvable cycle that fails Tomcat startup while the integration tests
     * still pass. Same reason EQAPanelRestController field-injects it rather than
     * taking it in its constructor.
     */
    /**
     * The analyte is the grain a panel target is stored against and the name a
     * score is matched by across instances, so a panel sample needs one. Most of
     * the catalog has none: no administration screen writes a test analyte, and on
     * a fresh database the great majority of active tests carry none, which used to
     * make them unusable as panel material however orderable they were.
     *
     * <p>
     * So the analyte is resolved on write and created from the test's own name when
     * it is missing, which is what the reflex-rule editor already does with the
     * analytes it needs. Existing analytes are reused, and creating one is
     * idempotent for a given test.
     */
    @Override
    public Long analyteIdForTest(String testId) {
        if (GenericValidator.isBlankOrNull(testId)) {
            return null;
        }
        Test test = loadTest(testId);
        if (test == null) {
            throw new IllegalArgumentException("Unknown test " + testId);
        }
        List<TestAnalyte> analytes = testAnalyteService.getAllTestAnalytesPerTest(test);
        if (!analytes.isEmpty()) {
            return Long.valueOf(analytes.get(0).getAnalyte().getId());
        }
        return Long.valueOf(analyteFor(test).getId());
    }

    /**
     * An analyte already named after the test is the one meant, so it is adopted
     * rather than duplicated — analyte names are unique, and a catalog commonly
     * carries the analyte while leaving the link to the test unmade. Only the link
     * is new in that case. analyte.name is 60 characters, and a test name can be
     * longer.
     */
    private Analyte analyteFor(Test test) {
        String name = test.getName() == null ? "Test " + test.getId() : test.getName().trim();
        String analyteName = name.length() > 60 ? name.substring(0, 60) : name;

        Analyte probe = new Analyte();
        probe.setAnalyteName(analyteName);
        Analyte analyte = analyteService.getAnalyteByName(probe, true);
        if (analyte == null) {
            probe.setIsActive(IActionConstants.YES);
            analyte = analyteService.save(probe);
        }

        TestAnalyte link = new TestAnalyte();
        link.setTest(test);
        link.setAnalyte(analyte);
        testAnalyteService.save(link);
        return analyte;
    }

    @Override
    @Transactional(readOnly = true)
    public Long findAnalyteIdForTest(String testId) {
        if (GenericValidator.isBlankOrNull(testId)) {
            return null;
        }
        Test test = loadTest(testId);
        if (test == null) {
            return null;
        }
        List<TestAnalyte> analytes = testAnalyteService.getAllTestAnalytesPerTest(test);
        return analytes.isEmpty() ? null : Long.valueOf(analytes.get(0).getAnalyte().getId());
    }

    /**
     * Returns null for a test id with no row behind it. Deliberately
     * {@code getTestById}, which runs a query, rather than {@code get}, which hands
     * back a lazy proxy and throws ObjectNotFoundException on the first dereference
     * of an id that is not there. That throw crosses TestService's own transaction
     * proxy, which marks the shared transaction rollback-only before either caller
     * below can catch it, and the caller's commit then fails with
     * UnexpectedRollback even though it handled the miss.
     */
    private Test loadTest(String testId) {
        return SpringContext.getBean(TestService.class).getTestById(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTestableTestIds() {
        // What disqualifies a test as panel material is that no participant could
        // answer it: a panel sample the receiving laboratory cannot raise an order
        // for is a dead end. Its analyte, by contrast, is created on write when the
        // catalog has none, so it is not a precondition. Filtering on the analyte
        // instead — as this did — left 17 of 210 active tests offerable on a fresh
        // database and no numeric test that was also orderable, so a numeric cycle
        // could be built but never answered.
        //
        // Resolved here rather than in the controller: test is held in a legacy
        // ValueHolder, which needs the session open to answer.
        Set<String> ids = new LinkedHashSet<>();
        for (TypeOfSampleTest sampleTypeTest : typeOfSampleTestService.getAllTypeOfSampleTests()) {
            if (sampleTypeTest.getTestId() != null) {
                ids.add(sampleTypeTest.getTestId());
            }
        }
        return new ArrayList<>(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> toPanelDto(EQAPanel panel) {
        // Callers such as the unblind endpoint and GET /panels/{id} map the DTO
        // after the transaction that produced the panel has ended, so the lazy
        // cycle on the instance they hold cannot load. Re-read by id: inside a
        // transaction this is a first-level-cache hit, outside it is the one query
        // that keeps the mapping from failing with the work already committed.
        if (panel.getId() != null) {
            panel = eqaPanelDAO.get(panel.getId()).orElse(panel);
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", panel.getId());
        dto.put("panelName", panel.getPanelName());
        dto.put("panelType", panel.getPanelType());
        dto.put("status", panel.getStatus() == null ? null : panel.getStatus().name());
        dto.put("schemeId", panel.getScheme() == null ? null : panel.getScheme().getId());
        dto.put("cycleId", panel.getCycle() == null ? null : panel.getCycle().getId());
        // Resolved here, inside the transaction, so the landing list can label a
        // panel by its cycle without a second call per row.
        dto.put("cycleNumber", panel.getCycle() == null ? null : panel.getCycle().getCycleNumber());
        dto.put("cycleName", panel.getCycle() == null ? null : panel.getCycle().getCycleName());
        dto.put("sampleCount", panel.getId() == null ? 0 : samplesOf(panel.getId()).size());
        dto.put("sourceType", panel.getSourceType() == null ? null : panel.getSourceType().name());
        dto.put("lotNumber", panel.getLotNumber());
        dto.put("unblindDate", panel.getUnblindDate() == null ? null : panel.getUnblindDate().toString());
        dto.put("aliquotsProduced", panel.getAliquotsProduced());
        dto.put("aliquotsReserved", panel.getAliquotsReserved());
        dto.put("aliquotsShipped", panel.getAliquotsShipped());
        dto.put("homogeneityQcPassed", panel.getHomogeneityQcPassed());
        dto.put("homogeneityQcNotes", panel.getHomogeneityQcNotes());
        dto.put("expirationDate", panel.getExpirationDate() == null ? null : panel.getExpirationDate().toString());
        // When the targets were revealed, and how. The landing list states the seal
        // as a fact an auditor can read (FR-V2.1-16) rather than leaving it to be
        // inferred from the lifecycle status.
        dto.put("unblindedAt", panel.getUnblindedAt() == null ? null : panel.getUnblindedAt().toString());
        dto.put("unblindMethod", panel.getUnblindMethod() == null ? null : panel.getUnblindMethod().name());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSampleDtos(Long panelId, boolean callerCanUnblind) {
        EQAPanel panel = get(panelId);
        boolean revealTargets = TARGETS_REVEALED.contains(panel.getStatus()) || callerCanUnblind;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAPanelSample sample : samplesOf(panelId)) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", sample.getId());
            dto.put("panelId", panelId);
            dto.put("sampleCode", sample.getSampleCode());
            dto.put("blindCode", sample.getBlindCode());
            dto.put("analyteId", sample.getAnalyteId());
            // Names, not ids, so the pack list a courier reads is legible (T-25).
            dto.put("analyteName", analyteName(sample.getAnalyteId()));
            // The blinding guarantee: nulls, not omissions, so the shape is stable and a
            // client cannot infer anything from missing keys.
            dto.put("targetValue", revealTargets ? sample.getTargetValue() : null);
            dto.put("targetUnit", revealTargets ? sample.getTargetUnit() : null);
            dto.put("acceptanceRangeLow", revealTargets ? sample.getAcceptanceRangeLow() : null);
            dto.put("acceptanceRangeHigh", revealTargets ? sample.getAcceptanceRangeHigh() : null);
            dto.put("targetsRevealed", revealTargets);
            rows.add(dto);
        }
        return rows;
    }

    /**
     * The analyte's name, or null when the sample names no analyte.
     * {@code analyteService.get} throws rather than answering null for a missing
     * row, which fk_eqa_panel_sample_analyte makes unreachable — so there is no
     * not-found branch to write here.
     */
    private String analyteName(Long analyteId) {
        return analyteId == null ? null : analyteService.get(String.valueOf(analyteId)).getAnalyteName();
    }

    private List<EQAPanelSample> samplesOf(Long panelId) {
        return eqaPanelSampleDAO.getAllMatchingOrdered("panel.id", panelId, "sampleCode", false);
    }
}
