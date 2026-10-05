package org.openelisglobal.vector.identification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.SpecimenDetailDTO;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VectorSpecimenIdentificationService extends BaseObjectService<VectorSpecimenIdentification, Long> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Optional<VectorSpecimenIdentification> getBySampleItemId(Long sampleItemId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<VectorSpecimenIdentification> getBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    long countBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    long countBySampleItemIds(List<Long> sampleItemIds);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    IdentificationResult identify(IdentificationRequest request, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    String recomputeSampleIdentificationStatus(Long sampleId);

    /**
     * Molecular detail is not bulk-copied — the request DTO has no molecular
     * fields, and existing per-specimen records are left untouched. Each intake
     * pool's identificationStatus is recomputed once per touched lot at the end.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    BulkIdentifyResult bulkIdentify(BulkIdentifyRequest request, String sysUserId);

    /**
     * Returns the list of specimens (SampleItems) belonging to the given lot
     * (VectorPool id), enriched with pool membership, identification records, and
     * sample-type details. Returns an empty list when the lot is not found.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<SpecimenDetailDTO> getSpecimensForLot(Long lotId);
}
