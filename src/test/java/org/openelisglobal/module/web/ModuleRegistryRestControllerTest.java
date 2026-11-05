package org.openelisglobal.module.web;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.manifest.FrontendSection;
import org.openelisglobal.module.api.manifest.MenuEntry;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.api.manifest.PermissionDeclaration;
import org.openelisglobal.module.api.manifest.Requires;
import org.openelisglobal.module.manifest.ManifestReader;
import org.openelisglobal.module.runtime.ContractVersions;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleSystemProperties;
import org.openelisglobal.module.runtime.spring.ModuleContextFactory;
import org.openelisglobal.module.testkit.ModuleJarBuilder;
import org.openelisglobal.module.testkit.ModuleRecords;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ModuleRegistryRestControllerTest {

    private MockMvc mockMvc;

    private static ModuleManifest manifest(String id) {
        ModuleManifest manifest = new ModuleManifest();
        manifest.setId(id);
        manifest.setKind("module");
        manifest.setName("Module " + id);
        manifest.setVersion("1.2.3");
        Requires requires = new Requires();
        requires.setOpenelis(">=3.4.0");
        requires.setFrontendContract("^1.0");
        manifest.setRequires(requires);
        PermissionDeclaration view = new PermissionDeclaration();
        view.setKey("view");
        manifest.setPermissions(List.of(view));
        MenuEntry entry = new MenuEntry();
        entry.setElementId("menu_mod_" + id.replace('-', '_'));
        entry.setParent("menu_sample");
        entry.setDisplayKey("mod." + id + ".menu");
        entry.setActionURL("/m/" + id + "/home");
        entry.setPermission("view");
        manifest.setMenu(List.of(entry));
        FrontendSection frontend = new FrontendSection();
        FrontendSection.RouteDeclaration route = new FrontendSection.RouteDeclaration();
        route.setPath("/m/" + id + "/home");
        route.setExpose("./Home");
        route.setPermission("view");
        frontend.setRoutes(List.of(route));
        manifest.setFrontend(frontend);
        return manifest;
    }

    @Before
    public void controller() {
        ModuleRecord started = ModuleRecords.record(Paths.get("/modules/alpha-1.2.3.jar"), manifest("alpha"));
        started.transition(ModuleState.STARTED);
        ModuleRecord skipped = ModuleRecords.record(Paths.get("/modules/beta-1.2.3.jar"), manifest("beta"));
        skipped.transition(ModuleState.INCOMPATIBLE, "requires OpenELIS >=9 but this is 3.4.0");
        ModuleRegistry registry = new ModuleRegistry(new ModuleSystemProperties(new MockEnvironment()),
                new ContractVersions("3.4.0-SNAPSHOT", "1.0.0", "1.0.0"), List.of(started, skipped));
        ModuleRegistryRestController controller = new ModuleRegistryRestController(registry,
                mock(ModuleContextFactory.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void listsEveryModuleWithItsStateAndTheStartedOnesWithTheirFrontend() throws Exception {
        mockMvc.perform(get("/rest/module-registry")).andExpect(status().isOk())
                .andExpect(jsonPath("$.openelisVersion").value("3.4.0-SNAPSHOT"))
                .andExpect(jsonPath("$.frontendContractVersion").value(ModuleFrontendContract.VERSION))
                .andExpect(jsonPath("$.modules.length()").value(2))
                .andExpect(jsonPath("$.modules[0].id").value("alpha"))
                .andExpect(jsonPath("$.modules[0].state").value("STARTED"))
                .andExpect(jsonPath("$.modules[0].remoteName").value("alpha"))
                .andExpect(jsonPath("$.modules[0].assetBase").value("/modules/alpha/"))
                .andExpect(jsonPath("$.modules[0].routePrefix").value("/m/alpha/"))
                .andExpect(jsonPath("$.modules[0].frontend.routes[0].path").value("/m/alpha/home"))
                .andExpect(jsonPath("$.modules[0].menu[0].elementId").value("menu_mod_alpha"))
                .andExpect(jsonPath("$.modules[0].permissions[0].key").value("view"))
                .andExpect(jsonPath("$.modules[1].id").value("beta"))
                .andExpect(jsonPath("$.modules[1].state").value("INCOMPATIBLE"))
                .andExpect(jsonPath("$.modules[1].reason").value("requires OpenELIS >=9 but this is 3.4.0"))
                .andExpect(jsonPath("$.modules[1].frontend").doesNotExist())
                .andExpect(jsonPath("$.extensionPoints[0].name").value("report"))
                .andExpect(jsonPath("$.extensionPoints[0].providers.length()").value(0));
    }

    @Test
    public void detailNamesTheJarAndDigestAndUnknownIdIs404() throws Exception {
        mockMvc.perform(get("/rest/module-registry/alpha")).andExpect(status().isOk())
                .andExpect(jsonPath("$.jar").value("alpha-1.2.3.jar"))
                .andExpect(jsonPath("$.sha256").value("0".repeat(64)))
                .andExpect(jsonPath("$.manifest.id").value("alpha"));
        mockMvc.perform(get("/rest/module-registry/nope")).andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/module-registry/alpha/config")).andExpect(status().isNotFound());
    }

    @Test
    public void aStartedModuleCarriesTheMessageBundlesReadFromItsJar() throws Exception {
        Path directory = Files.createTempDirectory("module-messages");
        Path jar = ModuleJarBuilder.module().manifest(new ManifestReader().write(manifest("alpha")))
                .resource("META-INF/resources/modules/alpha/remoteEntry.js", "export {};")
                .resource("META-INF/resources/modules/alpha/messages/en.json",
                        "{\"mod.alpha.menu\":\"Alpha\",\"sidenav.label.home\":\"stolen\"}")
                .resource("META-INF/resources/modules/alpha/messages/fr.json", "{\"mod.alpha.menu\":\"Alpha (fr)\"}")
                .build(directory.resolve("alpha-1.2.3.jar"));
        ModuleRecord started = ModuleRecords.record(jar, manifest("alpha"));
        started.transition(ModuleState.STARTED);
        ModuleRegistry registry = new ModuleRegistry(new ModuleSystemProperties(new MockEnvironment()),
                new ContractVersions("3.4.0-SNAPSHOT", "1.0.0", "1.0.0"), List.of(started));
        MockMvc withJar = MockMvcBuilders
                .standaloneSetup(new ModuleRegistryRestController(registry, mock(ModuleContextFactory.class))).build();

        withJar.perform(get("/rest/module-registry")).andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].messages.en['mod.alpha.menu']").value("Alpha"))
                .andExpect(jsonPath("$.modules[0].messages.en['sidenav.label.home']").doesNotExist())
                .andExpect(jsonPath("$.modules[0].messages.fr['mod.alpha.menu']").value("Alpha (fr)"));
    }

    @Test
    public void aModuleWhoseJarCannotBeReadCarriesNoBundles() throws Exception {
        mockMvc.perform(get("/rest/module-registry")).andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].messages").isMap())
                .andExpect(jsonPath("$.modules[0].messages.en").doesNotExist());
    }
}
