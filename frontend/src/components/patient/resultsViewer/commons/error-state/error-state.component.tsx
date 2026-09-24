import React from "react";
import { Layer, Tile } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useLayoutType } from "../utils";
//import styles from './error-state.scss';
import "./error-state.scss";

export interface ErrorStateProps {
  error: any;
  headerTitle: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  error,
  headerTitle,
}) => {
  const isTablet = useLayoutType() === "tablet";

  return (
    <Layer>
      <Tile className="tile">
        <div className={isTablet ? "tabletHeading" : "desktopHeading"}>
          <h4>{headerTitle}</h4>
        </div>
        <p className="errorMessage">
          <FormattedMessage id="label.error" /> {`${error?.response?.status}: `}
          {error?.response?.statusText}
        </p>
        <p className="errorCopy">
          <FormattedMessage id="label.patientHistory.errorCopy" />
        </p>
      </Tile>
    </Layer>
  );
};
