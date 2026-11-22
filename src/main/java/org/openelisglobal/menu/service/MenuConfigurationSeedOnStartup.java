package org.openelisglobal.menu.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.menu.util.MenuUtil;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Seeds the menu from its configuration file once the application is up. A
 * failure is logged and leaves the menu as the database has it.
 */
@Component
public class MenuConfigurationSeedOnStartup implements ApplicationListener<ContextRefreshedEvent> {

    private final MenuConfigurationSeeder seeder;
    private final AtomicBoolean done = new AtomicBoolean();

    public MenuConfigurationSeedOnStartup(MenuConfigurationSeeder seeder) {
        this.seeder = seeder;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!done.compareAndSet(false, true)) {
            return;
        }
        try {
            seeder.seed();
            MenuUtil.forceRebuild();
        } catch (RuntimeException e) {
            LogEvent.logError("Could not seed the menu from its configuration file", e);
        }
    }
}
