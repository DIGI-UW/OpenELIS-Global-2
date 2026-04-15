import React from "react";
import { Layer, Tile } from "@carbon/react";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
  const isTablet = useLayoutType() === "tablet";
  const errorStatus = error?.response?.status;
  const errorMessage = error?.response?.statusText || error?.message || "";

  return (
    <Layer>
      <Tile className="tile">
        <div className={isTablet ? "tabletHeading" : "desktopHeading"}>
          <h4>{headerTitle}</h4>
        </div>
        <p className="errorMessage">
          {t("error", "Error")} {errorStatus ? `${errorStatus}: ` : ""}
          {errorMessage}
        </p>
        <p className="errorCopy">
          {t(
            "errorCopy",
            "Sorry, there was a problem displaying this information. You can try to reload this page, or contact the site administrator and quote the error code above.",
          )}
        </p>
      </Tile>
    </Layer>
  );
};
