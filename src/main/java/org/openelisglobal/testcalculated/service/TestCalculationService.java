package org.openelisglobal.testcalculated.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The calculated-test definitions: which results are derived from which others.
 *
 * <p>
 * Configuring a calculation is administrative and belongs to
 * PRIV_TEST_CONFIGURE. Reading the definitions is not: every result save walks
 * them, because saving one result may derive another
 * ({@code TestCalculatedUtil#addNewTestsToDBForCalculatedTests}, reached from
 * {@code LogbookPersistServiceImpl}). While this interface required
 * PRIV_TEST_CONFIGURE alone, that read denied mid-save and the Results role
 * could not save a result at all: POST /rest/LogbookResults answered 403 after
 * the audit trail had already begun writing, and no result row was created.
 *
 * <p>
 * So the three authorities that save a result value may read, matching the
 * sibling {@link ResultCalculationService} that the same loop calls two lines
 * later. The gate stays a type-level {@code @PreAuthorize} rather than
 * {@code @CrudPrivileges} because the latter takes a single authority and this
 * needs any of several; {@code CrudGate} evaluates the type-level expression
 * for the inherited CRUD.
 */
@PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_RESULT_ENTER','PRIV_RESULT_VALIDATE',"
        + "'PRIV_ANALYZER_IMPORT')")
public interface TestCalculationService extends BaseObjectService<Calculation, Integer> {
}
