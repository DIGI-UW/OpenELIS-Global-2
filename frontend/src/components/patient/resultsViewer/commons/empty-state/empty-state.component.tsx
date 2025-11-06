import React from "react";
import { Layer, Link, Tile } from "@carbon/react";
import { Trans, useTranslation } from "react-i18next";
import { EmptyDataIllustration } from "./empty-data-illustration.component";
import { useLayoutType } from "../utils";
//import styles from './empty-state.scss';
import "./empty-state.scss";

export interface EmptyStateProps {
  displayText: string;
  headerTitle: string;
  launchForm?(): void;
}

export const EmptyState: React.FC<EmptyStateProps> = (props) => {
  const { t } = useTranslation();
  const isTablet = useLayoutType() === "tablet";

  return (
    <Layer>
      <Tile className="tile">
        <div className={isTablet ? "tabletHeading" : "desktopHeading"}>
          <h4>{props.headerTitle}</h4>
        </div>
        <EmptyDataIllustration />
        <p className="content">
          <Trans
            i18nKey="emptyStateText"
            values={{ displayText: props.displayText.toLowerCase() }}
          >
            There are no {props.displayText.toLowerCase()} to display for this
            patient
          </Trans>
        </p>
        <p className="action">
          {props.launchForm && (
            <span>
              <Link onClick={() => props.launchForm()}>
                <>
                  {t("record", "Record")} {props.displayText.toLowerCase()}
                </>
              </Link>
            </span>
          )}
        </p>
      </Tile>
    </Layer>
  );
};
