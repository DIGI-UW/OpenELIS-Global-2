package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class MicroCaseRoutingRuleTest {
    @Test
    public void existingSampleMembershipWinsOverAnOlderMatchingType() {
        var oldest = new MicroCaseRoutingRule.Candidate("oldest", "unit", "blood");
        var member = new MicroCaseRoutingRule.Candidate("member", "unit", "blood");
        member.samples.add("bottle");
        assertSame(member, MicroCaseRoutingRule.choose(List.of(oldest, member), "unit", "blood", null, "bottle"));
    }

    @Test
    public void twoRelatedCandidatesChooseTheFirstOpenedCase() {
        var first = new MicroCaseRoutingRule.Candidate("first", "unit", "blood");
        var second = new MicroCaseRoutingRule.Candidate("second", "unit", "blood");
        first.samples.add("bottle");
        second.samples.add("bottle");
        assertSame(first, MicroCaseRoutingRule.choose(List.of(first, second), "unit", "blood", null, "bottle"));
    }

    @Test
    public void setCulturesJoinAcrossTypesButNeverAcrossLabUnits() {
        var culture = new MicroCaseRoutingRule.Candidate("case", "unit", "blood");
        culture.setsTests.add("culture");
        assertSame(culture, MicroCaseRoutingRule.choose(List.of(culture), "unit", "otherType", "culture", "newBottle"));
        assertNull(MicroCaseRoutingRule.choose(List.of(culture), "anotherUnit", "blood", "culture", "newBottle"));
    }
}
