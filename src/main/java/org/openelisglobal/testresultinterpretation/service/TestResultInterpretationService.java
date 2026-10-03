package org.openelisglobal.testresultinterpretation.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The interpretation text shown beside a result value.
 *
 * <p>
 * Configuring interpretations is administrative and keeps PRIV_TEST_CONFIGURE.
 * Reading them is not: the result-entry screen calls
 * {@code ResultEntryRestController#getTestInterpretations} - itself gated
 * hasRole('RESULTS') - to show the technician what each value means. While this
 * interface required PRIV_TEST_CONFIGURE for every method, that endpoint
 * answered 403 for the Results role on its own screen, so
 * {@link #getActiveByComponentId(String)} admits PRIV_CATALOGUE_VIEW too,
 * matching {@code TestResultComponentService#getActiveComponentsByTestId} which
 * the same handler calls one line earlier.
 */
@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface TestResultInterpretationService extends BaseObjectService<TestResultInterpretation, String> {

    List<TestResultInterpretation> getByComponentId(String componentId);

    @PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_CATALOGUE_VIEW')")
    List<TestResultInterpretation> getActiveByComponentId(String componentId);

    /**
     * Reconciles a component's active interpretations to the desired set: a desired
     * interpretation whose id matches an existing active row is updated; one
     * without a matching id is inserted; an existing active row absent from the
     * desired set is soft-deleted (is_active='N'). Returns the resulting active
     * list.
     */
    List<TestResultInterpretation> saveInterpretationsForComponent(String componentId,
            List<TestResultInterpretation> desired, String sysUserId);
}
