package org.openelisglobal.module.runtime.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.runtime.ContractVersions;
import org.openelisglobal.module.runtime.ModuleDiscovery;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.openelisglobal.module.runtime.ModuleSystemProperties;
import org.openelisglobal.module.runtime.contributions.ModuleMenuContributor;
import org.openelisglobal.module.runtime.contributions.ModulePermissionRegistrar;
import org.openelisglobal.module.testkit.ModuleJarBuilder;
import org.openelisglobal.reports.action.implementation.IReportCreator;
import org.openelisglobal.reports.action.implementation.ModuleReportCreator;
import org.openelisglobal.reports.action.implementation.ReportImplementationFactory;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.spi.event.SampleCreatedEvent;
import org.openelisglobal.spi.report.ReportProvider;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Starts a fixture module, compiled at test time and loaded only through its
 * own class loader, in a child context of a root MVC context: its service
 * injects a core bean, its controller is served under the module prefix with
 * method security, its extension reaches the report factory, a forwarded SPI
 * event reaches its listener, and the root never sees its beans or its refresh
 * event.
 */
public class ModuleRuntimeIntegrationTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private AnnotationConfigWebApplicationContext root;
    private ModuleRegistry registry;
    private ModuleContextFactory factory;
    private MockMvc mockMvc;
    private static final AtomicInteger ROOT_REFRESHES = new AtomicInteger();

    /** A core bean the module injects. */
    public static class CoreGreeting {
        public String greet(String who) {
            return "hello " + who + " from core";
        }
    }

    public static class RefreshCounter implements ApplicationListener<ContextRefreshedEvent> {
        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            ROOT_REFRESHES.incrementAndGet();
        }
    }

    /**
     * Set by the test before the root refreshes; a refreshable context has no bean
     * factory before that.
     */
    private static ModuleRegistry registryForRoot;

    @Configuration
    @Import(ModuleWebMvcConfiguration.class)
    static class RootConfig {
        @Bean
        ModuleRegistry moduleRegistry() {
            return registryForRoot;
        }

        @Bean
        CoreGreeting coreGreeting() {
            return new CoreGreeting();
        }

        @Bean
        RefreshCounter refreshCounter() {
            return new RefreshCounter();
        }

        @Bean
        SpringContext springContext() {
            return new SpringContext();
        }

        @Bean
        MenuService menuService() {
            MenuService menuService = mock(MenuService.class);
            Menu sample = new Menu();
            sample.setId("2");
            sample.setElementId("menu_sample");
            when(menuService.getMenuByElementId("menu_sample")).thenReturn(sample);
            return menuService;
        }

        @Bean
        SystemModuleService systemModuleService() {
            SystemModuleService service = mock(SystemModuleService.class);
            when(service.insert(any(SystemModule.class))).thenAnswer(invocation -> {
                SystemModule module = invocation.getArgument(0);
                module.setId("900");
                return "900";
            });
            return service;
        }

        @Bean
        SystemModuleUrlService systemModuleUrlService() {
            return mock(SystemModuleUrlService.class);
        }

        @Bean
        RoleModuleService roleModuleService() {
            return mock(RoleModuleService.class);
        }

        @Bean
        RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean("daemonSysUserId")
        String daemonSysUserId() {
            return "1";
        }

        @Bean
        DaemonContextExecutor daemonContextExecutor() {
            return new DaemonContextExecutor();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            return manager;
        }

        @Bean
        DataSource dataSource() {
            return mock(DataSource.class);
        }
    }

    private static final String PKG = "org.openelisglobal.module.ctx";

    private static String manifest(String id, String restController) {
        return "{\"schemaVersion\":1,\"id\":\"" + id + "\",\"kind\":\"module\",\"name\":\"Fixture\","
                + "\"version\":\"1.0.0\",\"requires\":{\"openelis\":\">=3.4.0\",\"spi\":\"^1.0\"},"
                + "\"backend\":{\"basePackages\":[\"" + PKG + "\"],\"activator\":\"" + PKG + ".Activator\","
                + "\"extensions\":[{\"spi\":\"report\",\"class\":\"" + PKG + ".FixtureReport\"}]},"
                + "\"permissions\":[{\"key\":\"view\",\"description\":\"see\",\"defaultRoles\":[]}],"
                + "\"menu\":[{\"elementId\":\"menu_mod_" + id.replace('-', '_') + "\",\"parent\":\"menu_sample\","
                + "\"displayKey\":\"mod." + id + ".menu\",\"actionURL\":\"/m/" + id
                + "/home\",\"permission\":\"view\"}]}";
    }

    private Path buildFixture(Path directory, String id, String mapping) {
        return ModuleJarBuilder.module().manifest(manifest(id, mapping)).source(PKG + ".Activator", "package " + PKG
                + ";\n" + "public class Activator implements org.openelisglobal.module.api.OpenElisModule {\n"
                + "  public static int starts;\n"
                + "  public void onStart(org.openelisglobal.module.api.ModuleContext c) { starts++; c.log().info(\"up\"); }\n}\n")
                .source(PKG + ".GreetingService", "package " + PKG + ";\n" + "@org.springframework.stereotype.Service\n"
                        + "public class GreetingService {\n"
                        + "  @org.springframework.beans.factory.annotation.Autowired @org.springframework.beans.factory.annotation.Qualifier(\"coreGreeting\") org.openelisglobal.module.runtime.spring.ModuleRuntimeIntegrationTest.CoreGreeting core;\n"
                        + "  @org.springframework.beans.factory.annotation.Autowired org.openelisglobal.module.api.ModuleContext context;\n"
                        + "  public String greet() { return core.greet(context.descriptor().id()); }\n}\n")
                .source(PKG + ".ShadowGreeting", "package " + PKG + ";\n"
                        + "@org.springframework.context.annotation.Primary @org.springframework.stereotype.Component\n"
                        + "public class ShadowGreeting extends org.openelisglobal.module.runtime.spring.ModuleRuntimeIntegrationTest.CoreGreeting {\n"
                        + "  public String greet(String who) { return \"shadowed\"; }\n}\n")
                .source(PKG + ".HelloController",
                        "package " + PKG + ";\n" + "@org.springframework.web.bind.annotation.RestController\n"
                                + "@org.springframework.web.bind.annotation.RequestMapping(\"" + mapping + "\")\n"
                                + "public class HelloController {\n"
                                + "  @org.springframework.beans.factory.annotation.Autowired GreetingService service;\n"
                                + "  @org.springframework.web.bind.annotation.GetMapping(\"/hello\")\n"
                                + "  @org.springframework.security.access.prepost.PreAuthorize(\"hasAuthority('mod."
                                + id + ".view')\")\n" + "  public String hello() { return service.greet(); }\n}\n")
                .source(PKG + ".EventSink", "package " + PKG + ";\n" + "@org.springframework.stereotype.Component\n"
                        + "public class EventSink {\n" + "  public static volatile String lastSample;\n"
                        + "  @org.springframework.context.event.EventListener\n"
                        + "  public void on(org.openelisglobal.spi.event.SampleCreatedEvent e) { lastSample = e.getSampleId(); }\n}\n")
                .source(PKG + ".FixtureReport", "package " + PKG + ";\n"
                        + "public class FixtureReport implements org.openelisglobal.spi.report.ReportProvider {\n"
                        + "  public java.util.Set<String> reportIds() { return java.util.Set.of(\"fixtureReport\"); }\n"
                        + "  public org.openelisglobal.spi.report.ReportOutput run(org.openelisglobal.spi.report.ReportRequest r) {\n"
                        + "    return org.openelisglobal.spi.report.ReportOutput.pdf((\"report for \" + r.getReportId()).getBytes(), \"f.pdf\"); }\n}\n")
                .build(directory.resolve(id + "-1.0.0.jar"));
    }

    private void startRoot(Path modulesDirectory) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(ModuleSystemProperties.DIRECTORY, modulesDirectory.toString());
        environment.setProperty(ModuleSystemProperties.CACHE_DIRECTORY, modulesDirectory.resolve("cache").toString());
        environment.setProperty(ModuleSystemProperties.DATA_DIRECTORY, modulesDirectory.resolve("data").toString());
        ModuleSystemProperties properties = new ModuleSystemProperties(environment);
        List<ModuleRecord> records = new ModuleDiscovery(properties, new ContractVersions("3.4.0", "1.0.0", "1.0.0"),
                getClass().getClassLoader()).discover();
        for (ModuleRecord record : records) {
            assertEquals(record.reason().orElse(""), ModuleState.VALIDATED, record.state());
            record.transition(ModuleState.MIGRATED);
            record.setRecordedVersion(null);
        }
        registry = new ModuleRegistry(properties, new ContractVersions("3.4.0", "1.0.0", "1.0.0"), records);

        root = new AnnotationConfigWebApplicationContext();
        root.setServletContext(new MockServletContext());
        root.setEnvironment(environment);
        root.register(RootConfig.class);
        registryForRoot = registry;
        ROOT_REFRESHES.set(0);
        root.refresh();

        RequestMappingHandlerMapping mapping = root.getBean(RequestMappingHandlerMapping.class);
        ModulePermissionRegistrar permissions = new ModulePermissionRegistrar(root.getBean(SystemModuleService.class),
                root.getBean(SystemModuleUrlService.class), root.getBean(RoleModuleService.class),
                root.getBean(RoleService.class), root.getBean(DaemonContextExecutor.class),
                new org.springframework.transaction.support.TransactionTemplate(
                        root.getBean(PlatformTransactionManager.class)));
        factory = new ModuleContextFactory(registry, (ModuleAwareRequestMappingHandlerMapping) mapping, permissions,
                new ModuleMenuContributor(root.getBean(MenuService.class)), root.getBean(DataSource.class));
        mockMvc = MockMvcBuilders.webAppContextSetup(root).build();
    }

    @Before
    public void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @After
    public void closeRoot() {
        SecurityContextHolder.clearContext();
        if (factory != null) {
            factory.stopAll();
        }
        if (root != null && root.isActive()) {
            root.close();
        }
    }

    @Test
    public void moduleStartsInAChildContextAndServesItsEndpoint() throws Exception {
        Path directory = temp.newFolder("modules").toPath();
        buildFixture(directory, "ctx", "/rest/modules/ctx");
        startRoot(directory);
        ModuleRecord module = registry.find("ctx").orElseThrow();

        factory.startAll(root);

        assertEquals(module.reason().orElse(""), ModuleState.STARTED, module.state());
        assertEquals(1, ROOT_REFRESHES.get());
        ModuleApplicationContext child = factory.contexts().get("ctx");
        assertNotNull(child);
        assertSame(root, child.getParent());
        Class<?> activator = Class.forName(PKG + ".Activator", true, module.classLoader());
        assertEquals(1, activator.getField("starts").getInt(null));

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("tech", "n/a", "mod.ctx.view"));
        mockMvc.perform(get("/rest/modules/ctx/hello")).andExpect(status().isOk())
                .andExpect(content().string("hello ctx from core"));

        verify(root.getBean(SystemModuleService.class)).insert(any(SystemModule.class));
        verify(root.getBean(SystemModuleUrlService.class)).insert(any());
    }

    @Test
    public void methodSecurityGatesTheModuleEndpoint() throws Exception {
        Path directory = temp.newFolder("modules").toPath();
        buildFixture(directory, "ctx", "/rest/modules/ctx");
        startRoot(directory);
        factory.startAll(root);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("tech", "n/a", "ROLE_RESULTS"));
        try {
            mockMvc.perform(get("/rest/modules/ctx/hello"));
            fail("expected access to be denied");
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null && !(cause instanceof AccessDeniedException)) {
                cause = cause.getCause();
            }
            assertTrue(e.toString(), cause instanceof AccessDeniedException);
        }
    }

    @Test
    public void coreNeverSeesModuleBeansAndModuleSeesCore() throws Exception {
        Path directory = temp.newFolder("modules").toPath();
        buildFixture(directory, "ctx", "/rest/modules/ctx");
        startRoot(directory);
        factory.startAll(root);
        ModuleApplicationContext child = factory.contexts().get("ctx");

        assertTrue(root.getBeansOfType(CoreGreeting.class).size() == 1);
        assertEquals("hello x from core", root.getBean(CoreGreeting.class).greet("x"));
        assertEquals(0, root.getBeanNamesForType(Class.forName(PKG + ".GreetingService", true,
                registry.find("ctx").orElseThrow().classLoader())).length);
        assertEquals("shadowed", child.getBean(CoreGreeting.class).greet("x"));
        assertSame(root.getBean(CoreGreeting.class), child.getParent().getBean(CoreGreeting.class));
    }

    @Test
    public void declaredExtensionReachesTheReportFactoryAndEventsAreForwarded() throws Exception {
        Path directory = temp.newFolder("modules").toPath();
        buildFixture(directory, "ctx", "/rest/modules/ctx");
        startRoot(directory);
        factory.startAll(root);

        assertEquals(1, registry.extensions().providers(ReportProvider.class).size());
        IReportCreator creator = ReportImplementationFactory.getReportCreator("fixtureReport");
        assertTrue(String.valueOf(creator), creator instanceof ModuleReportCreator);
        creator.setRequestedReport("fixtureReport");
        creator.initializeReport(null);
        assertEquals("report for fixtureReport", new String(creator.runReport()));
        assertEquals("application/pdf", creator.getContentType());
        assertTrue(ReportImplementationFactory.getReportCreator("noSuchReport") == null
                || !(ReportImplementationFactory.getReportCreator("noSuchReport") instanceof ModuleReportCreator));

        new ModuleEventForwarder(factory).forward(new SampleCreatedEvent("4711", "25ACC", "77", Instant.now()));
        Class<?> sink = Class.forName(PKG + ".EventSink", true, registry.find("ctx").orElseThrow().classLoader());
        assertEquals("4711", sink.getField("lastSample").get(null));
        assertEquals(1, ROOT_REFRESHES.get());
    }

    @Test
    public void controllerOutsideThePrefixFailsTheModule() throws Exception {
        Path directory = temp.newFolder("modules").toPath();
        buildFixture(directory, "ctx", "/rest/elsewhere");
        startRoot(directory);

        try {
            factory.startAll(root);
            fail("expected the module to fail");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("/rest/elsewhere"));
            assertTrue(e.getMessage(), e.getMessage().contains("ctx"));
        }
        assertEquals(ModuleState.FAILED, registry.find("ctx").orElseThrow().state());
        assertTrue(factory.contexts().isEmpty());
    }

    @Test
    public void stopAllClosesContextsAndCallsOnStop() throws Exception {
        Path directory = temp.newFolder("modules").toPath();
        buildFixture(directory, "ctx", "/rest/modules/ctx");
        startRoot(directory);
        factory.startAll(root);
        ModuleApplicationContext child = factory.contexts().get("ctx");

        factory.stopAll();

        assertEquals(ModuleState.STOPPED, registry.find("ctx").orElseThrow().state());
        assertTrue(!child.isActive());
        assertTrue(factory.contexts().isEmpty());
    }

}
