import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import PropTypes from "prop-types";
import classNames from "classnames";

const SlideOver = ({
  open,
  setOpen,
  children,
  slideFrom = "right",
  dialogClass = "",
  title,
  onlyChild = false,
  backdropBlur = true,
  closeOnBackdropClick = true,
  onCloseClick,
}) => {
  const directionClasses = {
    left: {
      stick: "left-0 top-0 h-full",
      animateStart: "-translate-x-full",
      animateEnd: "translate-x-0",
      proportions: "oeui-slideover-x",
    },
    right: {
      stick: "right-0 top-0 h-full",
      animateStart: "translate-x-full",
      animateEnd: "translate-x-0",
      proportions: "oeui-slideover-x",
    },
    top: {
      stick: "top-0 left-0 w-full",
      animateStart: "-translate-y-full",
      animateEnd: "translate-y-0",
      proportions: "oeui-slideover-y",
    },
    bottom: {
      stick: "bottom-0 left-0 w-full",
      animateStart: "translate-y-full",
      animateEnd: "translate-y-0",
      proportions: "oeui-slideover-y",
    },
  };

  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "auto";
  }, [open]);

  return ReactDOM.createPortal(
    <div className={classNames("slide-over-root", { show: open })}>
      <div
        className={classNames("slide-over-backdrop", {
          "backdrop-blur": backdropBlur,
        })}
        onClick={closeOnBackdropClick ? () => setOpen(false) : null}
      ></div>
      <div
        className={classNames(
          "slide-over-panel",
          {
            [directionClasses[slideFrom].stick]: true,
            [directionClasses[slideFrom].animateEnd]: open,
            [directionClasses[slideFrom].animateStart]: !open,
          },
          slideFrom === "left" ? "slide-over-left" : "slide-over-right",
        )}
      >
        {onlyChild ? (
          children
        ) : (
          <div
            className={classNames(
              "slide-over-content",
              directionClasses[slideFrom].proportions,
              dialogClass,
            )}
          >
            <div
              className="slide-over-header"
              style={{ marginTop: "1%", padding: "2%" }}
            >
              <button
                id="close-slide-over"
                className="close-button"
                onClick={() => {
                  setOpen(false);
                  onCloseClick && onCloseClick();
                }}
              >
                &rarr;
              </button>
              <div className="slide-over-title">{title}</div>
            </div>
            <div className="slide-over-body">{children}</div>
            <div style={{ marginTop: "40px" }}></div>
          </div>
        )}
      </div>
    </div>,
    document.body,
  );
};

SlideOver.propTypes = {
  open: PropTypes.bool.isRequired,
  setOpen: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  slideFrom: PropTypes.oneOf(["left", "right", "top", "bottom"]),
  dialogClass: PropTypes.string,
  title: PropTypes.node,
  onlyChild: PropTypes.bool,
  backdropBlur: PropTypes.bool,
  closeOnBackdropClick: PropTypes.bool,
  onCloseClick: PropTypes.func,
};

export default SlideOver;
