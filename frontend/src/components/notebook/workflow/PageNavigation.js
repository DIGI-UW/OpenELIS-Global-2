import React from "react";
import { FormattedMessage } from "react-intl";
import { Tag } from "@carbon/react";
import { Checkmark, InProgress, CircleDash } from "@carbon/react/icons";
import "./NotebookWorkflow.css";

/**
 * PageNavigation - Displays the 9 workflow pages with progress indicators.
 *
 * @param {Object} props
 * @param {Array} props.pages - Array of notebook pages
 * @param {number} props.activePage - Currently active page index
 * @param {function} props.onPageChange - Callback when page is selected
 * @param {Object} props.pageProgress - Map of page ID to progress object
 */
function PageNavigation({ pages, activePage, onPageChange, pageProgress }) {
  const getProgressStatus = (pageId) => {
    const progress = pageProgress[pageId];
    if (!progress) {
      return "pending";
    }

    if (progress.completed === progress.total && progress.total > 0) {
      return "complete";
    } else if (progress.inProgress > 0 || progress.completed > 0) {
      return "current";
    }
    return "pending";
  };

  const getProgressPercentage = (pageId) => {
    const progress = pageProgress[pageId];
    if (!progress || progress.total === 0) {
      return 0;
    }
    return Math.round((progress.completed / progress.total) * 100);
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "complete":
        return <Checkmark size={16} className="status-icon complete" />;
      case "current":
        return <InProgress size={16} className="status-icon in-progress" />;
      default:
        return <CircleDash size={16} className="status-icon pending" />;
    }
  };

  /**
   * T149: Get color-coded tag based on percentage ranges
   * - 0%: gray (not started)
   * - 1-24%: red (critical - just started)
   * - 25-49%: orange (low progress)
   * - 50-74%: yellow (moderate progress)
   * - 75-99%: teal (good progress)
   * - 100%: green (complete)
   */
  const getPercentageTagType = (percentage) => {
    if (percentage === 0) return "gray";
    if (percentage === 100) return "green";
    if (percentage < 25) return "red";
    if (percentage < 50) return "orange";
    if (percentage < 75) return "yellow";
    return "teal"; // 75-99%
  };

  const getStatusTag = (status, percentage) => {
    // Use percentage-based coloring for all states
    const tagType = getPercentageTagType(percentage);
    return <Tag type={tagType}>{percentage}%</Tag>;
  };

  const handlePageClick = (index) => {
    if (onPageChange) {
      onPageChange(index);
    }
  };

  return (
    <div className="page-navigation">
      <h4 className="navigation-title">
        <FormattedMessage
          id="notebook.workflow.pages"
          defaultMessage="Workflow Pages"
        />
      </h4>

      <div className="page-list">
        {pages.map((page, index) => {
          const status = getProgressStatus(page.id);
          const percentage = getProgressPercentage(page.id);
          const isActive = index === activePage;

          return (
            <div
              key={page.id}
              role="button"
              aria-pressed={isActive}
              className={`page-item ${isActive ? "active" : ""} ${status}`}
              onClick={() => handlePageClick(index)}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  handlePageClick(index);
                }
              }}
              tabIndex={0}
            >
              <div className="page-status-cell">{getStatusIcon(status)}</div>
              <div className="page-title-cell">
                <span className="page-number">{index + 1}.</span>
                <span className="page-title">{page.title}</span>
              </div>
              <div className="page-progress-cell">
                {getStatusTag(status, percentage)}
              </div>
            </div>
          );
        })}
      </div>

      <div className="navigation-summary">
        <FormattedMessage
          id="notebook.workflow.pagesCompleted"
          defaultMessage="{completed} of {total} pages completed"
          values={{
            completed: pages.filter(
              (p) => getProgressStatus(p.id) === "complete",
            ).length,
            total: pages.length,
          }}
        />
      </div>
    </div>
  );
}

export default PageNavigation;
