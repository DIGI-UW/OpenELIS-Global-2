import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

export const FACILITY_LOCATION_API = "/rest/facility-location";

export const fetchFacilityLocation = (callback) =>
  getFromOpenElisServer(FACILITY_LOCATION_API, callback);

export const sendBrowserPosition = (latitude, longitude, callback) =>
  postToOpenElisServerJsonResponse(
    FACILITY_LOCATION_API,
    JSON.stringify({ latitude, longitude }),
    callback,
  );

export const isFailure = (response) =>
  !response || (typeof response.status === "number" && response.status >= 400);

export const readBrowserPosition = (onPosition, onError) => {
  if (!navigator.geolocation) {
    onError("unsupported");
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (position) =>
      onPosition(position.coords.latitude, position.coords.longitude),
    (error) => onError(error && error.code === 1 ? "denied" : "unavailable"),
    { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 },
  );
};
