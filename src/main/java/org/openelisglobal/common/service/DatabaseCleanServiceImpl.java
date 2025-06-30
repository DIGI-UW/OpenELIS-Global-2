package org.openelisglobal.common.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseCleanServiceImpl implements DatabaseCleanService {

    private static final String CLEAN_SQL = "truncate sample_projects, " + "sample_human, " + "result_inventory, "
            + "result_signature, " + "result, " + "analysis, " + "analyzer_results, " + "sample_item, "
            + "observation_history, " + "sample, " + "provider, " + "patient_identity, " + "patient_contact, "
            + "patient_patient_type, " + "note, " + "sample_requester, " + "sample_qaevent, " + "referral, "
            + "patient, " + "person, " + "person_address, " + "report_external_export, " + "report_external_import, "
            + "document_track, " + "qa_observation," + "electronic_order," + "history CASCADE; "
            + "ALTER SEQUENCE note_seq restart 1; " + "ALTER SEQUENCE sample_human_seq restart 1; "
            + "ALTER SEQUENCE result_inventory_seq restart 1; " + "ALTER SEQUENCE result_signature_seq restart 1; "
            + "ALTER SEQUENCE result_seq restart 1; " + "ALTER SEQUENCE analysis_seq restart 1; "
            + "ALTER SEQUENCE sample_item_seq restart 1; " + "ALTER SEQUENCE sample_seq restart 1; "
            + "ALTER SEQUENCE provider_seq restart 1; " + "ALTER SEQUENCE patient_identity_seq restart 1; "
            + "ALTER SEQUENCE patient_patient_type_seq restart 1; " + "ALTER SEQUENCE patient_seq restart 1; "
            + "ALTER SEQUENCE person_seq restart 1; " + "ALTER SEQUENCE report_external_import_seq restart 1; "
            + "ALTER SEQUENCE report_queue_seq restart 1; " + "ALTER SEQUENCE sample_qaevent_seq restart 1; "
            + "ALTER SEQUENCE sample_proj_seq restart 1; " + "ALTER SEQUENCE qa_observation_seq restart 1; "
            + "ALTER SEQUENCE electronic_order_seq restart 1; " + "ALTER SEQUENCE history_seq restart 1; ";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void cleanDatabase() {
        entityManager.unwrap(Session.class).doWork(new Work() {

            @Override
            public void execute(Connection connection) throws SQLException {
                connection.prepareStatement(CLEAN_SQL).execute();
            }
        });
    }
}
