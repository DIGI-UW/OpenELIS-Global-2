package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.dto.EqaCoverageView;
import org.openelisglobal.accreditation.dto.TestAccreditationView;
import org.openelisglobal.accreditation.valueholder.AccreditationStatus;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-686 — per-test accreditation enrollment.
 *
 * <p>
 * An enrollment is pure membership: which test falls under which body. Status
 * and expiry are read from the owning {@link AccreditingBody}, so renewing a
 * certificate is one update on the body and never a sweep over these rows.
 *
 * <p>
 * {@link TestService} is resolved at call time via {@link SpringContext} rather
 * than field-injected, following the idiom used by
 * {@code UserTestSectionServiceImpl} and {@code ResultsLoadUtility}. Injecting
 * it eagerly pulls {@code TestServiceImpl} into creation while this bean is
 * being built, and its static initializer reaches for
 * {@code TypeOfSampleServiceImpl}, which itself injects {@code TestService} —
 * an unresolvable cycle that fails Tomcat context startup outright. Normal
 * startup happens to create that pair in the opposite order, so the cycle is
 * invisible until something forces it.
 */
@Service
public class TestAccreditationServiceImpl extends AuditableBaseObjectServiceImpl<TestAccreditation, Long>
        implements TestAccreditationService {

    private static final Comparator<String> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());

    @Autowired
    protected TestAccreditationDAO baseObjectDAO;

    @Autowired
    private AccreditingBodyDAO accreditingBodyDAO;

    @Autowired
    private EQALabProgramEnrollmentDAO eqaLabProgramEnrollmentDAO;

    public TestAccreditationServiceImpl() {
        super(TestAccreditation.class);
        this.auditTrailLog = true;
    }

    @Override
    protected TestAccreditationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditationView> getEnrollmentViews(Long accreditingBodyId, String testId) {
        List<TestAccreditation> rows;
        if (accreditingBodyId != null) {
            rows = baseObjectDAO.getByBody(accreditingBodyId);
            if (testId != null && !testId.isBlank()) {
                rows.removeIf(r -> !testId.equals(r.getTestId()));
            }
        } else if (testId != null && !testId.isBlank()) {
            rows = baseObjectDAO.getByTest(testId);
        } else {
            rows = baseObjectDAO.getAll();
        }

        Map<Long, AccreditingBody> bodies = bodiesById();
        LocalDate today = LocalDate.now();

        List<TestAccreditationView> views = new ArrayList<>();
        for (TestAccreditation row : rows) {
            views.add(toView(row, bodies.get(row.getAccreditingBodyId()), today));
        }
        // Group by body (logo order), then by test name, so the table reads the way
        // the bodies list does.
        views.sort(Comparator.comparing((TestAccreditationView v) -> v.bodyCode, NULLS_FIRST)
                .thenComparing(v -> v.testName, NULLS_FIRST));
        return views;
    }

    @Override
    @Transactional
    public TestAccreditation enroll(String testId, Long accreditingBodyId, LocalDate effectiveFrom, String sysUserId) {
        if (testId == null || testId.isBlank()) {
            throw new IllegalArgumentException("Test is required");
        }
        if (accreditingBodyId == null) {
            throw new IllegalArgumentException("Accrediting body is required");
        }
        if (accreditingBodyDAO.get(accreditingBodyId).isEmpty()) {
            throw new IllegalArgumentException("No accrediting body with id " + accreditingBodyId);
        }
        if (testService().getTestById(testId) == null) {
            throw new IllegalArgumentException("No test with id " + testId);
        }
        // FR-19: the DB unique constraint is the backstop; checking here produces the
        // "already accredited by that body" message the UI links to the existing row.
        if (baseObjectDAO.getByTestAndBody(testId, accreditingBodyId) != null) {
            throw new IllegalArgumentException("This test is already accredited by that body");
        }

        TestAccreditation row = new TestAccreditation();
        row.setTestId(testId);
        row.setAccreditingBodyId(accreditingBodyId);
        row.setEffectiveFrom(effectiveFrom);
        row.setSysUserId(sysUserId);
        insert(row);
        return row;
    }

    @Override
    @Transactional
    public void unenroll(Long id, String sysUserId) {
        if (id == null) {
            throw new IllegalArgumentException("Enrollment id is required");
        }
        Optional<TestAccreditation> found = baseObjectDAO.get(id);
        TestAccreditation row = found
                .orElseThrow(() -> new IllegalArgumentException("No test accreditation with id " + id));
        row.setSysUserId(sysUserId);
        delete(row);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EqaCoverageView> getEqaCoverage() {
        Set<String> coveredTestIds = new HashSet<>(eqaLabProgramEnrollmentDAO.findEqaCoveredTestIds());
        Map<Long, AccreditingBody> bodies = bodiesById();
        LocalDate today = LocalDate.now();

        Map<Long, EqaCoverageView> rowsByBody = new LinkedHashMap<>();
        // getAll() is already the whole enrollment table; grouping here beats a
        // per-body query, and the table is one row per accredited test.
        for (TestAccreditation enrollment : baseObjectDAO.getAll()) {
            AccreditingBody body = bodies.get(enrollment.getAccreditingBodyId());
            if (body == null) {
                continue;
            }
            EqaCoverageView row = rowsByBody.computeIfAbsent(body.getId(), id -> newCoverageRow(body, today));
            row.enrolledTestCount++;
            if (coveredTestIds.contains(enrollment.getTestId())) {
                row.coveredTestCount++;
            } else {
                row.gaps.add(
                        new EqaCoverageView.GapTest(enrollment.getTestId(), testDisplayName(enrollment.getTestId())));
            }
        }

        List<EqaCoverageView> rows = new ArrayList<>(rowsByBody.values());
        for (EqaCoverageView row : rows) {
            row.gaps.sort(Comparator.comparing((EqaCoverageView.GapTest g) -> g.testName, NULLS_FIRST));
        }
        rows.sort(Comparator.comparing((EqaCoverageView v) -> v.bodyCode, NULLS_FIRST));
        return rows;
    }

    private EqaCoverageView newCoverageRow(AccreditingBody body, LocalDate asOf) {
        EqaCoverageView row = new EqaCoverageView();
        row.accreditingBodyId = body.getId();
        row.bodyCode = body.getCode();
        row.bodyName = body.getName();
        row.status = AccreditationStatus.of(body.getActive(), body.getExpiresOn(), asOf).name();
        return row;
    }

    private Map<Long, AccreditingBody> bodiesById() {
        Map<Long, AccreditingBody> map = new HashMap<>();
        for (AccreditingBody body : accreditingBodyDAO.getAllOrdered()) {
            map.put(body.getId(), body);
        }
        return map;
    }

    private TestAccreditationView toView(TestAccreditation row, AccreditingBody body, LocalDate asOf) {
        TestAccreditationView view = new TestAccreditationView();
        view.id = row.getId();
        view.testId = row.getTestId();
        view.testName = testDisplayName(row.getTestId());
        view.accreditingBodyId = row.getAccreditingBodyId();
        view.effectiveFrom = row.getEffectiveFrom();
        if (body != null) {
            view.bodyCode = body.getCode();
            view.bodyName = body.getName();
            view.bodyExpiresOn = body.getExpiresOn();
            view.status = AccreditationStatus.of(body.getActive(), body.getExpiresOn(), asOf).name();
        }
        return view;
    }

    /**
     * Resolved in-transaction; same helper the Test Catalog editor renders with.
     */
    private String testDisplayName(String testId) {
        org.openelisglobal.test.valueholder.Test test = testService().getTestById(testId);
        return test == null ? null : TestServiceImpl.getLocalizedTestNameWithType(test);
    }

    /**
     * See the class javadoc: field-injecting this breaks Tomcat context startup.
     */
    private TestService testService() {
        return SpringContext.getBean(TestService.class);
    }

}
