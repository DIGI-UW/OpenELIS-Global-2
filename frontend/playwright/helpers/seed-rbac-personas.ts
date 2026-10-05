import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

/**
 * Seed deterministic single-role RBAC personas for privilege E2E (spec 012
 * T029/T044). Idempotent — safe to call in every run.
 *
 * Each persona has exactly one global role, so the tests exercise real
 * privilege inheritance rather than the admin's all-privileges session:
 *   - rbac_reception  → Reception only (no result:enter)
 *   - rbac_validation → Validation only (inherits result:enter via
 *                       Validation → Results → Reception)
 *
 * Direct SQL over `docker exec` mirrors the other seed-* helpers' container
 * contract; system_user / login_user share the same id (no id sequence is
 * assumed). Returns false — tests then self-skip — when Docker/psql is
 * unavailable (e.g. a remote deployment).
 */

// bcrypt($2a$, cost 12) hash of RBAC_PERSONA_PASSWORD, matching the app's
// BCryptPasswordEncoder.
const BCRYPT_HASH =
  "$2a$12$ONId7mVmvzfzWTdnba2iDuua5Gb2HAEkz2QGQN127RS3ApZachqHe";

export const RBAC_PERSONA_PASSWORD = "RBACpass1!";
export const RBAC_RECEPTION_USER = "rbac_reception";
export const RBAC_RESULTS_USER = "rbac_results";
export const RBAC_VALIDATION_USER = "rbac_validation";

const PERSONAS = [
  {
    login: RBAC_RECEPTION_USER,
    role: "Reception",
    first: "Rbac",
    last: "Reception",
  },
  {
    login: RBAC_RESULTS_USER,
    role: "Results",
    first: "Rbac",
    last: "Results",
  },
  {
    login: RBAC_VALIDATION_USER,
    role: "Validation",
    first: "Rbac",
    last: "Validation",
  },
];

export function seedRbacPersonas(): boolean {
  const container = resolveDbContainer();
  const blocks = PERSONAS.map(
    (p) => `
DO $$
DECLARE
  v_user_id numeric;
  v_role_id numeric;
BEGIN
  SELECT id INTO v_role_id FROM clinlims.system_role WHERE trim(name) = '${p.role}';
  IF v_role_id IS NULL THEN RAISE NOTICE 'role ${p.role} missing, skipping ${p.login}'; RETURN; END IF;

  SELECT id INTO v_user_id FROM clinlims.system_user WHERE login_name = '${p.login}';
  IF v_user_id IS NULL THEN
    -- system_user.id has no column default; draw from its sequence explicitly.
    v_user_id := nextval('clinlims.system_user_seq');
    INSERT INTO clinlims.system_user
      (id, login_name, first_name, last_name, initials, is_active, is_employee, lastupdated)
    VALUES (v_user_id, '${p.login}', '${p.first}', '${p.last}', 'RB', 'Y', 'Y', now());
  END IF;

  IF EXISTS (SELECT 1 FROM clinlims.login_user WHERE login_name = '${p.login}') THEN
    UPDATE clinlims.login_user
       SET password = '${BCRYPT_HASH}', account_locked = 'N', account_disabled = 'N',
           password_expired_dt = '2099-01-01', is_admin = 'N'
     WHERE login_name = '${p.login}';
  ELSE
    INSERT INTO clinlims.login_user
      (id, login_name, password, password_expired_dt, account_locked, account_disabled, is_admin, user_time_out, last_updated)
    VALUES (v_user_id, '${p.login}', '${BCRYPT_HASH}', '2099-01-01', 'N', 'N', 'N', '220', now());
  END IF;

  DELETE FROM clinlims.system_user_role WHERE system_user_id = v_user_id;
  INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (v_user_id, v_role_id);
END $$;
`,
  ).join("\n");

  try {
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
        blocks,
      ],
      { stdio: "pipe", timeout: 30000 },
    );
    return true;
  } catch {
    return false;
  }
}
