export const isDeviceSelectionRequired = (deviceCount, isGlobalAdmin) =>
  deviceCount > 1 && !isGlobalAdmin;

export const validateRoundCapacity = (
  boxesPerRound,
  samplesPerBox,
  eligibleBoxes,
  eligibleSamples,
) => {
  const requestedSamples = boxesPerRound * samplesPerBox;
  if (eligibleBoxes < boxesPerRound) {
    return { ok: false, reason: "boxes" };
  }
  if (eligibleSamples < requestedSamples) {
    return { ok: false, reason: "samples" };
  }
  return { ok: true, reason: null };
};
