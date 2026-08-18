/**
 * Provider-lane EQA writes (scheme and distribution management, participant
 * enrollment) require the qa.eqa.provider grant server-side (OGC-609). Reads
 * stay open to the qa.view.eqa umbrella, so a bench user can view these pages
 * but must not be offered controls that answer 403.
 */
export const canManageEqaProvider = (userSessionDetails) =>
  !!userSessionDetails?.permissions?.includes("qa.eqa.provider") ||
  !!userSessionDetails?.roles?.includes("Global Administrator");
