import React from "react";
import { Grid, Column, Tile, Tag } from "@carbon/react";
import { Information } from "@carbon/react/icons";
import { FormattedMessage } from "react-intl";
import "../workflow/NotebookWorkflow.css";

/**
 * GenericWorkflowPage - A fallback page component for workflow types
 * that do not yet have custom UI components.
 *
 * Renders page metadata (title, order, content) from the backend template
 * so that ANY lab workflow can display its pages without requiring
 * lab-specific React components.
 *
 * @param {Object} props
 * @param {Object} props.pageData - The notebook page data from backend
 * @param {Object} props.progress - Page progress {total, completed, percentage}
 * @param {string} props.workflowType - The workflow type from the backend template
 */
function GenericWorkflowPage({ pageData, progress, workflowType }) {
  return (
    <div className="generic-workflow-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tile className="generic-page-tile">
            <div className="generic-page-header">
              <Information size={20} />
              <span className="generic-page-info">
                <FormattedMessage
                  id="notebook.workflow.generic.info"
                  defaultMessage="This page uses the standard workflow view for the {workflowType} laboratory."
                  values={{ workflowType: workflowType || "unknown" }}
                />
              </span>
            </div>

            {pageData?.content && (
              <div
                className="generic-page-content"
                dangerouslySetInnerHTML={{ __html: pageData.content }}
              />
            )}

            <div className="generic-page-meta">
              <Tag type="blue">
                <FormattedMessage
                  id="notebook.workflow.generic.step"
                  defaultMessage="Step {order}"
                  values={{ order: pageData?.order || 1 }}
                />
              </Tag>
              {workflowType && (
                <Tag type="teal">
                  <FormattedMessage
                    id="notebook.workflow.generic.type"
                    defaultMessage="Workflow: {type}"
                    values={{ type: workflowType }}
                  />
                </Tag>
              )}
            </div>

            {progress && progress.total > 0 && (
              <div className="generic-page-progress">
                <FormattedMessage
                  id="notebook.workflow.generic.progress"
                  defaultMessage="{completed} of {total} samples completed"
                  values={{
                    completed: progress.completed || 0,
                    total: progress.total || 0,
                  }}
                />
              </div>
            )}
          </Tile>
        </Column>
      </Grid>
    </div>
  );
}

export default GenericWorkflowPage;
