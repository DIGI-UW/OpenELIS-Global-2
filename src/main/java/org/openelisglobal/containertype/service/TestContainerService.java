package org.openelisglobal.containertype.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.containertype.form.SampleTypeContainersView;
import org.openelisglobal.containertype.form.TestContainersForm;
import org.openelisglobal.containertype.form.TestContainersView;

/**
 * OGC-1611 FR-G5 to G7: the containers each test expects, and the container a
 * sample type is taken in when a test names none.
 */
public interface TestContainerService {

    Optional<TestContainersView> forTest(String testId);

    /** Replaces the test's expected containers; nothing is written when refused. */
    TestContainersView save(String testId, TestContainersForm form, String sysUserId);

    Optional<SampleTypeContainersView> forSampleType(String sampleTypeId);

    SampleTypeContainersView setDefaultContainer(String sampleTypeId, Long containerTypeId);

    /** The names of the tests expecting this container type, A to Z (FR-G3). */
    List<String> testNamesUsing(Long containerTypeId);
}
