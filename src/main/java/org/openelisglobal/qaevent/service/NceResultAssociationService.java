package org.openelisglobal.qaevent.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.form.NCEBadgeResponseForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation.AssociationType;

/**
 * Service interface for managing associations between lab results and NCEs.
 * Provides business logic for linking results to quality issues and tracking
 * relationships.
 */
public interface NceResultAssociationService extends BaseObjectService<NceResultAssociation, Integer> {

    /**
     * Create a new association between a result and an NCE
     *
     * @param resultId        the lab result ID
     * @param ncEvent         the NCE
     * @param associationType the type of association
     * @param createdBy       the sys user ID creating the association
     * @param description     optional description of the association
     * @return the created association
     */
    NceResultAssociation createAssociation(String resultId, NcEvent ncEvent, AssociationType associationType,
            String createdBy, String description);

    /**
     * Create a new association between a result and an NCE (without description)
     *
     * @param resultId        the lab result ID
     * @param ncEvent         the NCE
     * @param associationType the type of association
     * @param createdBy       the sys user ID creating the association
     * @return the created association
     */
    NceResultAssociation createAssociation(String resultId, NcEvent ncEvent, AssociationType associationType,
            String createdBy);

    /**
     * Get all NCE associations for a specific result
     * 
     * @param resultId the ID of the result
     * @return list of associations for the result
     */
    List<NceResultAssociation> getNceAssociationsForResult(String resultId);

    /**
     * Get all result associations for a specific NCE
     * 
     * @param nceId the ID of the NCE
     * @return list of associations for the NCE
     */
    List<NceResultAssociation> getResultAssociationsForNCE(String nceId);

    /**
     * Check if a result is already associated with an NCE
     * 
     * @param resultId the ID of the result
     * @param nceId    the ID of the NCE
     * @return true if association exists, false otherwise
     */
    boolean isResultLinkedToNCE(String resultId, String nceId);

    /**
     * Check if a result has any NCE associations
     * 
     * @param resultId the ID of the result
     * @return true if result has NCE associations, false otherwise
     */
    boolean hasNceAssociations(String resultId);

    /**
     * Get the count of NCEs associated with a result
     * 
     * @param resultId the ID of the result
     * @return count of associated NCEs
     */
    int getNceCountForResult(String resultId);

    /**
     * Get the count of results associated with an NCE
     * 
     * @param nceId the ID of the NCE
     * @return count of associated results
     */
    int getResultCountForNCE(String nceId);

    /**
     * Get associations by type
     * 
     * @param associationType the type of association
     * @return list of associations of the specified type
     */
    List<NceResultAssociation> getAssociationsByType(AssociationType associationType);

    /**
     * Get associations created by a specific user
     * 
     * @param createdBy the user who created the associations
     * @return list of associations created by the user
     */
    List<NceResultAssociation> getAssociationsCreatedBy(String createdBy);

    /**
     * Get recent associations within a specified time period
     * 
     * @param daysSince number of days to look back
     * @return list of recent associations
     */
    List<NceResultAssociation> getRecentAssociations(int daysSince);

    /**
     * Remove all associations for a specific result
     * 
     * @param resultId the ID of the result
     * @return number of associations removed
     */
    int removeAssociationsForResult(String resultId);

    /**
     * Remove all associations for a specific NCE
     * 
     * @param nceId the ID of the NCE
     * @return number of associations removed
     */
    int removeAssociationsForNCE(String nceId);

    /**
     * Remove a specific association between a result and NCE
     * 
     * @param resultId the ID of the result
     * @param nceId    the ID of the NCE
     * @return true if association was removed, false if it didn't exist
     */
    boolean removeAssociation(String resultId, String nceId);

    /**
     * Get the highest severity level among NCEs associated with a result
     * 
     * @param resultId the ID of the result
     * @return the highest severity ID, or null if no associations exist
     */
    String getHighestSeverityForResult(String resultId);

    /**
     * Update the description of an existing association
     * 
     * @param associationId  the ID of the association
     * @param newDescription the new description
     * @return the updated association, or null if not found
     */
    NceResultAssociation updateAssociationDescription(Integer associationId, String newDescription);

    /**
     * Validate that an association can be created (business rules)
     *
     * @param resultId        the lab result ID
     * @param ncEvent         the NCE
     * @param associationType the type of association
     * @throws IllegalArgumentException if association violates business rules
     */
    void validateAssociation(String resultId, NcEvent ncEvent, AssociationType associationType);

    /**
     * Build context information for a result (lab number, test name, patient info).
     * Must be called within a transaction to safely traverse lazy entity
     * relationships.
     * 
     * @param resultId the ID of the result
     * @return map containing context information, or empty map if result not found
     */
    Map<String, Object> buildResultContextInfo(String resultId);

    /**
     * Build a complete NCE badge response for a result. Compiles all badge data
     * within a transaction to avoid lazy loading issues.
     * 
     * @param resultId the ID of the result
     * @return the fully populated badge response form
     */
    NCEBadgeResponseForm buildBadgeResponseForResult(String resultId);

    /**
     * Get NCE numbers associated with a result.
     * 
     * @param resultId the ID of the result
     * @return list of NCE numbers
     */
    List<String> getNceNumbersForResult(String resultId);
}