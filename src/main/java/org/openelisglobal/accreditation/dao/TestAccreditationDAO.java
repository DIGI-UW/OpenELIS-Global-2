/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 */
package org.openelisglobal.accreditation.dao;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.dao.BaseDAO;

public interface TestAccreditationDAO extends BaseDAO<TestAccreditation, Long> {
    List<TestAccreditation> findByTestId(Long testId);

    List<TestAccreditation> findByAccreditingBodyId(Long accreditingBodyId);

    TestAccreditation findByTestAndBody(Long testId, Long accreditingBodyId);

    List<TestAccreditation> findExpiringOnOrBefore(LocalDate expirationDate);

    List<TestAccreditation> findAllActive();

    long countActiveByTestId(Long testId);

    boolean existsByTestAndBody(Long testId, Long accreditingBodyId);

    List<TestAccreditation> findByFilters(Long testId, Long accreditingBodyId, Long sectionId, String q);
}
