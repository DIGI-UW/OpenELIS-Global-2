package org.openelisglobal.containertype.daoimpl;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.containertype.dao.TestExpectedContainerDAO;
import org.openelisglobal.containertype.valueholder.TestExpectedContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestExpectedContainerDAOImpl extends BaseDAOImpl<TestExpectedContainer, Long>
        implements TestExpectedContainerDAO {

    public TestExpectedContainerDAOImpl() {
        super(TestExpectedContainer.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestExpectedContainer> getForTest(Integer testId) {
        return entityManager
                .createQuery("FROM TestExpectedContainer c WHERE c.testId = :testId ORDER BY c.displayOrder, c.id",
                        TestExpectedContainer.class)
                .setParameter("testId", testId).getResultList();
    }

    @Override
    public void lockTest(Integer testId) {
        entityManager.createNativeQuery("SELECT id FROM clinlims.test WHERE id = :testId FOR UPDATE")
                .setParameter("testId", testId).getResultList();
    }

    @Override
    public void deleteForTest(Integer testId) {
        entityManager.createQuery("DELETE FROM TestExpectedContainer c WHERE c.testId = :testId")
                .setParameter("testId", testId).executeUpdate();
        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> testIdsUsing(Long containerTypeId) {
        return entityManager
                .createQuery("SELECT DISTINCT c.testId FROM TestExpectedContainer c"
                        + " WHERE c.containerTypeId = :containerTypeId", Integer.class)
                .setParameter("containerTypeId", containerTypeId).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestExpectedContainer> getUsedAs() {
        return entityManager.createQuery("FROM TestExpectedContainer c WHERE c.usedAsSampleTypeId IS NOT NULL",
                TestExpectedContainer.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean needsOwnContainer(Integer testId) {
        List<?> rows = entityManager
                .createNativeQuery("SELECT needs_own_container FROM clinlims.test WHERE id = :testId")
                .setParameter("testId", testId).getResultList();
        return !rows.isEmpty() && Boolean.TRUE.equals(rows.get(0));
    }

    @Override
    public void setNeedsOwnContainer(Integer testId, boolean needsOwnContainer) {
        entityManager.createNativeQuery("UPDATE clinlims.test SET needs_own_container = :value WHERE id = :testId")
                .setParameter("value", needsOwnContainer).setParameter("testId", testId).executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> defaultContainerOf(Integer sampleTypeId) {
        List<?> rows = entityManager
                .createNativeQuery(
                        "SELECT default_container_type_id FROM clinlims.type_of_sample WHERE id = :sampleTypeId")
                .setParameter("sampleTypeId", sampleTypeId).getResultList();
        return rows.isEmpty() || rows.get(0) == null ? Optional.empty()
                : Optional.of(((Number) rows.get(0)).longValue());
    }

    @Override
    public void setDefaultContainer(Integer sampleTypeId, Long containerTypeId) {
        if (containerTypeId == null) {
            entityManager.createNativeQuery(
                    "UPDATE clinlims.type_of_sample SET default_container_type_id = NULL WHERE id = :sampleTypeId")
                    .setParameter("sampleTypeId", sampleTypeId).executeUpdate();
            return;
        }
        entityManager
                .createNativeQuery("UPDATE clinlims.type_of_sample SET default_container_type_id = :containerId"
                        + " WHERE id = :sampleTypeId")
                .setParameter("containerId", containerTypeId).setParameter("sampleTypeId", sampleTypeId)
                .executeUpdate();
    }
}
