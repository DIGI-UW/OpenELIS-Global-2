import React, { useState, useEffect, useContext } from "react";
import { Checkbox, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";

const DAY_NAMES = [
  "Sunday",
  "Monday",
  "Tuesday",
  "Wednesday",
  "Thursday",
  "Friday",
  "Saturday",
];

function WeekendConfig() {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [weekendDays, setWeekendDays] = useState([]);
  const [showSaved, setShowSaved] = useState(false);

  useEffect(() => {
    getFromOpenElisServer("/rest/calendar/weekends", (res) => {
      if (res && res.weekendDays) {
        setWeekendDays(res.weekendDays);
      }
    });
  }, []);

  const handleToggle = (dayNum, checked) => {
    const newDays = checked
      ? [...weekendDays, dayNum]
      : weekendDays.filter((d) => d !== dayNum);

    setWeekendDays(newDays);

    fetch("/rest/calendar/weekends", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ weekendDays: newDays }),
    })
      .then((r) => {
        if (r.ok) {
          setShowSaved(true);
          setTimeout(() => setShowSaved(false), 3000);
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "calendar.management.saveError" }),
          });
          setNotificationVisible(true);
        }
      })
      .catch(() => {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "calendar.management.saveError" }),
        });
        setNotificationVisible(true);
      });
  };

  return (
    <div
      style={{
        marginBottom: "1rem",
        padding: "1rem",
        border: "1px solid #e0e0e0",
        borderRadius: "4px",
      }}
    >
      <label
        style={{ fontSize: "12px", fontWeight: 600, marginBottom: "0.5rem" }}
      >
        <FormattedMessage id="calendar.management.weekendDays" />
      </label>
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
        {DAY_NAMES.map((name, idx) => (
          <Checkbox
            key={idx}
            id={`weekend-checkbox-${idx}`}
            data-testid={`weekend-checkbox-${idx}`}
            labelText={name.substring(0, 3)}
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
