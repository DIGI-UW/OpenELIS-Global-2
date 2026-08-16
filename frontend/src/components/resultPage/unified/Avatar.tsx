import React, { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * OGC-811 gallery parity — the patient avatar shown on each clinical analysis
 * row. Shows the patient's photo (thumbnail from /rest/patient-photos) when
 * one exists; falls back to the gallery's initials chip when there is no
 * photo, no patient id, or the image fails to load — the same
 * photo-else-initials contract as AsyncAvatar on the legacy pages.
 * Deterministic chip color from a stable hash of the patient id/name,
 * initials from the first letter of each comma-separated name part.
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

// One thumbnail fetch per patient per page load — worklists repeat the same
// patient across many rows. null = known to have no photo.
const photoCache = new Map<string, string | null>();
const photoWaiters = new Map<string, ((photo: string | null) => void)[]>();

export const clearAvatarPhotoCache = (): void => {
  photoCache.clear();
  photoWaiters.clear();
};

const toDataUri = (data?: string): string | null => {
  if (!data || data.trim() === "") {
    return null;
  }
  return data.startsWith("data:") ? data : `data:image/jpeg;base64,${data}`;
};

const fetchPhoto = (id: string, onPhoto: (photo: string | null) => void) => {
  if (photoCache.has(id)) {
    onPhoto(photoCache.get(id) ?? null);
    return;
  }
  const waiters = photoWaiters.get(id);
  if (waiters) {
    waiters.push(onPhoto);
    return;
  }
  photoWaiters.set(id, [onPhoto]);
  getFromOpenElisServer(
    `/rest/patient-photos/${id}/true`,
    (response?: { data?: string }) => {
      const photo = toDataUri(response?.data);
      photoCache.set(id, photo);
      const pending = photoWaiters.get(id) || [];
      photoWaiters.delete(id);
      pending.forEach((waiter) => waiter(photo));
    },
  );
};

const Avatar: React.FC<{ name?: string; id?: string; size?: number }> = ({
  name,
  id,
  size = 28,
}) => {
  const [photo, setPhoto] = useState<string | null>(
    id ? (photoCache.get(id) ?? null) : null,
  );
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }
    let mounted = true;
    fetchPhoto(id, (result) => {
      if (mounted) {
        setPhoto(result);
      }
    });
    return () => {
      mounted = false;
    };
  }, [id]);

  if (!name) {
    return null;
  }
  if (photo && !broken) {
    return (
      <span
        className="unifiedAvatar unifiedAvatar--photo"
        style={{ width: size, height: size }}
        data-testid="row-avatar"
      >
        <img
          src={photo}
          alt={name}
          data-testid="row-avatar-photo"
          onError={() => setBroken(true)}
        />
      </span>
    );
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
