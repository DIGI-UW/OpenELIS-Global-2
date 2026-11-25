import React from "react";

// Gaps (null points) break the line rather than dropping to zero.
const Sparkline = ({ points }) => {
  const values = points.filter((p) => p != null);
  if (values.length < 2) {
    return null;
  }
  const min = Math.min(...values);
  const range = Math.max(...values) - min || 1;
  const x = (i) => (i / (points.length - 1)) * 100;
  const y = (v) => 22 - ((v - min) / range) * 20;
  const segments = [];
  let current = [];
  points.forEach((p, i) => {
    if (p == null) {
      if (current.length) segments.push(current);
      current = [];
    } else {
      current.push(`${x(i).toFixed(2)},${y(p).toFixed(2)}`);
    }
  });
  if (current.length) segments.push(current);
  const lastIndex = points.map((p) => p != null).lastIndexOf(true);

  return (
    <svg
      className="mgmt-sparkline"
      viewBox="0 0 100 24"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      {segments.map((segment) => (
        <polyline key={segment[0]} points={segment.join(" ")} />
      ))}
      <circle cx={x(lastIndex)} cy={y(points[lastIndex])} r="1.8" />
    </svg>
  );
};

export default Sparkline;
