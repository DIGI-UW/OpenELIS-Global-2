package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.dao.EQAProgramTestDAO;
import org.openelisglobal.eqa.dao.EQASchemeAnalystDAO;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAProgramServiceImpl extends BaseObjectServiceImpl<EQAProgram, Long> implements EQAProgramService {

    @Autowired
    private EQAProgramDAO eqaProgramDAO;

    @Autowired
    private EQAProgramTestDAO eqaProgramTestDAO;

    @Autowired
    private EQASchemeAnalystDAO eqaSchemeAnalystDAO;

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    public EQAProgramServiceImpl() {
        super(EQAProgram.class);
    }

    @Override
    protected EQAProgramDAO getBaseObjectDAO() {
        return eqaProgramDAO;
    }

    /**
     * External arrangement types have a real provider organization behind them;
     * only in-house schemes may omit it. save() delegates to insert/update, so both
     * service-level write paths are covered. activateProgram/deactivateProgram
     * write through the DAO directly and stay exempt — which is what still lets a
     * legacy provider-less scheme be retired.
     */
    @Override
    public Long insert(EQAProgram program) {
        validateProviderRequired(program);
        return super.insert(program);
    }

    @Override
    public EQAProgram update(EQAProgram program) {
        validateProviderRequired(program);
        validateSchemeTypeNotChangedUnderLiveCycles(program);
        return super.update(program);
    }

    /**
     * The scheme type decides whether a provider organization is required, which
     * blinding lane the scheme travels and which cycle state machine applies, so
     * changing it reinterprets everything already underneath it. This sits on
     * update() rather than on the endpoint because the configuration loader writes
     * the same field on its upsert branch, as the daemon user; a guard on one
     * caller would only move the hole to the quieter one.
     *
     * <p>
     * A closed cycle cannot be worked on again, which is what makes the rule "no
     * live cycle" rather than "no cycle at all". A deployment upgraded from V1
     * carries a backfilled CLOSED cycle for every completed legacy distribution,
     * and those schemes all took the INTERNATIONAL_PT default; barring a type
     * change outright would strand every one of them outside in-house blinding,
     * with no route out from any screen.
     *
     * <p>
     * SCORED is deliberately not treated as closed. The line between them is that a
     * SCORED cycle is still workable and a CLOSED one is only readable: SCORED is
     * on the scoring path and scores are distributed from it, while CLOSED has no
     * outgoing edge on any of the three machines. Both are still read — the rolling
     * window that decides persistent failure selects SCORED and CLOSED alike — so
     * being unread is not the distinction and must not be argued as one. That makes
     * this rule strict in practice: nothing in the product closes a cycle today, so
     * a scheme that has run a V2 cycle keeps its type. The V1 backfill writes
     * CLOSED directly, which is the case the rule exists to admit.
     *
     * <p>
     * The comparison has to reach the database for the previous type. Both callers
     * mutate a scheme that is still managed and then hand that same object to
     * update(), so by the time this runs the object carries only the new type, and
     * an ordinary read is answered from the persistence context with that same
     * object. findStoredSchemeType reads the committed column instead.
     */
    private void validateSchemeTypeNotChangedUnderLiveCycles(EQAProgram program) {
        if (program.getId() == null) {
            return;
        }
        EQASchemeType stored = eqaProgramDAO.findStoredSchemeType(program.getId());
        if (stored == null || stored == program.getSchemeType()) {
            return;
        }
        // Named by number, not by name: cycle_name is nullable and the number is what
        // the provider scheme list shows.
        List<String> live = eqaCycleDAO.findBySchemeIds(List.of(program.getId())).stream()
                .filter(cycle -> cycle.getStatus() != EQACycleStatus.CLOSED)
                .map(cycle -> "cycle " + cycle.getCycleNumber()).collect(Collectors.toList());
        if (!live.isEmpty()) {
            // The message offers the new scheme and nothing else on purpose. Closing a
            // cycle would also lift the bar, but nothing in the product asks for CLOSED
            // — the edge is legal on all three machines and unreachable in practice — so
            // telling an operator to close it first is advice nobody can take.
            throw new LIMSRuntimeException("This scheme's type cannot change from " + stored + " to "
                    + program.getSchemeType() + ": " + String.join(", ", live)
                    + " has run under the current type and its results are still read against it."
                    + " Create a new scheme of the type you need.");
        }
    }

    private void validateProviderRequired(EQAProgram program) {
        if (program.getSchemeType() != EQASchemeType.IN_HOUSE
                && (program.getProvider() == null || program.getProvider().isBlank())) {
            throw new LIMSRuntimeException("Provider is required unless scheme type is IN_HOUSE");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgram> findActivePrograms() {
        return eqaProgramDAO.findByIsActive(true);
    }

    @Override
    public EQAProgram deactivateProgram(Long programId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));
        program.setIsActive(false);
        return eqaProgramDAO.update(program);
    }

    @Override
    public EQAProgram activateProgram(Long programId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));
        program.setIsActive(true);
        return eqaProgramDAO.update(program);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAProgramTest> getTestAssignments(Long programId) {
        return eqaProgramTestDAO.findByProgramId(programId);
    }

    @Override
    public EQAProgramTest assignTest(Long programId, Long testId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));

        // UNIQUE(eqa_program_id, test_id) outlives removeTestAssignment, which only
        // clears is_active, so re-assigning a test must revive the existing row —
        // a second insert violates the constraint.
        EQAProgramTest programTest = eqaProgramTestDAO.findByProgramId(programId).stream()
                .filter(existing -> testId.equals(existing.getTestId())).findFirst().orElse(null);

        if (programTest != null) {
            programTest.setIsActive(true);
            return eqaProgramTestDAO.update(programTest);
        }

        programTest = new EQAProgramTest();
        programTest.setEqaProgram(program);
        programTest.setTestId(testId);
        programTest.setIsActive(true);
        // sys_user_id is NOT NULL and nothing else populates it; the assignment
        // inherits the scheme's owner, the only identity available here.
        programTest.setSysUserId(program.getSysUserId());
        eqaProgramTestDAO.insert(programTest);
        return programTest;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQASchemeAnalyst> getAnalysts(Long programId) {
        return eqaSchemeAnalystDAO.getAllMatching("scheme.id", programId);
    }

    @Override
    public List<EQASchemeAnalyst> setAnalysts(Long programId, List<Long> systemUserIds, String sysUserId) {
        EQAProgram program = eqaProgramDAO.get(programId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program not found: " + programId));

        // uq_eqa_scheme_analyst_scheme_user has no soft-delete column behind it, so
        // "replace the roster" is a delete of what left plus an insert of what
        // arrived — re-inserting a kept analyst would violate it.
        List<EQASchemeAnalyst> existing = getAnalysts(programId);
        for (EQASchemeAnalyst analyst : existing) {
            if (!systemUserIds.contains(analyst.getSystemUserId())) {
                eqaSchemeAnalystDAO.delete(analyst);
            }
        }
        for (Long systemUserId : systemUserIds) {
            if (existing.stream().noneMatch(analyst -> systemUserId.equals(analyst.getSystemUserId()))) {
                EQASchemeAnalyst analyst = new EQASchemeAnalyst();
                analyst.setScheme(program);
                analyst.setSystemUserId(systemUserId);
                analyst.setSysUserId(sysUserId);
                eqaSchemeAnalystDAO.insert(analyst);
            }
        }
        return getAnalysts(programId);
    }

    @Override
    public void removeTestAssignment(Long programTestId) {
        EQAProgramTest programTest = eqaProgramTestDAO.get(programTestId)
                .orElseThrow(() -> new IllegalArgumentException("EQA Program Test not found: " + programTestId));
        programTest.setIsActive(false);
        eqaProgramTestDAO.update(programTest);
    }
}
