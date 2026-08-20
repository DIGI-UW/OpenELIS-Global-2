// Data seam for the provider scheme list and cycle wizard (OGC-613).
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Schemes this lab runs for other laboratories. In-house schemes are left out:
 * they have their own wizard and never ship a panel anywhere.
 */
export const fetchProviderSchemes = (callback) =>
  getFromOpenElisServer("/rest/eqa/programs", (data) =>
    callback((data || []).filter((scheme) => scheme.schemeType !== "IN_HOUSE")),
  );

export const fetchEnrollments = (schemeId, callback) =>
  getFromOpenElisServer(`/rest/eqa/programs/${schemeId}/enrollments`, (data) =>
    callback(data || []),
  );
