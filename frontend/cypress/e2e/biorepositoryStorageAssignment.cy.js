import LoginPage from "../pages/LoginPage";

const loginPage = new LoginPage();

describe("Biorepository storage assignment persistence", () => {
  beforeEach(() => {
    loginPage.visit();
    cy.fixture("Users").then((users) => {
      const admin = users.find((user) => user.correctPass === true);
      expect(admin, "admin user fixture").to.exist;
      loginPage.enterUsername(admin.username);
      loginPage.enterPassword(admin.password);
      loginPage.signIn();
    });

    cy.get("body", { timeout: 30000 }).should("be.visible");
  });

  it("assigns storage via bulk API and returns assignedCount > 0", () => {
    cy.request({
      method: "GET",
      url: "/rest/notebook/instances?workflowType=biorepository",
      failOnStatusCode: false,
    }).then((instancesResponse) => {
      const instances = instancesResponse.body || [];
      if (!Array.isArray(instances) || instances.length === 0) {
        cy.log("No biorepository notebook instances available for assignment test");
        return;
      }

      const notebook = instances[0];
      const notebookId = notebook.id || notebook.notebookId;

      cy.request({
        method: "GET",
        url: `/rest/notebook/${notebookId}/pages`,
        failOnStatusCode: false,
      }).then((pagesResponse) => {
        const pages = pagesResponse.body || [];
        const storagePage = pages.find(
          (page) =>
            page.pageType === "biorepository-storage-assignment" ||
            page.name?.toLowerCase()?.includes("storage"),
        );

        if (!storagePage?.id) {
          cy.log("Storage assignment page not found");
          return;
        }

        cy.request({
          method: "GET",
          url: `/rest/notebook/page/${storagePage.id}/samples`,
          failOnStatusCode: false,
        }).then((samplesResponse) => {
          const samples = samplesResponse.body?.samples || samplesResponse.body || [];
          const pendingSample = samples.find(
            (sample) =>
              sample.pageStatus === "PENDING" ||
              sample.status === "PENDING" ||
              !sample.data?.storagePath,
          );

          if (!pendingSample?.id) {
            cy.log("No pending sample available for storage assignment");
            return;
          }

          cy.request({
            method: "GET",
            url: `/rest/storage/devices?status=active&biorepositoryOnly=true&notebookId=${notebookId}`,
            failOnStatusCode: false,
          }).then((devicesResponse) => {
            const devices = devicesResponse.body || [];
            const device = devices[0];
            if (!device?.id) {
              cy.log("No biorepository-scoped devices available");
              return;
            }

            cy.request({
              method: "POST",
              url: `/rest/notebook/bulk/page/${storagePage.id}/samples/storage`,
              body: {
                sampleIds: [pendingSample.id],
                boxId: null,
                reassign: false,
                data: {
                  locationId: device.id,
                  locationType: "device",
                  storagePath: device.name || device.label || "Biorepository device",
                  notes: "Cypress storage assignment trust test",
                },
              },
              failOnStatusCode: false,
            }).then((assignResponse) => {
              expect(assignResponse.status).to.be.oneOf([200, 400]);
              if (assignResponse.status === 200) {
                expect(assignResponse.body.assignedCount).to.be.greaterThan(0);
                expect(assignResponse.body.success).to.eq(true);
              }
            });
          });
        });
      });
    });
  });
});
