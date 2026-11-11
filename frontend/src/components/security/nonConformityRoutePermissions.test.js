import { readFileSync } from "fs";
import { resolve } from "path";

/**
 * The non-conformity routes are shared: the EQA lane's coverage tile deep-links
 * the dashboard, and the dashboard's own row actions push to the other three. A
 * QA Officer holds neither Reception nor Validation, so with a role list alone
 * every one of them answered Access Denied — the data permission was never the
 * problem, only the route declaration.
 *
 * SecureRoute ORs role and permission, so this is a declaration-level check, the
 * frontend counterpart of the backend's guard matrix: the routes a lane links
 * must name a permission, or the wall comes back the next time a role list is
 * written without one.
 */
const APP = readFileSync(resolve(__dirname, "../../App.jsx"), "utf8");

/**
 * The `<SecureRoute …>` block declaring a given path. It ends at the element's
 * own self-closing tag, which is the one sitting on its own line — a bare search
 * for "/>" finds the inline component element first and cuts the block short.
 */
const routeBlock = (path) => {
  const at = APP.indexOf(`path="${path}"`);
  expect(at).toBeGreaterThan(-1);
  const opened = APP.lastIndexOf("<SecureRoute", at);
  const closed = APP.indexOf("\n                />", at);
  expect(closed).toBeGreaterThan(at);
  return APP.slice(opened, closed);
};

describe("non-conformity route permissions", () => {
  test.each([
    ["/NceDashboard", "qa.view.eqa"],
    ["/ReportNonConformingEvent", "qa.view.eqa"],
    ["/ViewNonConformingEvent", "qa.view.eqa"],
    ["/NCECorrectiveAction", "qa.view.eqa"],
    ["/qa/qms/nce-register", "qa.view.qms"],
  ])("%s is reachable by permission, not by role alone", (path, permission) => {
    const block = routeBlock(path);
    expect(block).toContain(`permission="${permission}"`);
    // The roles stay: this widens access, it does not move it.
    expect(block).toContain("Roles.RECEPTION");
  });
});
