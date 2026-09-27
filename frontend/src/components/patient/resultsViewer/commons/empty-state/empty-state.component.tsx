import React from "react";
import { Layer, Link, Tile } from "@carbon/react";
import { FormattedMessage } from "react-intl";
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
  const isTablet = useLayoutType() === "tablet";

  return (
    <Layer>
      <Tile className="tile">
        <div className={isTablet ? "tabletHeading" : "desktopHeading"}>
          <h4>{props.headerTitle}</h4>
        </div>
        <EmptyDataIllustration />
        <p className="content">
          <FormattedMessage
            id="label.patientHistory.emptyState"
            values={{ displayText: props.displayText.toLowerCase() }}
          />
        </p>
        <p className="action">
          {props.launchForm && (
            <span>
              <Link onClick={() => props.launchForm()}>
                <FormattedMessage
                  id="label.patientHistory.record"
                  values={{ displayText: props.displayText.toLowerCase() }}
                />
              </Link>
            </span>
          )}
        </p>
      </Tile>
    </Layer>
  );
};
