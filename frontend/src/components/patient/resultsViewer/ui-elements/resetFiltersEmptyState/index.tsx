import React from "react";
import { Layer, Tile } from "@carbon/react";
import { useTranslation } from "react-i18next";
//import styles from './index.scss';
import "./index.scss";
import FilterEmptyDataIllustration from "./filter-empty-data-illustration";

export interface EmptyStateProps {
  clearFilter(): void;
}

export const FilterEmptyState: React.FC<EmptyStateProps> = (props) => {
  const { t } = useTranslation();

  return (
    <Layer>
      <Tile className="tile">
        <FilterEmptyDataIllustration />
        <p className="content">
          {t("noResultsToDisplay", "No results to display")}
        </p>
        {props.clearFilter && (
          <p className="action">
            <span
              className="link"
              role="button"
              tabIndex={0}
              onClick={() => props.clearFilter()}
            >
              {t("clearFilters", "Clear filters")}
            </span>{" "}
            <span>{t("resetView", "to reset this view.")}</span>
          </p>
        )}
      </Tile>
    </Layer>
  );
};
