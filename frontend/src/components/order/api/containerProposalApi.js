import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * The containers proposed for each sample (OGC-1611 FR-B24, B25): one view
 * per sample sent, keyed as sent, with the containers to draw, the tests no
 * container is proposed for, the container choices and whether a container
 * is required.
 * @param {Array<{key:string, sampleTypeId:string, testIds:string[], panelIds:string[]}>} samples
 * @returns {Promise<Array>}
 */
export const proposeContainers = (samples) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      "/rest/container-proposal",
      JSON.stringify({ samples }),
      (response) => {
        if (Array.isArray(response)) {
          resolve(response);
        } else {
          reject(new Error("Failed to propose containers"));
        }
      },
    );
  });
