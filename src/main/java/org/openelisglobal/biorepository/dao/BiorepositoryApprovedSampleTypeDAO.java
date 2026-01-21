package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType.SampleCategory;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for BiorepositoryApprovedSampleType entity operations. Provides
 * access to the biorepository-approved sample type configuration.
 */
public interface BiorepositoryApprovedSampleTypeDAO extends BaseDAO<BiorepositoryApprovedSampleType, Integer> {

    /**
     * Get all active approved sample types.
     *
     * @return list of active approved sample types ordered by display_order
     */
    List<BiorepositoryApprovedSampleType> getAllActive();

    /**
     * Get all approved sample types for a specific category.
     *
     * @param category the sample category
     * @return list of approved sample types in that category
     */
    List<BiorepositoryApprovedSampleType> getByCategory(SampleCategory category);

    /**
     * Check if a type_of_sample is approved for biorepository use.
     *
     * @param typeOfSampleId the type_of_sample ID to check
     * @return true if the sample type is approved and active
     */
    boolean isApprovedSampleType(String typeOfSampleId);

    /**
     * Find approved sample type by type_of_sample ID.
     *
     * @param typeOfSampleId the type_of_sample ID
     * @return the approved sample type record or null if not found
     */
    BiorepositoryApprovedSampleType getByTypeOfSampleId(String typeOfSampleId);

    /**
     * Get count of active approved sample types.
     *
     * @return count of active approved types
     */
    long countActive();
}
