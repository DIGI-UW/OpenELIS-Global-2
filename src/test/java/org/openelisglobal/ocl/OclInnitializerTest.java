package org.openelisglobal.ocl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class OclInnitializerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    private OclImportInitializer oclImportInitializer;

    @Autowired
    TestService testService;

    @Autowired
    private RoleModuleService roleModuleService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DataSource dataSource;

    private static String oclDirPath;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/ocl-import.xml");
        executeDataSetWithStateManagement("testdata/type-of-testresult.xml");
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
        oclDirPath = this.getClass().getClassLoader().getResource("ocl").getFile();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void ImportOclPackage_withValidZip_shouldImportTestsSuccessfully() throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("ocl_imported", ".flag");

        org.openelisglobal.test.valueholder.Test test = testService.getTestByLocalizedName("TEST C en", Locale.ENGLISH);
        assertNull("Test should not exist before OCL import", test);

        oclImportInitializer.performOclImport(oclDirPath, tempFile.getAbsolutePath());

        test = testService.getTestByLocalizedName("TEST C en", Locale.ENGLISH);
        assertNotNull("Test should exist after OCL import", test);

        assertNotNull("Imported test should have a result type", testService.getResultType(test));
    }

    @Test
    public void ImportOclPackage_missingRolesAutoCreates_shouldCreateDefaultRoles() throws IOException {
        removeRole(Constants.ROLE_RESULTS);
        removeRole(Constants.ROLE_VALIDATION);

        assertNull("Results role should be missing before import", roleService.getRoleByName(Constants.ROLE_RESULTS));
        assertNull("Validation role should be missing before import",
                roleService.getRoleByName(Constants.ROLE_VALIDATION));

        java.io.File tempFile = java.io.File.createTempFile("ocl_imported_missing_roles", ".flag");
        oclImportInitializer.performOclImport(oclDirPath, tempFile.getAbsolutePath());

        Role recreatedResults = roleService.getRoleByName(Constants.ROLE_RESULTS);
        Role recreatedValidation = roleService.getRoleByName(Constants.ROLE_VALIDATION);

        assertNotNull("Results role should be auto-created during import", recreatedResults);
        assertNotNull("Validation role should be auto-created during import", recreatedValidation);
    }

    @Test
    public void InsertRoleModule_withNullRole_shouldThrowLimsRuntimeException() {
        RoleModule roleModule = new RoleModule();
        SystemModule systemModule = new SystemModule();
        roleModule.setSystemModule(systemModule);

        try {
            roleModuleService.insert(roleModule);
            fail("Expected LIMSRuntimeException when inserting RoleModule with null role");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().toLowerCase().contains("role or system module is null"));
        }
    }

    private void removeRole(String roleName) {
        List<Integer> roleIds = jdbcTemplate.queryForList("SELECT id FROM system_role WHERE name = ?", Integer.class,
                roleName);
        for (Integer roleId : roleIds) {
            jdbcTemplate.update("DELETE FROM system_role_module WHERE system_role_id = ?", roleId);
            jdbcTemplate.update("DELETE FROM system_user_role WHERE role_id = ?", roleId);
            jdbcTemplate.update("DELETE FROM system_role WHERE id = ?", roleId);
        }
    }
}
