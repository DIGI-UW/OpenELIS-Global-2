package org.openelisglobal.testreagentlink.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Which reagents a test consumes.
 *
 * <p>
 * Linking reagents to a test is administrative and keeps PRIV_TEST_CONFIGURE.
 * Reading the links is not: the result-entry screen calls
 * {@code ResultEntryRestController#getTestReagentLinks} - itself gated
 * hasRole('RESULTS') - to show the technician which reagents the test uses.
 * While this interface required PRIV_TEST_CONFIGURE for every method, that
 * endpoint answered 403 for the Results role on its own screen.
 */
@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface TestReagentLinkService extends BaseObjectService<TestReagentLink, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_CATALOGUE_VIEW')")
    List<TestReagentLink> getByTestId(String testId);

    TestReagentLink getByTestIdAndReagentId(String testId, Long reagentId);
}
