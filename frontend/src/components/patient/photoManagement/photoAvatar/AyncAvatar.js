import React, { useState, useEffect } from "react";
import { UserAvatar } from "@carbon/icons-react";
import { SkeletonPlaceholder } from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import "./AsyncAvatar.css";

/**
 * Composant AsyncAvatar
 *
 * @param {Object} props
 * @param {number|string} props.patientId - patient ID
 * @param {boolean} props.hasPhoto - enable photo load from backend ( default true)
 * @param {string} props.patientName - Patient Name (optionnal)
 * @param {number} props.size - photo size (default: 40)
 *
 */
const AsyncAvatar = ({
  patientId,
  hasPhoto,
  patientName = "Patient",
  size = 40,
  gender,
}) => {
  const [thumbnail, setThumbnail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!hasPhoto) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(false);
    setThumbnail(null);

    getFromOpenElisServer(
      `/rest/patient-photos/${patientId}/${true}`,
      (response) => {
        if (response && response.data) {
          setThumbnail(response.data);
          setError(false);
        } else {
          setError(true);
        }
        setLoading(false);
      },
    );
  }, [patientId, hasPhoto]);

  if (!hasPhoto) {
    return (
      <div
        className="async-avatar-placeholder"
        style={{ width: size, height: size }}
      >
        <UserAvatar size={Math.floor(size * 0.6)} />
      </div>
    );
  }

  if (loading) {
    return (
      <div
        className="async-avatar-skeleton"
        style={{ width: size, height: size }}
      >
        <SkeletonPlaceholder
          style={{ width: size, height: size, borderRadius: "50%" }}
        />
      </div>
    );
  }

  if (error || !thumbnail) {
    return (
      <div
        className="async-avatar-placeholder"
        style={{ width: size, height: size }}
      >
        <UserAvatar size={Math.floor(size * 0.6)} />
      </div>
    );
  }

  return (
    <div
      className="async-avatar-container"
      style={{ width: size, height: size }}
    >
      <img
        src={"data:image/jpeg;base64," + thumbnail}
        alt={patientName}
        className="async-avatar-image"
        style={{ width: size, height: size }}
      />
    </div>
  );
};

export default AsyncAvatar;
