package org.openelisglobal.module.runtime;

import javax.sql.DataSource;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.module.runtime.contributions.ModuleMenuContributor;
import org.openelisglobal.module.runtime.contributions.ModulePermissionRegistrar;
import org.openelisglobal.module.runtime.persistence.ModuleEntityRegistrar;
import org.openelisglobal.module.runtime.persistence.ModuleLiquibaseRunner;
import org.openelisglobal.module.runtime.spring.ModuleAwareRequestMappingHandlerMapping;
import org.openelisglobal.module.runtime.spring.ModuleContextFactory;
import org.openelisglobal.module.runtime.spring.ModuleEventForwarder;
import org.openelisglobal.module.runtime.spring.ModuleWebMvcConfiguration;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The module runtime's beans in the core root context. Order of events at
 * start-up: the registry discovers and validates the JARs when it is created;
 * {@code moduleLiquibase} runs the module changelogs after the core changelog;
 * the entity manager factory (which depends on both) is built with the module
 * entities; after the root refresh, {@link ModuleContextFactory} starts the
 * module contexts and registers their contributions.
 *
 * <p>
 * No module bean is defined here: modules live in their own contexts.
 */
@Configuration
@Import(ModuleWebMvcConfiguration.class)
public class ModuleConfig {

    @Bean
    public ModuleSystemProperties moduleSystemProperties(Environment environment) {
        return new ModuleSystemProperties(environment);
    }

    @Bean
    public ModuleRegistry moduleRegistry(ModuleSystemProperties properties) {
        return ModuleRegistry.discover(properties, ContractVersions.running(), ModuleConfig.class.getClassLoader());
    }

    @Bean("moduleLiquibase")
    @DependsOn("liquibase")
    public ModuleLiquibaseRunner moduleLiquibase(ModuleRegistry registry, DataSource dataSource,
            @Value("${spring.liquibase.contexts:default}") String contexts) {
        ModuleLiquibaseRunner runner = new ModuleLiquibaseRunner(registry, dataSource, contexts);
        runner.run();
        return runner;
    }

    @Bean
    public ModuleEntityRegistrar moduleEntityRegistrar(ModuleRegistry registry) {
        return new ModuleEntityRegistrar(registry);
    }

    @Bean
    public ModulePermissionRegistrar modulePermissionRegistrar(SystemModuleService systemModuleService,
            SystemModuleUrlService systemModuleUrlService, RoleModuleService roleModuleService, RoleService roleService,
            DaemonContextExecutor daemonContextExecutor, PlatformTransactionManager transactionManager) {
        return new ModulePermissionRegistrar(systemModuleService, systemModuleUrlService, roleModuleService,
                roleService, daemonContextExecutor, new TransactionTemplate(transactionManager));
    }

    @Bean
    public ModuleMenuContributor moduleMenuContributor(MenuService menuService) {
        return new ModuleMenuContributor(menuService);
    }

    @Bean
    public ModuleContextFactory moduleContextFactory(ModuleRegistry registry,
            RequestMappingHandlerMapping requestMappingHandlerMapping, ModulePermissionRegistrar permissionRegistrar,
            ModuleMenuContributor menuContributor, DataSource dataSource) {
        if (!(requestMappingHandlerMapping instanceof ModuleAwareRequestMappingHandlerMapping mapping)) {
            throw new IllegalStateException("the request mapping is not module aware; ModuleWebMvcConfiguration"
                    + " must replace @EnableWebMvc");
        }
        return new ModuleContextFactory(registry, mapping, permissionRegistrar, menuContributor, dataSource);
    }

    @Bean
    public ModuleContextFactory.Shutdown moduleContextShutdown(ModuleContextFactory factory) {
        return new ModuleContextFactory.Shutdown(factory);
    }

    @Bean
    public ModuleEventForwarder moduleEventForwarder(ModuleContextFactory factory) {
        return new ModuleEventForwarder(factory);
    }
}
