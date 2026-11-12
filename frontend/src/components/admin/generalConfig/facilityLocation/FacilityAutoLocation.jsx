import { useContext, useEffect } from "react";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import { hasRole, Roles } from "../../../utils/Utils";
import {
  fetchFacilityLocation,
  isFailure,
  readBrowserPosition,
  sendBrowserPosition,
} from "./facilityLocationApi";

export const ASKED_KEY = "openelis.facilityLocation.browserAsked";

const alreadyAsked = () => {
  try {
    return window.localStorage.getItem(ASKED_KEY) === "true";
  } catch {
    return false;
  }
};

const rememberAsked = () => {
  try {
    window.localStorage.setItem(ASKED_KEY, "true");
  } catch {
    return;
  }
};

/**
 * In automatic location mode, asks a signed-in administrator's browser for its
 * position once and reports it, so the facility registry shows where the lab
 * really is rather than where its internet connection appears to be.
 */
export default function FacilityAutoLocation() {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const isAdmin =
    !!userSessionDetails?.authenticated &&
    hasRole(userSessionDetails, Roles.GLOBAL_ADMIN);

  useEffect(() => {
    if (!isAdmin || alreadyAsked()) {
      return;
    }
    fetchFacilityLocation((status) => {
      if (
        isFailure(status) ||
        !status.autolocation ||
        status.source === "browser"
      ) {
        return;
      }
      rememberAsked();
      readBrowserPosition(
        (latitude, longitude) =>
          sendBrowserPosition(latitude, longitude, () => {}),
        () => {},
      );
    });
  }, [isAdmin]);

  return null;
}
