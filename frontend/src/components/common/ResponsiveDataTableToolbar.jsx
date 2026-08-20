import React from "react";
import { TableToolbar } from "@carbon/react";
import "./responsiveDataTableToolbar.scss";

export const ResponsiveBatchActionLabel = ({ children }) => (
  <span className="oe-responsive-data-table-toolbar__batch-action-label">
    {children}
  </span>
);

const ResponsiveDataTableToolbar = ({
  batchActive = false,
  children,
  ...toolbarProps
}) => (
  <div
    className={`oe-responsive-data-table-toolbar${
      batchActive ? " oe-responsive-data-table-toolbar--batch" : ""
    }`}
  >
    <TableToolbar {...toolbarProps}>{children}</TableToolbar>
  </div>
);

export default ResponsiveDataTableToolbar;
