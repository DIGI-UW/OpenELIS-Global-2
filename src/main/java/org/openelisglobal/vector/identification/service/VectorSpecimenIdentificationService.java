package org.openelisglobal.vector.identification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.BulkIdentifyResult;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationRequest;
import org.openelisglobal.vector.identification.dto.IdentificationDTOs.IdentificationResult;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;

public interface VectorSpecimenIdentificationService extends BaseObjectService<VectorSpecimenIdentification, Long> {

    Optional<VectorSpecimenIdentification> getBySampleItemId(Long sampleItemId);

    List<VectorSpecimenIdentification> getBySampleId(Long sampleId);

    long countBySampleId(Long sampleId);

    long countBySampleItemIds(List<Long> sampleItemIds);

    IdentificationResult identify(IdentificationRequest request, String sysUserId);

    String recomputeSampleIdentificationStatus(Long sampleId);

    /**
     * Molecular detail is not bulk-copied — the request DTO has no molecular
     * fields, and existing per-specimen records are left untouched. Each intake
     * pool's identificationStatus is recomputed once per touched lot at the end.
     */
    BulkIdentifyResult bulkIdentify(BulkIdentifyRequest request, String sysUserId);
}
