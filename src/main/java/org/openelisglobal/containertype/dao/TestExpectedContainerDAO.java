package org.openelisglobal.containertype.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.containertype.valueholder.TestExpectedContainer;

public interface TestExpectedContainerDAO extends BaseDAO<TestExpectedContainer, Long> {

    /** The test's expected containers in their order (FR-G5). */
    List<TestExpectedContainer> getForTest(Integer testId);

    void deleteForTest(Integer testId);

    /** The ids of the tests naming this container type. */
    List<Integer> testIdsUsing(Long containerTypeId);

    /** Every container listed as can be used as a test's sample type (FR-G6). */
    List<TestExpectedContainer> getUsedAs();

    boolean needsOwnContainer(Integer testId);

    void setNeedsOwnContainer(Integer testId, boolean needsOwnContainer);

    /** The container a sample type is taken in when a test names none (FR-G7). */
    Optional<Long> defaultContainerOf(Integer sampleTypeId);

    void setDefaultContainer(Integer sampleTypeId, Long containerTypeId);
}
