package org.openelisglobal.statusofsample.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SAMPLE_STATUS_VIEW')")
public interface StatusOfSampleService extends BaseObjectService<StatusOfSample, String> {
    void getData(StatusOfSample sourceOfSample);

    // void updateData(StatusOfSample sourceOfSample);

    // boolean insertData(StatusOfSample sourceOfSample);

    List<StatusOfSample> getPageOfStatusOfSamples(int startingRecNo);

    Integer getTotalStatusOfSampleCount();

    StatusOfSample getDataByStatusTypeAndStatusCode(StatusOfSample statusofsample);

    List<StatusOfSample> getAllStatusOfSamples();
}
