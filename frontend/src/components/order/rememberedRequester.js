const STORAGE_KEY = "openelis.order.rememberedRequester";

const REMEMBERED_FIELDS = [
  "referringSiteId",
  "referringSiteName",
  "referringSiteDepartment",
  "providerPersonId",
  "providerId",
  "providerFirstName",
  "providerLastName",
  "providerWorkPhone",
  "providerFax",
  "providerEmail",
];

/**
 * The site and requester a user asked to carry over to their next order.
 *
 * A clinic entering a day's work from one referring site re-typed it on every
 * order; the legacy screen had a "remember site & requester" tick for exactly
 * this. Kept per browser rather than on the order, because it is a data-entry
 * convenience and not a property of any one order.
 */
export const readRememberedRequester = () => {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
};

export const rememberRequester = (sampleOrderItems = {}) => {
  const remembered = REMEMBERED_FIELDS.reduce((carried, field) => {
    if (sampleOrderItems[field]) {
      carried[field] = sampleOrderItems[field];
    }
    return carried;
  }, {});
  try {
    if (Object.keys(remembered).length === 0) {
      window.localStorage.removeItem(STORAGE_KEY);
    } else {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(remembered));
    }
  } catch {
    // A browser that refuses storage simply does not remember.
  }
};

export const forgetRequester = () => {
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Nothing to do — the value was never stored.
  }
};
