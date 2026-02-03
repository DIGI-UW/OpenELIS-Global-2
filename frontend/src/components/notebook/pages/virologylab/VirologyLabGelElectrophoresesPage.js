import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Tag } from "@carbon/react";
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

export const VirologyLabGelElectrophoresesPage = ({
  samples = [],
  pageData = {},
  onSampleUpdate,
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { canAccessGelElectrophoresis } = useVirologyLabPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VirologyLab Lab Technician",
    "VirologyLab Manager",
    "VirologyLab PI",
  ];

  if (!canAccessGelElectrophoresis() && !hasAnyRole(allowedRoles)) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.gelElectrophoresis.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab Gel Electrophoresis.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-gel-electrophoresis">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.gelElectrophoresis.title"
                  defaultMessage="VirologyLab Gel Electrophoresis"
                />
              </h4>
              <Tag type="blue" size="sm">Stage 5</Tag>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            isLoading={isLoading}
            pageContext="virologylab-gel-electrophoresis"
            onSampleUpdate={onSampleUpdate}
          />
        </Column>
      </Grid>
    </div>
  );
};

export default VirologyLabGelElectrophoresesPage;