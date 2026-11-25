import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  fetchFacilityLocation,
  isFailure,
  readBrowserPosition,
  sendBrowserPosition,
} from "./facilityLocationApi";

const Row = ({ labelId, children }) => (
  <StructuredListRow>
    <StructuredListCell noWrap>
      <FormattedMessage id={labelId} />
    </StructuredListCell>
    <StructuredListCell>{children}</StructuredListCell>
  </StructuredListRow>
);

/**
 * The facility's coordinates, where they came from and its registration in the
 * facility registry. In automatic mode an administrator can replace the
 * position with this device's.
 */
export default function FacilityLocationSection() {
  const intl = useIntl();
  const [status, setStatus] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;
    fetchFacilityLocation((response) => {
      if (!mounted) {
        return;
      }
      if (isFailure(response)) {
        setLoadFailed(true);
      } else {
        setStatus(response);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const useThisDevice = () => {
    setBusy(true);
    setError(null);
    readBrowserPosition(
      (latitude, longitude) =>
        sendBrowserPosition(latitude, longitude, (response) => {
          setBusy(false);
          if (isFailure(response)) {
            setError(
              response?.message ||
                intl.formatMessage({ id: "facilityLocation.error.save" }),
            );
          } else {
            setStatus(response);
          }
        }),
      (reason) => {
        setBusy(false);
        setError(
          intl.formatMessage({ id: `facilityLocation.error.${reason}` }),
        );
      },
    );
  };

  if (loadFailed) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "facilityLocation.error.load" })}
      />
    );
  }
  if (!status) {
    return null;
  }

  const automatic = status.mode === "AUTO";
  const hasPosition = status.latitude != null && status.longitude != null;

  return (
    <Tile data-testid="facility-location-section">
      <h4>
        <FormattedMessage id="facilityLocation.title" />
      </h4>
      <p>
        <FormattedMessage
          id={
            automatic
              ? "facilityLocation.mode.auto.description"
              : "facilityLocation.mode.manual.description"
          }
        />
      </p>
      <StructuredListWrapper isCondensed>
        <StructuredListBody>
          <Row labelId="facilityLocation.label.mode">
            <Tag type={automatic ? "blue" : "gray"}>
              <FormattedMessage
                id={
                  automatic
                    ? "facilityLocation.mode.auto"
                    : "facilityLocation.mode.manual"
                }
              />
            </Tag>
          </Row>
          <Row labelId="facilityLocation.label.coordinates">
            {hasPosition ? (
              <span data-testid="facility-location-coordinates">
                {status.latitude}, {status.longitude}
              </span>
            ) : (
              <FormattedMessage id="facilityLocation.coordinates.none" />
            )}
          </Row>
          {hasPosition && (
            <Row labelId="facilityLocation.label.source">
              <FormattedMessage
                id={`facilityLocation.source.${status.source}`}
              />
            </Row>
          )}
          <Row labelId="facilityLocation.label.registry">
            {status.registryConfigured ? (
              status.registryUrl
            ) : (
              <FormattedMessage id="facilityLocation.registry.none" />
            )}
          </Row>
          {status.registryConfigured &&
            status.lastRegistrationSuccess != null && (
              <Row labelId="facilityLocation.label.registration">
                <Tag
                  type={status.lastRegistrationSuccess ? "green" : "red"}
                  data-testid="facility-location-registration"
                >
                  <FormattedMessage
                    id={
                      status.lastRegistrationSuccess
                        ? "facilityLocation.registration.success"
                        : "facilityLocation.registration.failed"
                    }
                  />
                </Tag>{" "}
                {status.lastRegistrationMessage}
              </Row>
            )}
        </StructuredListBody>
      </StructuredListWrapper>
      {status.manualCoordinatesIgnored && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "facilityLocation.notice.manualIgnored",
          })}
        />
      )}
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          onClose={() => setError(null)}
          title={error}
        />
      )}
      {automatic && (
        <Button
          kind="tertiary"
          size="sm"
          disabled={busy}
          onClick={useThisDevice}
        >
          <FormattedMessage id="facilityLocation.button.useDevice" />
        </Button>
      )}
    </Tile>
  );
}
