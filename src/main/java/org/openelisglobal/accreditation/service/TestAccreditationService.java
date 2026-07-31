package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.service.BaseObjectService;

public interface TestAccreditationService extends BaseObjectService<TestAccreditation, Long> {

    List<TestAccreditation> getByTestId(String testId);

    List<TestAccreditation> getByAccreditingBodyId(Long accreditingBodyId);

    TestAccreditation getByTestAndBody(String testId, Long accreditingBodyId);

    List<TestAccreditation> getExpiringOnOrBefore(LocalDate date);

    List<TestAccreditation> getAllActive();

    boolean existsByTestAndBody(String testId, Long accreditingBodyId);

    void bulkExtend(List<Long> ids, LocalDate newExpiresOn, String sysUserId);

    List<TestAccreditation> getByFilters(String testId, Long accreditingBodyId, Long sectionId, String q);
}
