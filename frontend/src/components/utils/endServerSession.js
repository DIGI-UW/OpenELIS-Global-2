/**
 * Ends the server session for a sign-out. The server answers the POST with a
 * redirect to its login page built from the request's host, which drops the
 * port when OpenELIS runs behind a proxy on a non-default port; following it
 * failed cross-origin and the page stayed where it was (OGC-1448). The session
 * ends on the POST itself, so the redirect is not followed, and the promise
 * resolves whatever the answer so the caller always moves on to its own login
 * page on this origin.
 */
export const endServerSession = (serverBaseUrl, csrfToken) =>
  fetch(serverBaseUrl + "/Logout", {
    method: "POST",
    redirect: "manual",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken,
    },
  })
    .then(() => undefined)
    .catch((error) => {
      console.error(error);
    });
