package org.openelisglobal.testtrailer.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;

public interface TestTrailerService extends BaseObjectService<TestTrailer, String> {
    void getData(TestTrailer testTrailer);

    List<TestTrailer> getPageOfTestTrailers(int startingRecNo);

    Integer getTotalTestTrailerCount();

    TestTrailer getTestTrailerByName(TestTrailer testTrailer);

    List<TestTrailer> getAllTestTrailers();

    List<TestTrailer> getTestTrailers(String filter);
}
