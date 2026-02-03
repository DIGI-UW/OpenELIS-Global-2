import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Tag } from "@carbon/react";
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

export const VirologyLabSequencingPage = ({
  samples = [],
  pageData = {},
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { canAccessSequencing } = useVirologyLabPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VirologyLab Lab Technician",
    "VirologyLab Bioinformatician",
    "VirologyLab Manager",
    "VirologyLab PI",
  ];

  if (!canAccessSequencing() && !hasAnyRole(allowedRoles)) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.sequencing.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab Sequencing.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-sequencing">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.sequencing.title"
                  defaultMessage="VirologyLab Sequencing"
                />
              </h4>
              <Tag type="blue" size="sm">Stage 8</Tag>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            isLoading={isLoading}
            pageContext="virologylab-sequencing"
          />
        </Column>
      </Grid>
    </div>
  );
};

export default VirologyLabSequencingPage;