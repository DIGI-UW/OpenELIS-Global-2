package org.openelisglobal.biorepository.service;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType.SampleCategory;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Service interface for BiorepositoryApprovedSampleType entity operations.
 * Provides business logic for managing approved sample types in the
 * biorepository.
 */
public interface BiorepositoryApprovedSampleTypeService
        extends BaseObjectService<BiorepositoryApprovedSampleType, Integer> {

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
     * Check if a TypeOfSample entity is approved for biorepository use.
     *
     * @param typeOfSample the TypeOfSample entity to check
     * @return true if the sample type is approved and active
     */
    boolean isApprovedSampleType(TypeOfSample typeOfSample);

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

    /**
     * Get a list of approved TypeOfSample entities. Convenience method for UI
     * dropdowns.
     *
     * @return list of TypeOfSample entities that are approved
     */
    List<TypeOfSample> getApprovedTypeOfSamples();
}
