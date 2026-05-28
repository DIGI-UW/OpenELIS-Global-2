import React from "react";
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";
import { Download } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import config from "../../../config.json";

function TATExport({ filters, buildQueryString }) {
  const intl = useIntl();

  const handleExport = (format) => {
    const qs = buildQueryString(filters, `&format=${format}`);
    window.open(`${config.serverBaseUrl}/rest/reports/tat/export?${qs}`, "_blank");
  };

  return (
    <OverflowMenu
      renderIcon={Download}
      menuButtonLabel={intl.formatMessage({ id: "reports.tat.export" })}
      flipped
    >
      <OverflowMenuItem
        itemText={intl.formatMessage({ id: "reports.tat.exportCsv" })}
        onClick={() => handleExport("CSV")}
      />
    </OverflowMenu>
  );
}

export default TATExport;
