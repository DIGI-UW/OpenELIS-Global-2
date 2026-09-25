package org.openelisglobal.typeofsample.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
public interface TypeOfSampleTestService extends BaseObjectService<TypeOfSampleTest, String> {
    void getData(TypeOfSampleTest typeOfSampleTest);

    List<TypeOfSampleTest> getTypeOfSampleTestsForTest(String testId);

    List<TypeOfSampleTest> getPageOfTypeOfSampleTests(int startingRecNo);

    List<TypeOfSampleTest> getAllTypeOfSampleTests();

    Integer getTotalTypeOfSampleTestCount();

    // TypeOfSampleTest getTypeOfSampleTestForTest(String testId);

    List<TypeOfSampleTest> getTypeOfSampleTestsForSampleType(String sampleTypeId);

    /**
     * Persist the per-sample-type display order for the given tests (OGC-949 M12 /
     * OGC-985). Only junction rows in this sample type whose testId appears in the
     * map are updated; the whole load-set-update runs in one transaction.
     *
     * <p>
     * A catalogue WRITE, so it is pinned to PRIV_SAMPLE_TYPE_MANAGE rather than
     * inheriting the interface gate, that one accepts PRIV_CATALOGUE_VIEW, which
     * every order-entry role holds.
     */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    void updateDisplayOrder(String sampleTypeId, Map<String, Integer> displayOrderByTestId, String sysUserId);
}
