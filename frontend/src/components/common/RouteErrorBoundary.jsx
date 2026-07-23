import React from "react";
import { injectIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import { Button, Layer, Tile } from "@carbon/react";

/**
 * Catches render errors for a single route subtree so the rest of the app keeps working.
 * Copy uses react-intl message ids from `frontend/src/languages/*.json`.
 */
import PropTypes from "prop-types";

const propTypes = {
  intl: PropTypes.object.isRequired,
  children: PropTypes.node,
  titleKey: PropTypes.string,
  messageKey: PropTypes.string,
  resetKey: PropTypes.string,
  onReload: PropTypes.func,
};

const defaultProps = {
  titleKey: "errorBoundary.default.title",
  messageKey: "errorBoundary.default.message",
  onReload: undefined,
};

class RouteErrorBoundaryClass extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidUpdate(prevProps) {
    const { resetKey } = this.props;
    if (
      resetKey !== undefined &&
      resetKey !== prevProps.resetKey &&
      this.state.hasError
    ) {
      this.setState({ hasError: false });
    }
  }

  componentDidCatch(error, errorInfo) {
    console.error("RouteErrorBoundary caught an error", error, errorInfo);
  }

  handleReload = () => {
    const { onReload } = this.props;
    if (onReload) {
      onReload();
    } else {
      window.location.reload();
    }
  };

  render() {
    const { hasError } = this.state;
    const { intl, children, titleKey, messageKey } = this.props;

    if (hasError) {
      return (
        <Layer>
          <Tile style={{ maxWidth: "32rem", margin: "2rem" }}>
            <h2>{intl.formatMessage({ id: titleKey })}</h2>
            <p>{intl.formatMessage({ id: messageKey })}</p>
            <Button kind="primary" onClick={this.handleReload}>
              {intl.formatMessage({ id: "errorBoundary.reload" })}
            </Button>
          </Tile>
        </Layer>
      );
    }

    return children;
  }
}

RouteErrorBoundaryClass.propTypes = propTypes;
RouteErrorBoundaryClass.defaultProps = defaultProps;

const RouteErrorBoundary = injectIntl(RouteErrorBoundaryClass);

function RouteErrorBoundaryWithLocation(props) {
  const location = useLocation();
  const resetKey = `${location.pathname}${location.search}`;
  return <RouteErrorBoundary {...props} resetKey={resetKey} />;
}

export { RouteErrorBoundary };
export default RouteErrorBoundaryWithLocation;
