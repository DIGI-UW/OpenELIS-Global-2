-- Maps every active catalog test carrying one of these LOINCs; vector-pathogen
-- sections are skipped because 32700-7 also tags the mosquito malaria assay.
INSERT INTO clinlims.test_surveillance_mapping
    (id, test_id, program_id, metric_label, metric_key, match_mode, numeric_threshold, display_order)
SELECT nextval('clinlims.test_surveillance_mapping_seq'), t.id, p.id, v.metric_label, v.metric_key,
       v.match_mode, v.numeric_threshold, v.display_order
FROM (VALUES
    ('TB',      '85362-2', 'MTB detected',            'MTB_DETECTION',  'ALL_ABNORMAL',  NULL::numeric, 1),
    ('TB',      '46244-0', 'RIF resistance',          'MTB_RESISTANCE', 'ALL_ABNORMAL',  NULL,          2),
    ('TB',      '11477-7', 'Smear positive',          'SMEAR_POSITIVE', 'ALL_ABNORMAL',  NULL,          3),
    ('HIV',     '68961-2', 'HIV positive',            'HIV_POSITIVE',   'ALL_ABNORMAL',  NULL,          1),
    ('HIV',     '10351-5', 'VL suppressed (< 1000)',  'VL_SUPPRESSED',  'NUMERIC_BELOW', 1000,          2),
    ('HIV',     '20447-9', 'VL suppressed (< 1000)',  'VL_SUPPRESSED',  'NUMERIC_BELOW', 1000,          2),
    ('MALARIA', '51587-4', 'RDT positive',            'RDT_POSITIVE',   'ALL_ABNORMAL',  NULL,          1),
    ('MALARIA', '32700-7', 'Smear positive',          'SMEAR_POSITIVE', 'ALL_ABNORMAL',  NULL,          2)
) AS v(program_code, loinc, metric_label, metric_key, match_mode, numeric_threshold, display_order)
JOIN clinlims.surveillance_program p ON p.code = v.program_code
JOIN clinlims.test t ON t.loinc = v.loinc AND t.is_active = 'Y'
LEFT JOIN clinlims.test_section ts ON ts.id = t.test_section_id
WHERE COALESCE(ts.domain, '') <> 'VECTOR'
  AND NOT EXISTS (
      SELECT 1 FROM clinlims.test_surveillance_mapping m
      WHERE m.program_id = p.id AND m.test_id = t.id AND m.metric_key = v.metric_key);
