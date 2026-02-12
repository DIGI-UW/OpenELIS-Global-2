import {
  Button,
  InlineNotification,
  Layer,
  Loading,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import {
  CheckmarkFilled,
  Crossroads,
  ErrorFilled,
  Location,
  LocationFilled,
} from "@carbon/react/icons";
import { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationKinds } from "../common/CustomNotification";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

/**
 * Modern GPS Coordinates Capture Component
 * Always visible design with progressive disclosure for manual entry
 */
const GpsCoordinatesCapture = ({
  index,
  sampleXml,
  onChange,
  disabled = false,
}) => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { addNotification } = useContext(NotificationContext);

  // Get GPS configuration from standard configuration context
  const gpsConfig = {
    requiredAccuracyMeters: parseInt(
      configurationProperties.GPS_ACCURACY_METERS || "100",
    ),
    timeoutSeconds: parseInt(
      configurationProperties.GPS_TIMEOUT_SECONDS || "10",
    ),
  };

  const isMounted = useRef(true);
  const [gpsStatus, setGpsStatus] = useState("idle"); // idle, loading, success, error
  const [errorMessage, setErrorMessage] = useState("");
  const [latitude, setLatitude] = useState(sampleXml?.gpsLatitude || "");
  const [longitude, setLongitude] = useState(sampleXml?.gpsLongitude || "");
  const [accuracy, setAccuracy] = useState(sampleXml?.gpsAccuracy || null);
  const [captureMethod, setCaptureMethod] = useState(
    sampleXml?.gpsCaptureMethod || "",
  );
  const [showManualEntry, setShowManualEntry] = useState(
    !!(sampleXml?.gpsLatitude || sampleXml?.gpsLongitude),
  );

  useEffect(() => {
    const gpsData = {
      gpsLatitude: latitude,
      gpsLongitude: longitude,
      gpsAccuracy: accuracy,
      gpsCaptureMethod: captureMethod,
    };
    onChange(gpsData);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [latitude, longitude, accuracy, captureMethod]);

  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  const handleGeolocationSuccess = (position) => {
    if (!isMounted.current) return;

    const lat = position.coords.latitude.toFixed(6);
    const lng = position.coords.longitude.toFixed(6);
    const acc = Math.round(position.coords.accuracy);

    setLatitude(lat);
    setLongitude(lng);
    setAccuracy(acc);
    setCaptureMethod("AUTO");
    setGpsStatus("success");
    setErrorMessage("");

    if (
      gpsConfig.requiredAccuracyMeters > 0 &&
      acc > gpsConfig.requiredAccuracyMeters
    ) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "gps.accuracy.warning.title" }),
        message: intl.formatMessage(
          { id: "gps.accuracy.warning.message" },
          { accuracy: acc, required: gpsConfig.requiredAccuracyMeters },
        ),
      });
    }
  };

  const handleGeolocationError = (error) => {
    if (!isMounted.current) return;

    setGpsStatus("error");
    let message = "";

    switch (error.code) {
      case error.PERMISSION_DENIED:
        message = intl.formatMessage({ id: "gps.error.permission.denied" });
        break;
      case error.POSITION_UNAVAILABLE:
        message = intl.formatMessage({ id: "gps.error.position.unavailable" });
        break;
      case error.TIMEOUT:
        message = intl.formatMessage({ id: "gps.error.timeout" });
        break;
      default:
        message = intl.formatMessage({ id: "gps.error.unknown" });
    }

    setErrorMessage(message);
  };

  const requestLocation = () => {
    if (!navigator.geolocation) {
      setGpsStatus("error");
      setErrorMessage(intl.formatMessage({ id: "gps.error.not.supported" }));
      return;
    }

    setGpsStatus("loading");
    setErrorMessage("");

    navigator.geolocation.getCurrentPosition(
      handleGeolocationSuccess,
      handleGeolocationError,
      {
        enableHighAccuracy: true,
        timeout: gpsConfig.timeoutSeconds * 1000,
        maximumAge: 60000,
      },
    );
  };

  const handleLatitudeChange = (event) => {
    const value = event.target.value;
    setLatitude(value);
    if (value && longitude) {
      setCaptureMethod("MANUAL");
    }
  };

  const handleLongitudeChange = (event) => {
    const value = event.target.value;
    setLongitude(value);
    if (latitude && value) {
      setCaptureMethod("MANUAL");
    }
  };

  const handleClear = () => {
    setLatitude("");
    setLongitude("");
    setAccuracy(null);
    setCaptureMethod("");
    setGpsStatus("idle");
    setErrorMessage("");
  };

  const isValidLatitude = (lat) => {
    const num = parseFloat(lat);
    return !isNaN(num) && num >= -90 && num <= 90;
  };

  const isValidLongitude = (lng) => {
    const num = parseFloat(lng);
    return !isNaN(num) && num >= -180 && num <= 180;
  };

  const hasValidCoordinates = () => {
    return (
      latitude &&
      longitude &&
      isValidLatitude(latitude) &&
      isValidLongitude(longitude)
    );
  };

  const hasPartialCoordinates = () => {
    return (latitude && !longitude) || (!latitude && longitude);
  };

  return (
    <div className="gps-coordinates-section" style={{ marginBottom: "1.5rem" }}>
      <Layer style={{ padding: "1.5rem" }}>
        {/* Header Section */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.75rem",
            marginBottom: "1rem",
          }}
        >
          {hasValidCoordinates() ? (
            <LocationFilled size={20} color="#24a148" />
          ) : (
            <Location size={20} color="#525252" />
          )}
          <div style={{ flex: 1 }}>
            <h4
              style={{
                margin: 0,
                fontSize: "1rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              <FormattedMessage id="gps.section.title" />
            </h4>
            <p
              style={{
                margin: "0.25rem 0 0 0",
                fontSize: "0.875rem",
                color: "#6f6f6f",
              }}
            >
              <FormattedMessage id="gps.section.description" />
            </p>
          </div>
          {hasValidCoordinates() ? (
            <Tag type="green" size="sm">
              <FormattedMessage id="gps.status.captured" />
            </Tag>
          ) : (
            <Tag type="gray" size="sm">
              <FormattedMessage id="gps.status.optional" />
            </Tag>
          )}
        </div>

        {/* Current Coordinates Display */}
        {hasValidCoordinates() && (
          <div
            style={{
              padding: "0.75rem",
              backgroundColor: "#e7f6f1",
              border: "1px solid #24a148",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <div style={{ fontSize: "0.875rem", color: "#161616" }}>
              <strong>
                <FormattedMessage id="gps.coordinates.current" />:
              </strong>{" "}
              {latitude}, {longitude}
              {accuracy && (
                <span style={{ color: "#6f6f6f" }}> (±{accuracy}m)</span>
              )}
            </div>
            <div
              style={{
                fontSize: "0.75rem",
                color: "#6f6f6f",
                marginTop: "0.25rem",
              }}
            >
              <FormattedMessage
                id="gps.coordinates.captured.via"
                values={{
                  method:
                    captureMethod === "AUTO"
                      ? "browser location"
                      : "manual entry",
                }}
              />
            </div>
          </div>
        )}

        {/* Error Display */}
        {gpsStatus === "error" && errorMessage && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "gps.error.title" })}
            subtitle={errorMessage}
            hideCloseButton={true}
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Primary GPS Button */}
        <div style={{ marginBottom: "1.5rem" }}>
          <Button
            kind={gpsStatus === "success" ? "primary" : "secondary"}
            size="lg"
            onClick={requestLocation}
            disabled={disabled || gpsStatus === "loading"}
            style={{ minWidth: "200px" }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              {gpsStatus === "loading" && (
                <div style={{ width: "16px", height: "16px" }}>
                  <Loading small />
                </div>
              )}
              {gpsStatus === "success" && <CheckmarkFilled size={16} />}
              {gpsStatus === "error" && <ErrorFilled size={16} />}
              {gpsStatus === "idle" && <Crossroads size={16} />}

              {gpsStatus === "loading" && (
                <FormattedMessage id="gps.button.getting.location" />
              )}
              {gpsStatus === "success" && (
                <FormattedMessage id="gps.button.location.captured" />
              )}
              {(gpsStatus === "idle" || gpsStatus === "error") && (
                <FormattedMessage id="gps.button.use.my.location" />
              )}
            </div>
          </Button>
        </div>

        {/* Manual Entry Toggle */}
        <div style={{ marginBottom: "1rem" }}>
          <Toggle
            id={`gps-manual-toggle-${index}`}
            labelText={intl.formatMessage({ id: "gps.manual.entry.toggle" })}
            labelA={intl.formatMessage({ id: "gps.manual.entry.off" })}
            labelB={intl.formatMessage({ id: "gps.manual.entry.on" })}
            toggled={showManualEntry}
            onToggle={(toggled) => setShowManualEntry(toggled)}
            disabled={disabled}
          />
        </div>

        {/* Manual Entry Fields */}
        {showManualEntry && (
          <div
            style={{
              padding: "1rem",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              backgroundColor: "#f4f4f4",
              marginBottom: "1rem",
            }}
          >
            <h5
              style={{
                margin: "0 0 0.75rem 0",
                fontSize: "0.875rem",
                fontWeight: "600",
              }}
            >
              <FormattedMessage id="gps.manual.entry.title" />
            </h5>
            <div style={{ display: "flex", gap: "1rem" }}>
              <TextInput
                id={`gps-latitude-${index}`}
                labelText={intl.formatMessage({ id: "gps.latitude.label" })}
                placeholder={intl.formatMessage({
                  id: "gps.latitude.placeholder",
                })}
                value={latitude}
                onChange={handleLatitudeChange}
                disabled={disabled}
                invalid={latitude ? !isValidLatitude(latitude) : false}
                invalidText={intl.formatMessage({ id: "gps.latitude.invalid" })}
                helperText={intl.formatMessage({ id: "gps.latitude.helper" })}
              />

              <TextInput
                id={`gps-longitude-${index}`}
                labelText={intl.formatMessage({ id: "gps.longitude.label" })}
                placeholder={intl.formatMessage({
                  id: "gps.longitude.placeholder",
                })}
                value={longitude}
                onChange={handleLongitudeChange}
                disabled={disabled}
                invalid={longitude ? !isValidLongitude(longitude) : false}
                invalidText={intl.formatMessage({
                  id: "gps.longitude.invalid",
                })}
                helperText={intl.formatMessage({ id: "gps.longitude.helper" })}
              />
            </div>
          </div>
        )}

        {/* Actions */}
        {(hasValidCoordinates() || hasPartialCoordinates()) && (
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button
              kind="danger--tertiary"
              size="sm"
              onClick={handleClear}
              disabled={disabled}
            >
              <FormattedMessage id="gps.button.clear" />
            </Button>
          </div>
        )}
      </Layer>
    </div>
  );
};

export default GpsCoordinatesCapture;
