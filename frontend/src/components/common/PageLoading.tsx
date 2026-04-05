import React from "react";
import { Loading } from "@carbon/react";
import { useIntl } from "react-intl";

interface PageLoadingProps {
  messageId?: string;
}

export default function PageLoading({ messageId }: PageLoadingProps) {
  const intl = useIntl();
  const description = intl.formatMessage({
    id: messageId || "label.loading",
  });

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "50vh",
      }}
    >
      <Loading withOverlay={false} description={description} />
    </div>
  );
}
