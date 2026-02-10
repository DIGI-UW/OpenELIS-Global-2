import React, { useState } from "react";
import { Button } from "@carbon/react";
import { useIntl } from "react-intl";
import config from "../../../config.json";

const PerformanceReport = ({ distributionId }) => {
  const intl = useIntl();
  const [downloading, setDownloading] = useState(false);

  const handleGenerateReport = () => {
    setDownloading(true);
    const url = `${config.serverBaseUrl}/rest/eqa/distributions/${distributionId}/report`;

    fetch(url, {
      credentials: "include",
      method: "GET",
    })
      .then((response) => {
        if (response.ok) {
          return response.blob();
        }
        throw new Error("Failed to generate report");
      })
      .then((blob) => {
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = downloadUrl;
        a.download = `eqa-report-${distributionId}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(downloadUrl);
      })
      .finally(() => {
        setDownloading(false);
      });
  };

  return (
    <Button onClick={handleGenerateReport} disabled={downloading}>
      {downloading
        ? intl.formatMessage({ id: "eqa.results.report.downloading" })
        : intl.formatMessage({ id: "eqa.results.report.generate" })}
    </Button>
  );
};

export default PerformanceReport;
