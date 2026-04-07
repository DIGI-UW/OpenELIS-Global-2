package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.valueholder.Sample;

public interface SampleEditService {

    void editSample(SampleEditForm form, HttpServletRequest request, Sample updatedSample, boolean sampleChanged,
            String sysUserId);

    List<String> getUpdatedAnalysisList();
}
