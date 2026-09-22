-- Reusable public synthetic Non-Conformance cases; requires reporting-repeated-results.sql.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL search_path TO clinlims, public;
DO $fixture$
DECLARE
    ordered_sample sample%ROWTYPE;
    specimen sample_item%ROWTYPE;
    ordered_analysis analysis%ROWTYPE;
    subject_id numeric;
    reason_id numeric;
    event_id integer;
    occurrence integer;
    expected_event date;
    expected_recorded date;
BEGIN
    SELECT patient_id INTO STRICT subject_id FROM sample_human
      WHERE samp_id=(SELECT id FROM sample WHERE fhir_uuid='47900000-0000-4000-8000-000000000002');
    SELECT id INTO reason_id FROM qa_event WHERE name='RPT-MVP-HAEMOLYSIS';
    IF reason_id IS NULL THEN
        reason_id := nextval('qa_event_seq');
        INSERT INTO qa_event(id,name,description,is_holdable,is_billable,lastupdated)
          VALUES(reason_id,'RPT-MVP-HAEMOLYSIS','Synthetic reporting rejection','N','N',now());
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sample WHERE accession_number='REPORTING-MVP-NCE') THEN
        SELECT * INTO STRICT ordered_sample FROM sample WHERE fhir_uuid='47900000-0000-4000-8000-000000000002';
        ordered_sample.id := nextval('sample_seq');
        ordered_sample.fhir_uuid := '47920000-0000-4000-8000-000000000001';
        ordered_sample.accession_number := 'REPORTING-MVP-NCE';
        ordered_sample.collection_date := timestamp '2026-05-10 07:00:00';
        ordered_sample.entered_date := timestamp '2026-05-10 08:00:00';
        ordered_sample.received_date := timestamp '2026-05-10 09:00:00';
        INSERT INTO sample SELECT ordered_sample.*;
        INSERT INTO sample_human(id,samp_id,patient_id,lastupdated)
          VALUES(nextval('sample_human_seq'),ordered_sample.id,subject_id,now());
        SELECT * INTO STRICT specimen FROM sample_item WHERE fhir_uuid='47900000-0000-4000-8000-000000000003';
        specimen.id := nextval('sample_item_seq');
        specimen.fhir_uuid := '47920000-0000-4000-8000-000000000002';
        specimen.samp_id := ordered_sample.id;
        specimen.collection_date := timestamp '2026-05-10 07:00:00';
        specimen.received_date := timestamptz '2026-05-10 09:00:00+00';
        specimen.rejected := true;
        specimen.reject_reason_id := reason_id;
        INSERT INTO sample_item SELECT specimen.*;
        SELECT * INTO STRICT ordered_analysis FROM analysis WHERE fhir_uuid='47900000-0000-4000-8000-000000000004';
        ordered_analysis.id := nextval('analysis_seq');
        ordered_analysis.fhir_uuid := '47920000-0000-4000-8000-000000000003';
        ordered_analysis.sampitem_id := specimen.id;
        INSERT INTO analysis SELECT ordered_analysis.*;
    END IF;
    SELECT * INTO STRICT ordered_sample FROM sample
      WHERE accession_number='REPORTING-MVP-NCE' AND fhir_uuid='47920000-0000-4000-8000-000000000001';
    SELECT * INTO STRICT specimen FROM sample_item
      WHERE fhir_uuid='47920000-0000-4000-8000-000000000002' AND samp_id=ordered_sample.id AND reject_reason_id=reason_id;
    SELECT * INTO STRICT ordered_analysis FROM analysis
      WHERE fhir_uuid='47920000-0000-4000-8000-000000000003' AND sampitem_id=specimen.id;
    FOR occurrence IN 1..5 LOOP
        expected_event := CASE occurrence WHEN 1 THEN date '2026-05-10' WHEN 4 THEN date '2026-05-09' ELSE NULL END;
        expected_recorded := CASE occurrence WHEN 1 THEN date '2026-05-12' WHEN 5 THEN NULL ELSE date '2026-05-10' END;
        SELECT id INTO event_id FROM nc_event WHERE nce_number='RPT-MVP-NCE-' || occurrence;
        IF event_id IS NULL THEN
            event_id := nextval('nc_event_id_seq');
            INSERT INTO nc_event(id,nce_number,name,lab_order_number,reporting_unit_id,name_of_reporter,date_of_event,report_date,status)
              VALUES(event_id,'RPT-MVP-NCE-' || occurrence,'Synthetic Reporting UAT',ordered_sample.accession_number,
                ordered_analysis.test_sect_id,'Synthetic reporter',expected_event,expected_recorded,'OPEN');
            INSERT INTO nce_specimen(id,nce_id,sample_item_id,analysis_id)
              VALUES(nextval('nce_specimen_id_seq'),event_id,specimen.id,ordered_analysis.id);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM nc_event e JOIN nce_specimen link ON link.nce_id=e.id
            WHERE e.id=event_id AND e.name='Synthetic Reporting UAT' AND link.sample_item_id=specimen.id
              AND e.date_of_event IS NOT DISTINCT FROM expected_event AND e.report_date IS NOT DISTINCT FROM expected_recorded) THEN
            RAISE EXCEPTION 'Non-Conformance fixture identity/date mismatch for occurrence %',occurrence;
        END IF;
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM sample_qaevent WHERE sampleitem_id=specimen.id AND qa_event_id=reason_id) THEN
        INSERT INTO sample_qaevent(id,sample_id,sampleitem_id,qa_event_id,entered_date,lastupdated)
          VALUES(nextval('sample_qaevent_seq'),ordered_sample.id,specimen.id,reason_id,timestamp '2026-05-10 12:00:00',now());
    END IF;
    IF (SELECT count(*) FROM sample_qaevent WHERE sampleitem_id=specimen.id AND qa_event_id=reason_id
        AND entered_date=timestamp '2026-05-10 12:00:00') <> 1 THEN
        RAISE EXCEPTION 'Synthetic legacy rejection occurrence does not match';
    END IF;
END
$fixture$;
COMMIT;
