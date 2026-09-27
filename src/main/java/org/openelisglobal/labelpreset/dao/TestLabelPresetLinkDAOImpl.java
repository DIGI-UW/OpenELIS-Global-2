package org.openelisglobal.labelpreset.dao;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestLabelPresetLinkDAOImpl extends BaseDAOImpl<TestLabelPresetLink, Integer>
        implements TestLabelPresetLinkDAO {

    public TestLabelPresetLinkDAOImpl() {
        super(TestLabelPresetLink.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestLabelPresetLink> listByTestId(String testId) {
        return entityManager
                .createQuery("FROM TestLabelPresetLink l WHERE l.test.id = :testId", TestLabelPresetLink.class)
                .setParameter("testId", testId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestLabelPresetLink> listByPresetId(Integer presetId) {
        return entityManager
                .createQuery("FROM TestLabelPresetLink l WHERE l.preset.id = :presetId", TestLabelPresetLink.class)
                .setParameter("presetId", presetId).getResultList();
    }
}
