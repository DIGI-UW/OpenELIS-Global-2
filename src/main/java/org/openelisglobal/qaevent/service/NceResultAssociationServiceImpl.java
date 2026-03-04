package org.openelisglobal.qaevent.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.dao.NceResultAssociationDAO;
import org.openelisglobal.qaevent.form.NCEBadgeResponseForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation.AssociationType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing associations between lab results and
 * NCEs. Provides business logic for linking results to quality issues and
 * maintaining relationships.
 */
@Service
@DependsOn({ "entityManagerFactory" })
@Transactional
public class NceResultAssociationServiceImpl extends AuditableBaseObjectServiceImpl<NceResultAssociation, Integer>
        implements NceResultAssociationService {

    @Autowired
    protected NceResultAssociationDAO baseObjectDAO;

    @Autowired
    private ResultService resultService;

    @Autowired
    private SampleHumanService sampleHumanService;

    public NceResultAssociationServiceImpl() {
        super(NceResultAssociation.class);
    }

    @Override
    protected NceResultAssociationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public NceResultAssociation createAssociation(String resultId, NcEvent ncEvent, AssociationType associationType,
            String createdBy, String description) {
        validateAssociation(resultId, ncEvent, associationType);

        // Check if association already exists
        if (baseObjectDAO.associationExists(resultId, ncEvent.getId())) {
            throw new IllegalArgumentException(
                    "Association already exists between result " + resultId + " and NCE " + ncEvent.getId());
        }

        NceResultAssociation association = new NceResultAssociation();
        association.setResultId(resultId);
        association.setNcEvent(ncEvent);
        association.setAssociationTypeEnum(associationType);
        association.setCreatedBy(createdBy);
        association.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        association.setDescription(description);

        // Required by BaseObject audit trail
        association.setSysUserId(createdBy);

        return save(association);
    }

    @Override
    public NceResultAssociation createAssociation(String resultId, NcEvent ncEvent, AssociationType associationType,
            String createdBy) {
        return createAssociation(resultId, ncEvent, associationType, createdBy, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getNceAssociationsForResult(String resultId) {
        return baseObjectDAO.getNceAssociationsForResult(resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getResultAssociationsForNCE(String nceId) {
        return baseObjectDAO.getResultAssociationsForNCE(nceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isResultLinkedToNCE(String resultId, String nceId) {
        return baseObjectDAO.associationExists(resultId, nceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNceAssociations(String resultId) {
        return baseObjectDAO.countNCEsForResult(resultId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getNceCountForResult(String resultId) {
        return baseObjectDAO.countNCEsForResult(resultId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getResultCountForNCE(String nceId) {
        return baseObjectDAO.countResultsForNCE(nceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getAssociationsByType(AssociationType associationType) {
        return baseObjectDAO.getAssociationsByType(associationType.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getAssociationsCreatedBy(String createdBy) {
        return baseObjectDAO.getAssociationsCreatedBy(createdBy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceResultAssociation> getRecentAssociations(int daysSince) {
        return baseObjectDAO.getRecentAssociations(daysSince);
    }

    @Override
    public int removeAssociationsForResult(String resultId) {
        return baseObjectDAO.deleteAssociationsForResult(resultId);
    }

    @Override
    public int removeAssociationsForNCE(String nceId) {
        return baseObjectDAO.deleteAssociationsForNCE(nceId);
    }

    @Override
    public boolean removeAssociation(String resultId, String nceId) {
        List<NceResultAssociation> associations = getNceAssociationsForResult(resultId);

        for (NceResultAssociation association : associations) {
            if (nceId.equals(association.getNcEvent().getId())) {
                delete(association);
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public String getHighestSeverityForResult(String resultId) {
        List<NceResultAssociation> associations = getNceAssociationsForResult(resultId);

        if (associations.isEmpty()) {
            return null;
        }

        // Severity priority: Critical > High > Medium > Low
        String highestSeverity = null;
        int highestPriority = -1;

        for (NceResultAssociation association : associations) {
            NcEvent ncEvent = association.getNcEvent();
            if (ncEvent != null) {
                String severity = ncEvent.getSeverityId();
                int priority = getSeverityPriority(severity);

                if (priority > highestPriority) {
                    highestPriority = priority;
                    highestSeverity = severity;
                }
            }
        }

        return highestSeverity;
    }

    @Override
    public NceResultAssociation updateAssociationDescription(Integer associationId, String newDescription) {
        NceResultAssociation association = get(associationId);
        if (association != null) {
            association.setDescription(newDescription);
            return save(association);
        }
        return null;
    }

    @Override
    public void validateAssociation(String resultId, NcEvent ncEvent, AssociationType associationType) {
        if (resultId == null || resultId.isBlank()) {
            throw new IllegalArgumentException("Result ID cannot be null or blank");
        }
        if (ncEvent == null) {
            throw new IllegalArgumentException("NCE cannot be null");
        }
        if (associationType == null) {
            throw new IllegalArgumentException("Association type cannot be null");
        }
        if (ncEvent.getId() == null) {
            throw new IllegalArgumentException("NCE must have an ID");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildResultContextInfo(String resultId) {
        Map<String, Object> context = new HashMap<>();
        if (resultId == null) {
            return context;
        }

        Result result = resultService.get(resultId);
        if (result == null) {
            return context;
        }

        // Lab number - traverse lazy relationships safely within transaction
        if (result.getAnalysis() != null && result.getAnalysis().getSampleItem() != null
                && result.getAnalysis().getSampleItem().getSample() != null) {
            Sample sample = result.getAnalysis().getSampleItem().getSample();
            context.put("labNumber", sample.getAccessionNumber());

            // Patient info (anonymized) - use SampleHumanService for patient lookup
            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient != null) {
                context.put("patientInfo", "Patient ID: " + patient.getId());
            }
        }

        // Test name
        if (result.getAnalysis() != null && result.getAnalysis().getTest() != null) {
            context.put("testName", result.getAnalysis().getTest().getName());
        }

        // Result value
        context.put("resultValue", result.getValue());

        context.put("qualityFlags", new ArrayList<>());

        return context;
    }

    @Override
    @Transactional(readOnly = true)
    public NCEBadgeResponseForm buildBadgeResponseForResult(String resultId) {
        NCEBadgeResponseForm response = new NCEBadgeResponseForm();

        // Filter out associations where all linked NCEs are Closed-Verified (spec §6.3)
        List<NceResultAssociation> associations = getNceAssociationsForResult(resultId).stream()
                .filter(assoc -> !isClosedVerified(assoc.getNcEvent())).collect(Collectors.toList());

        response.setHasNCE(!associations.isEmpty());
        response.setNceCount(associations.size());

        if (!associations.isEmpty()) {
            String highestSeverity = getHighestSeverityForResult(resultId);
            response.setHighestSeverity(highestSeverity);
            response.setBadgeColor(NCEBadgeResponseForm.getBadgeColorForSeverity(highestSeverity));

            List<String> nceNumbers = associations.stream().filter(assoc -> assoc.getNcEvent() != null)
                    .map(assoc -> assoc.getNcEvent().getNceNumber()).collect(Collectors.toList());
            response.setNceNumbers(nceNumbers);
        } else {
            response.setHighestSeverity(null);
            response.setBadgeColor(null);
            response.setNceNumbers(new ArrayList<>());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getNceNumbersForResult(String resultId) {
        List<NceResultAssociation> associations = getNceAssociationsForResult(resultId);
        return associations.stream().filter(assoc -> assoc.getNcEvent() != null)
                .map(assoc -> assoc.getNcEvent().getNceNumber()).collect(Collectors.toList());
    }

    /**
     * Check if an NCE is in Closed-Verified status. Handles variations like "Closed
     * - Verified", "Closed – Verified", "Closed-Verified".
     */
    private boolean isClosedVerified(NcEvent ncEvent) {
        if (ncEvent == null || ncEvent.getStatus() == null) {
            return false;
        }
        String normalized = ncEvent.getStatus().toLowerCase().replaceAll("[\\s\\u2013\\u2014-]+", "");
        return normalized.contains("closedverified");
    }

    /**
     * Helper method to get priority value for severity comparison
     *
     * @param severity the severity string
     * @return priority value (higher = more severe)
     */
    private int getSeverityPriority(String severity) {
        if (severity == null) {
            return 0;
        }

        switch (severity.toLowerCase()) {
        case "critical":
            return 3;
        case "major":
            return 2;
        case "minor":
            return 1;
        default:
            return 0;
        }
    }
}