import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";

/**
 * The sample type names by id, from the list the sample-type picker offers, so
 * a message about a sample can name the type the user chose on it. Empty until
 * the list arrives.
 */
export default function useSampleTypeNames() {
  const [namesById, setNamesById] = useState({});
  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer("/rest/user-sample-types", (response) => {
      if (mounted && Array.isArray(response)) {
        const byId = {};
        response.forEach((type) => {
          byId[String(type.id)] = type.value;
        });
        setNamesById(byId);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);
  return namesById;
}
