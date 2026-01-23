package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.dao.DocumentationVerificationDAO;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.OverallStatus;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.VerificationItemStatus;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.DocumentationStatus;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for DocumentationVerification entity operations.
 * Documentation verification is now at shipment level (one verification per
 * shipment).
 */
@Service
public class DocumentationVerificationServiceImpl extends
        AuditableBaseObjectServiceImpl<DocumentationVerification, Integer> implements DocumentationVerificationService {

    @Autowired
    protected DocumentationVerificationDAO baseObjectDAO;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SystemUserService systemUserService;

    DocumentationVerificationServiceImpl() {
        super(DocumentationVerification.class);
    }

    @Override
    protected DocumentationVerificationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentationVerification getByShipmentId(Integer shipmentId) {
        return baseObjectDAO.getByShipmentId(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentationVerification> getByOverallStatus(OverallStatus status) {
        return baseObjectDAO.getByOverallStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentationVerification> getByVerifiedByUserId(Integer verifiedByUserId) {
        return baseObjectDAO.getByVerifiedByUserId(verifiedByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentationVerification> getPendingVerifications(int limit) {
        return baseObjectDAO.getPendingVerifications(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOverallStatus(OverallStatus status) {
        return baseObjectDAO.countByOverallStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByShipmentId(Integer shipmentId) {
        return baseObjectDAO.existsByShipmentId(shipmentId);
    }

    @Override
    @Transactional
    public DocumentationVerification createForShipment(Integer shipmentId, String sysUserId) {
        if (existsByShipmentId(shipmentId)) {
            return getByShipmentId(shipmentId);
        }

        Shipment shipment = shipmentService.get(shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment not found: " + shipmentId);
        }

        DocumentationVerification verification = new DocumentationVerification();
        verification.setShipment(shipment);
        verification.setOverallStatus(OverallStatus.PENDING);
        verification.setSysUserId(sysUserId);

        // Initialize all items as pending
        verification.setStatusSampleIdentifiers(VerificationItemStatus.PENDING);
        verification.setStatusProjectLinkage(VerificationItemStatus.PENDING);
        verification.setStatusEthicsApproval(VerificationItemStatus.PENDING);
        verification.setStatusBiosafetyMatch(VerificationItemStatus.PENDING);
        verification.setStatusPackagingIntegrity(VerificationItemStatus.PENDING);
        verification.setStatusConsentRecord(VerificationItemStatus.PENDING);
        verification.setStatusMtaDocumented(VerificationItemStatus.PENDING);

        return save(verification);
    }

    @Override
    @Transactional
    public DocumentationVerification updateVerificationItem(Integer verificationId, String itemName, boolean verified,
            boolean notApplicable, String naJustification) {
        DocumentationVerification verification = get(verificationId);
        if (verification == null) {
            throw new IllegalArgumentException("Verification not found: " + verificationId);
        }

        VerificationItemStatus status;
        if (notApplicable) {
            status = VerificationItemStatus.N_A;
        } else if (verified) {
            status = VerificationItemStatus.VERIFIED;
        } else {
            status = VerificationItemStatus.PENDING;
        }

        switch (itemName) {
        case "sampleIdentifiers":
            verification.setCheckSampleIdentifiers(verified);
            verification.setStatusSampleIdentifiers(status);
            break;
        case "projectLinkage":
            verification.setCheckProjectLinkage(verified);
            verification.setStatusProjectLinkage(status);
            break;
        case "ethicsApproval":
            verification.setCheckEthicsApproval(verified);
            verification.setStatusEthicsApproval(status);
            break;
        case "biosafetyMatch":
            verification.setCheckBiosafetyMatch(verified);
            verification.setStatusBiosafetyMatch(status);
            break;
        case "packagingIntegrity":
            verification.setCheckPackagingIntegrity(verified);
            verification.setStatusPackagingIntegrity(status);
            break;
        case "consentRecord":
            verification.setCheckConsentRecord(verified);
            verification.setStatusConsentRecord(status);
            if (notApplicable) {
                verification.setNaJustificationConsent(naJustification);
            }
            break;
        case "mtaDocumented":
            verification.setCheckMtaDocumented(verified);
            verification.setStatusMtaDocumented(status);
            if (notApplicable) {
                verification.setNaJustificationMta(naJustification);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown verification item: " + itemName);
        }

        verification.updateOverallStatus();
        return update(verification);
    }

    @Override
    @Transactional
    public DocumentationVerification completeVerification(Integer verificationId, Integer verifierUserId) {
        DocumentationVerification verification = get(verificationId);
        if (verification == null) {
            throw new IllegalArgumentException("Verification not found: " + verificationId);
        }

        if (!verification.isComplete()) {
            throw new IllegalStateException("Cannot complete verification - not all items are verified");
        }

        // Set verifier and timestamp
        SystemUser verifier = systemUserService.get(verifierUserId.toString());
        verification.setVerifiedByUser(verifier);
        verification.setVerifiedTimestamp(new Timestamp(System.currentTimeMillis()));
        verification.setOverallStatus(OverallStatus.VERIFIED);

        // Update shipment documentation status
        shipmentService.updateDocumentationStatus(verification.getShipment().getId(), DocumentationStatus.VERIFIED);

        return update(verification);
    }

    @Override
    @Transactional
    public DocumentationVerification quarantineShipment(Integer verificationId, Integer verifierUserId,
            String quarantineReason) {
        DocumentationVerification verification = get(verificationId);
        if (verification == null) {
            throw new IllegalArgumentException("Verification not found: " + verificationId);
        }

        // Set verifier and timestamp
        SystemUser verifier = systemUserService.get(verifierUserId.toString());
        verification.setVerifiedByUser(verifier);
        verification.setVerifiedTimestamp(new Timestamp(System.currentTimeMillis()));
        verification.setOverallStatus(OverallStatus.QUARANTINE);
        verification.setVerificationNotes(quarantineReason);

        // Update shipment documentation status
        shipmentService.updateDocumentationStatus(verification.getShipment().getId(), DocumentationStatus.QUARANTINE);

        return update(verification);
    }
}
