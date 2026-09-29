package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroIsolateService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroIsolate createIsolate(String caseId, String isolateLabel, String gramStain, String colonyMorphology,
            MicroIsolateSignificance significance, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroIsolate updateIdentification(String isolateId, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, MicroIsolateIdentificationStatus identificationStatus,
            String identificationMethod, BigDecimal identificationConfidence, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroIsolate updateIdentification(String isolateId, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, MicroIsolateIdentificationStatus identificationStatus,
            String identificationMethod, BigDecimal identificationConfidence, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroIsolate> getIsolatesForCase(String caseId);
}
