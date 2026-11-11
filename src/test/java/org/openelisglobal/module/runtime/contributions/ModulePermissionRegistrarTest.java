package org.openelisglobal.module.runtime.contributions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.manifest.ManifestReader;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.testkit.ModuleRecords;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The registrar's grants against the core services as they really behave: the
 * role-module lookup answers an absent grant with an empty object, never null.
 */
public class ModulePermissionRegistrarTest {

    private static final String MANIFEST = "{\"schemaVersion\":1,\"id\":\"grants\",\"kind\":\"module\","
            + "\"name\":\"Grants\",\"version\":\"1.0.0\",\"requires\":{\"openelis\":\">=3.4.0\",\"spi\":\"^1.0\"},"
            + "\"permissions\":[{\"key\":\"view\",\"description\":\"see\",\"defaultRoles\":[\"Results\",\"Nobody\"]}],"
            + "\"menu\":[{\"elementId\":\"menu_mod_grants\",\"parent\":\"menu_sample\",\"displayKey\":\"mod.grants.menu\","
            + "\"actionURL\":\"/m/grants/home\",\"permission\":\"view\"}]}";

    private final SystemModuleService systemModuleService = mock(SystemModuleService.class);
    private final SystemModuleUrlService systemModuleUrlService = mock(SystemModuleUrlService.class);
    private final RoleModuleService roleModuleService = mock(RoleModuleService.class);
    private final RoleService roleService = mock(RoleService.class);
    private final DaemonContextExecutor daemon = mock(DaemonContextExecutor.class);
    private final Role administrators = role("1", Constants.ROLE_GLOBAL_ADMIN);
    private final Role results = role("5", "Results");
    private ModulePermissionRegistrar registrar;

    @Before
    public void wire() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(daemon).executeAsDaemon(any(Runnable.class));
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(systemModuleService.insert(any(SystemModule.class))).thenAnswer(invocation -> {
            SystemModule module = invocation.getArgument(0);
            module.setId("900");
            return "900";
        });
        when(roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN)).thenReturn(administrators);
        when(roleService.getRoleByName("Results")).thenReturn(results);
        when(roleModuleService.getRoleModuleByRoleAndModuleId(anyString(), anyString())).thenReturn(new RoleModule());
        registrar = new ModulePermissionRegistrar(systemModuleService, systemModuleUrlService, roleModuleService,
                roleService, daemon, new TransactionTemplate(manager));
    }

    @Test
    public void aFirstInstallGrantsTheAdministratorsAndTheDefaultRoles() {
        ModuleRecord module = module(null);

        registrar.register(module);

        List<RoleModule> grants = grants();
        assertEquals(2, grants.size());
        assertEquals(administrators, grants.get(0).getRole());
        assertEquals(results, grants.get(1).getRole());
        for (RoleModule grant : grants) {
            assertEquals("mod.grants.view", grant.getSystemModule().getSystemModuleName());
            assertEquals("Y", grant.getHasSelect());
        }
        assertEquals(1, module.warnings().size());
        assertTrue(module.warnings().get(0), module.warnings().get(0).contains("'Nobody'"));
    }

    @Test
    public void aLaterStartGrantsOnlyTheAdministrators() {
        ModuleRecord module = module("1.0.0");

        registrar.register(module);

        List<RoleModule> grants = grants();
        assertEquals(1, grants.size());
        assertEquals(administrators, grants.get(0).getRole());
        verify(roleService, never()).getRoleByName("Results");
    }

    @Test
    public void aPersistedGrantIsNotRepeated() {
        RoleModule persisted = new RoleModule();
        persisted.setId("77");
        when(roleModuleService.getRoleModuleByRoleAndModuleId("1", "900")).thenReturn(persisted);
        ModuleRecord module = module("1.0.0");

        registrar.register(module);

        verify(roleModuleService, never()).insert(any(RoleModule.class));
    }

    private List<RoleModule> grants() {
        ArgumentCaptor<RoleModule> captor = ArgumentCaptor.forClass(RoleModule.class);
        verify(roleModuleService, org.mockito.Mockito.atLeast(0)).insert(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    private static ModuleRecord module(String recordedVersion) {
        ModuleRecord module = ModuleRecords.record(Paths.get("/modules/grants-1.0.0.jar"),
                new ManifestReader().read(MANIFEST));
        module.transition(ModuleState.MIGRATED);
        module.setRecordedVersion(recordedVersion);
        return module;
    }

    private static Role role(String id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
