import { ArrowRight } from "@carbon/icons-react";
import classNames from "classnames";
import React from "react";
import ReactDOM from "react-dom";
import { useIntl } from "react-intl";

const UserSlidePanel = ({ open, onClose, title, children }) => {
  const intl = useIntl();

  React.useEffect(() => {
    if (typeof document === "undefined") {
      return;
    }
    const body = document.body;
    if (!body) {
      return;
    }
    const originalOverflow = body.style.overflow;
    body.style.overflow = open ? "hidden" : "auto";
    return () => {
      body.style.overflow = originalOverflow;
    };
  }, [open]);

  return ReactDOM.createPortal(
    <div
      className={classNames("slide-over-root user-slide-panel", { show: open })}
    >
      <div
        className={classNames("slide-over-backdrop", "backdrop-blur")}
        onClick={onClose}
      />
      <div
        className={classNames(
          "slide-over-panel",
          "right-0",
          "top-0",
          "h-full",
          "slide-over-right",
          {
            "translate-x-0": open,
            "translate-x-full": !open,
          },
        )}
      >
        <div className="slide-over-content oeui-slideover-x">
          <div className="slide-over-header">
            <button
              type="button"
              className="close-button"
              onClick={onClose}
              aria-label="Close panel"
            >
              <ArrowRight size={20} />
            </button>
            <div className="slide-over-title">
              {intl.formatMessage(
                { id: "header.label.user.slide.title" },
                { defaultValue: title },
              )}
            </div>
          </div>
          <div className="slide-over-body">{children}</div>
          <div className="slide-over-spacer"></div>
        </div>
      </div>
    </div>,
    document.body,
  );
};

export default UserSlidePanel;
