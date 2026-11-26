package org.openelisglobal.testcatalog.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.resultrun.service.RunQcService;
import org.openelisglobal.testcatalog.dao.TestQcPolicyDAO;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestQcPolicyServiceImpl implements TestQcPolicyService {

    @Autowired
    private TestQcPolicyDAO policyDAO;
    @Autowired
    private TestQcTargetService targetService;
    @Lazy
    @Autowired
    private RunQcService runQcService;

    @Override
    @Transactional(readOnly = true)
    public TestQcPolicy policyOf(String testId) {
        return policiesOf(List.of(testId)).get(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, TestQcPolicy> policiesOf(Collection<String> testIds) {
        List<String> ids = testIds == null ? List.of()
                : testIds.stream().filter(StringUtils::isNumeric).distinct().toList();
        Map<String, TestQcPolicy> policies = new LinkedHashMap<>();
        ids.forEach(id -> policies.put(id, TestQcPolicy.none(id)));
        policyDAO.getByTestIds(ids.stream().map(Long::valueOf).toList())
                .forEach(stored -> policies.put(String.valueOf(stored.getId()), stored));
        return policies;
    }

    @Override
    @Transactional
    public List<TestQcTarget> saveSection(String testId, List<TestQcTarget> targets, TestQcPolicy policy,
            String sysUserId) {
        List<TestQcTarget> saved = targetService.saveTargetsForTest(testId, targets, sysUserId);
        if (policy != null && savePolicy(testId, policy, sysUserId)) {
            runQcService.refreshActiveRunsOfTest(testId, sysUserId);
        }
        return saved;
    }

    /** Stores the policy; true when it differs from what the test had. */
    private boolean savePolicy(String testId, TestQcPolicy wanted, String sysUserId) {
        TestQcPolicy stored = policyDAO.get(Long.valueOf(testId)).orElse(null);
        TestQcPolicy.WhenMissing whenMissing = wanted.getControlWhenMissing() == null ? TestQcPolicy.WhenMissing.WARN
                : wanted.getControlWhenMissing();
        TestQcPolicy current = stored == null ? TestQcPolicy.none(testId) : stored;
        if (current.isControlRequired() == wanted.isControlRequired() && current.getControlWhenMissing() == whenMissing
                && current.isBlankRequired() == wanted.isBlankRequired()
                && current.getBlankWhenMissing() == wanted.getBlankWhenMissing()
                && current.getBlankWhenFails() == wanted.getBlankWhenFails()
                && current.isDuplicateRequired() == wanted.isDuplicateRequired()
                && current.getDuplicateWhenMissing() == wanted.getDuplicateWhenMissing()
                && current.getDuplicateWhenFails() == wanted.getDuplicateWhenFails()) {
            return false;
        }
        current.setControlRequired(wanted.isControlRequired());
        current.setControlWhenMissing(whenMissing);
        current.setBlankRequired(wanted.isBlankRequired());
        current.setBlankWhenMissing(wanted.getBlankWhenMissing());
        current.setBlankWhenFails(wanted.getBlankWhenFails());
        current.setDuplicateRequired(wanted.isDuplicateRequired());
        current.setDuplicateWhenMissing(wanted.getDuplicateWhenMissing());
        current.setDuplicateWhenFails(wanted.getDuplicateWhenFails());
        current.setSystemUserId(StringUtils.isNumeric(sysUserId) ? Integer.valueOf(sysUserId) : null);
        current.setSysUserId(sysUserId);
        if (stored == null) {
            policyDAO.insert(current);
        } else {
            policyDAO.update(current);
        }
        return true;
    }
}
