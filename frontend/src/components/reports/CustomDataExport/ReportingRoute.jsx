import React from "react";
import { Redirect, useLocation } from "react-router-dom";
import CustomDataExport from "./CustomDataExport";
import { CUSTOM_DATA_EXPORT_PATH } from "./routes";

export default function ReportingRoute() {
  const location = useLocation();
  return location.pathname === CUSTOM_DATA_EXPORT_PATH
    ? <CustomDataExport />
    : <Redirect to={{ ...location, pathname: CUSTOM_DATA_EXPORT_PATH }} />;
}
