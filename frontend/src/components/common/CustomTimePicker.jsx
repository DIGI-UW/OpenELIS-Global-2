import React, { useEffect, useMemo, useState } from "react";
import { Select, SelectItem } from "@carbon/react";

const pad = (n) => String(n).padStart(2, "0");

const parseTime = (value) => {
  if (!value || typeof value !== "string") {
    return { hour: "", minute: "" };
  }
  const match = value.match(/^(\d{1,2}):(\d{1,2})/);
  if (!match) {
    return { hour: "", minute: "" };
  }
  const h = Math.min(23, Math.max(0, parseInt(match[1], 10)));
  const m = Math.min(59, Math.max(0, parseInt(match[2], 10)));
  return { hour: pad(h), minute: pad(m) };
};

const CustomTimePicker = (props) => {
  const initial = parseTime(props.value);
  const [hour, setHour] = useState(initial.hour);
  const [minute, setMinute] = useState(initial.minute);

  useEffect(() => {
    if (initial.hour !== "" && initial.minute !== "") {
      props.onChange(`${initial.hour}:${initial.minute}`);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const parsed = parseTime(props.value);
    setHour(parsed.hour);
    setMinute(parsed.minute);
  }, [props.value]);

  const hours = useMemo(() => Array.from({ length: 24 }, (_, i) => pad(i)), []);
  const minutes = useMemo(
    () => Array.from({ length: 60 }, (_, i) => pad(i)),
    [],
  );

  const emit = (h, m) => {
    if (h !== "" && m !== "") {
      props.onChange(`${h}:${m}`);
    } else if (h === "" && m === "") {
      props.onChange("");
    }
  };

  const handleHour = (e) => {
    const h = e.target.value;
    setHour(h);
    emit(h, minute);
  };

  const handleMinute = (e) => {
    const m = e.target.value;
    setMinute(m);
    emit(hour, m);
  };

  return (
    <div
      style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}
      className={props.className}
    >
      <Select
        id={`${props.id}_hour`}
        labelText={props.labelText == null ? "" : props.labelText}
        value={hour}
        onChange={handleHour}
      >
        <SelectItem value="" text="hh" />
        {hours.map((h) => (
          <SelectItem key={h} value={h} text={h} />
        ))}
      </Select>
      <Select
        id={`${props.id}_minute`}
        labelText=""
        value={minute}
        onChange={handleMinute}
      >
        <SelectItem value="" text="mm" />
        {minutes.map((m) => (
          <SelectItem key={m} value={m} text={m} />
        ))}
      </Select>
    </div>
  );
};

export default CustomTimePicker;
