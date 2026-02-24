package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation;

/**
 * DAO interface for managing NCE-Result associations. Provides methods to
 * create, query, and manage links between lab results and NCEs.
 */
public interface NceResultAssociationDAO extends BaseDAO<NceResultAssociation, Integer> {

    /**
     * Find all NCE associations for a specific result
     * 
     * @param resultId the ID of the result
     * @return list of associations for the result
     */
    List<NceResultAssociation> getNceAssociationsForResult(String resultId);

    /**
     * Find all result associations for a specific NCE
     * 
     * @param nceId the ID of the NCE
     * @return list of associations for the NCE
     */
    List<NceResultAssociation> getResultAssociationsForNCE(String nceId);

    /**
     * Find associations by association type
     * 
     * @param associationType the type of association to search for
     * @return list of associations of the specified type
     */
    List<NceResultAssociation> getAssociationsByType(String associationType);

    /**
     * Check if a result is already linked to an NCE
     * 
     * @param resultId the ID of the result
     * @param nceId    the ID of the NCE
     * @return true if association exists, false otherwise
     */
    boolean associationExists(String resultId, String nceId);

    /**
     * Find associations created by a specific user
     * 
     * @param createdBy the user who created the associations
     * @return list of associations created by the user
     */
    List<NceResultAssociation> getAssociationsCreatedBy(String createdBy);

    /**
     * Count the number of NCEs associated with a result
     * 
     * @param resultId the ID of the result
     * @return count of associated NCEs
     */
    int countNCEsForResult(String resultId);

    /**
     * Count the number of results associated with an NCE
     * 
     * @param nceId the ID of the NCE
     * @return count of associated results
     */
    int countResultsForNCE(String nceId);

    /**
     * Get the most recent associations within a time period
     * 
     * @param daysSince number of days to look back
     * @return list of recent associations
     */
    List<NceResultAssociation> getRecentAssociations(int daysSince);

    /**
     * Delete all associations for a specific result
     * 
     * @param resultId the ID of the result
     * @return number of associations deleted
     */
    int deleteAssociationsForResult(String resultId);

    /**
     * Delete all associations for a specific NCE
     * 
     * @param nceId the ID of the NCE
     * @return number of associations deleted
     */
    int deleteAssociationsForNCE(String nceId);
}