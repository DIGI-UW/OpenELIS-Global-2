package org.openelisglobal.labelpreset.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestLabelConfigDAOImpl extends BaseDAOImpl<TestLabelConfig, Integer> implements TestLabelConfigDAO {

    public TestLabelConfigDAOImpl() {
        super(TestLabelConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TestLabelConfig> getByTestId(String testId) {
        List<TestLabelConfig> results = entityManager
                .createQuery("FROM TestLabelConfig c WHERE c.test.id = :testId", TestLabelConfig.class)
                .setParameter("testId", testId).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
