package org.openelisglobal.sample.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

public interface SamplePatientEntryService {

    void persistData(SamplePatientUpdateData updateData, PatientManagementUpdate patientUpdate,
            PatientManagementInfo patientInfo, SamplePatientEntryForm form, HttpServletRequest request);

    /**
     * OGC-285 M5b — persist the Order Entry label requests for a just-saved order
     * (FRS §7.3). Builds the correlation maps the persistence service needs by
     * <em>list position</em> over {@code updateData.getSampleItemsTests()} — the
     * canonical, ordered, ID-bearing source after {@code persistData}: the
     * {@code i}-th {@code SampleTestCollection} is keyed {@code sample_id_local =
     * String.valueOf(i)}, matching the positional {@code sample_id_local} the
     * frontend assigns over the same filtered sample list. Then delegates to
     * {@link org.openelisglobal.labelpreset.service.OrderLabelRequestService#persistRequest}.
     *
     * <p>
     * NOTE: positional keying is deliberate — {@code SampleItem.sortOrder} is a
     * {@code String} ordered lexically ("10" before "2"), so a re-fetch +
     * sort-order path would mis-correlate at &ge;10 samples. {@code updateData} is
     * the in-memory order the controller already holds.
     *
     * @return the persisted rows (empty when {@code payload} is null/empty)
     */
    List<OrderLabelRequest> persistLabelRequests(SamplePatientUpdateData updateData, OrderLabelPersistRequest payload,
            String sysUserId);
}
