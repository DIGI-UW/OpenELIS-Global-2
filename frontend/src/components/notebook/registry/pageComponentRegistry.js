/**
 * Page Component Registry
 *
 * Maps page component keys from pageRegistry.json to actual React components.
 * This allows workflows to reference pages by ID and dynamically render
 * the appropriate component.
 *
 * Usage:
 *   import { getPageComponent, isPageAvailable } from './pageComponentRegistry';
 *
 *   const PageComponent = getPageComponent('sample-reception');
 *   if (PageComponent) {
 *     return <PageComponent entryId={entryId} pageData={pageData} />;
 *   }
 */

import React from "react";

// Import all available page components
import SampleReceptionPage from "../pages/SampleReceptionPage";
import InitialProcessingPage from "../pages/InitialProcessingPage";
import AssaysPage from "../pages/AssaysPage";
import ChildSampleCreationPage from "../pages/ChildSampleCreationPage";
import SampleRoutingPage from "../pages/SampleRoutingPage";
import PrepPage from "../pages/PrepPage";
import AnalysisPage from "../pages/AnalysisPage";
import StoragePage from "../pages/StoragePage";
import ResultCompilationPage from "../pages/ResultCompilationPage";
import EndOfProjectArchivingPage from "../pages/EndOfProjectArchivingPage";
import PatientOrderEntryPage from "../pages/PatientOrderEntryPage";
import SampleCollectionPage from "../pages/SampleCollectionPage";
import QualityCheckPage from "../pages/QualityCheckPage";
import ResultEntryPage from "../pages/ResultEntryPage";
import ResultVerificationPage from "../pages/ResultVerificationPage";
import ReportingPage from "../pages/ReportingPage";
import TransportPackagingPage from "../pages/TransportPackagingPage";
import SampleStoragePage from "../pages/SampleStoragePage";
import SampleProcessingPage from "../pages/SampleProcessingPage";
import TestingAnalyzerPage from "../pages/TestingAnalyzerPage";
import ValidationReportingPage from "../pages/ValidationReportingPage";

// Import MedLab-specific page components
import {
  MedLabSampleRoutingPage,
  MedLabSampleProcessingPage,
  MedLabQualityCheckPage,
  MedLabChildSampleCreationPage,
} from "../pages/medlab";

// Import registry data
import pageRegistry from "./pageRegistry.json";
import workflowDefinitions from "./workflowDefinitions.json";

/**
 * Component registry mapping componentKey to actual React component.
 * Add new page components here as they are created.
 */
const componentRegistry = {
  SampleReceptionPage,
  InitialProcessingPage,
  AssaysPage,
  ChildSampleCreationPage,
  SampleRoutingPage,
  PrepPage,
  AnalysisPage,
  StoragePage,
  ResultCompilationPage,
  EndOfProjectArchivingPage,
  PatientOrderEntryPage,
  SampleCollectionPage,
  QualityCheckPage,
  ResultEntryPage,
  ResultVerificationPage,
  ReportingPage,
  TransportPackagingPage,
  // SampleStoragePage for MedLab workflow with environmental monitoring
  SampleStoragePage,
  // SampleProcessingPage for MedLab workflow with aliquoting
  SampleProcessingPage,
  // TestingAnalyzerPage for MedLab workflow with QC and deviation handling
  TestingAnalyzerPage,
  // ValidationReportingPage for MedLab workflow with validation, reporting, and performance monitoring
  ValidationReportingPage,
  // MedLab-specific page components (duplicated from shared pages)
  MedLabSampleRoutingPage,
  MedLabSampleProcessingPage,
  MedLabQualityCheckPage,
  MedLabChildSampleCreationPage,
  // Placeholder components for pages not yet implemented
  CentrifugationPage: createPlaceholderPage("Centrifugation"),
  AliquotingPage: createPlaceholderPage("Aliquoting"),
  AnalyzerLoadingPage: createPlaceholderPage("Analyzer Loading"),
  QCReviewPage: createPlaceholderPage("QC Review"),
};

/**
 * Creates a placeholder component for pages not yet implemented.
 * @param {string} pageName - Display name for the placeholder
 * @returns {React.Component} Placeholder component
 */
function createPlaceholderPage(pageName) {
  return function PlaceholderPage({ entryId, pageData, progress }) {
    return (
      <div
        className="page-placeholder"
        style={{ padding: "2rem", textAlign: "center" }}
      >
        <h4 style={{ marginBottom: "1rem", color: "#525252" }}>{pageName}</h4>
        <p style={{ color: "#6f6f6f" }}>
          This page is not yet implemented. Component will be added in a future
          update.
        </p>
        {pageData?.instructions && (
          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <p style={{ fontStyle: "italic", color: "#525252" }}>
              {pageData.instructions}
            </p>
          </div>
        )}
      </div>
    );
  };
}

/**
 * Gets page metadata from the registry by page ID.
 * @param {string} pageId - The page ID from pageRegistry.json
 * @returns {Object|null} Page metadata or null if not found
 */
export function getPageMetadata(pageId) {
  return pageRegistry.pages[pageId] || null;
}

/**
 * Gets the React component for a page by its ID.
 * @param {string} pageId - The page ID from pageRegistry.json
 * @returns {React.Component|null} The React component or null if not found
 */
export function getPageComponent(pageId) {
  const metadata = getPageMetadata(pageId);
  if (!metadata) {
    console.warn(`Page not found in registry: ${pageId}`);
    return null;
  }

  const component = componentRegistry[metadata.componentKey];
  if (!component) {
    console.warn(
      `Component not found for page: ${pageId} (key: ${metadata.componentKey})`,
    );
    return createPlaceholderPage(metadata.defaultTitle);
  }

  return component;
}

/**
 * Checks if a page component is available (implemented).
 * @param {string} pageId - The page ID
 * @returns {boolean} True if the component is available
 */
export function isPageAvailable(pageId) {
  const metadata = getPageMetadata(pageId);
  if (!metadata) return false;

  const component = componentRegistry[metadata.componentKey];
  // Check if it's a real component, not a placeholder
  return component && !component.name?.startsWith("PlaceholderPage");
}

/**
 * Gets all registered page IDs.
 * @returns {string[]} Array of page IDs
 */
export function getAllPageIds() {
  return Object.keys(pageRegistry.pages);
}

/**
 * Gets pages by category.
 * @param {string} category - Category name (e.g., 'sample-management', 'testing')
 * @returns {Object[]} Array of page metadata objects
 */
export function getPagesByCategory(category) {
  return Object.values(pageRegistry.pages).filter(
    (page) => page.category === category,
  );
}

/**
 * Gets a workflow definition by ID.
 * @param {string} workflowId - The workflow ID (e.g., 'immunology', 'medlab')
 * @returns {Object|null} Workflow definition or null if not found
 */
export function getWorkflowDefinition(workflowId) {
  return workflowDefinitions.workflows[workflowId] || null;
}

/**
 * Gets all available workflow IDs.
 * @returns {string[]} Array of workflow IDs
 */
export function getAllWorkflowIds() {
  return Object.keys(workflowDefinitions.workflows);
}

/**
 * Builds the effective pages array for a workflow.
 * Combines workflow definition with page registry metadata.
 *
 * @param {string} workflowId - The workflow ID
 * @returns {Object[]} Array of page objects with full metadata
 */
export function buildWorkflowPages(workflowId) {
  const workflow = getWorkflowDefinition(workflowId);
  if (!workflow) {
    console.warn(`Workflow not found: ${workflowId}`);
    return [];
  }

  return workflow.pages.map((workflowPage) => {
    const pageMetadata = getPageMetadata(workflowPage.pageId);
    if (!pageMetadata) {
      console.warn(`Page not found in registry: ${workflowPage.pageId}`);
      return {
        id: `${workflowId}-${workflowPage.order}`,
        order: workflowPage.order,
        title: workflowPage.titleOverride || workflowPage.pageId,
        pageId: workflowPage.pageId,
        available: false,
      };
    }

    return {
      id:
        workflowPage.instanceId ||
        `${workflowId}-${workflowPage.pageId}-${workflowPage.order}`,
      order: workflowPage.order,
      title: workflowPage.titleOverride || pageMetadata.defaultTitle,
      titleKey: pageMetadata.titleKey,
      instructions: pageMetadata.defaultDescription,
      pageId: workflowPage.pageId,
      componentKey: pageMetadata.componentKey,
      category: pageMetadata.category,
      capabilities: pageMetadata.capabilities,
      required: workflowPage.required,
      additionalComponents: workflowPage.additionalComponents || [],
      available: isPageAvailable(workflowPage.pageId),
    };
  });
}

/**
 * Renders a page component with the given props.
 * Handles additional components (like routing combined with child samples).
 *
 * @param {Object} pageConfig - Page configuration from buildWorkflowPages
 * @param {Object} props - Props to pass to the component (entryId, pageData, progress, etc.)
 * @returns {React.Element|null} Rendered component or null
 */
export function renderPageComponent(pageConfig, props) {
  const Component = getPageComponent(pageConfig.pageId);
  if (!Component) return null;

  const elements = [
    <Component
      key={`page-${pageConfig.id}`}
      {...props}
      pageData={{ ...props.pageData, ...pageConfig }}
    />,
  ];

  // Render additional components if specified
  if (
    pageConfig.additionalComponents &&
    pageConfig.additionalComponents.length > 0
  ) {
    pageConfig.additionalComponents.forEach((additionalPageId, index) => {
      const AdditionalComponent = getPageComponent(additionalPageId);
      if (AdditionalComponent) {
        elements.push(
          <div
            key={`additional-${additionalPageId}-${index}`}
            className="additional-component-section"
            style={{ marginTop: "2rem" }}
          >
            <AdditionalComponent
              {...props}
              pageData={{ ...props.pageData, pageId: additionalPageId }}
            />
          </div>,
        );
      }
    });
  }

  return elements.length === 1 ? (
    elements[0]
  ) : (
    <React.Fragment>{elements}</React.Fragment>
  );
}

// Export the raw registries for advanced use cases
export { pageRegistry, workflowDefinitions };

export default {
  getPageMetadata,
  getPageComponent,
  isPageAvailable,
  getAllPageIds,
  getPagesByCategory,
  getWorkflowDefinition,
  getAllWorkflowIds,
  buildWorkflowPages,
  renderPageComponent,
};
