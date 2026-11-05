package org.openelisglobal.module.runtime.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Registered in every module context before the module's own packages are
 * scanned. Transactions resolve the core's {@code JpaTransactionManager}
 * through the parent, so a module {@code @Transactional} joins the core
 * transaction; method security enforces {@code @PreAuthorize} on module beans
 * with the same security context the core uses.
 */
@Configuration
@EnableTransactionManagement
@EnableMethodSecurity(prePostEnabled = true)
public class ModuleChildConfig {
}
