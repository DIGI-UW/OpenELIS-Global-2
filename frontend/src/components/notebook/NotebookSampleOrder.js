import React from "react";
import { useParams, useHistory } from "react-router-dom";
import { FormattedMessage } from "react-intl";
import GenericSampleOrder from "../genericSample/GenericSampleOrder";

/**
 * NotebookSampleOrder - Sample order entry component for notebooks
 *
 * This component wraps GenericSampleOrder with notebook-specific configuration:
 * - Pre-selects the notebook based on URL parameter
 * - Uses notebook-specific breadcrumbs
 * - Redirects back to notebook after successful save
 * - Hides notebook selection (since it's already selected via URL)
 *
 * Routes:
 * - /NotebookSampleOrder/:notebookId - Create sample for specific notebook
 * - /NotebookSampleOrder/:notebookId/:notebookEntryId - Create sample for notebook instance
 */
export default function NotebookSampleOrder() {
  const { notebookId, notebookEntryId } = useParams();
  const history = useHistory();

  // Build breadcrumbs based on context
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "notebook.label", link: "/NoteBookDashboard" },
    notebookEntryId
      ? {
          label: "notebook.instance.label",
          link: `/NoteBookInstanceEditForm/${notebookEntryId}`,
        }
      : {
          label: "notebook.template.label",
          link: `/NoteBookEntryForm/${notebookId}`,
        },
    { label: "notebook.sample.order.label" },
  ];

  // Handle successful save - redirect back to notebook
  const handleSaveSuccess = (data) => {
    // Navigate back to the notebook entry form with the samples tab active
    if (notebookEntryId) {
      history.push(`/NoteBookInstanceEditForm/${notebookEntryId}?tab=samples`);
    } else if (notebookId) {
      history.push(`/NoteBookInstanceEntryForm/${notebookId}?tab=samples`);
    } else {
      history.push("/NoteBookDashboard");
    }
  };

  return (
    <GenericSampleOrder
      title="notebook.sample.order.title"
      titleDefault="Create Sample for Notebook"
      breadcrumbs={breadcrumbs}
      // Hide notebook selection since we're already in a notebook context
      showNotebookSelection={false}
      // Show all standard sample fields
      showSampleType={true}
      showQuantity={true}
      showUom={true}
      showFrom={true}
      showCollector={true}
      showCollectionDate={true}
      showCollectionTime={true}
      // Show questionnaire if notebook has one associated
      showQuestionnaire={true}
      // Use notebook-specific save endpoint if needed
      saveEndpoint="/rest/GenericSampleOrder"
      // Initial values can include notebook reference
      initialValues={{
        notebookId: notebookId ? parseInt(notebookId) : null,
        notebookEntryId: notebookEntryId ? parseInt(notebookEntryId) : null,
      }}
      // Callbacks
      onSaveSuccess={handleSaveSuccess}
      // Custom content to show notebook info
      renderCustomContent={(formData, updateField) => (
        <div
          style={{
            marginBottom: "1rem",
            padding: "0.5rem",
            backgroundColor: "#f4f4f4",
            borderRadius: "4px",
          }}
        >
          <p style={{ margin: 0, fontSize: "0.875rem" }}>
            <FormattedMessage
              id="notebook.sample.order.info"
              defaultMessage="This sample will be associated with the current notebook."
            />
          </p>
        </div>
      )}
    />
  );
}
