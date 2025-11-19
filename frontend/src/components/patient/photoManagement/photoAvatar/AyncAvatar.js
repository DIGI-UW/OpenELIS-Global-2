import React, { useState, useEffect } from "react";
import { UserAvatar } from "@carbon/icons-react";
import { SkeletonPlaceholder } from "@carbon/react";
import Avatar from "react-avatar";
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
  const [imageLoadError, setImageLoadError] = useState(false);

  useEffect(() => {
    if (!hasPhoto) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(false);
    setThumbnail(null);
    setImageLoadError(false);

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
      <Avatar
        alt="Patient avatar"
        color="rgba(0,0,0,0)"
        name={patientName}
        src=""
        size={String(size)}
        textSizeRatio={1}
        style={{
          backgroundImage: `url('/images/patient-background.svg')`,
          backgroundRepeat: "round",
        }}
      />
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
      <Avatar
        alt="Patient avatar"
        color="rgba(0,0,0,0)"
        name={patientName}
        src=""
        size={String(size)}
        textSizeRatio={1}
        style={{
          backgroundImage: `url('/images/patient-background.svg')`,
          backgroundRepeat: "round",
        }}
      />
    );
  }

  // If image fails to load, show generated avatar with initials
  if (imageLoadError) {
    return (
      <Avatar
        alt="Patient avatar"
        color="rgba(0,0,0,0)"
        name={patientName}
        src=""
        size={String(size)}
        textSizeRatio={1}
        style={{
          backgroundImage: `url('/images/patient-background.svg')`,
          backgroundRepeat: "round",
        }}
      />
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
        style={{ width: size, height: size, borderRadius: "50%" }}
        onError={() => {
          // If image fails to load, show generated avatar with initials
          setImageLoadError(true);
        }}
      />
    </div>
  );
};

export default AsyncAvatar;
