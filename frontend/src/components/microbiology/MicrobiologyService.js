import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

export const getPatientOrigins = (organizationId) =>
  new Promise((resolve) => {
    const query = organizationId
      ? `?organizationId=${encodeURIComponent(organizationId)}`
      : "";
    getFromOpenElisServer(
      `/rest/microbiology/reference/patient-origins${query}`,
      resolve,
    );
  });

export const saveOrderDetail = (caseId, payload) =>
  new Promise((resolve) => {
    putToOpenElisServerFullResponse(
      `/rest/microbiology/cases/${caseId}/order-detail`,
      JSON.stringify(payload),
      (response) => {
        if (!response) {
          resolve({ status: 0 });
          return;
        }
        response.json().then(resolve);
      },
    );
  });

const MicrobiologyService = {
  getPatientOrigins,
  saveOrderDetail,
};

export default MicrobiologyService;
