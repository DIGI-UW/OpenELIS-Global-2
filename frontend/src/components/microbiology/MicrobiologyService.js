import { putToOpenElisServerFullResponse } from "../utils/Utils";

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
  saveOrderDetail,
};

export default MicrobiologyService;
