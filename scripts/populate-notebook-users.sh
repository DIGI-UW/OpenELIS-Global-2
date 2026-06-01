#!/bin/bash
#
# populate-notebook-users.sh
#
# Creates SRS persona smoke users (department + persona) for notebook RBAC proof.
# Also supports legacy one-user-per-department rows (optional).
#
# Usage:
#   ./scripts/populate-notebook-users.sh              # persona users (default)
#   ./scripts/populate-notebook-users.sh --legacy-dept  # also create dept-only users 1000-1012
#   ./scripts/populate-notebook-users.sh --clean-install
#   ./scripts/populate-notebook-users.sh --clean
#
# Local smoke password for all demo users: adminADMIN!
#

set -e

CONTAINER="${CONTAINER:-openelisglobal-database}"
DB_NAME="${DB_NAME:-clinlims}"
DB_USER="${DB_USER:-clinlims}"
CLEAN_ONLY=false
CLEAN_INSTALL=false
DRY_RUN=false
LEGACY_DEPT=false

# bcrypt for adminADMIN! — same hash as local admin user (verified login)
PW_HASH='$2a$12$PHzs1wNGcTxIuuDOLl4I7.aMxDtiD5puOwMYd2Nxa.I7luPh7k1hm'

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

while [[ $# -gt 0 ]]; do
    case $1 in
        --container) CONTAINER="$2"; shift 2 ;;
        -d|--database) DB_NAME="$2"; shift 2 ;;
        -U|--user) DB_USER="$2"; shift 2 ;;
        -c|--clean) CLEAN_ONLY=true; shift ;;
        --clean-install) CLEAN_INSTALL=true; shift ;;
        --dry-run) DRY_RUN=true; shift ;;
        --legacy-dept) LEGACY_DEPT=true; shift ;;
        --help)
            head -30 "$0" | tail -27
            exit 0
            ;;
        *) echo -e "${RED}Unknown option: $1${NC}"; exit 1 ;;
    esac
done

if [ "$DRY_RUN" = false ]; then
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: docker not found${NC}"
        exit 1
    fi
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
        echo -e "${RED}Error: container '${CONTAINER}' not running${NC}"
        exit 1
    fi
fi

execute_sql() {
    if [ "$DRY_RUN" = true ]; then
        cat
    else
        docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
    fi
}

echo -e "${GREEN}=== AHRI Notebook / SRS Persona Smoke Users ===${NC}"

do_clean() {
    echo -e "${YELLOW}Removing demo users (ids 1000-1119)...${NC}"
    execute_sql <<'EOF'
DELETE FROM clinlims.system_user_section WHERE system_user_id BETWEEN 1000 AND 1119;
DELETE FROM clinlims.system_user_role WHERE system_user_id BETWEEN 1000 AND 1119;
DELETE FROM clinlims.lab_unit_roles WHERE system_user_id BETWEEN 1000 AND 1119;
DELETE FROM clinlims.user_lab_unit_roles WHERE system_user_id BETWEEN 1000 AND 1119;
-- Keep login_user/system_user rows: notebook history and entries may reference
-- smoke users. The seed step below updates them idempotently via ON CONFLICT.
EOF
    echo -e "${GREEN}Cleanup complete.${NC}"
}

if [ "$CLEAN_ONLY" = true ]; then
    do_clean
    exit 0
fi

if [ "$CLEAN_INSTALL" = true ]; then
    do_clean
    echo ""
fi

echo -e "${CYAN}Seeding System Admin role and smoke notebook workflow types...${NC}"
execute_sql <<'EOF'
INSERT INTO clinlims.system_role (id, name, description, is_grouping_role, grouping_parent, display_key, active, editable)
SELECT nextval('clinlims.system_role_seq'), 'System Admin',
       'System configuration without automatic unrestricted scientific lab data access.',
       FALSE, parent.id, 'role.systemAdmin', TRUE, TRUE
FROM clinlims.system_role parent
WHERE parent.name = 'Global Roles'
  AND NOT EXISTS (SELECT 1 FROM clinlims.system_role existing WHERE existing.name = 'System Admin');

UPDATE clinlims.notebook SET workflow_type = 'mntd'
WHERE id IN (SELECT DISTINCT nb.id FROM clinlims.notebook nb
             JOIN clinlims.notebook_departments nd ON nd.notebook_id = nb.id
             JOIN clinlims.test_section ts ON ts.id = nd.test_section_id
             WHERE ts.name ILIKE '%MNTD%' OR ts.name ILIKE '%Malaria and Neglected%');

UPDATE clinlims.notebook SET workflow_type = 'biorepository'
WHERE id IN (SELECT DISTINCT nb.id FROM clinlims.notebook nb
             JOIN clinlims.notebook_departments nd ON nd.notebook_id = nb.id
             JOIN clinlims.test_section ts ON ts.id = nd.test_section_id
             WHERE ts.name ILIKE '%Biorepository%');
EOF

# username|id|first_name|test_section_id|dept_role_id|dept_role_name|global_role_name
PERSONAS=(
  "mntd_collector|1100|MNTD Collector|177|86|Sample Collector|"
  "mntd_technician|1101|MNTD Technician|177|87|Laboratory Technician|"
  "mntd_researcher|1102|MNTD Researcher|177|128|Junior Researcher|"
  "mntd_manager|1103|MNTD Manager|177|126|Lab Manager|"
  "mntd_biomedical|1104|MNTD Biomedical|177|121|Biomedical Staff|"
  "biorepo_collector|1105|Biorepo Collector|182|86|Sample Collector|"
  "biorepo_technician|1106|Biorepo Technician|182|87|Laboratory Technician|"
  "biorepo_researcher|1107|Biorepo Researcher|182|128|Junior Researcher|"
  "biorepo_manager|1108|Biorepo Manager|182|126|Lab Manager|"
  "global_admin|1109|Global Admin|-|-|-|Global Administrator"
  "system_admin|1110|System Admin|-|-|-|System Admin"
  "admin_staff|1111|Admin Staff|-|-|-|Administrative Staff"
  "eqa_user|1112|EQA User|-|-|-|EQA Personnel"
)

create_persona() {
    local username="$1"
    local user_id="$2"
    local first_name="$3"
    local test_section_id="$4"
    local dept_role_id="$5"
    local dept_role_name="$6"
    local global_role_name="$7"

    echo -e "${CYAN}User: ${username}${NC} (${dept_role_name:-}${global_role_name:+/ ${global_role_name}})"

    execute_sql <<EOF
DO \$\$
DECLARE
    v_map_id INTEGER;
    v_global_role_id INTEGER;
BEGIN
    INSERT INTO clinlims.login_user (id, login_name, password, password_expired_dt, account_locked, account_disabled, is_admin, user_time_out)
    VALUES (${user_id}, '${username}', '${PW_HASH}', '2027-12-31', 'N', 'N', 'N', '480')
    ON CONFLICT (id) DO UPDATE SET login_name = EXCLUDED.login_name, password = EXCLUDED.password;

    INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, initials, is_active, is_employee, lastupdated)
    VALUES (${user_id}, '${username}', '${first_name}', 'Smoke', UPPER(LEFT('${username}', 3)), 'Y', 'Y', NOW())
    ON CONFLICT (id) DO UPDATE SET first_name = EXCLUDED.first_name;

    DELETE FROM clinlims.system_user_role WHERE system_user_id = ${user_id};
    DELETE FROM clinlims.lab_unit_roles WHERE system_user_id = ${user_id};
    DELETE FROM clinlims.user_lab_unit_roles WHERE system_user_id = ${user_id};

    IF NULLIF('${dept_role_id}', '') IS NOT NULL
       AND NULLIF('${test_section_id}', '') IS NOT NULL
       AND '${test_section_id}' <> '-' THEN
        INSERT INTO clinlims.lab_unit_role_map (lab_unit)
        VALUES ('${test_section_id}')
        RETURNING lab_unit_role_map_id INTO v_map_id;

        INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role)
        VALUES (v_map_id, '${dept_role_id}');

        INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated)
        VALUES (${user_id}, NOW())
        ON CONFLICT (system_user_id) DO UPDATE SET last_updated = NOW();

        INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id)
        VALUES (${user_id}, v_map_id);
    END IF;

    IF NULLIF('${global_role_name}', '') IS NOT NULL THEN
        SELECT id INTO v_global_role_id FROM clinlims.system_role WHERE name = '${global_role_name}' LIMIT 1;
        IF v_global_role_id IS NOT NULL THEN
            INSERT INTO clinlims.system_user_role (system_user_id, role_id)
            VALUES (${user_id}, v_global_role_id);
        ELSE
            RAISE NOTICE 'Global role not found: ${global_role_name}';
        END IF;
    END IF;
END \$\$;
EOF
}

echo ""
echo -e "${YELLOW}Creating SRS persona users (1100-1112)...${NC}"
for entry in "${PERSONAS[@]}"; do
    IFS='|' read -r u i f ts dr drn grn <<< "$entry"
    [ "$ts" = "-" ] && ts=""
    [ "$dr" = "-" ] && dr=""
    [ "$drn" = "-" ] && drn=""
    [ "$grn" = "-" ] && grn=""
    create_persona "$u" "$i" "$f" "$ts" "$dr" "$drn" "$grn"
done

if [ "$LEGACY_DEPT" = true ]; then
    echo ""
    echo -e "${YELLOW}Creating legacy department-only users (1000-1012)...${NC}"
    LEGACY_NOTEBOOKS=(
        "Immunology|immuno|Immunology|1000"
        "Pathology Laboratory|patho|Pathology|1001"
        "Bacteriology|bacterio|Bacteriology|1002"
        "Malaria and Neglected Tropical Disease (MNTD) Laboratory|mntd|MNTD|1003"
        "Pharmaceuticals Laboratory|pharma|Pharmaceuticals|1004"
        "Tuberculosis Laboratory|tb|Tuberculosis|1006"
        "Biorepository Laboratory|biorepo|Biorepository|1008"
    )
    for notebook_entry in "${LEGACY_NOTEBOOKS[@]}"; do
        IFS='|' read -r lab_unit username first_name user_id <<< "$notebook_entry"
        execute_sql <<EOF
DO \$\$
DECLARE v_test_section_id INTEGER; v_lab_unit_role_map_id INTEGER;
BEGIN
    SELECT id INTO v_test_section_id FROM clinlims.test_section WHERE name = '${lab_unit}' LIMIT 1;
    IF v_test_section_id IS NULL THEN RETURN; END IF;
    INSERT INTO clinlims.login_user (id, login_name, password, password_expired_dt, account_locked, account_disabled, is_admin, user_time_out)
    VALUES (${user_id}, '${username}', '${PW_HASH}', '2027-12-31', 'N', 'N', 'N', '480')
    ON CONFLICT (id) DO UPDATE SET login_name = EXCLUDED.login_name;
    INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, initials, is_active, is_employee, lastupdated)
    VALUES (${user_id}, '${username}', '${first_name}', 'User', UPPER(LEFT('${username}', 3)), 'Y', 'Y', NOW())
    ON CONFLICT (id) DO UPDATE SET first_name = EXCLUDED.first_name;
    INSERT INTO clinlims.lab_unit_role_map (lab_unit) VALUES (v_test_section_id::VARCHAR)
    RETURNING lab_unit_role_map_id INTO v_lab_unit_role_map_id;
    INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role) VALUES
      (v_lab_unit_role_map_id, '86'), (v_lab_unit_role_map_id, '87');
    INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (${user_id}, NOW()) ON CONFLICT DO NOTHING;
    INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (${user_id}, v_lab_unit_role_map_id) ON CONFLICT DO NOTHING;
END \$\$;
EOF
    done
fi

execute_sql <<'EOF'
SELECT setval('clinlims.login_user_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM clinlims.login_user)::bigint, 1119::bigint) + 1, false);
SELECT setval('clinlims.system_user_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM clinlims.system_user)::bigint, 1119::bigint) + 1, false);
EOF

echo ""
echo -e "${GREEN}=== Persona users ready ===${NC}"
if [ "$DRY_RUN" = false ]; then
    execute_sql -c "
SELECT su.login_name AS username,
       COALESCE(ts.name, '(global only)') AS department,
       COALESCE(sr_lab.name, '-') AS lab_persona,
       COALESCE(sr_glob.name, '-') AS global_role
FROM clinlims.system_user su
LEFT JOIN clinlims.lab_unit_roles lur ON lur.system_user_id = su.id
LEFT JOIN clinlims.lab_unit_role_map lurm ON lurm.lab_unit_role_map_id = lur.lab_unit_role_map_id
LEFT JOIN clinlims.lab_roles lr ON lr.lab_unit_role_map_id = lurm.lab_unit_role_map_id
LEFT JOIN clinlims.system_role sr_lab ON sr_lab.id::text = lr.role
LEFT JOIN clinlims.test_section ts ON ts.id = CASE
    WHEN lurm.lab_unit ~ '^[0-9]+$' THEN lurm.lab_unit::integer
    ELSE NULL
END
LEFT JOIN clinlims.system_user_role sur ON sur.system_user_id = su.id
LEFT JOIN clinlims.system_role sr_glob ON sr_glob.id = sur.role_id
WHERE su.id BETWEEN 1100 AND 1112
ORDER BY su.id;
"
fi
echo ""
echo "Password (local smoke only): adminADMIN!"
