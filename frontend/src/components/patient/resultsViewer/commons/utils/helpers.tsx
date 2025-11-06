import * as React from "react";

export const Grid: React.FC<{
  children?: React.ReactNode;
  style: React.CSSProperties;
  padding?: boolean;
  dataColumns: number;
}> = ({ dataColumns, style = {}, padding = false, ...props }) => (
  <div
    style={{
      ...style,
      gridTemplateColumns: `${padding ? "9em " : ""} repeat(${dataColumns}, 5em)`,
      margin: "1px",
    }}
    className="grid"
    {...props}
  />
);

export const PaddingContainer = React.forwardRef<HTMLElement, any>(
  (props, ref) => <div ref={ref} className="padding-container" {...props} />,
);

const TimeSlotsInner: React.FC<{
  style?: React.CSSProperties;
  className?: string;
}> = ({ className, ...props }) => (
  <div className="time-slot-inner" {...props} />
);

export const Main: React.FC = () => <main className="padded-main" />;

export const ShadowBox: React.FC = () => <div className="shadow-box" />;
