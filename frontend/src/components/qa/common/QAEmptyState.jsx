import React from "react";
import { Document } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import "./QAEmptyState.css";

/**
 * Calm, designed empty-state for QA v1 surfaces (OGC-729). Used where a query
 * legitimately returns zero rows — a quiet day, a fresh install, zero critical
 * NCEs (the goal state) — so the view reads as "nothing to report" rather than
 * broken or mid-load. Copy names the state (not the gap) and promises nothing.
 *
 * size: "page" (detail pages) | "inline" (overview sections / feeds).
 */
const QAEmptyState = ({
  titleKey,
  subheadKey,
  icon: Icon = Document,
  size = "page",
  values,
}) => (
  <div className={`qa-empty qa-empty--${size}`}>
    <Icon className="qa-empty__icon" size={size === "page" ? 48 : 20} />
    <p className="qa-empty__title">
      <FormattedMessage id={titleKey} values={values} />
    </p>
    {subheadKey && (
      <p className="qa-empty__subhead">
        <FormattedMessage id={subheadKey} values={values} />
      </p>
    )}
  </div>
);

/**
 * Sparse-state fallback (OGC-729): when a category chart would draw only 1-2
 * bars, a labeled list reads better than a near-empty axis. `items` is
 * [{ label, value }]; the headline names the count.
 */
export const QASparseList = ({ headlineKey, headlineValues, items }) => (
  <div className="qa-sparse">
    <p className="qa-sparse__headline">
      <FormattedMessage id={headlineKey} values={headlineValues} />
    </p>
    <ul className="qa-sparse__list">
      {items.map((item) => (
        <li key={item.label} className="qa-sparse__row">
          <span className="qa-sparse__label">{item.label}</span>
          <span className="qa-sparse__value">{item.value}</span>
        </li>
      ))}
    </ul>
  </div>
);

export default QAEmptyState;
