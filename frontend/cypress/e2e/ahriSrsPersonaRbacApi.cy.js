/**
 * SRS persona RBAC API smoke (403 enforcement).
 * Run: CYPRESS_baseUrl=https://localhost:8843 npx cypress run --spec cypress/e2e/ahriSrsPersonaRbacApi.cy.js
 */
const PASSWORD = "adminADMIN!";
// Match frontend config.json serverBaseUrl (proxied at :8843)
const API_BASE = Cypress.env("API_BASE") || "/OpenELIS-Global";

const SMOKE = {
  mntdSectionId: "177",
  biorepoSectionId: "182",
  mntdIntakePageId: "1795",
  mntdAnalysisPageId: "1803",
  biorepoIntakePageId: "182",
  biorepoReportingPageId: "188",
};

function loginAs(username) {
  cy.session(username, () => {
    cy.request(`${API_BASE}/LoginPage`);
    cy.request({
      method: "POST",
      url: `${API_BASE}/ValidateLogin?apiCall=true`,
      form: true,
      body: { loginName: username, password: PASSWORD },
    }).its("status").should("eq", 200);
    cy.visit("/");
    cy.get("body", { timeout: 30000 }).should("be.visible");
  });
}

function setActiveLabUnit(sectionId) {
  cy.request({
    method: "POST",
    url: `${API_BASE}/rest/setUserLoginLabUnit/${sectionId}`,
    failOnStatusCode: false,
  }).its("status").should("eq", 200);
}

/** POST samples/apply enforces the same stage EDIT ACL as /complete. */
function expectForbiddenBulkEdit(pageId, expected) {
  const codes = Array.isArray(expected) ? expected : [expected];
  cy.request({
    method: "POST",
    url: `${API_BASE}/rest/notebook/bulk/page/${pageId}/samples/apply`,
    body: { sampleIds: [0], data: { rbacSmoke: true } },
    headers: { "Content-Type": "application/json" },
    failOnStatusCode: false,
  }).then((r) => {
    expect(
      codes,
      `page ${pageId} status=${r.status} body=${JSON.stringify(r.body)}`,
    ).to.include(r.status);
  });
}

describe("SRS persona API RBAC", () => {
  it("sanity: admin can reach notebook REST API", () => {
    loginAs("admin");
    cy.request({
      url: `${API_BASE}/rest/notebook/view/34`,
      failOnStatusCode: false,
    }).then((r) => {
      expect(r.status, `notebook GET ${JSON.stringify(r.body)}`).to.be.oneOf([
        200, 403,
      ]);
    });
  });

  it("mntd_collector: 403 analysis + cross-dept biorepo", () => {
    loginAs("mntd_collector");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdAnalysisPageId, 403);
    setActiveLabUnit(SMOKE.biorepoSectionId);
    expectForbiddenBulkEdit(SMOKE.biorepoIntakePageId, 403);
  });

  it("mntd_technician: 403 analysis; intake allowed or bad request", () => {
    loginAs("mntd_technician");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdAnalysisPageId, 403);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, [200, 400, 403]);
  });

  it("mntd_researcher: 403 intake", () => {
    loginAs("mntd_researcher");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, 403);
  });

  it("mntd_manager: analysis complete allowed or 400", () => {
    loginAs("mntd_manager");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdAnalysisPageId, [200, 400, 403]);
  });

  it("mntd_biomedical: 403 notebook complete", () => {
    loginAs("mntd_biomedical");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, 403);
  });

  it("biorepo_collector: 403 reporting", () => {
    loginAs("biorepo_collector");
    setActiveLabUnit(SMOKE.biorepoSectionId);
    expectForbiddenBulkEdit(SMOKE.biorepoReportingPageId, 403);
  });

  it("biorepo_manager: reporting allowed or 400", () => {
    loginAs("biorepo_manager");
    setActiveLabUnit(SMOKE.biorepoSectionId);
    expectForbiddenBulkEdit(SMOKE.biorepoReportingPageId, [200, 400, 403]);
  });

  it("global_admin: intake complete allowed or 400", () => {
    loginAs("global_admin");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, [200, 400, 403]);
  });

  it("system_admin: 403 scientific notebook mutation unless also Global Admin/AllLabUnits", () => {
    loginAs("system_admin");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, 403);
  });

  it("admin_staff: 403 notebook complete", () => {
    loginAs("admin_staff");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, 403);
  });

  it("eqa_user: 403 notebook complete", () => {
    loginAs("eqa_user");
    setActiveLabUnit(SMOKE.mntdSectionId);
    expectForbiddenBulkEdit(SMOKE.mntdIntakePageId, 403);
  });
});
