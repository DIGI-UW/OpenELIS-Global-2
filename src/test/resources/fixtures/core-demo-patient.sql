SET search_path TO clinlims;

-- Core Playwright demo patient fixture.
-- Required by demo/core/ogc-284-barcode-workflow.spec.ts patient selection.

INSERT INTO person (id, last_name, first_name, middle_name, city, state, zip_code, country,
                    work_phone, home_phone, cell_phone, primary_phone, email, lastupdated)
SELECT 9001000, 'TEST-Smith', 'John', 'Test', 'Test City', 'Test State', '12345', 'USA',
       '555-0101', '555-0102', '555-0103', '555-0101', 'john.test@openelis.org', NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM person
  WHERE first_name = 'John' AND last_name = 'TEST-Smith'
);

INSERT INTO patient (id, person_id, race, gender, birth_date, birth_time, national_id,
                     ethnicity, external_id, entered_birth_date, lastupdated)
SELECT 9001000, p.id, 'black', 'M', '1990-01-15 00:00:00'::timestamp,
       '1990-01-15 10:00:00'::timestamp, 'E2E-PAT-001', 'U', 'E2E-PAT-001',
       '01/15/1990', NOW()
FROM person p
WHERE p.first_name = 'John'
  AND p.last_name = 'TEST-Smith'
  AND NOT EXISTS (
    SELECT 1
    FROM patient pat
    WHERE pat.id = 9001000
       OR pat.external_id = 'E2E-PAT-001'
       OR pat.national_id = 'E2E-PAT-001'
  );
