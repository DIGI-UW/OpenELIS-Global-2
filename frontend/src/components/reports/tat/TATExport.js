import React from "react";
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";
import { useIntl } from "react-intl";

function TATExport({ filters, buildQueryString }) {
  const intl = useIntl();

  const handleExport = (format) => {
    const qs = buildQueryString(filters, `&format=${format}`);
    window.open(`/rest/reports/tat/export?${qs}`, "_blank");
  };

  return (
    <OverflowMenu
      renderIcon={() => intl.formatMessage({ id: "reports.tat.export" })}
      flipped
    >
      <OverflowMenuItem
        itemText={intl.formatMessage({ id: "reports.tat.exportCsv" })}
        onClick={() => handleExport("CSV")}
      />
      <OverflowMenuItem
        itemText={intl.formatMessage({ id: "reports.tat.exportPdf" })}
        onClick={() => handleExport("PDF")}
      />
    </OverflowMenu>
  );
}

export default TATExport;
