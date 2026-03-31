import { ArrowRight } from "@carbon/icons-react";
import classNames from "classnames";
import React from "react";
import ReactDOM from "react-dom";

const UserSlidePanel = ({ open, onClose, title, children }) => {
  return ReactDOM.createPortal(
    <div className={classNames("slide-over-root", { show: open })}>
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
            <button className="close-button" onClick={onClose}>
              <ArrowRight size={20} />
            </button>
            <div className="slide-over-title">{title}</div>
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
