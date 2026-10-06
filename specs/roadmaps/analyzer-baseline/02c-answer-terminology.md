# Step 2c: Answers carry standard codes in any system

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: an answer (a dictionary entry used as a result option) keeps LOINC,
SNOMED, CIEL and OCL codes the way a test already does, every path that reads
or writes an answer's code uses them, and analyzer defaults match a profile
value to an answer on any code system they share.

### Facts

- Why answers lag tests: issue #2029 (May 2025) asked for a LOINC code on
  result select options so coded results carry a LOINC coding in FHIR; #2044
  added one column, `dictionary.loinc_code`
  (`liquibase/3.1.x.x/dictionary-loinc.xml`, `Dictionary.java:84`), copying
  the single `TEST.LOINC` column tests had then. OGC-949 (#3709, June 2026)
  gave tests a multi-system table and left answers out of scope (Casey's
  catalog FRS v2.5 §2.7 defines terminology for the test only). No decision
  ever excluded answers.
- The test pattern to mirror:
  - Table `test_terminology_mapping(id, test_id, source, code, relationship,
display_name, is_active, lastupdated, last_updated, component_id,
sample_type_id)` (changesets 043, 053, 057, 060 in `3.5.x.x`); 043
    deprecated `TEST.LOINC` in place and backfilled one LOINC row per test.
  - Package `org.openelisglobal.testterminology`: valueholder, DAO,
    `TestTerminologyMappingService` (incl. `syncLegacyLoinc`, which keeps the
    legacy column and the table in step).
  - Config import: `TerminologyConfigurationHandler`, domain `terminology`,
    columns `testName,sampleType,componentCode,source,code,relationship,displayName`,
    sources LOINC, SNOMED, CIEL, OCL; relationships SAME_AS (default),
    BROADER_THAN, NARROWER_THAN; rows merge by (component, specimen, source,
    code).
  - REST `GET/PUT /rest/test-catalog/tests/{testId}/terminology`
    (`TestCatalogEditorRestController.java:1926-1990`); editor
    `frontend/src/components/admin/testCatalog/sections/TerminologySection.jsx`.
  - FHIR: `TerminologyTransformServiceImpl` emits one coding per system, the
    SAME_AS mapping first; system URIs in `terminologySystemUrl` (216-234).
- Every reader and writer of an answer's code today:
  - `DictionaryConfigurationHandler.java:339-341` reads the `loincCode` CSV
    column (header
    `category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode,localization:*`).
  - `frontend/src/components/admin/menu/DictionaryManagement.jsx` (65-332)
    and `ResultSelectListAdd.jsx` / `ResultSelectListServiceImpl.java:108`
    edit the one LOINC field.
  - `OclToOpenElisMapper.java:600-640` stores an answer's CIEL concept ID in
    `localAbbreviation` and its LOINC mapping in `loincCode`.
  - `ObservationTransformServiceImpl.java:290-310` writes a coded result's
    `valueCodeableConcept` with the LOINC code and OE2's own
    `dictionary_entry` coding only.
  - Analyzer defaults: `AnalyzerMappingCatalogServiceImpl.answerCode`
    (128-130) exposes `Dictionary.loincCode` as the answer's one code;
    `AnalyzerMappingDefaults.resolveAnswer` (61-76) compares it with the one
    profile coding read by `BridgeAnalyzerProfile.valueCodes` (194-212),
    ignoring the system.
  - The catalog editor's option table
    (`frontend/src/components/admin/testCatalog/sections/SampleResultsSection.jsx`)
    shows no codes.
- An answer row is shared by every test that offers it, and standard answer
  codes are global (LA6576-8 is Positive everywhere), so mappings key on the
  answer itself, with no test, component or specimen scope.
- Profiles declare `value_codes{value -> [{system, code}]}` (step 6, rule 1).

### Build

```
- [x] T2c.1 Red: integration test, a changeset backfills one LOINC SAME_AS row per dictionary entry with loinc_code; saving an answer's LOINC mapping keeps loinc_code in step
- [x] T2c.2 Red: integration test, config import of an answer terminology CSV adds LOINC, SNOMED and CIEL rows to one answer and merges on re-import
- [ ] T2c.3 Red: unit test, a coded result's FHIR valueCodeableConcept carries every mapped system, SAME_AS first, plus the dictionary_entry coding
- [x] T2c.4 Red: unit tests, resolveAnswer binds when the profile value and the answer share any (system, code); two answers sharing it is AMBIGUOUS; a SNOMED-only value binds to an answer carrying that SNOMED code
- [ ] T2c.5 Red: integration test, OCL import writes an answer's CIEL concept and its LOINC and SNOMED mappings as terminology rows
- [x] T2c.6 Changeset (129 in 3.5.x.x): dictionary_terminology_mapping(id, dictionary_id, source, code, relationship, display_name, is_active, lastupdated, last_updated), unique (dictionary_id, source, code), backfill from dictionary.loinc_code
- [x] T2c.7 Package org.openelisglobal.dictionaryterminology mirroring panelterminology (the unscoped mirror with a denormalized legacy LOINC column, `panel.loinc`), with syncLegacyLoinc
- [x] T2c.8 Config import domain `answer-terminology`: category,dictEntry,source,code,relationship,displayName; the dictionary CSV's loincCode column keeps working through syncLegacyLoinc (only a row that carries a loincCode syncs, so a dictionary re-import never clears codes the answer-terminology import added); load order 310, after dictionaries
- [ ] T2c.9 FHIR output, OCL import and analyzer defaults per Facts; ResultOption carries the answer's codings; BridgeAnalyzerProfile reads a list of codings per value (analyzer defaults, ResultOption and the profile done; the source-to-system URIs moved to `TerminologySystems`, shared with FHIR output)
- [ ] T2c.10 REST GET/PUT /rest/test-catalog/answers/{dictionaryId}/terminology; Dictionary Management edits mappings with the TerminologySection pattern; the catalog option table shows each answer's codes read-only
- [ ] T2c.11 Green; format cold; commit; stack PR on step 2b (backend T2c.1-T2c.9 and the UI T2c.10 as two PRs if one exceeds a reviewable size)
```

### Verify

```bash
mvn -Dtest='DictionaryTerminology*Test,AnswerTerminologyConfigurationHandler*Test,ObservationTransformService*Test,AnalyzerMappingDefaults*Test,OclToOpenElisMapper*Test' test
cd frontend && npx vitest run src/components/admin/menu src/components/admin/testCatalog && cd ..
gh pr checks <PR>
```

### Done when

1. Every answer with a LOINC code has a matching terminology row, and the
   legacy column stays in step. (T2c.1)
2. An answer holds codes in LOINC, SNOMED, CIEL and OCL, from config import,
   OCL import or the editor. (T2c.2, T2c.5, T2c.10)
3. A coded result in FHIR carries every system the answer has. (T2c.3)
4. Analyzer defaults bind on any shared (system, code), so a value with only
   a SNOMED or CIEL code binds out of the box. (T2c.4)
5. All three CI checkpoints pass. (`gh pr checks`)

### Background (optional)

- Issue #2029 and PR #2044 (answer LOINC column); OGC-949 #3709 (test
  terminology)
- Casey's catalog FRS: `openelis-work/designs/admin-config/test-catalog/test-catalog-requirements-v2.5.md` §2.7
