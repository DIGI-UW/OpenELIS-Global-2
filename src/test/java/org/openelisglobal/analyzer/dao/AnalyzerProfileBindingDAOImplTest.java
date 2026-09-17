package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL query discrimination; mapping decisions are exercised in
 * AnalyzerMappingLifecycleIntegrationTest.
 */
@Transactional
public class AnalyzerProfileBindingDAOImplTest extends BaseWebContextSensitiveTest {
    @Autowired
    private AnalyzerProfileBindingDAO dao;
    @PersistenceContext
    private EntityManager entityManager;

    private String profileId;
    private String otherProfileId;
    private AnalyzerProfileBinding firstRevision;
    private AnalyzerProfileBinding secondRevision;
    private AnalyzerProfileBinding otherProfile;
    private String alpha;
    private String beta;
    private String zeta;
    private String unrelated;

    @Before
    public void createQueryFixtures() {
        profileId = "query." + UUID.randomUUID();
        otherProfileId = "query.other." + UUID.randomUUID();
        firstRevision = profile(profileId, 1);
        secondRevision = profile(profileId, 2);
        otherProfile = profile(otherProfileId, 1);
        AnalyzerSiteBinding firstBinding = binding(firstRevision);
        alpha = analyzer("Alpha", revision(firstBinding, 1));
        zeta = analyzer("zeta", revision(firstBinding, 2));
        beta = analyzer("beta", revision(binding(secondRevision), 1));
        unrelated = analyzer("Unrelated", revision(binding(otherProfile), 1));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void profileLookupRequiresBothExactProfileAndRevision() {
        assertEquals(firstRevision.getId(),
                dao.findByProfileIdAndRevision(" " + profileId + " ", 1).orElseThrow().getId());
        assertEquals(secondRevision.getId(), dao.findByProfileIdAndRevision(profileId, 2).orElseThrow().getId());
        assertEquals(otherProfile.getId(), dao.findByProfileIdAndRevision(otherProfileId, 1).orElseThrow().getId());
        assertTrue(dao.findByProfileIdAndRevision(profileId, 3).isEmpty());
        assertTrue(dao.findByProfileIdAndRevision("missing", 1).isEmpty());
    }

    @Test
    public void countsOnlyAnalyzersWhoseSelectedMappingBelongsToTheRequestedProfileRevision() {
        assertEquals(2L, dao.countAnalyzersByBindingId(firstRevision.getId()));
        assertEquals(1L, dao.countAnalyzersByBindingId(secondRevision.getId()));
        assertEquals(1L, dao.countAnalyzersByBindingId(otherProfile.getId()));
        assertEquals(0L, dao.countAnalyzersByBindingId("0"));
    }

    @Test
    public void profileQueryIncludesBothPinnedRevisionsAndExcludesOtherProfiles() {
        assertEquals(List.of(alpha, beta, zeta), ids(dao.findAnalyzersByProfileId(" " + profileId + " ")));
        assertEquals(List.of(unrelated), ids(dao.findAnalyzersByProfileId(otherProfileId)));
        assertTrue(dao.findAnalyzersByProfileId("missing").isEmpty());
        assertTrue(dao.findAnalyzersByProfileId(null).isEmpty());
    }

    private AnalyzerProfileBinding profile(String id, int number) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setProfileId(id);
        profile.setProfileRevision(number);
        profile.setProfileFingerprint("sha256:" + "a".repeat(64));
        entityManager.persist(profile);
        return profile;
    }

    private AnalyzerSiteBinding binding(AnalyzerProfileBinding profile) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profile);
        binding.setCreatedBy(TEST_SYS_USER_ID);
        entityManager.persist(binding);
        return binding;
    }

    private AnalyzerSiteBindingRevision revision(AnalyzerSiteBinding binding, int number) {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(number);
        revision.setBindingFingerprint("sha256:" + Integer.toHexString(number).repeat(64));
        revision.setCreatedBy(TEST_SYS_USER_ID);
        entityManager.persist(revision);
        return revision;
    }

    private String analyzer(String name, AnalyzerSiteBindingRevision revision) {
        Analyzer analyzer = new Analyzer();
        analyzer.setName(name);
        analyzer.ensureFhirUuid();
        analyzer.setSiteBindingRevision(revision);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setActive(false);
        entityManager.persist(analyzer);
        return analyzer.getId();
    }

    private List<String> ids(List<Analyzer> analyzers) {
        return analyzers.stream().map(Analyzer::getId).toList();
    }
}
