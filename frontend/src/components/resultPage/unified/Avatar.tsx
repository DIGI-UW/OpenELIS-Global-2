import React from "react";

/**
 * OGC-811 gallery parity — the patient initials chip shown on each clinical
 * analysis row (same affordance the legacy Results page had). Deterministic
 * color from a stable hash of the patient id/name, initials from the first
 * letter of each comma-separated name part.
 */
const PALETTE = [
  "#0f62fe",
  "#8a3ffc",
  "#198038",
  "#ff832b",
  "#d12771",
  "#005d5d",
];

export const avatarColor = (seed: string): string => {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = ((hash << 5) - hash + seed.charCodeAt(i)) | 0;
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
};

export const avatarInitials = (name: string): string =>
  name
    .split(",")
    .map((part) => part.trim()[0] || "")
    .join("")
    .slice(0, 2)
    .toUpperCase();

const Avatar: React.FC<{ name?: string; id?: string; size?: number }> = ({
  name,
  id,
  size = 28,
}) => {
  if (!name) {
    return null;
  }
  return (
    <span
      className="unifiedAvatar"
      style={{
        width: size,
        height: size,
        background: avatarColor(id || name),
      }}
      data-testid="row-avatar"
    >
      {avatarInitials(name)}
    </span>
  );
};

export default Avatar;
