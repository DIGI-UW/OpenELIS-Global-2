-- 32700-7 also tags the mosquito malaria assay, so vector sections are skipped.
INSERT INTO clinlims.test_surveillance_mapping
    (id, test_id, program_id, metric_label, metric_key, match_mode, positive_result_codes, numeric_threshold,
     display_order)
SELECT nextval('clinlims.test_surveillance_mapping_seq'), t.id, p.id, v.metric_label, v.metric_key,
       v.match_mode, v.positive_result_codes, v.numeric_threshold, v.display_order
FROM (VALUES
    ('TB',      '85362-2', 'label.mgmt.surveillance.metric.mtbDetected',   'MTB_DETECTION',  'ALL_ABNORMAL',  NULL::varchar, NULL::numeric, 1),
    ('TB',      '46244-0', 'label.mgmt.surveillance.metric.rifResistance', 'MTB_RESISTANCE', 'ALL_ABNORMAL',  NULL,          NULL,          2),
    ('TB',      '11477-7', 'label.mgmt.surveillance.metric.smearPositive', 'SMEAR_POSITIVE', 'ALL_ABNORMAL',  NULL,          NULL,          3),
    ('HIV',     '68961-2', 'label.mgmt.surveillance.metric.hivPositive',   'HIV_POSITIVE',   'ALL_ABNORMAL',  NULL,          NULL,          1),
    ('HIV',     '10351-5', 'label.mgmt.surveillance.metric.vlSuppressed',  'VL_SUPPRESSED',  'NUMERIC_BELOW', 'Target not detected,Not detected,Undetectable', 1000, 2),
    ('HIV',     '20447-9', 'label.mgmt.surveillance.metric.vlSuppressed',  'VL_SUPPRESSED',  'NUMERIC_BELOW', 'Target not detected,Not detected,Undetectable', 1000, 2),
    ('MALARIA', '51587-4', 'label.mgmt.surveillance.metric.rdtPositive',   'RDT_POSITIVE',   'ALL_ABNORMAL',  NULL,          NULL,          1),
    ('MALARIA', '32700-7', 'label.mgmt.surveillance.metric.smearPositive', 'SMEAR_POSITIVE', 'ALL_ABNORMAL',  NULL,          NULL,          2)
) AS v(program_code, loinc, metric_label, metric_key, match_mode, positive_result_codes, numeric_threshold,
       display_order)
JOIN clinlims.surveillance_program p ON p.code = v.program_code
JOIN clinlims.test t ON t.loinc = v.loinc AND t.is_active = 'Y'
LEFT JOIN clinlims.test_section ts ON ts.id = t.test_section_id
WHERE COALESCE(ts.domain, '') <> 'VECTOR'
  AND NOT EXISTS (
      SELECT 1 FROM clinlims.test_surveillance_mapping m
      WHERE m.program_id = p.id AND m.test_id = t.id AND m.metric_key = v.metric_key);
