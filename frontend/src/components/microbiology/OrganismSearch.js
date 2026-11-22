import { getFromOpenElisServer } from "../utils/Utils";

const URL = "/rest/microbiology/reference/organisms";

const read = (query) =>
  new Promise((resolve) =>
    getFromOpenElisServer(`${URL}?${query}`, (found) =>
      resolve(Array.isArray(found) ? found : []),
    ),
  );

// The server ranks the matches: an exact WHONET code, then the organisms WHONET marks as common.
export const searchOrganisms = (text) =>
  read(`q=${encodeURIComponent(text || "")}`);

export const getOrganism = (id) =>
  read(`id=${encodeURIComponent(id)}`).then((found) => found[0] || null);

// "Klebsiella sp. (kl-)": WHONET's name and code.
export const organismText = (organism) => {
  if (!organism) return "";
  return organism.code
    ? `${organism.label} (${organism.code})`
    : organism.label;
};
