import React, { useState, useEffect, useRef, useContext } from "react";
import { Checkbox, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, putToOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";

const DAY_KEYS = [
  "calendar.management.day.sun",
  "calendar.management.day.mon",
  "calendar.management.day.tue",
  "calendar.management.day.wed",
  "calendar.management.day.thu",
  "calendar.management.day.fri",
  "calendar.management.day.sat",
];

function WeekendConfig() {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [weekendDays, setWeekendDays] = useState([]);
  const [showSaved, setShowSaved] = useState(false);
  const savedTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (savedTimerRef.current) clearTimeout(savedTimerRef.current);
    };
  }, []);

  useEffect(() => {
    getFromOpenElisServer("/rest/calendar/weekends", (res) => {
      if (res && res.weekendDays) {
        setWeekendDays(res.weekendDays);
      }
    });
  }, []);

  const handleToggle = (dayNum, checked) => {
    const prev = [...weekendDays];
    const newDays = checked
      ? [...weekendDays, dayNum]
      : weekendDays.filter((d) => d !== dayNum);

    setWeekendDays(newDays);

    putToOpenElisServer(
      "/rest/calendar/weekends",
      JSON.stringify({ weekendDays: newDays }),
      (status) => {
        if (status === 200) {
          setShowSaved(true);
          savedTimerRef.current = setTimeout(() => setShowSaved(false), 3000);
        } else {
          setWeekendDays(prev);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "calendar.management.saveError" }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  return (
    <div
      style={{
        marginBottom: "1rem",
        padding: "1rem",
        border: "1px solid var(--cds-border-subtle)",
        borderRadius: "4px",
      }}
    >
      <label
        style={{ fontSize: "12px", fontWeight: 600, marginBottom: "0.5rem" }}
      >
        <FormattedMessage id="calendar.management.weekendDays" />
      </label>
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
        {DAY_KEYS.map((key, idx) => (
          <Checkbox
            key={idx}
            id={`weekend-checkbox-${idx}`}
            data-testid={`weekend-checkbox-${idx}`}
            labelText={intl.formatMessage({ id: key })}
            checked={weekendDays.includes(idx)}
            onChange={(_, { checked }) => handleToggle(idx, checked)}
          />
        ))}
      </div>
      {showSaved && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "calendar.management.weekendSaved",
          })}
          lowContrast
          hideCloseButton
          style={{ marginTop: "0.5rem" }}
        />
      )}
    </div>
  );
}

export default WeekendConfig;
