import React, { useEffect, useState } from "react";
import { TimePicker } from "@carbon/react";

const TIME_REGEX = /^([01]\d|2[0-3]):([0-5]\d)$/;

const CustomTimePicker = (props) => {
  const getCurrentTime = () => {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  const [currentTime, setCurrentTime] = useState(
    props.value ? props.value : getCurrentTime(),
  );
  const [isInvalid, setIsInvalid] = useState(false);

  const validateTime = (time) => {
    if (!time || time === "") {
      return true;
    }
    return TIME_REGEX.test(time);
  };

  function handleTimePicker(e) {
    let time = e.target.value;
    const isDeleting = time.length < currentTime.length;

    time = time.replace(/[^\d:]/g, "");

    if (!isDeleting) {
      if (time.length === 2 && !time.includes(":")) {
        time = time + ":";
      }

      if (time.length > 2 && !time.includes(":")) {
        time = time.slice(0, 2) + ":" + time.slice(2);
      }
    }

    if (time.length > 5) {
      time = time.slice(0, 5);
    }

    if (time.length >= 2) {
      const hoursStr = time.substring(0, 2);
      const hours = parseInt(hoursStr, 10);
      if (!isNaN(hours) && hours > 23) {
        time = "23" + (time.length > 2 ? time.substring(2) : "");
      }
    }

    if (time.length === 5) {
      const minsStr = time.substring(3, 5);
      const mins = parseInt(minsStr, 10);
      if (!isNaN(mins) && mins > 59) {
        time = time.substring(0, 3) + "59";
      }
    }

    const isValid = validateTime(time);
    setIsInvalid(time.length === 5 && !isValid);

    setCurrentTime(time);
    props.onChange(time);
  }

  useEffect(() => {
    if (props.value !== currentTime && props.value !== undefined) {
      setCurrentTime(props.value || "");
      setIsInvalid(!validateTime(props.value) && props.value?.length === 5);
    }
  }, [props.value]);

  useEffect(() => {
    if (currentTime) {
      props.onChange(currentTime);
    }
  }, []);

  return (
    <>
      <TimePicker
        id={props.id}
        value={currentTime == null ? "" : currentTime}
        onChange={(e) => handleTimePicker(e)}
        labelText={props.labelText == null ? "" : props.labelText}
        invalid={isInvalid}
        invalidText="Invalid time format. Use HH:mm (00:00-23:59)"
        maxLength={5}
        pattern="[0-9]{2}:[0-9]{2}"
        placeholder="hh:mm"
      />
    </>
  );
};

export default CustomTimePicker;
