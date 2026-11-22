import React from "react";
import { ActionableNotification, Loading } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * The full-screen state for a required query read.  Keeping it at the query
 * boundary prevents a failed initial request from being rendered forever as
 * a loading indicator, and gives the user a direct retry action.
 */
const ServerDataState = ({ query }) => {
  const intl = useIntl();

  if (!query.isError) {
    return <Loading />;
  }

  return (
    <ActionableNotification
      kind="error"
      inline
      lowContrast
      title={intl.formatMessage({ id: "server.error.msg" })}
      actionButtonLabel={intl.formatMessage({ id: "common.retry" })}
      onActionButtonClick={() => query.refetch()}
      hideCloseButton
      statusIconDescription={intl.formatMessage({ id: "notification.title" })}
    />
  );
};

export default ServerDataState;
