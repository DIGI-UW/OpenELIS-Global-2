import React, { useState, useContext, useEffect } from "react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { ConfigurationContext } from "../layout/Layout";
import { Route, useLocation } from "react-router-dom";
import { useIdleTimer } from "react-idle-timer";
import { confirmAlert } from "react-confirm-alert";
import "react-confirm-alert/src/react-confirm-alert.css"; // Import css
import { Loading, Modal } from "@carbon/react/";
import config from "../../config.json";
import { Roles } from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";
import type { RouteProps } from "react-router-dom";

const idleTimeout = 1000 * 60 * 30; // milliseconds until idle warning will appear
const idleWarningTimeout = 1000 * 60; // milliseconds until logout is automatically processed from idle warning
const idleLogoutTimeout = idleTimeout + idleWarningTimeout;

type RoleProp = string | string[];
type LabUnitRoles = Record<string, string[]>;

interface UserSessionDetails {
  authenticated?: boolean;
  loginLabUnit?: unknown;
  roles?: string[];
  userLabRolesMap?: Record<string, string[]>;
}

interface SecureRouteProps extends RouteProps {
  role?: RoleProp;
  labUnitRole?: LabUnitRoles;
}

function SecureRoute(props: SecureRouteProps) {
  const [permissionGranted, setPermissionGranted] = useState(false);
  const [loading, setLoading] = useState(true);
  const [stillThereOpen, setStillThereOpen] = useState(false);

  const intl = useIntl();
  const location = useLocation();

  const {
    userSessionDetails,
    errorLoadingSessionDetails,
    isCheckingLogin,
    logout,
  } = useContext(UserSessionDetailsContext);

  const { configurationProperties } = useContext(ConfigurationContext);

  // Reset permission when route changes to prevent stale grants from previous routes
  useEffect(() => {
    setPermissionGranted(false);
  }, [location.pathname]);

  useEffect(() => {
    const stillChecking = !errorLoadingSessionDetails && isCheckingLogin();
    setLoading(stillChecking);

    if (stillChecking) {
      return;
    }

    if (userSessionDetails.authenticated) {
      const allowed = hasPermission(userSessionDetails);
      setPermissionGranted(allowed);
      if (allowed) {
        if (
          configurationProperties.REQUIRE_LAB_UNIT_AT_LOGIN === "true" &&
          !userSessionDetails.loginLabUnit &&
          !userSessionDetails.roles.includes(Roles.GLOBAL_ADMIN)
        ) {
          window.location.href = "/landing";
        }
      } else {
        const options = {
          title: intl.formatMessage({ id: "accessDenied.title" }),
          message: intl.formatMessage({ id: "accessDenied.message" }),
          buttons: [
            {
              label: intl.formatMessage({ id: "accessDenied.okButton" }),
              onClick: () => {
                window.location.href = window.location.origin;
              },
            },
          ],
          closeOnClickOutside: false,
          closeOnEscape: false,
        };
        confirmAlert(options);
      }
    } else if ("authenticated" in userSessionDetails) {
      window.location.href = config.loginRedirect;
    }
  }, [userSessionDetails, errorLoadingSessionDetails, location.pathname]);

  const hasPermission = (
    userDetails: UserSessionDetails = userSessionDetails,
  ) => {
    var hasRole =
      !props.role ||
      ([] as string[])
        .concat(props.role)
        .some((role) => userDetails.roles && userDetails.roles.includes(role));
    var containsLabUnitRole = false;
    if (props.labUnitRole) {
      Object.keys(props.labUnitRole).forEach((labunit) => {
        if (userDetails.userLabRolesMap) {
          const userRoles = userDetails.userLabRolesMap["AllLabUnits"]
            ? userDetails.userLabRolesMap["AllLabUnits"]
            : userDetails.userLabRolesMap[labunit] || [];
          const roles = props.labUnitRole[labunit];
          roles.forEach((r) => {
            if (userRoles.includes(r)) {
              containsLabUnitRole = true;
            }
          });
        }
      });
    }
    var hasLabUnitRole = !props.labUnitRole || containsLabUnitRole;
    return hasRole && hasLabUnitRole;
  };

  const onIdle = () => {
    setStillThereOpen(false);
    console.debug("idleTimer now idle");
    logout();
  };

  const onActive = () => {
    setStillThereOpen(false);
    console.debug("idleTimer now active");
  };

  const onPrompt = () => {
    setStillThereOpen(true);
    console.debug("idleTimer now prompting");
  };

  const { activate } = useIdleTimer({
    onIdle,
    onActive,
    onPrompt,
    timeout: idleLogoutTimeout,
    promptBeforeIdle: idleWarningTimeout,
    crossTab: true,
    syncTimers: true,
  });

  const handleStillHere = () => {
    activate();
  };

  return (
    <>
      <Modal
        open={stillThereOpen}
        onRequestClose={() => {
          setStillThereOpen(false);
          handleStillHere();
        }}
        modalHeading={intl.formatMessage({ id: "stillThere.title" })}
        passiveModal
      >
        <FormattedMessage id="stillThere.message" />
      </Modal>
      {loading && <Loading />}
      {!loading && !userSessionDetails.authenticated && <Loading />}
      {!loading && userSessionDetails.authenticated && permissionGranted && (
        <>{!stillThereOpen && <Route {...props} />}</>
      )}
    </>
  );
}

export default SecureRoute;
