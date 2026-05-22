import React from "react";

const isPresent = (value) => typeof value === "string" && value.trim() !== "";

export const getPhoneFormatHint = (intl, configurationProperties = {}) => {
  const localFormat =
    configurationProperties.PHONE_FORMAT_LABEL ||
    configurationProperties.PHONE_FORMAT ||
    "";
  const internationalValidation =
    configurationProperties.PHONE_INTERNATIONAL_VALIDATION || "";
  const isInternationalValidationEnabled =
    internationalValidation.toUpperCase() === "E164";
  const internationalFormat =
    isInternationalValidationEnabled &&
    (configurationProperties.PHONE_INTERNATIONAL_FORMAT_LABEL ||
      "+CC XXXXXXXX");
  const localLabel = intl.formatMessage({
    id: "phone.format.local.label",
    defaultMessage: "Local",
  });
  const internationalLabel = intl.formatMessage({
    id: "phone.format.international.label",
    defaultMessage: "International",
  });

  return (
    <>
      <span>
        {localLabel}: {localFormat}
      </span>
      {isPresent(internationalFormat) && (
        <>
          <br />
          <span>
            {internationalLabel}: {internationalFormat}
          </span>
        </>
      )}
    </>
  );
};
