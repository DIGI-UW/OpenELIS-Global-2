import React from "react";
import { Route, Switch, useParams } from "react-router-dom";
import { Loading } from "@carbon/react";
import { useIntl } from "react-intl";
import { InlineNotification } from "@carbon/react";
import { SecureRoute } from "../components/security";
import LazyRemote from "./LazyRemote";
import { useModuleHost } from "./ModuleHostProvider";

/**
 * Every module page lives under `/m/<module id>/`. This single route, placed
 * before the catch-all, waits for the registry, then renders the module's
 * declared routes behind the same `SecureRoute` gate the core pages use
 * (the manifest's permission, expanded to `mod.<id>.<key>`, or a role). An
 * unknown module or path shows a notice instead of the catch-all's redirect.
 */
export default function ModuleRoutes() {
  return (
    <Route path="/m/:moduleId">
      <ModulePages />
    </Route>
  );
}

function ModulePages() {
  const { moduleId } = useParams();
  const host = useModuleHost();
  const intl = useIntl();
  if (host.status === "idle" || host.status === "loading") {
    return <Loading withOverlay={false} />;
  }
  const routes = host.routes.filter((route) => route.moduleId === moduleId);
  if (routes.length === 0) {
    const skipped = host.skipped.find((module) => module.id === moduleId);
    return (
      <div className="pageContent">
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "modules.unavailable.title" })}
          subtitle={intl.formatMessage(
            { id: "modules.unavailable.message" },
            {
              module: moduleId,
              detail: skipped
                ? skipped.frontendReason || skipped.reason || ""
                : "",
            },
          )}
        />
      </div>
    );
  }
  return (
    <Switch>
      {routes.map((route) => (
        <SecureRoute
          key={route.path}
          path={route.path}
          exact={route.exact}
          permission={route.permission || undefined}
          role={route.role || (route.permission ? undefined : "")}
          render={(routeProps) => (
            <LazyRemote
              moduleId={route.moduleId}
              remoteName={route.remoteName}
              expose={route.expose}
              match={routeProps.match}
              location={routeProps.location}
              history={routeProps.history}
            />
          )}
        />
      ))}
      <Route
        render={() => (
          <div className="pageContent">
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "modules.unavailable.title" })}
              subtitle={intl.formatMessage(
                { id: "modules.unavailable.noRoute" },
                { module: moduleId },
              )}
            />
          </div>
        )}
      />
    </Switch>
  );
}
