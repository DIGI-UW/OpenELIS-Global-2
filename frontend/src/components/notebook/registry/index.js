/**
 * Notebook Page Registry
 *
 * This module provides a dynamic registry system for notebook workflow pages.
 * Instead of hardcoding page references, workflows reference pages by unique IDs
 * from the registry, enabling:
 *
 * - Reusable page components across multiple workflows
 * - Easy addition of new pages without modifying workflow code
 * - Workflow definitions stored as configuration
 * - Dynamic page rendering based on workflow type
 *
 * Structure:
 * - pageRegistry.json: All available pages with metadata
 * - workflowDefinitions.json: Workflow templates referencing page IDs
 * - pageComponentRegistry.js: Maps page IDs to React components
 */

export {
  getPageMetadata,
  getPageComponent,
  isPageAvailable,
  getAllPageIds,
  getPagesByCategory,
  getWorkflowDefinition,
  getAllWorkflowIds,
  buildWorkflowPages,
  renderPageComponent,
  pageRegistry,
  workflowDefinitions,
} from "./pageComponentRegistry";

export { default as pageComponentRegistry } from "./pageComponentRegistry";
