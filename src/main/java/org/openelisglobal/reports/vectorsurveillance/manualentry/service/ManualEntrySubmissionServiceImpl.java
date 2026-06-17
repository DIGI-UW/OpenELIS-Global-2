package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.reports.vectorsurveillance.manualentry.dao.ManualEntrySubmissionAuditDAO;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntrySubmissionAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualEntrySubmissionServiceImpl implements ManualEntrySubmissionService {

    private final ManualEntrySubmissionAuditDAO auditDAO;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ManualEntrySubmissionServiceImpl(ManualEntrySubmissionAuditDAO auditDAO) {
        this.auditDAO = auditDAO;
    }

    @Override
    @Transactional
    public ManualEntrySubmissionAudit submit(LocalDate periodStart, LocalDate periodEnd, Integer siteId,
            Map<String, String> valueSnapshot, String sysUserId) {
        if (periodStart == null || periodEnd == null) {
            throw new LIMSRuntimeException("Manual entry submission requires a period start and end");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new LIMSRuntimeException("Manual entry submission periodStart must be <= periodEnd");
        }
        if (valueSnapshot == null || valueSnapshot.isEmpty()) {
            throw new LIMSRuntimeException("Manual entry submission requires a non-empty value snapshot");
        }
        if (GenericValidator.isBlankOrNull(sysUserId)) {
            throw new LIMSRuntimeException("Manual entry submission requires the acting user");
        }

        ManualEntrySubmissionAudit audit = new ManualEntrySubmissionAudit();
        audit.setPeriodStart(periodStart);
        audit.setPeriodEnd(periodEnd);
        audit.setSiteId(siteId);
        audit.setValueSnapshot(serialize(valueSnapshot));
        audit.setSubmittedByUserId(sysUserId);
        audit.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
        audit.setSysUserId(sysUserId);

        // Insert-only: re-submitting a week is a new distinct row (FR-008 / US4-4).
        Integer id = auditDAO.insert(audit);
        return auditDAO.get(id).orElse(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualEntrySubmissionAudit> getAudit(LocalDate periodStart, LocalDate periodEnd) {
        List<ManualEntrySubmissionAudit> rows = auditDAO.getByPeriod(periodStart, periodEnd);
        return rows != null ? rows : Collections.emptyList();
    }

    private String serialize(Map<String, String> valueSnapshot) {
        try {
            return objectMapper.writeValueAsString(valueSnapshot);
        } catch (JsonProcessingException e) {
            throw new LIMSRuntimeException("Could not serialise manual entry value snapshot", e);
        }
    }
}
