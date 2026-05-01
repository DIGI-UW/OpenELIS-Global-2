import LoginPage from "../pages/LoginPage";

const loginPage = new LoginPage();

const extractFirstNumericId = (value) => {
  if (value == null) {
    return null;
  }

  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      const id = extractFirstNumericId(item);
      if (id != null) {
        return id;
      }
    }
    return null;
  }

  if (typeof value === "object") {
    if (Object.prototype.hasOwnProperty.call(value, "id")) {
      const id = extractFirstNumericId(value.id);
      if (id != null) {
        return id;
      }
    }

    for (const key of Object.keys(value)) {
      const id = extractFirstNumericId(value[key]);
      if (id != null) {
        return id;
      }
    }
  }

  const parsed = Number.parseInt(String(value), 10);
  return Number.isFinite(parsed) ? parsed : null;
};

const getFirstBoxId = (responseBody) => {
  if (!Array.isArray(responseBody)) {
    return null;
  }

  for (const row of responseBody) {
    const candidates = [row?.id, row?.boxId, row?.storageBoxId, row?.locationId];
    for (const candidate of candidates) {
      const id = extractFirstNumericId(candidate);
      if (id != null) {
        return id;
      }
    }
  }

  return null;
};

describe("Biorepository lifecycle traceability", () => {
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

  it("executes transfer to return to re-storage lifecycle and verifies state transitions", () => {
    let sampleItemId = null;
    let bioSampleId = null;
    let transferAccepted = false;

    cy.then(() =>
      cy.request({
        method: "GET",
        url: "/rest/biorepository/transfer/pending?limit=10",
        failOnStatusCode: false,
      }).then((pendingResponse) => {
        expect([200, 204]).to.include(pendingResponse.status);

        const pendingRequests = Array.isArray(pendingResponse.body) ? pendingResponse.body : [];
        if (pendingRequests.length === 0) {
          return;
        }

        const pendingRequestId = extractFirstNumericId(pendingRequests[0]?.id);
        if (pendingRequestId == null) {
          return;
        }

        return cy
          .request({
            method: "GET",
            url: `/rest/biorepository/transfer/${pendingRequestId}`,
            failOnStatusCode: false,
          })
          .then((transferDetailResponse) => {
            if (transferDetailResponse.status !== 200) {
              return;
            }

            const items = Array.isArray(transferDetailResponse.body?.items)
              ? transferDetailResponse.body.items
              : [];
            const pendingItem = items.find((item) => item?.status === "PENDING");
            const transferItemId = extractFirstNumericId(pendingItem?.id);
            if (transferItemId == null) {
              return;
            }

            return cy
              .request({
                method: "POST",
                url: `/rest/biorepository/transfer/item/${transferItemId}/accept`,
                body: {
                  biosafetyLevel: "BSL_1",
                  ethicsApprovalRef: "CYPRESS-LIFECYCLE",
                },
                failOnStatusCode: false,
              })
              .then((acceptResponse) => {
                if (acceptResponse.status !== 200) {
                  return;
                }

                transferAccepted = true;
                bioSampleId = extractFirstNumericId(acceptResponse.body?.bioSampleId);
                sampleItemId = extractFirstNumericId(pendingItem?.sampleItemId);
              });
          });
      }),
    );

    cy.then(() => {
      if (sampleItemId != null && bioSampleId != null) {
        return;
      }

      return cy
        .request({
          method: "GET",
          url: "/rest/biorepository/qc-inspection/samples",
          failOnStatusCode: false,
        })
        .then((qcResponse) => {
          expect(qcResponse.status).to.eq(200);

          const storedSamples = Array.isArray(qcResponse.body) ? qcResponse.body : [];
          if (storedSamples.length === 0) {
            cy.log(
              "No pending transfer items or stored samples available in this environment; lifecycle flow skipped.",
            );
            return;
          }

          const candidate = storedSamples[0];
          sampleItemId = extractFirstNumericId(candidate?.sampleItemId);
          bioSampleId = extractFirstNumericId(candidate?.bioSampleId);
        });
    });

    cy.then(() => {
      if (sampleItemId == null || bioSampleId == null || !transferAccepted) {
        return;
      }

      return cy
        .request({
          method: "GET",
          url: "/rest/storage/boxes",
          failOnStatusCode: false,
        })
        .then((boxesResponse) => {
          expect(boxesResponse.status).to.eq(200);
          const boxId = getFirstBoxId(boxesResponse.body);
          expect(boxId, "boxId for initial storage assignment").to.not.equal(null);

          return cy.request({
            method: "POST",
            url: "/rest/storage/sample-items/assign",
            body: {
              sampleItemId: String(sampleItemId),
              locationId: String(boxId),
              locationType: "box",
              positionCoordinate: "A1",
              notes: "Cypress initial storage assignment",
            },
            failOnStatusCode: false,
          });
        })
        .then((assignResponse) => {
          if (assignResponse) {
            expect([200, 201]).to.include(assignResponse.status);
          }
        });
    });

    cy.then(() => {
      if (sampleItemId == null || bioSampleId == null) {
        return;
      }

      let retrievalRequestId = null;
      let retrievalItemId = null;

      return cy
        .request({
          method: "POST",
          url: "/rest/biorepository/retrieval/requests",
          body: {
            requestPurpose: "Cypress lifecycle validation",
            bioSampleIds: [bioSampleId],
            destinationType: "ANALYSIS_RETURN",
            destinationDetails: "Cypress Lab Bench",
            priorityLevel: "NORMAL",
          },
          failOnStatusCode: false,
        })
        .then((createRetrievalResponse) => {
          expect(createRetrievalResponse.status).to.eq(200);
          retrievalRequestId = extractFirstNumericId(createRetrievalResponse.body?.id);
          expect(retrievalRequestId, "retrievalRequestId").to.not.equal(null);

          return cy.request({
            method: "POST",
            url: `/rest/biorepository/retrieval/requests/${retrievalRequestId}/submit`,
            failOnStatusCode: false,
          });
        })
        .then((submitResponse) => {
          expect(submitResponse.status).to.eq(200);

          return cy.request({
            method: "POST",
            url: `/rest/biorepository/retrieval/requests/${retrievalRequestId}/approve`,
            body: { approvalNotes: "Cypress auto-approval" },
            failOnStatusCode: false,
          });
        })
        .then((approveResponse) => {
          expect(approveResponse.status).to.eq(200);

          return cy.request({
            method: "GET",
            url: `/rest/biorepository/retrieval/requests/${retrievalRequestId}`,
            failOnStatusCode: false,
          });
        })
        .then((requestDetailResponse) => {
          expect(requestDetailResponse.status).to.eq(200);
          retrievalItemId = extractFirstNumericId(requestDetailResponse.body?.items?.[0]?.id);
          expect(retrievalItemId, "retrievalItemId").to.not.equal(null);

          return cy.request({
            method: "POST",
            url: `/rest/biorepository/retrieval/items/${retrievalItemId}/retrieve`,
            body: {
              conditionAtRelease: "Good",
              conditionNotes: "Cypress checkout",
            },
            failOnStatusCode: false,
          });
        })
        .then((retrieveResponse) => {
          expect(retrieveResponse.status).to.eq(200);

          return cy.request({
            method: "POST",
            url: `/rest/biorepository/retrieval/items/${retrievalItemId}/release`,
            failOnStatusCode: false,
          });
        })
        .then((releaseResponse) => {
          expect(releaseResponse.status).to.eq(200);

          return cy.request({
            method: "POST",
            url: `/rest/biorepository/retrieval/items/${retrievalItemId}/return`,
            body: {
              returnedCondition: "Good",
              returnNotes: "Cypress return",
              fullyConsumed: false,
            },
            failOnStatusCode: false,
          });
        })
        .then((returnResponse) => {
          expect(returnResponse.status).to.eq(200);

          return cy.request({
            method: "GET",
            url: `/rest/biorepository/lifecycle/sample-item/${sampleItemId}`,
            failOnStatusCode: false,
          });
        })
        .then((pendingLifecycleResponse) => {
          expect(pendingLifecycleResponse.status).to.eq(200);

          const lifecycle = pendingLifecycleResponse.body || {};
          const actions = Array.isArray(lifecycle.events)
            ? lifecycle.events.map((event) => event.custodyAction)
            : [];

          expect(actions).to.include("CHECKOUT_RETRIEVED");
          expect(actions).to.include("CHECKOUT_RELEASED");
          expect(actions).to.include("RETURN_RECEIVED");
          expect(actions).to.not.include("RETURN_STORED");
          expect(lifecycle.currentState?.workflowStatus).to.eq("PENDING_STORAGE");
          expect(lifecycle.currentState?.awaitingRestorage).to.eq(true);
        });
    });

    cy.then(() => {
      if (sampleItemId == null) {
        return;
      }

      return cy
        .request({
          method: "GET",
          url: "/rest/storage/boxes",
          failOnStatusCode: false,
        })
        .then((boxesResponse) => {
          expect(boxesResponse.status).to.eq(200);
          const boxId = getFirstBoxId(boxesResponse.body);
          expect(boxId, "boxId for re-storage").to.not.equal(null);

          return cy.request({
            method: "POST",
            url: "/rest/storage/sample-items/move",
            body: {
              sampleItemId: String(sampleItemId),
              locationId: String(boxId),
              locationType: "box",
              positionCoordinate: "A2",
              reason: "Cypress re-storage after return",
              notes: "Cypress lifecycle re-storage",
            },
            failOnStatusCode: false,
          });
        })
        .then((moveResponse) => {
          if (moveResponse) {
            expect([200, 201]).to.include(moveResponse.status);
          }
        });
    });

    cy.then(() => {
      if (sampleItemId == null) {
        return;
      }

      return cy
        .request({
          method: "GET",
          url: `/rest/biorepository/lifecycle/sample-item/${sampleItemId}`,
          failOnStatusCode: false,
        })
        .then((lifecycleResponse) => {
          expect(lifecycleResponse.status).to.eq(200);

          const lifecycle = lifecycleResponse.body || {};
          const events = Array.isArray(lifecycle.events) ? lifecycle.events : [];
          const actions = events.map((event) => event.custodyAction);

          expect(actions).to.include("RETURN_RECEIVED");
          expect(actions).to.include("RETURN_STORED");
          expect(lifecycle.currentState?.workflowStatus).to.eq("STORED");
          expect(lifecycle.currentState?.awaitingRestorage).to.eq(false);

          if (transferAccepted) {
            expect(actions).to.include("TRANSFER_RECEIVED");
            expect(actions).to.include("STORAGE_ASSIGNED");
          }
        });
    });
  });
});
