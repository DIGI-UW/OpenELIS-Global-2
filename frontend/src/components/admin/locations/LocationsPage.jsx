import React, { useCallback, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Redirect,
  Route,
  Switch,
  useHistory,
  useLocation,
  useRouteMatch,
} from "react-router-dom";
import {
  ActionableNotification,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Section,
} from "@carbon/react";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import PageIntro from "./PageIntro";
import OrganizationsView from "./OrganizationsView";
import AreasView from "./AreasView";
import ImportExportView from "./ImportExportView";
import { getLists } from "./locationsApi";
import "./locations.css";

/**
 * OGC-1363: Admin → Locations & Organizations. One menu for every place the
 * lab deals with: organizations (with their wards / depts), sampling sites,
 * geographic areas, and the CSV import / export that keeps them current.
 */

export const VIEWS = {
  organizations: {
    labelId: "sidenav.label.admin.locations.organizations",
    route: "",
  },
  sites: { labelId: "sidenav.label.admin.locations.sites", route: "/sites" },
  areas: { labelId: "sidenav.label.admin.locations.areas", route: "/areas" },
  import: {
    labelId: "sidenav.label.admin.locations.import",
    route: "/import",
  },
};

/**
 * Where the old organizationEdit?ID= link lands: the Organizations view with
 * that record expanded, or the Add form for ID=0.
 */
export const legacyOrganizationEditTarget = (basePath, location) => {
  const id = new URLSearchParams(location.search).get("ID");
  if (!id || id === "0") {
    return `${basePath}/locations?add=1`;
  }
  return `${basePath}/locations?id=${encodeURIComponent(id)}`;
};

export const LocationsContext = React.createContext({
  lists: null,
  reloadLists: () => {},
  notify: () => {},
});

const EMPTY_LISTS = {
  categories: [],
  ownerships: [],
  serviceTypes: [],
  siteTypes: [],
  environmentalZones: [],
  referralStatuses: [],
  identifierLabels: [],
  facilityTypes: [],
  areaLevels: [],
};

const UNDO_SECONDS = 10;

const LocationsPage = () => {
  const intl = useIntl();
  const { path, url } = useRouteMatch();
  const location = useLocation();
  const history = useHistory();
  const [lists, setLists] = useState(null);
  const [toast, setToast] = useState(null);
  const toastTimer = useRef(null);

  const reloadLists = useCallback(() => {
    getLists()
      .then((loaded) => setLists({ ...EMPTY_LISTS, ...loaded }))
      .catch(() => setLists({ ...EMPTY_LISTS }));
  }, []);

  useEffect(() => {
    reloadLists();
  }, [reloadLists]);

  useEffect(
    () => () => {
      if (toastTimer.current) {
        clearTimeout(toastTimer.current);
      }
    },
    [],
  );

  /**
   * A message at the top of the page. With `action`, it carries that button
   * (the Undo after a deactivation) and stays at least ten seconds (FR-L3).
   */
  const notify = useCallback((message, kind = "success", action = null) => {
    if (toastTimer.current) {
      clearTimeout(toastTimer.current);
    }
    setToast({ message, kind, action, key: Date.now() });
    toastTimer.current = setTimeout(
      () => setToast(null),
      action ? UNDO_SECONDS * 1000 : 6000,
    );
  }, []);

  const viewKey = (() => {
    const rest = location.pathname.replace(url, "").replace(/\/+$/, "");
    if (rest.startsWith("/sites")) return "sites";
    if (rest.startsWith("/areas")) return "areas";
    if (rest.startsWith("/import")) return "import";
    return "organizations";
  })();

  const go = useCallback(
    (view, search) => {
      history.push({
        pathname: `${url}${VIEWS[view].route}`,
        search: search || "",
      });
    },
    [history, url],
  );

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    { label: "sidenav.label.admin.locations", link: `${url}` },
    { label: VIEWS[viewKey].labelId, link: `${url}${VIEWS[viewKey].route}` },
  ];

  return (
    <LocationsContext.Provider value={{ lists, reloadLists, notify, go }}>
      <div className="adminPageContent locationsPage">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id={VIEWS[viewKey].labelId} />
              </Heading>
            </Section>
            <PageIntro view={viewKey} go={go} />
            {toast && toast.action ? (
              <ActionableNotification
                key={toast.key}
                kind={toast.kind}
                lowContrast
                inline
                hasFocus={false}
                role="status"
                title=""
                subtitle={toast.message}
                actionButtonLabel={toast.action.label}
                onActionButtonClick={() => {
                  toast.action.run();
                  setToast(null);
                }}
                onClose={() => setToast(null)}
                className="locationsToast"
              />
            ) : null}
            {toast && !toast.action ? (
              <InlineNotification
                key={toast.key}
                kind={toast.kind}
                lowContrast
                role="status"
                title=""
                subtitle={toast.message}
                onClose={() => setToast(null)}
                className="locationsToast"
              />
            ) : null}
          </Column>
        </Grid>
        <Switch>
          <Route
            exact
            path={`${path}/sites`}
            render={() => (
              <OrganizationsView key="sites" view="sites" lists={lists} />
            )}
          />
          <Route
            exact
            path={`${path}/areas`}
            render={() => <AreasView lists={lists} />}
          />
          <Route
            exact
            path={`${path}/import`}
            render={() => <ImportExportView lists={lists} />}
          />
          <Route
            exact
            path={path}
            render={() => (
              <OrganizationsView
                key="organizations"
                view="organizations"
                lists={lists}
              />
            )}
          />
          <Redirect to={url} />
        </Switch>
        <p className="cds--label locationsFooterNote">
          {intl.formatMessage({ id: "help.locations.status.inactive" })}
        </p>
      </div>
    </LocationsContext.Provider>
  );
};

export default LocationsPage;
