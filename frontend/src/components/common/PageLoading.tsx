import React from "react";
import { Loading } from "@carbon/react";

interface PageLoadingProps {
  message?: string;
}

export default function PageLoading({ message }: PageLoadingProps) {
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
      <Loading withOverlay={false} description={message || "Loading..."} />
    </div>
  );
}
