package org.openelisglobal.qaevent.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.qaevent.dao.NCEventDAO;
import org.openelisglobal.qaevent.form.InlineNCERequestForm;
import org.openelisglobal.qaevent.form.NCEResponseForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NCEventServiceImpl extends AuditableBaseObjectServiceImpl<NcEvent, String> implements NCEventService {

    @Autowired
    protected NCEventDAO baseObjectDAO;

    @Autowired
    private ResultService resultService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private IStatusService statusService;

    public NCEventServiceImpl() {
        super(NcEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId) {
        return baseObjectDAO.findByNCENumberOrLabOrderId(nceNumber, labOrderId);
    }

    @Override
    protected NCEventDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public NcEvent createNCEFromInlineRequest(InlineNCERequestForm request, String resultId, SystemUser user,
            Map<String, Object> contextInfo) {
        NcEvent ncEvent = new NcEvent();

        // Generate collision-safe NCE number using UUID
        ncEvent.setNceNumber(NcEvent.NCE_NUMBER_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        ncEvent.setName(request.getTitle());
        ncEvent.setDescription(request.getDescription());
        ncEvent.setNameOfReporter(user.getFirstName() + " " + user.getLastName());
        ncEvent.setLabOrderNumber((String) contextInfo.get("labNumber"));

        ncEvent.setReportDate(new java.sql.Date(System.currentTimeMillis()));
        ncEvent.setDateOfEvent(new java.sql.Date(System.currentTimeMillis()));
        ncEvent.setStatus(NcEvent.STATUS_PENDING);

        // Persist trigger context for audit trail
        ncEvent.setSourceType(request.getSourceType());
        ncEvent.setTriggerType(request.getTriggerType());
        ncEvent.setTriggerAction(request.getTriggerAction());

        // Required by BaseObject audit trail
        ncEvent.setSysUserId(user.getId().toString());

        NcEvent savedEvent = save(ncEvent);

        // FR-009: Handle sample rejection when sampleAction is REJECT_SAMPLE
        if (NcEvent.ACTION_REJECT_SAMPLE.equals(request.getSampleAction()) && resultId != null) {
            rejectSampleForResult(resultId);
        }

        return savedEvent;
    }

    /**
     * Reject the parent sample of a result: set sample status to SampleRejected and
     * cancel all pending analyses.
     */
    private void rejectSampleForResult(String resultId) {
        Result result = resultService.get(resultId);
        if (result == null || result.getAnalysis() == null) {
            return;
        }

        Analysis analysis = result.getAnalysis();
        SampleItem sampleItem = analysis.getSampleItem();
        if (sampleItem == null || sampleItem.getSample() == null) {
            return;
        }

        Sample sample = sampleItem.getSample();
        String rejectedStatusId = statusService.getStatusID(SampleStatus.SampleRejected);
        String canceledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        String finalizedStatusId = statusService.getStatusID(AnalysisStatus.Finalized);

        // Set sample status to Rejected
        sample.setStatusId(rejectedStatusId);
        sampleService.update(sample);

        // Cancel all non-finalized analyses for this sample
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        for (Analysis a : analyses) {
            if (!finalizedStatusId.equals(a.getStatusId()) && !canceledStatusId.equals(a.getStatusId())) {
                a.setStatusId(canceledStatusId);
                analysisService.update(a);
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "rejectSampleForResult",
                "Sample " + sample.getAccessionNumber() + " rejected via NCE inline form for result " + resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public NCEResponseForm buildNCEResponse(NcEvent ncEvent, List<String> associatedResults) {
        NCEResponseForm response = new NCEResponseForm();
        response.setId(ncEvent.getId());
        response.setNceNumber(ncEvent.getNceNumber());
        response.setStatus(ncEvent.getStatus());
        response.setCreatedDate(DateUtil.formatDateAsText(ncEvent.getReportDate()));
        response.setAssociatedResults(associatedResults);
        return response;
    }
}
