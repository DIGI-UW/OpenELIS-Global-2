import { useCallback } from "react";
import { deleteFromOpenElisServerFullResponse } from "../../../utils/Utils";

const ENDPOINT_BY_TYPE = {
  room: "rooms",
  device: "devices",
  shelf: "shelves",
  rack: "racks",
  box: "boxes",
};

export default function useDeleteLocation() {
  return useCallback((type, id) => {
    const endpoint = ENDPOINT_BY_TYPE[type];
    if (!endpoint) {
      return Promise.reject(new Error(`Unsupported location type: ${type}`));
    }

    return new Promise((resolve, reject) => {
      deleteFromOpenElisServerFullResponse(
        `/rest/storage/${endpoint}/${encodeURIComponent(String(id))}`,
        async (response) => {
          if (!response) {
            reject(new Error("Network error while deleting location"));
            return;
          }

          if (response.status === 204) {
            resolve();
            return;
          }

          const body = await response.json().catch(() => ({}));
          reject({
            status: response.status,
            message:
              body?.message || body?.error || "Failed to delete location",
            errorCode: body?.errorCode,
            sampleCount: body?.sampleCount,
          });
        },
      );
    });
  }, []);
}
