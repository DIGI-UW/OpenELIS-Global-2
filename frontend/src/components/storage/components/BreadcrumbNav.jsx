import React from "react";
import { Link } from "react-router-dom";
import { Breadcrumb, BreadcrumbItem } from "@carbon/react";

/**
 * BreadcrumbNav — small wrapper around Carbon's Breadcrumb that:
 *   - Takes a `crumbs` array: [{ label, href }, ...]
 *   - Each crumb except the LAST is a clickable react-router Link (so
 *     navigation stays SPA, no full-page reload)
 *   - The last crumb is marked aria-current="page" and is non-clickable
 *
 * The page shells (LocationPickerPage, EditLocationPage, EditBoxPage,
 * SampleItemsPage, etc.) pass this in as a prop so each route owns
 * its own crumb chain.
 */
export default function BreadcrumbNav({ crumbs }) {
  if (!crumbs || crumbs.length === 0) return null;
  return (
    <Breadcrumb noTrailingSlash>
      {crumbs.map((crumb, idx) => {
        const isLast = idx === crumbs.length - 1;
        if (isLast) {
          // Carbon 1.15 `BreadcrumbItem` only forwards `aria-current` to the
          // DOM when (a) the prop is passed explicitly AND (b) the child is
          // a React element (not a string) — Carbon clones the element and
          // injects `aria-current` onto it. Passing `isCurrentPage` alone
          // only applies a CSS modifier class; accessibility needs the
          // attribute. Ref: node_modules/@carbon/react/es/components/
          // Breadcrumb/BreadcrumbItem.js lines 28, 60-66.
          return (
            <BreadcrumbItem key={crumb.href} isCurrentPage aria-current="page">
              <span>{crumb.label}</span>
            </BreadcrumbItem>
          );
        }
        return (
          <BreadcrumbItem key={crumb.href}>
            <Link to={crumb.href}>{crumb.label}</Link>
          </BreadcrumbItem>
        );
      })}
    </Breadcrumb>
  );
}
