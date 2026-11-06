import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

const SCHEMA = "clinlims";

function psql(sql: string): void {
  const container = resolveDbContainer();
  execFileSync(
    "docker",
    [
      "exec",
      "-i",
      container,
      "psql",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-c",
      sql,
    ],
    { encoding: "utf8" },
  );
}

function esc(value: string): string {
  return `'${value.replace(/'/g, "''")}'`;
}

/**
 * Seed a freezer with one OPEN critical alert directly in the database.
 * Alerts have no create API (they are raised by the monitoring service), so the
 * rows are inserted via docker exec psql against the compose DB container.
 * The alert references the freezer by name (subquery) rather than capturing an
 * id from psql output, and sets last_updated because the alert entity uses it
 * as an optimistic-lock @Version (a NULL there makes resolve/acknowledge fail).
 * Idempotent: any prior row with the same name is removed first.
 */
export function seedFreezerWithOpenAlert(name: string): void {
  cleanupFreezerAlert(name);
  psql(
    `INSERT INTO ${SCHEMA}.freezer ` +
      `(id, name, protocol, slave_id, temperature_register, target_temperature, warning_threshold, critical_threshold, active) ` +
      `VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM ${SCHEMA}.freezer), ` +
      `${esc(name)}, 'RTU', 1, 0, -20, -15, -10, true);`,
  );
  psql(
    `INSERT INTO ${SCHEMA}.alert ` +
      `(id, alert_type, alert_entity_type, alert_entity_id, severity, status, start_time, last_updated, message, duplicate_count) ` +
      `VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM ${SCHEMA}.alert), ` +
      `'FREEZER_TEMPERATURE', 'Freezer', (SELECT id FROM ${SCHEMA}.freezer WHERE name = ${esc(name)}), ` +
      `'CRITICAL', 'OPEN', now(), now(), 'E2E cold-storage alert', 0);`,
  );
}

export function cleanupFreezerAlert(name: string): void {
  try {
    psql(
      `DELETE FROM ${SCHEMA}.alert WHERE alert_entity_type = 'Freezer' ` +
        `AND alert_entity_id IN (SELECT id FROM ${SCHEMA}.freezer WHERE name = ${esc(name)}); ` +
        `DELETE FROM ${SCHEMA}.freezer WHERE name = ${esc(name)};`,
    );
  } catch (e) {
    console.warn(`cleanupFreezerAlert failed for "${name}": ${e}`);
  }
}
