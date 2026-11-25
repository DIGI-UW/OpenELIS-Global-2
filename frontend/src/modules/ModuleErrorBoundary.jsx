import React from "react";
import { Button, InlineNotification } from "@carbon/react";
import { injectIntl } from "react-intl";

/**
 * Keeps a broken module page or slot contribution from taking the shell
 * down: renders a notification naming the module, with a retry that remounts
 * the children (a failed bundle load is retried; a render error is retried).
 * The retry button sits beside the notification: Carbon's InlineNotification
 * takes plain strings and refuses interactive children.
 */
class ModuleErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null, attempt: 0 };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error) {
    console.error(`module ${this.props.moduleId || "?"} failed`, error);
    if (this.props.onError) {
      this.props.onError(error);
    }
  }

  retry = () => {
    this.setState((state) => ({ error: null, attempt: state.attempt + 1 }));
    if (this.props.onRetry) {
      this.props.onRetry();
    }
  };

  render() {
    if (this.state.error) {
      if (this.props.silent) {
        return null;
      }
      const { intl } = this.props;
      return (
        <div className="module-error" data-testid="module-error">
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={
              this.props.title ||
              intl.formatMessage({ id: "modules.error.title" })
            }
            subtitle={intl.formatMessage(
              { id: "modules.error.message" },
              {
                module: this.props.moduleId || "",
                detail: shortDetail(this.state.error),
              },
            )}
          />
          <Button kind="ghost" size="sm" onClick={this.retry}>
            {intl.formatMessage({ id: "modules.error.retry" })}
          </Button>
        </div>
      );
    }
    return (
      <React.Fragment key={this.state.attempt}>
        {this.props.children}
      </React.Fragment>
    );
  }
}

/** The first line of the error without the federation runtime's banner, for the notice; the console has the rest. */
export function shortDetail(error) {
  const text = String((error && error.message) || error || "");
  return text
    .replace(/\[\s*Federation Runtime\s*\]:?\s*/i, "")
    .split("\n")[0]
    .trim()
    .slice(0, 160);
}

export default injectIntl(ModuleErrorBoundary);
