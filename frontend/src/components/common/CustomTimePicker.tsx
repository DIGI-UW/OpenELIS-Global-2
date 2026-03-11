import React, { useEffect, useState } from "react";
import { TimePicker } from "@carbon/react";

export interface CustomTimePickerProps {
  id?: string;
  value?: string;
  labelText?: React.ReactNode;
  onChange: (value: string) => void;
}

const CustomTimePicker: React.FC<CustomTimePickerProps> = ({
  id,
  value = "",
  labelText = "",
  onChange,
}) => {
  const [currentTime, setCurrentTime] = useState<string>(value);

  const handleTimePicker = (e: React.ChangeEvent<HTMLInputElement>) => {
    const time = e.target.value;
    setCurrentTime(time);
    onChange(time);
  };

  useEffect(() => {
    onChange(currentTime);
  }, [currentTime, onChange]);

  return (
    <TimePicker
      id={id ?? "custom-time-picker"}
      value={currentTime}
      onChange={handleTimePicker}
      labelText={labelText}
    />
  );
};

export default CustomTimePicker;
