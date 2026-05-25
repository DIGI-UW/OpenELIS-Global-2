export const convertToISODateTime = (dateString) => {
  if (!dateString || !String(dateString).trim()) {
    return null;
  }

  try {
    if (dateString.includes("T")) {
      return dateString;
    }
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return null;
    }
    return date.toISOString().slice(0, 19);
  } catch {
    return null;
  }
};

export const convertFromISODateTime = (isoString) => {
  if (!isoString || !String(isoString).trim()) {
    return "";
  }

  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) {
      return "";
    }
    return date.toLocaleDateString("en-US");
  } catch {
    return "";
  }
};
