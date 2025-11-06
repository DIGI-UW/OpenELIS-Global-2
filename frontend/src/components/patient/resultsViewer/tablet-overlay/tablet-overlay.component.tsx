import React from "react";
import { Button, Header } from "@carbon/react";
import { ArrowLeft, Close } from "@carbon/react/icons";
//import styles from './tablet-overlay.scss';
import "./tablet-overlay.scss";

interface OverlayProps {
  children?: React.ReactNode;
  close: () => void;
  headerText: string | React.ReactElement;
  buttonsGroup?: React.ReactElement;
}

const Overlay: React.FC<OverlayProps> = ({
  close,
  children,
  headerText,
  buttonsGroup,
}) => (
  <div className="tabletOverlay">
    <Header className="tabletOverlayHeader">
      <Button onClick={close} hasIconOnly>
        <ArrowLeft size={16} onClick={close} />
      </Button>
      <div className="headerContent">{headerText}</div>
    </Header>
    <div className="overlayContent">{children}</div>
    <div className="buttonsGroup">{buttonsGroup}</div>
  </div>
);

export default Overlay;
