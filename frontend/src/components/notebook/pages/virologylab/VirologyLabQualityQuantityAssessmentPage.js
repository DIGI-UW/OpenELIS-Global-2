import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Tag } from "@carbon/react";
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

export const VirologyLabQualityQuantityAssessmentPage = ({
  samples = [],
  pageData = {},
  onSampleUpdate,
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { canAccessQualityQuantityAssessment } = useVirologyLabPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VirologyLab Lab Technician",
    "VirologyLab Manager",
    "VirologyLab PI",
  ];

  if (!canAccessQualityQuantityAssessment() && !hasAnyRole(allowedRoles)) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.qualityAssessment.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab Quality & Quantity Assessment.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-quality-assessment">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.qualityAssessment.title"
                  defaultMessage="VirologyLab Quality & Quantity Assessment"
                />
              </h4>
              <Tag type="blue" size="sm">Stage 3</Tag>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            isLoading={isLoading}
            pageContext="virologylab-quality-assessment"
            onSampleUpdate={onSampleUpdate}
          />
        </Column>
      </Grid>
    </div>
  );
};

export default VirologyLabQualityQuantityAssessmentPage;