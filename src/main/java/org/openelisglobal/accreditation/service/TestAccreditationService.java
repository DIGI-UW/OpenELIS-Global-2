package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.service.BaseObjectService;

public interface TestAccreditationService extends BaseObjectService<TestAccreditation, Long> {

    List<TestAccreditation> getByTestId(Long testId);

    List<TestAccreditation> getByAccreditingBodyId(Long accreditingBodyId);

    TestAccreditation getByTestAndBody(Long testId, Long accreditingBodyId);

    List<TestAccreditation> getExpiringOnOrBefore(LocalDate date);

    List<TestAccreditation> getAllActive();

    boolean existsByTestAndBody(Long testId, Long accreditingBodyId);

    void bulkExtend(List<Long> ids, LocalDate newExpiresOn, String sysUserId);

    List<TestAccreditation> getByFilters(Long testId, Long accreditingBodyId, Long sectionId, String q);
}
