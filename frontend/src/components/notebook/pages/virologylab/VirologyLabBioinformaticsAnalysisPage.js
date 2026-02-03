import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Tag } from "@carbon/react";
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

export const VirologyLabBioinformaticsAnalysisPage = ({
  samples = [],
  pageData = {},
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { canAccessBioinformaticsAnalysis } = useVirologyLabPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VirologyLab Bioinformatician",
    "VirologyLab Manager",
    "VirologyLab PI",
    "VirologyLab Data Manager",
  ];

  if (!canAccessBioinformaticsAnalysis() && !hasAnyRole(allowedRoles)) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.bioinformatics.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab Bioinformatics Analysis.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-bioinformatics">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.bioinformatics.title"
                  defaultMessage="VirologyLab Bioinformatics Analysis & Data Submission"
                />
              </h4>
              <Tag type="blue" size="sm">Stage 9</Tag>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            isLoading={isLoading}
            pageContext="virologylab-bioinformatics"
          />
        </Column>
      </Grid>
    </div>
  );
};

export default VirologyLabBioinformaticsAnalysisPage;