/**
 * SRS persona RBAC smoke — department + persona (not department-only users).
 * Requires: ./scripts/populate-notebook-users.sh --clean-install
 * Base URL: CYPRESS_baseUrl=https://localhost:8843
 */
import LoginPage from "../pages/LoginPage";

const loginPage = new LoginPage();
const PASSWORD = "adminADMIN!";
const API_BASE = "/api/OpenELIS-Global";

const SMOKE = {
  mntdSectionId: "177",
  biorepoSectionId: "182",
  mntdEntryId: "9",
  biorepoEntryId: "1",
  mntdIntakePageId: "1795",
  mntdAnalysisPageId: "1803",
  biorepoIntakePageId: "182",
  biorepoReportingPageId: "188",
};

function loginAs(username) {
  cy.session(
    `persona-${username}`,
    () => {
      loginPage.visit();
      cy.get("body", { timeout: 30000 }).then(($body) => {
        if (!$body.find("#loginName").length) {
          cy.contains("Login", { timeout: 30000 }).click({ force: true });
        }
      });
      cy.get("#loginName", { timeout: 120000 }).should("be.visible");
      loginPage.clearInputs();
      loginPage.enterUsername(username);
      loginPage.enterPassword(PASSWORD);
      loginPage.signIn();
      cy.contains("Username or Password are incorrect", { timeout: 5000 }).should(
        "not.exist",
      );
      cy.url({ timeout: 60000 }).should("not.include", "/login");
    },
    { cacheAcrossSpecs: false },
  );
}

function setActiveLabUnit(sectionId) {
  cy.request({
    method: "POST",
    url: `${API_BASE}/rest/setUserLoginLabUnit/${sectionId}`,
    failOnStatusCode: false,
  });
}

function postPageComplete(pageId, expectStatus) {
  const expected = Array.isArray(expectStatus) ? expectStatus : [expectStatus];
  return cy
    .request({
      method: "POST",
      url: `${API_BASE}/rest/notebook/bulk/page/${pageId}/complete`,
      body: { requireComplete: false },
      failOnStatusCode: false,
    })
    .then((resp) => {
      expect(expected).to.include(resp.status);
      return resp;
    });
}

function visitNotebookWorkflow(entryId) {
  cy.visit(`/NoteBookInstanceEditForm/${entryId}`);
  cy.get("body", { timeout: 60000 }).should("be.visible");
  cy.contains(".cds--content-switcher-btn", /workflow/i, {
    timeout: 30000,
  }).click({ force: true });
  cy.get(".page-navigation .page-item", { timeout: 60000 }).should(
    "have.length.at.least",
    1,
  );
}

function assertPageNavAccess(pageIndex, shouldBeEnabled) {
  cy.get(".page-navigation .page-item").eq(pageIndex).as("pageItem");
  if (shouldBeEnabled) {
    cy.get("@pageItem").should("not.have.class", "disabled");
  } else {
    cy.get("@pageItem").should("have.class", "disabled");
  }
}

describe("AHRI SRS persona RBAC smoke", () => {
  describe("MNTD — Sample Collector (mntd_collector)", () => {
    beforeEach(() => {
      loginAs("mntd_collector");
      setActiveLabUnit(SMOKE.mntdSectionId);
    });

    it("allows intake stage navigation; blocks analysis stage in UI", () => {
      visitNotebookWorkflow(SMOKE.mntdEntryId);
      assertPageNavAccess(0, true);
      assertPageNavAccess(8, false);
    });

    it("returns 403 on COMPLETE for analysis stage (backend)", () => {
      postPageComplete(SMOKE.mntdAnalysisPageId, 403);
    });

    it("returns 403 on COMPLETE for biorepository intake (cross-department)", () => {
      setActiveLabUnit(SMOKE.biorepoSectionId);
      postPageComplete(SMOKE.biorepoIntakePageId, 403);
    });
  });

  describe("MNTD — Laboratory Technician (mntd_technician)", () => {
    beforeEach(() => {
      loginAs("mntd_technician");
      setActiveLabUnit(SMOKE.mntdSectionId);
    });

    it("can access processing-related stages; analysis stage disabled", () => {
      visitNotebookWorkflow(SMOKE.mntdEntryId);
      assertPageNavAccess(0, true);
      assertPageNavAccess(3, true);
      assertPageNavAccess(8, false);
    });

    it("403 on COMPLETE for analysis (manager/researcher only)", () => {
      postPageComplete(SMOKE.mntdAnalysisPageId, 403);
    });

    it("allows COMPLETE on intake when permitted", () => {
      postPageComplete(SMOKE.mntdIntakePageId, [200, 400]);
    });
  });

  describe("MNTD — Junior Researcher (mntd_researcher)", () => {
    beforeEach(() => {
      loginAs("mntd_researcher");
      setActiveLabUnit(SMOKE.mntdSectionId);
    });

    it("cannot access intake; can access mid-workflow processing", () => {
      visitNotebookWorkflow(SMOKE.mntdEntryId);
      assertPageNavAccess(0, false);
      assertPageNavAccess(3, true);
    });

    it("403 on intake COMPLETE", () => {
      postPageComplete(SMOKE.mntdIntakePageId, 403);
    });
  });

  describe("MNTD — Lab Manager (mntd_manager)", () => {
    beforeEach(() => {
      loginAs("mntd_manager");
      setActiveLabUnit(SMOKE.mntdSectionId);
    });

    it("can access QC and analysis stages", () => {
      visitNotebookWorkflow(SMOKE.mntdEntryId);
      assertPageNavAccess(5, true);
      assertPageNavAccess(8, true);
    });

    it("may COMPLETE analysis stage (200 or 400 if samples pending)", () => {
      postPageComplete(SMOKE.mntdAnalysisPageId, [200, 400]);
    });
  });

  describe("MNTD — Biomedical Staff (mntd_biomedical)", () => {
    beforeEach(() => {
      loginAs("mntd_biomedical");
      setActiveLabUnit(SMOKE.mntdSectionId);
    });

    it("notebook workflow stages are not accessible (equipment persona only)", () => {
      visitNotebookWorkflow(SMOKE.mntdEntryId);
      assertPageNavAccess(0, false);
      assertPageNavAccess(3, false);
    });

    it("403 on notebook bulk COMPLETE", () => {
      postPageComplete(SMOKE.mntdIntakePageId, 403);
    });
  });

  describe("Biorepository — Sample Collector (biorepo_collector)", () => {
    beforeEach(() => {
      loginAs("biorepo_collector");
      setActiveLabUnit(SMOKE.biorepoSectionId);
    });

    it("allows intake; blocks reporting stage", () => {
      visitNotebookWorkflow(SMOKE.biorepoEntryId);
      assertPageNavAccess(0, true);
      assertPageNavAccess(6, false);
    });

    it("403 on reporting COMPLETE", () => {
      postPageComplete(SMOKE.biorepoReportingPageId, 403);
    });
  });

  describe("Biorepository — Lab Manager (biorepo_manager)", () => {
    beforeEach(() => {
      loginAs("biorepo_manager");
      setActiveLabUnit(SMOKE.biorepoSectionId);
    });

    it("can access reporting stage", () => {
      visitNotebookWorkflow(SMOKE.biorepoEntryId);
      assertPageNavAccess(6, true);
    });
  });

  describe("Global Admin (global_admin)", () => {
    beforeEach(() => loginAs("global_admin"));

    it("can select a real department on landing and open MNTD notebook", () => {
      cy.visit("/landing");
      cy.contains(/Malaria|MNTD/i, { timeout: 30000 }).click();
      cy.get("button")
        .contains(/continue/i, { timeout: 10000 })
        .click({ force: true });
      visitNotebookWorkflow(SMOKE.mntdEntryId);
      assertPageNavAccess(0, true);
    });

    it("User Management shows All Lab Units assignment option", () => {
      cy.visit("/admin/userEdit");
      cy.contains("All Lab Units", { timeout: 30000 }).should("exist");
    });
  });

  describe("System Admin (system_admin)", () => {
    beforeEach(() => loginAs("system_admin"));

    it("can open admin user management", () => {
      cy.visit("/admin/userEdit");
      cy.contains(/user|role/i, { timeout: 30000 }).should("exist");
    });

    it("403 on scientific notebook COMPLETE without lab persona", () => {
      setActiveLabUnit(SMOKE.mntdSectionId);
      postPageComplete(SMOKE.mntdIntakePageId, 403);
    });
  });

  describe("Administrative Staff (admin_staff)", () => {
    beforeEach(() => loginAs("admin_staff"));

    it("403 on notebook bulk COMPLETE", () => {
      setActiveLabUnit(SMOKE.mntdSectionId);
      postPageComplete(SMOKE.mntdIntakePageId, 403);
    });
  });

  describe("EQA Personnel (eqa_user)", () => {
    beforeEach(() => loginAs("eqa_user"));

    it("403 on notebook bulk COMPLETE (QA global role only)", () => {
      setActiveLabUnit(SMOKE.mntdSectionId);
      postPageComplete(SMOKE.mntdIntakePageId, 403);
    });
  });
});
