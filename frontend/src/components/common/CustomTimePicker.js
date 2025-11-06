import React, { useEffect, useState } from "react";
import { TimePicker } from "@carbon/react";

const CustomTimePicker = (props) => {
  const [currentTime, setCurrentTime] = useState(
    props.value ? props.value : "",
  );

  function handleTimePicker(e) {
    let time = e.target.value;
    setCurrentTime(time);
    props.onChange(time);
  }
  useEffect(() => {
    props.onChange(currentTime);
  }, [currentTime]);

  return (
    <>
      <TimePicker
        id={props.id}
        value={currentTime == null ? "" : currentTime}
        onChange={(e) => handleTimePicker(e)}
        labelText={props.labelText == null ? "" : props.labelText}
      />
    </>
  );
};

export default CustomTimePicker;
