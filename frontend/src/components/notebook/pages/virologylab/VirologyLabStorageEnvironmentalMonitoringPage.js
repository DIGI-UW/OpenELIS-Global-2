import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Tag } from "@carbon/react";
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

export const VirologyLabStorageEnvironmentalMonitoringPage = ({
  samples = [],
  pageData = {},
  entryId,
  notebookId,
  onProgressUpdate,
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { canAccessSampleStorage } = useVirologyLabPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VirologyLab Lab Technician",
    "VirologyLab Manager",
    "VirologyLab PI",
    "VirologyLab Data Manager",
  ];

  if (!canAccessSampleStorage() && !hasAnyRole(allowedRoles)) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.storageMonitoring.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab Storage & Environmental Monitoring.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-storage-monitoring">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.storageMonitoring.title"
                  defaultMessage="VirologyLab Storage & Environmental Monitoring"
                />
              </h4>
              <Tag type="blue" size="sm">Stage 10</Tag>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            isLoading={isLoading}
            pageContext="virologylab-storage-monitoring"
            entryId={entryId}
          />
        </Column>
      </Grid>
    </div>
  );
};

export default VirologyLabStorageEnvironmentalMonitoringPage;