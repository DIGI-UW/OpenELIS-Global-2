import config from "../../config.json";

/**
 * Where a stored report version is read: one file at its own address, so a
 * link or a new tab can open it as any other page.
 */
export const reportFileUrl = (caseId, reportId) =>
  `${config.serverBaseUrl}/rest/pathology/caseView/${caseId}/reports/${reportId}/file`;

const NO_TAB = Object.freeze({
  blocked: true,
  show() {},
  close() {},
});

/**
 * Opens an empty tab inside the click that asked for a report, before the
 * request that makes it is sent. A browser lets a page open a tab only while
 * it handles the click; once a request has been awaited, the popup is
 * blocked. The tab says what it is waiting for, cannot reach back to this
 * page, and is pointed at the file once it exists, or closed.
 *
 * A blocked popup is not an error: the caller is told and the report is
 * still made.
 */
export const openPendingTab = (text) => {
  let tab = null;
  try {
    tab = window.open("", "_blank");
  } catch {
    tab = null;
  }
  if (!tab) {
    return NO_TAB;
  }
  try {
    tab.opener = null;
    tab.document.title = text;
    tab.document.body.appendChild(tab.document.createTextNode(text));
  } catch {
    // A tab whose page cannot be written to still serves show and close.
  }
  return {
    blocked: false,
    show: (url) => tab.location.replace(url),
    close: () => tab.close(),
  };
};
