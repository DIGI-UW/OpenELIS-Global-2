import { useCallback } from "react";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";

export default function useCreateLocation() {
  return useCallback((endpoint, payload) => {
    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        `/rest/storage/${endpoint}`,
        JSON.stringify(payload),
        (response) => {
          if (
            response &&
            !response.error &&
            !response.statusCode &&
            response.id != null
          ) {
            resolve(response);
            return;
          }

          reject(
            new Error(
              response?.message ||
                response?.error ||
                // Bean-validation failures arrive as {errors: {field: message}},
                // which carries neither an `error` nor a `message` key.
                (response?.errors &&
                  Object.values(response.errors).join(", ")) ||
                "Failed to create storage location",
            ),
          );
        },
      );
    });
  }, []);
}
