import React, { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
} from "@carbon/react";
import config from "../../config.json";
import { accessLogPath, getAccessLog, type AccessLogEntry } from "./addonApi";

/**
 * The access log of one add-on: every request it made to the add-on API, newest
 * first. The agent and purpose columns are what the add-on reported; the rest
 * is recorded by OpenELIS.
 */
export default function AccessLog({ addonId }: { addonId: string }) {
  const intl = useIntl();
  const [entries, setEntries] = useState<AccessLogEntry[] | null>(null);
  const [retention, setRetention] = useState<number | null>(null);
  const [failed, setFailed] = useState(false);
  const [actor, setActor] = useState("");
  const [subject, setSubject] = useState("");

  const load = () => {
    setFailed(false);
    getAccessLog(addonId, { actor, subject })
      .then((r) => {
        setEntries(r.entries || []);
        setRetention(r.retentionDays);
      })
      .catch(() => setFailed(true));
  };
  useEffect(load, [addonId]);

  const headers = [
    "addon.accessLog.at",
    "addon.accessLog.actor",
    "addon.accessLog.purpose",
    "addon.accessLog.endpoint",
    "addon.accessLog.subject",
    "addon.accessLog.status",
    "addon.accessLog.rows",
  ];
  return (
    <div
      className="addon-admin__section"
      data-testid={`addon-access-log-${addonId}`}
    >
      <h4>
        <FormattedMessage id="addon.accessLog.title" />
      </h4>
      <p className="addon-admin__meta">
        <FormattedMessage
          id="addon.accessLog.intro"
          values={{ days: retention ?? "…" }}
        />
      </p>
      <div className="addon-admin__form addon-admin__filters">
        <TextInput
          id={`addon-log-actor-${addonId}`}
          size="sm"
          labelText={intl.formatMessage({ id: "addon.accessLog.actor" })}
          value={actor}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            setActor(e.target.value)
          }
        />
        <TextInput
          id={`addon-log-subject-${addonId}`}
          size="sm"
          labelText={intl.formatMessage({ id: "addon.accessLog.subject" })}
          value={subject}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            setSubject(e.target.value)
          }
        />
        <div>
          <Button size="sm" kind="tertiary" onClick={load}>
            <FormattedMessage id="addon.accessLog.filter" />
          </Button>{" "}
          <Button
            size="sm"
            kind="ghost"
            href={`${config.serverBaseUrl}${accessLogPath(addonId, {
              actor,
              subject,
              limit: 5000,
              format: "csv",
            })}`}
          >
            <FormattedMessage id="addon.accessLog.csv" />
          </Button>
        </div>
      </div>
      {failed && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "addon.admin.error" })}
        />
      )}
      {!entries && !failed && <InlineLoading />}
      {entries && entries.length === 0 && (
        <p>
          <FormattedMessage id="addon.accessLog.empty" />
        </p>
      )}
      {entries && entries.length > 0 && (
        <Table size="sm" useZebraStyles>
          <TableHead>
            <TableRow>
              {headers.map((h) => (
                <TableHeader key={h}>
                  <FormattedMessage id={h} />
                </TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {entries.map((e, i) => (
              <TableRow key={`${e.at}-${i}`}>
                <TableCell>
                  {intl.formatDate(e.at, {
                    dateStyle: "short",
                    timeStyle: "medium",
                  } as Intl.DateTimeFormatOptions)}
                </TableCell>
                <TableCell>
                  {e.actor ||
                    (e.credential === "user"
                      ? intl.formatMessage(
                          { id: "addon.accessLog.user" },
                          { user: e.userId || "?" },
                        )
                      : "—")}
                </TableCell>
                <TableCell>{e.purpose || "—"}</TableCell>
                <TableCell>
                  {e.method} {e.endpoint}
                </TableCell>
                <TableCell>{e.subject || "—"}</TableCell>
                <TableCell>{e.status}</TableCell>
                <TableCell>{e.rows ?? ""}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
