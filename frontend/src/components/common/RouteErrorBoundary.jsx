import React from "react";
import { withTranslation } from "react-i18next";
import { Button, Layer, Tile } from "@carbon/react";

/**
 * Catches render errors for a single route subtree so the rest of the app keeps working.
 * Strings use react-i18next (`t`) with English defaults until resources are fully migrated.
 */
class RouteErrorBoundaryClass extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error("RouteErrorBoundary caught an error", error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    const { hasError } = this.state;
    const { t, children, titleKey, messageKey, titleDefault, messageDefault } =
      this.props;

    if (hasError) {
      return (
        <Layer>
          <Tile style={{ maxWidth: "32rem", margin: "2rem" }}>
            <h2>{t(titleKey, titleDefault)}</h2>
            <p>{t(messageKey, messageDefault)}</p>
            <Button kind="primary" onClick={this.handleReload}>
              {t("errorBoundary.reload", "Reload")}
            </Button>
          </Tile>
        </Layer>
      );
    }

    return children;
  }
}

const RouteErrorBoundary = withTranslation()(RouteErrorBoundaryClass);

export default RouteErrorBoundary;
