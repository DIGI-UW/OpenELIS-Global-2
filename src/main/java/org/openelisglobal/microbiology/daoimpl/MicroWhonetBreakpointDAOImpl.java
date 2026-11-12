package org.openelisglobal.microbiology.daoimpl;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroWhonetBreakpointDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetBreakpoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroWhonetBreakpointDAOImpl extends BaseDAOImpl<MicroWhonetBreakpoint, String>
        implements MicroWhonetBreakpointDAO {
    private static final int BATCH_SIZE = 1000;
    private static final String INSERT = "insert into clinlims.micro_whonet_breakpoint (id, guidelines, year,"
            + " test_method, potency, organism_code, organism_code_type, breakpoint_type, host, site_of_infection,"
            + " reference_table, reference_sequence, whonet_abx_code, whonet_test, r, i, sdd, s, ecv_ecoff,"
            + " ecv_ecoff_tentative, date_entered, date_modified, comments) values (" + "?, ".repeat(22) + "?)";

    public MicroWhonetBreakpointDAOImpl() {
        super(MicroWhonetBreakpoint.class);
    }

    @Override
    public void deleteAllRows() {
        entityManager.createQuery("delete from MicroWhonetBreakpoint").executeUpdate();
    }

    @Override
    public void insertAll(List<MicroWhonetBreakpoint> breakpoints) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
                for (int row = 0; row < breakpoints.size(); row++) {
                    MicroWhonetBreakpoint b = breakpoints.get(row);
                    String[] values = { b.getId(), b.getGuidelines(), b.getYear(), b.getTestMethod(), b.getPotency(),
                            b.getOrganismCode(), b.getOrganismCodeType(), b.getBreakpointType(), b.getHost(),
                            b.getSiteOfInfection(), b.getReferenceTable(), b.getReferenceSequence(),
                            b.getWhonetAbxCode(), b.getWhonetTest(), b.getR(), b.getI(), b.getSdd(), b.getS(),
                            b.getEcvEcoff(), b.getEcvEcoffTentative(), b.getDateEntered(), b.getDateModified(),
                            b.getComments() };
                    for (int column = 0; column < values.length; column++) {
                        insert.setString(column + 1, values[column]);
                    }
                    insert.addBatch();
                    if ((row + 1) % BATCH_SIZE == 0) {
                        insert.executeBatch();
                    }
                }
                insert.executeBatch();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public String latestYear(Collection<String> guidelines) {
        return entityManager
                .createQuery("select max(b.year) from MicroWhonetBreakpoint b where b.guidelines in :guidelines",
                        String.class)
                .setParameter("guidelines", guidelines).getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroWhonetBreakpoint> findHumanFromYear(Collection<String> guidelines, String fromYear) {
        return entityManager
                .createQuery(
                        "from MicroWhonetBreakpoint b where b.guidelines in :guidelines"
                                + " and b.year >= :fromYear and b.breakpointType = 'Human'",
                        MicroWhonetBreakpoint.class)
                .setParameter("guidelines", guidelines).setParameter("fromYear", fromYear).getResultList();
    }
}
