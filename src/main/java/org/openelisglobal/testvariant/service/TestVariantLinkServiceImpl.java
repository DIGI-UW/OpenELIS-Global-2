package org.openelisglobal.testvariant.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testvariant.dao.TestVariantLinkDAO;
import org.openelisglobal.testvariant.valueholder.TestVariantLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestVariantLinkServiceImpl extends AuditableBaseObjectServiceImpl<TestVariantLink, String>
        implements TestVariantLinkService {

    @Autowired
    protected TestVariantLinkDAO baseObjectDAO;

    TestVariantLinkServiceImpl() {
        super(TestVariantLink.class);
    }

    @Override
    protected TestVariantLinkDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public TestVariantLink getByTestId(String testId) {
        List<TestVariantLink> matches = getAllMatching("testId", testId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestVariantLink> getByGroupId(String groupId) {
        return getAllMatching("groupId", groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestVariantLink> getAllLinks() {
        return getAll();
    }

    @Override
    @Transactional
    public String linkTests(List<String> testIds, String sysUserId) {
        Set<String> distinct = new LinkedHashSet<>();
        if (testIds != null) {
            for (String id : testIds) {
                if (id != null && !id.isBlank()) {
                    distinct.add(id);
                }
            }
        }
        if (distinct.size() < 2) {
            return null;
        }
        // If any member is already grouped, everyone joins that group; else mint one.
        String groupId = null;
        for (String id : distinct) {
            TestVariantLink existing = getByTestId(id);
            if (existing != null) {
                groupId = existing.getGroupId();
                break;
            }
        }
        if (groupId == null) {
            groupId = UUID.randomUUID().toString();
        }
        for (String id : distinct) {
            assignToGroup(id, groupId, sysUserId);
        }
        return groupId;
    }

    @Override
    @Transactional
    public String addToGroupOf(String sourceTestId, String newTestId, String sysUserId) {
        TestVariantLink sourceLink = getByTestId(sourceTestId);
        String groupId;
        if (sourceLink != null) {
            groupId = sourceLink.getGroupId();
        } else {
            // Source isn't grouped yet — mint a group and enroll the source too, so
            // the two become a group of variants (FR-52).
            groupId = UUID.randomUUID().toString();
            assignToGroup(sourceTestId, groupId, sysUserId);
        }
        assignToGroup(newTestId, groupId, sysUserId);
        return groupId;
    }

    @Override
    @Transactional
    public void unlink(String testId, String sysUserId) {
        TestVariantLink existing = getByTestId(testId);
        if (existing != null) {
            delete(existing.getId(), sysUserId);
        }
    }

    // Upsert a test's membership to the given group (insert or move).
    private void assignToGroup(String testId, String groupId, String sysUserId) {
        TestVariantLink link = getByTestId(testId);
        if (link != null) {
            if (!groupId.equals(link.getGroupId())) {
                link.setGroupId(groupId);
                link.setSysUserId(sysUserId);
                update(link);
            }
            return;
        }
        TestVariantLink fresh = new TestVariantLink();
        fresh.setTestId(testId);
        fresh.setGroupId(groupId);
        fresh.setSysUserId(sysUserId);
        insert(fresh);
    }

    /** Members of a group other than the given test, as a plain list of ids. */
    public List<String> siblingTestIds(String groupId, String exceptTestId) {
        List<String> ids = new ArrayList<>();
        for (TestVariantLink link : getByGroupId(groupId)) {
            if (!link.getTestId().equals(exceptTestId)) {
                ids.add(link.getTestId());
            }
        }
        return ids;
    }
}
