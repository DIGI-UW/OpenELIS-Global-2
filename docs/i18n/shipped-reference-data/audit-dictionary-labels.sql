-- Audit of dictionary labels on an existing instance (OGC-1507 slice E).
--
-- Read only: it reports and suggests, it changes nothing. It needs the list of
-- keys OpenELIS ships, written next to it by the build check, then psql run
-- from this folder against the instance's database:
--   python3 scripts/ci/check-shipped-display-keys.py --list > shipped-display-keys.txt
--   psql -U clinlims -d clinlims -f audit-dictionary-labels.sql
--
-- A dictionary entry OpenELIS ships carries a display key under "dictionary."
-- that is in en.json, and is translated in Transifex. Every other entry is the
-- site's own: historical data from earlier releases or a country configuration,
-- translated, if at all, in the app's localization editor. This audit looks at
-- those other entries, which are the ones that need a cleanup decision.

CREATE TEMP TABLE shipped_key (key varchar PRIMARY KEY);
\copy shipped_key FROM 'shipped-display-keys.txt'

\echo '1. Entries per category: shipped, used, and the signs of entries that need a decision'
CREATE TEMP VIEW entry AS
SELECT d.id, d.dict_entry, d.display_key, c.name AS category, fr.value AS french,
       d.display_key IN (SELECT key FROM shipped_key) AS shipped,
       EXISTS (SELECT 1 FROM clinlims.test_result tr
               WHERE tr.tst_rslt_type IN ('D', 'M', 'C') AND tr.value = CAST(d.id AS varchar))
         OR EXISTS (SELECT 1 FROM clinlims.result r
                    WHERE r.result_type IN ('D', 'M', 'C') AND r.value = CAST(d.id AS varchar)) AS used,
       d.dict_entry ~ '[À-ÿ]' AS accented,
       d.dict_entry ~ '^[<>≤≥=]?\s*[0-9.,/:+-]+\s*[a-zA-Z/%µ]*$' AS number_like,
       length(d.dict_entry) > 60 AS long_text
FROM clinlims.dictionary d
JOIN clinlims.dictionary_category c ON c.id = d.dictionary_category_id
LEFT JOIN clinlims.localization_value fr ON fr.localization_id = d.name_localization_id AND fr.locale = 'fr'
WHERE d.is_active = 'Y';

-- A list some test answers from: its unused entries are candidates to retire.
-- Other lists (demographics, rejection reasons, checklists) are read by forms.
CREATE TEMP VIEW result_list AS
SELECT DISTINCT category FROM entry WHERE used;

SELECT e.category,
       COUNT(*) AS entries,
       COUNT(*) FILTER (WHERE e.shipped) AS shipped,
       bool_or(r.category IS NOT NULL) AS result_list,
       COUNT(*) FILTER (WHERE e.used) AS used_by_tests_or_results,
       COUNT(*) FILTER (WHERE e.accented) AS accented_english_slot,
       COUNT(*) FILTER (WHERE e.number_like) AS number_like,
       COUNT(*) FILTER (WHERE e.long_text) AS long_free_text,
       COUNT(*) FILTER (WHERE e.french IS NOT NULL AND e.french <> '') AS has_french
FROM entry e LEFT JOIN result_list r ON r.category = e.category
GROUP BY e.category
ORDER BY entries DESC;

\echo '2. Site entries with a suggested decision'
SELECT e.category, e.id, e.dict_entry, e.display_key, e.french,
       CASE
         WHEN r.category IS NOT NULL AND NOT e.used
           THEN 'retire: in a result list, but no test offers it and no result holds it'
         WHEN e.accented AND (e.french IS NULL OR e.french = '')
           THEN 'move to the French slot: the English slot holds French text'
         WHEN e.number_like THEN 'keep untranslated: a number, titer or MIC value'
         WHEN e.long_text THEN 'review: free text kept as an answer option'
         WHEN e.french IS NULL OR e.french = '' THEN 'translate in the localization editor'
         ELSE 'keep'
       END AS suggestion
FROM entry e LEFT JOIN result_list r ON r.category = e.category
WHERE NOT e.shipped
ORDER BY suggestion, e.category, e.dict_entry;
