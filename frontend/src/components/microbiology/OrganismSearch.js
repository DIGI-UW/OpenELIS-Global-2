import { getFromOpenElisServer } from "../utils/Utils";

const URL = "/rest/microbiology/reference/organisms";

const read = (query) =>
  new Promise((resolve) =>
    getFromOpenElisServer(`${URL}?${query}`, (found) =>
      resolve(Array.isArray(found) ? found : []),
    ),
  );

// The server ranks the matches: an exact WHONET code, then the most common organisms.
export const searchOrganisms = (text) =>
  read(`q=${encodeURIComponent(text || "")}`);

export const getOrganism = (id) =>
  read(`id=${encodeURIComponent(id)}`).then((found) => found[0] || null);

// "Klebsiella (Klebsiella sp., kl-)": the WHONET name and code a WHONET user knows it by.
export const organismText = (organism) => {
  if (!organism) return "";
  const known = [
    organism.whonetName !== organism.label && organism.whonetName,
    organism.code,
  ].filter(Boolean);
  return known.length
    ? `${organism.label} (${known.join(", ")})`
    : organism.label;
};
