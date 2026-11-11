package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Uses the same permission lookup as password and single sign-on
 * authentication.
 */
public class FhirReadPermissionTest extends BaseWebContextSensitiveTest {
    @Autowired
    private RoleModuleService permissions;

    @Before
    public void grants() throws Exception {
        executeDataSetWithStateManagement("testdata/fhir-read-permissions.xml");
    }

    @Test
    public void loginPermissionsContainOnlyExplicitSelectGrants() {
        assertEquals(Set.of("fhir.read.results"), permissions.getPermittedModuleNames(List.of("FHIR Results Reader")));
        assertEquals(Set.of("fhir.read.patients"), permissions.getPermittedModuleNames(List.of("FHIR Patient Reader")));
        assertEquals(Set.of("fhir.read.results", "fhir.read.patients"),
                permissions.getPermittedModuleNames(List.of("FHIR Results Reader", "FHIR Patient Reader")));
        assertTrue(permissions.getPermittedModuleNames(List.of("FHIR No Grants")).isEmpty());
        assertTrue(permissions.getPermittedModuleNames(List.of("Unknown Role")).isEmpty());
    }
}
