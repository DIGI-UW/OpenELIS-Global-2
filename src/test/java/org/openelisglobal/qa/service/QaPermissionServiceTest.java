package org.openelisglobal.qa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the qa.* permission derivation (QA v1 permission
 * model). The fixture mirrors the liquibase/qa/004 registry + grant matrix
 * through the managed dataset path — asserting against the live liquibase seeds
 * is order-dependent, because any earlier suite test whose dataset names these
 * tables (e.g. role-module.xml) truncates them. The migration itself runs at
 * context start; its content was SQL-mirror verified in dev.
 */
public class QaPermissionServiceTest extends BaseWebContextSensitiveTest {

    private static final List<String> ALL_VIEW_KEYS = Arrays.asList("qa.view.overview", "qa.view.qc", "qa.view.eqa",
            "qa.view.qi", "qa.view.qms");

    @Autowired
    private QaPermissionService qaPermissionService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qa-permission.xml");
    }

    @Test
    public void qaOfficer_bundlesOverviewAndAllFourPillarPermissions() {
        Set<String> permissions = qaPermissionService.getQaPermissionsForRoleNames(List.of("QA Officer"));

        for (String key : ALL_VIEW_KEYS) {
            assertTrue("QA Officer should hold " + key, permissions.contains(key));
        }
        // qa.manage.qi is registered but not granted in v1
        assertFalse(permissions.contains("qa.manage.qi"));
        assertEquals(ALL_VIEW_KEYS.size(), permissions.size());
    }

    @Test
    public void globalAdministrator_holdsAllViewKeys() {
        Set<String> permissions = qaPermissionService.getQaPermissionsForRoleNames(List.of("Global Administrator"));

        for (String key : ALL_VIEW_KEYS) {
            assertTrue("Global Administrator should hold " + key, permissions.contains(key));
        }
    }

    @Test
    public void compatGrants_matchEachRolesPreRegistryAudience() {
        // Reception: overview + qi + eqa + qms, never qc
        Set<String> reception = qaPermissionService.getQaPermissionsForRoleNames(List.of("Reception"));
        assertEquals(Set.of("qa.view.overview", "qa.view.qi", "qa.view.eqa", "qa.view.qms"), reception);

        // Results: overview + qi + eqa
        Set<String> results = qaPermissionService.getQaPermissionsForRoleNames(List.of("Results"));
        assertEquals(Set.of("qa.view.overview", "qa.view.qi", "qa.view.eqa"), results);

        // Validation: overview + qi + qms
        Set<String> validation = qaPermissionService.getQaPermissionsForRoleNames(List.of("Validation"));
        assertEquals(Set.of("qa.view.overview", "qa.view.qi", "qa.view.qms"), validation);
    }

    @Test
    public void permissionsUnionAcrossRoles() {
        Set<String> permissions = qaPermissionService
                .getQaPermissionsForRoleNames(Arrays.asList("Results", "Validation"));

        assertEquals(Set.of("qa.view.overview", "qa.view.qi", "qa.view.eqa", "qa.view.qms"), permissions);
    }

    @Test
    public void unknownBlankAndNullRoleNamesYieldNothing() {
        assertTrue(
                qaPermissionService.getQaPermissionsForRoleNames(Arrays.asList("No Such Role", " ", null)).isEmpty());
        assertTrue(qaPermissionService.getQaPermissionsForRoleNames(Collections.emptyList()).isEmpty());
        assertTrue(qaPermissionService.getQaPermissionsForRoleNames(null).isEmpty());
    }

    @Test
    public void roleWithoutQaGrantsYieldsNothing() {
        // Audit Trail is a real seeded role with no qa.* grants in the matrix
        assertTrue(qaPermissionService.getQaPermissionsForRoleNames(List.of("Audit Trail")).isEmpty());
    }
}
