package org.openelisglobal.module.runtime.spring;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.ResolvableType;

/**
 * A module's Spring context: an {@code AnnotationConfigApplicationContext}
 * whose parent is the core root context and whose class loader is the module's.
 * Beans resolve from the module first and then from the core, so a module
 * injects any core service; the core never resolves beans from here.
 *
 * <p>
 * Spring forwards every event a context publishes to its parent. A module's own
 * events are forwarded, so the core (or another module through the forwarder)
 * may listen to them; this context's lifecycle events
 * ({@code ContextRefreshedEvent}, {@code ContextClosedEvent}, ...) are kept
 * local, because core start-up listeners must run once for the root context,
 * not again for every module.
 */
public class ModuleApplicationContext extends AnnotationConfigApplicationContext {

    private final String moduleId;

    public ModuleApplicationContext(String moduleId) {
        this.moduleId = moduleId;
        setId("module:" + moduleId);
        setDisplayName("module " + moduleId);
    }

    public String getModuleId() {
        return moduleId;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        publishEvent(event, null);
    }

    @Override
    public void publishEvent(Object event) {
        publishEvent(event, null);
    }

    @Override
    protected void publishEvent(Object event, ResolvableType typeHint) {
        if (event instanceof ApplicationContextEvent contextEvent && contextEvent.getApplicationContext() == this) {
            multicastLocally(contextEvent, typeHint);
            return;
        }
        if (event instanceof ForwardedEvent forwarded) {
            ApplicationEvent applicationEvent = forwarded.payload() instanceof ApplicationEvent ae ? ae
                    : new PayloadApplicationEvent<>(this, forwarded.payload());
            multicastLocally(applicationEvent, null);
            return;
        }
        super.publishEvent(event, typeHint);
    }

    private void multicastLocally(ApplicationEvent event, ResolvableType typeHint) {
        if (!isActive()) {
            return;
        }
        ApplicationEventMulticaster multicaster = getBeanFactory().getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
                ApplicationEventMulticaster.class);
        multicaster.multicastEvent(event, typeHint);
    }

    /**
     * Publishes a core event to this module's listeners only, without forwarding it
     * back to the parent.
     */
    public void publishForwarded(Object payload) {
        publishEvent(new ForwardedEvent(payload), null);
    }

    /**
     * Marker wrapper the forwarder uses so a core event delivered here never
     * bounces back to the root.
     */
    record ForwardedEvent(Object payload) {
    }
}
