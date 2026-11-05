package org.openelisglobal.module.web;

/**
 * The version of the frontend contract this shell publishes: the shared
 * singletons (React 17, React Router 5, react-intl 5, Carbon, TanStack Query 4)
 * and the module SDK. A module declares the range it was built against in
 * {@code requires.frontendContract}; the shell loads a module's bundle only
 * when the range is satisfied. Kept in step with
 * {@code frontend/packages/module-sdk/package.json}.
 */
public final class ModuleFrontendContract {

    public static final String VERSION = "1.0.0";

    private ModuleFrontendContract() {
    }
}
