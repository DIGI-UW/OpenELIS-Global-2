import React from "react";
import { Button, Layer, Tile } from "@carbon/react";
import { FormattedMessage } from "react-intl";

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    const { hasError } = this.state;
    const { children } = this.props;

    if (!hasError) return children;

    return (
      <Layer>
        <Tile className="tile" data-testid="error-boundary-fallback">
          <h4>
            <FormattedMessage id="errorBoundary.title" />
          </h4>
          <p>
            <FormattedMessage id="errorBoundary.message" />
          </p>
          <Button kind="primary" onClick={this.handleReload}>
            <FormattedMessage id="errorBoundary.reload" />
          </Button>
        </Tile>
      </Layer>
    );
  }
}

export default ErrorBoundary;
